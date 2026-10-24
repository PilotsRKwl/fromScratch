package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Exact-byte M6 preservation, CAS, corruption, interrupted writes and acknowledged replay. */
public class BuybackPersistenceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root,d,current;private BankRunCodec codec;private BankRepository repository;
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());d=root.join("characters",SnapshotFixtures.CHARACTER_A);current=d.joinSegment("current.json");codec=new BankRunCodec(SnapshotFixtures.gson());repository=new BankRepository(codec,SnapshotFixtures.codec());}
	private BankRun active()throws Exception{return active(true);}
 private BankRun active(boolean prepare)throws Exception
	{
		BankRun funded=BuybackModelTest.funded(2000000,ItemID.BH_RUNE_POUCH,ItemID.TZHAAR_CAPE_FIRE);
		BankRun initial=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,funded.getInitialBank(),true,UUID.randomUUID(),SnapshotFixtures.TIME);repository.save(root,null,initial);
		BankRun run=repository.save(root,initial,BankEngine.observe(initial,new com.fromscratch.model.ContainerSnapshot(funded.getPhysical(),3),UUID.randomUUID(),SnapshotFixtures.TIME));return prepare?repository.save(root,run,BuybackModelTest.emptyPouch(run)):run;
	}
	private BankRun load()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	private Map<String,String> m6()throws Exception
	{
		BankRun old=active(false);old=repository.save(root,old,BankEngine.expense(old,15,"Verified expense",UUID.randomUUID(),SnapshotFixtures.TIME));
		old=repository.save(root,old,BankEngine.correct(old,ItemID.TZHAAR_CAPE_FIRE,-1,UUID.randomUUID(),SnapshotFixtures.TIME));
		UUID geSession=UUID.randomUUID();
		old=repository.save(root,old,BankEngine.exchange(old,new com.fromscratch.ge.GeObservation(UUID.randomUUID(),geSession,SnapshotFixtures.TIME,0,ItemID.WOODEN_SHIELD,"Wooden shield",net.runelite.api.GrandExchangeOfferState.SELLING,10,0,100,0,false)));
		old=repository.save(root,old,BankEngine.exchange(old,new com.fromscratch.ge.GeObservation(UUID.randomUUID(),geSession,SnapshotFixtures.TIME,0,ItemID.WOODEN_SHIELD,"Wooden shield",net.runelite.api.GrandExchangeOfferState.SELLING,10,3,100,300,false)));
		List<Filepath> files;try(Stream<Filepath> walk=d.walk(1)){files=walk.filter(f->f.getFileName().endsWith(".json")).collect(Collectors.toList());}
		for(Filepath file:files){if(!file.getFileName().startsWith("bank-ledger-")){JsonObject p=codec.payload(BankStorageFiles.readText(file),BankRunCodec.FORMAT);p.remove("potions");p.remove("buybacks");file.write(codec.envelope(p,7));}}
		for(Filepath file:files){if(file.getFileName().startsWith("bank-ledger-")){JsonObject p=codec.payload(BankStorageFiles.readText(file),1);String id=file.getFileName().substring(12,file.getFileName().length()-5);p.addProperty("result",SaveIntegrity.sha256(BankStorageFiles.readText(d.joinSegment("bank-checkpoint-"+id+".json"))));file.write(codec.envelope(p,1));}}
		Map<String,String> originals=new TreeMap<>();for(Filepath file:files){originals.put(file.getFileName(),BankStorageFiles.readText(file));}return originals;
	}
	@Test public void m6MigrationArchivesExactBytesWithoutChangingProgress()throws Exception
	{
		Map<String,String> originals=m6();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A),next=load();assertEquals(old.getRunId(),next.getRunId());assertEquals(old.getStartedAt(),next.getStartedAt());assertEquals(old.getInitialBank(),next.getInitialBank());assertEquals(old.getPhysical(),next.getPhysical());assertEquals(old.getBalances(),next.getBalances());assertEquals(old.getFinancial(),next.getFinancial());assertEquals(old.getExchange(),next.getExchange());assertEquals(BankAudit.Kind.BUYBACK_BASELINE,next.getRecent().get(0).getKind());
		for(Map.Entry<String,String> file:originals.entrySet()){assertEquals(file.getValue(),BankStorageFiles.readText(d.join(next.getBuybacks().getArchive(),file.getKey())));}assertEquals(next,load());
	}
	@Test public void interruptedMigrationLeavesOldCurrentRecoverable()throws Exception{Map<String,String> originals=m6();BankRepository bad=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Interrupted");});try{bad.load(root,SnapshotFixtures.CHARACTER_A);fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));assertEquals(0,load().getBuybacks().getCommittedGp());}
	@Test public void corruptM6JournalIsNotReplacedByEmptyState()throws Exception{Map<String,String> originals=m6();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A);d.joinSegment("bank-ledger-"+old.getRevision()+".json").write("damaged");try{load();fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));}
	@Test public void failedPurchasePublicationDoesNotSpendOrUnlock()throws Exception{BankRun old=active(),next=BuybackModelTest.buy(old,"rune_pouch_family");BankRepository bad=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Interrupted");});try{bad.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(old,load());assertEquals(0,load().getBuybacks().getCommittedGp());repository.save(root,old,next);assertEquals(1,load().getBuybacks().getPurchases().size());}
	@Test public void lostAcknowledgmentAndRetryNeverChargeTwice()throws Exception{BankRun old=active(),next=BuybackModelTest.buy(old,"rune_pouch_family");BankRepository bad=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{a.moveTo(b,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);throw new IOException("Lost ack");});try{bad.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(next,load());assertEquals(next,repository.save(root,old,next));assertEquals(1000000,load().getBuybacks().getCommittedGp());}
	@Test public void staleBuybackCannotOverwriteLaterBankMovement()throws Exception{BankRun old=active(),stale=BuybackModelTest.buy(old,"fire_cape");BankRun newer=repository.save(root,old,BankEngine.correct(old,ItemID.COINS,1500000,UUID.randomUUID(),SnapshotFixtures.TIME));try{repository.save(root,old,stale);fail();}catch(SaveException expected){}assertEquals(newer,load());}
	@Test public void missingM7PayloadNeverDefaultsToNoSpending()throws Exception{BankRun old=active();repository.save(root,old,BuybackModelTest.buy(old,"fire_cape"));JsonObject payload=codec.payload(BankStorageFiles.readText(current),BankRunCodec.FORMAT);payload.remove("buybacks");String bad=codec.envelope(payload,8);current.write(bad);try{load();fail();}catch(SaveException expected){}assertEquals(bad,BankStorageFiles.readText(current));}
	@Test public void purchaseStateCannotInventAnUnpaidEntitlement(){try{new BuybackState(1,Collections.singletonMap("rune_pouch_family",1),Collections.singletonMap("rune_pouch_family",0L),0,Collections.emptyList(),null);fail();}catch(IllegalArgumentException expected){}}
	@Test public void duplicatePurchaseIdentityCannotPayForAnotherItem(){BankRun old=BuybackModelTest.buy(BuybackModelTest.funded(2000000,ItemID.BH_RUNE_POUCH,ItemID.TZHAAR_CAPE_FIRE),"rune_pouch_family");try{BankEngine.buyback(old,"fire_cape",old.getBuybacks().getPurchases().get(0).getId(),UUID.randomUUID(),SnapshotFixtures.TIME);fail();}catch(IllegalArgumentException expected){}}
	@Test public void damagedMigrationArchiveBlocksLoadingAndRetainsCheckpoint()throws Exception{m6();BankRun next=load();String before=BankStorageFiles.readText(current);d.join(next.getBuybacks().getArchive(),"current.json").write("damaged");try{load();fail();}catch(SaveException expected){}assertEquals(before,BankStorageFiles.readText(current));}
	@Test public void bagPublicationFailureLeavesPaymentEntitlementAndCoinOffsetsTogetherUncommitted()throws Exception
	{Filepath isolated=root.joinSegment("bag-test");BankRun run=repository.save(isolated,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BuybackModelTest.bank(ItemID.LOOTING_BAG),true,UUID.randomUUID(),SnapshotFixtures.TIME));Map<Integer,Long> bank=new TreeMap<>(run.getPhysical());bank.put(ItemID.COINS,100000L);run=repository.save(isolated,run,BankEngine.observe(run,new com.fromscratch.model.ContainerSnapshot(bank,2),UUID.randomUUID(),SnapshotFixtures.TIME));BankRun next=BankEngine.buyBag(run,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,new BagObservation(ItemID.LOOTING_BAG,Collections.singletonMap(ItemID.COINS,25000L),Collections.singletonMap(ItemID.COINS,"Coins"),SnapshotFixtures.TIME));BankRepository bad=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Interrupted publication");});try{bad.save(isolated,run,next);fail();}catch(SaveException expected){}assertEquals(run,repository.load(isolated,run.getCharacter()).get());assertEquals(next,repository.save(isolated,run,next));assertEquals(65000,BuybackEngine.available(repository.load(isolated,run.getCharacter()).get()));}
}
