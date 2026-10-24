package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.ge.*;
import com.fromscratch.wealth.*;
import com.google.gson.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real Filepath migration/force/CAS/recovery. Never reads the player's authoritative save directory. */
public class GePersistenceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root,directory,current;private BankRepository repository;private BankRunCodec codec;
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());directory=root.join("characters",SnapshotFixtures.CHARACTER_A);current=directory.joinSegment("current.json");codec=new BankRunCodec(SnapshotFixtures.gson());repository=new BankRepository(codec,SnapshotFixtures.codec());}
	private BankRun active()throws Exception{return repository.save(root,null,run(4));}
	private BankRun load()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	private GeObservation offer(long q,long gp){return new GeObservation(UUID.randomUUID(),UUID.randomUUID(),Instant.now(),0,SHIELD,"Wooden shield",GrandExchangeOfferState.SELLING,10,q,100,gp,false);}
	private BankRun saveOffer(BankRun old,long q,long gp)throws Exception{return repository.save(root,old,BankEngine.exchange(old,offer(q,gp)));}
	private Map<String,String> m5()throws Exception
	{
		BankRun before=active();BankRun gained=BankEngine.observe(before,bank(6),UUID.randomUUID(),Instant.now(),null,Collections.singletonMap(SHIELD,new PriceQuote(SHIELD,100L,Instant.now(),PriceQuote.Source.RUNELITE_GE_GUIDE,false)));repository.save(root,before,gained);
		List<Filepath> files;try(Stream<Filepath> walk=directory.walk(1)){files=walk.filter(f->f.getFileName().endsWith(".json")).collect(Collectors.toList());}
		for(Filepath file:files){if(!file.getFileName().startsWith("bank-ledger-")){JsonObject p=codec.payload(BankStorageFiles.readText(file),BankRunCodec.FORMAT);p.remove("potions");p.remove("exchange");p.remove("buybacks");file.write(codec.envelope(p,6));}}
		for(Filepath file:files){if(file.getFileName().startsWith("bank-ledger-")){JsonObject p=codec.payload(BankStorageFiles.readText(file),1);String id=file.getFileName().substring("bank-ledger-".length(),file.getFileName().length()-5);p.addProperty("result",SaveIntegrity.sha256(BankStorageFiles.readText(directory.joinSegment("bank-checkpoint-"+id+".json"))));file.write(codec.envelope(p,1));}}
		Map<String,String> originals=new TreeMap<>();for(Filepath file:files){originals.put(file.getFileName(),BankStorageFiles.readText(file));}return originals;
	}
	@Test public void offersFillsFeesAndMatchingBudgetsRoundTrip()throws Exception{BankRun run=saveOffer(saveOffer(active(),0,0),3,300);assertEquals(run,load());assertEquals(6,load().getExchange().getEstimatedTaxGp());assertEquals(3,load().getExchange().getOffers().get(0).getLast().getFilled());assertFalse(load().getExchange().getCredits().isEmpty());}
	@Test public void m5MigrationArchivesEveryOriginalByteAndPreservesFinancialState()throws Exception
	{
		Map<String,String> originals=m5();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A);BankRun upgraded=load();
		assertEquals(old.getRunId(),upgraded.getRunId());assertEquals(old.getStartedAt(),upgraded.getStartedAt());assertEquals(old.getInitialBank(),upgraded.getInitialBank());assertEquals(old.getPhysical(),upgraded.getPhysical());assertEquals(old.getBalances(),upgraded.getBalances());assertEquals(old.getFinancial(),upgraded.getFinancial());assertEquals(BankAudit.Kind.EXCHANGE_BASELINE,upgraded.getRecent().get(0).getKind());
		assertNotNull(upgraded.getExchange().getArchive());for(Map.Entry<String,String> original:originals.entrySet()){assertEquals(original.getValue(),BankStorageFiles.readText(directory.join(upgraded.getExchange().getArchive(),original.getKey())));}assertEquals(upgraded,load());
	}
	@Test public void interruptedMigrationKeepsM5CurrentAndRetryDoesNotInventTrades()throws Exception
	{Map<String,String> originals=m5();BankRepository failing=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Interrupted publication");});try{failing.load(root,SnapshotFixtures.CHARACTER_A);fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));BankRun loaded=load();assertEquals(200,loaded.getFinancial().getGainedGp());assertTrue(loaded.getExchange().getRecent().isEmpty());}
	@Test public void brokenM5HistoryIsPreservedAndNotMigrated()throws Exception
	{Map<String,String> originals=m5();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A);directory.joinSegment("bank-ledger-"+old.getRevision()+".json").write("damaged");try{load();fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));}
	@Test public void missingGePayloadNeverDefaultsToEmpty()throws Exception
	{saveOffer(active(),0,0);JsonObject p=codec.payload(BankStorageFiles.readText(current),BankRunCodec.FORMAT);p.remove("potions");p.remove("exchange");p.remove("buybacks");String bad=codec.envelope(p,BankRunCodec.FORMAT);current.write(bad);try{load();fail();}catch(SaveException expected){}assertEquals(bad,BankStorageFiles.readText(current));}
	@Test public void failedOfferSaveDoesNotPublishAndRetryIsIdempotent()throws Exception
	{BankRun old=saveOffer(active(),0,0),next=BankEngine.exchange(old,offer(3,300));BankRepository failing=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Interrupted publication");});try{failing.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(old,load());repository.save(root,old,next);repository.save(root,old,next);assertEquals(6,load().getExchange().getEstimatedTaxGp());assertEquals(next,load());}
	@Test public void lostAcknowledgmentRestoresCommittedFillExactlyOnce()throws Exception
	{BankRun old=saveOffer(active(),0,0),next=BankEngine.exchange(old,offer(3,300));BankRepository lost=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{a.moveTo(b,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);throw new IOException("Lost acknowledgment");});try{lost.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(next,load());assertEquals(next,repository.save(root,old,next));assertEquals(1,load().getExchange().getSales());}
	@Test public void staleOfferCannotOverwriteNewerBankProgress()throws Exception
	{BankRun old=active(),stale=BankEngine.exchange(old,offer(0,0));BankRun bank=repository.save(root,old,BankEngine.observe(old,bank(5),UUID.randomUUID(),Instant.now()));try{repository.save(root,old,stale);fail();}catch(SaveException expected){}assertEquals(bank,load());}
	@Test public void journalHistoryIsPagedAndDoesNotRewriteOlderSegments()throws Exception
	{
		BankRun run=saveOffer(active(),0,0);String first=BankStorageFiles.readText(directory.joinSegment("bank-ledger-"+run.getRevision()+".json"));UUID firstId=run.getRevision();
		UUID session=UUID.randomUUID();
		// The same generation's full-fill and later terminal evidence span two public pages.
		run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),7,SHIELD,"Wooden shield",GrandExchangeOfferState.SELLING,10,10,100,1000,false)));
		for(int n=1;n<=40;n++)
		{
			if(n>1)
			{
				run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),0,SHIELD,"Wooden shield",GrandExchangeOfferState.EMPTY,0,0,0,0,false)));
				run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),0,SHIELD,"Wooden shield",GrandExchangeOfferState.SELLING,10,0,100,0,false)));
			}
			GeObservation o=new GeObservation(UUID.randomUUID(),session,Instant.now(),0,SHIELD,"Wooden shield",GrandExchangeOfferState.SOLD,10,10,100,1000+n,false);run=repository.save(root,run,BankEngine.exchange(run,o));
		}
		run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),7,SHIELD,"Wooden shield",GrandExchangeOfferState.SOLD,10,10,100,1000,false)));
		BankRun before=load();GeHistory.Page page=repository.exchangeHistory(root,run,null,false);assertTrue("Raw segments must yield before a finalized page is ready",page.isScanning());while(page.isScanning()){page=repository.exchangeHistory(root,run,page.getNext(),false);}assertEquals(25,page.getRows().size());assertNotNull(page.getNext());GeHistory.Page second=repository.exchangeHistory(root,run,page.getNext(),false);while(second.isScanning()){second=repository.exchangeHistory(root,run,second.getNext(),false);}assertEquals(16,second.getRows().size());assertNull(second.getNext());Set<UUID> ids=new HashSet<>();page.getRows().forEach(r->assertTrue(ids.add(r.getId())));second.getRows().forEach(r->assertTrue(ids.add(r.getId())));assertEquals(41,ids.size());assertEquals(before,load());assertEquals(first,BankStorageFiles.readText(directory.joinSegment("bank-ledger-"+firstId+".json")));
	}
}
