package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.wealth.*;
import com.google.gson.*;
import java.util.*;
import java.io.IOException;
import java.util.stream.*;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Exact M4-byte migration, linked financial evidence and interrupted publication/retry. Synthetic Filepath roots only. */
public class FinancialPersistenceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root,directory,current;private BankRepository repository;private BankRunCodec codec;
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());directory=root.join("characters",SnapshotFixtures.CHARACTER_A);current=directory.joinSegment("current.json");codec=new BankRunCodec(SnapshotFixtures.gson());repository=new BankRepository(codec,SnapshotFixtures.codec());}
	private BankRun load()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	private BankRun active()throws Exception{return repository.save(root,null,run(1));}
	private BankRun gain(BankRun old,long n)throws Exception{return repository.save(root,old,BankEngine.observe(old,bank(n),UUID.randomUUID(),SnapshotFixtures.TIME,null,Collections.singletonMap(SHIELD,new PriceQuote(SHIELD,100L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false))));}
	/** Convert all synthetic bank checkpoints/segments to the exact old schema, with matching original-byte hashes. */
	private Map<String,String> m4()throws Exception
	{
		BankRun run=active();gain(run,3);Map<String,String> originals=new TreeMap<>();List<Filepath> files;try(Stream<Filepath> walk=directory.walk(1)){files=walk.filter(f->f.getFileName().endsWith(".json")).collect(Collectors.toList());}
		for(Filepath f:files){if(f.getFileName().startsWith("bank-ledger-")){continue;}JsonObject payload=codec.payload(BankStorageFiles.readText(f),BankRunCodec.FORMAT);payload.remove("potions");payload.remove("financial");payload.remove("exchange");payload.remove("buybacks");for(JsonElement row:payload.getAsJsonArray("recent")){row.getAsJsonObject().remove("valuations");}if(!payload.get("hostSummary").isJsonNull()){payload.getAsJsonObject("hostSummary").remove("valuations");}f.write(codec.envelope(payload,5));}
		for(Filepath f:files){if(!f.getFileName().startsWith("bank-ledger-")){continue;}JsonObject payload=codec.payload(BankStorageFiles.readText(f),1);payload.getAsJsonObject("audit").remove("valuations");String id=f.getFileName().substring("bank-ledger-".length(),f.getFileName().length()-5);payload.addProperty("result",SaveIntegrity.sha256(BankStorageFiles.readText(directory.joinSegment("bank-checkpoint-"+id+".json"))));f.write(codec.envelope(payload,1));}
		for(Filepath f:files){originals.put(f.getFileName(),BankStorageFiles.readText(f));}return originals;
	}
	@Test public void financialSaveReloadPreservesHighWaterQuotesAndTotals()throws Exception{BankRun run=gain(active(),3);assertEquals(run,load());assertEquals(200,load().getFinancial().getGainedGp());assertEquals(Long.valueOf(100),load().getRecent().get(0).getValuations().get(0).getQuote().getUnitGp());}
	@Test public void m4MigrationBacksUpExactBytesAndPreservesBankIdentityAndHistory()throws Exception
	{
		Map<String,String> originals=m4();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A);BankRun migrated=load();String archive=migrated.getFinancial().getImportArchive();assertNotNull(archive);for(Map.Entry<String,String> entry:originals.entrySet()){assertEquals(entry.getValue(),BankStorageFiles.readText(directory.join(archive,entry.getKey())));}assertEquals(old.getRunId(),migrated.getRunId());assertEquals(old.getStartedAt(),migrated.getStartedAt());assertEquals(old.getInitialBank(),migrated.getInitialBank());assertEquals(old.getPhysical(),migrated.getPhysical());assertEquals(old.getBalances(),migrated.getBalances());assertEquals(old.getSequence()+1,migrated.getSequence());assertEquals(0,migrated.getFinancial().getGainedGp());assertTrue(migrated.getFinancial().isImportedBaseline());assertEquals(Long.valueOf(2),migrated.getFinancial().getHighWater().get(SHIELD));assertEquals(2,repository.history(root,migrated,null,false).getRows().get(0).getChange());
	}
	@Test public void m4MigrationDoesNotReplayOldGainAfterRestartOrCycle()throws Exception{m4();BankRun run=load();assertEquals(run,load());run=gain(run,2);run=gain(run,3);assertEquals(0,run.getFinancial().getGainedGp());run=gain(run,4);assertEquals(100,run.getFinancial().getGainedGp());assertEquals(run,load());}
	@Test public void failedM4MigrationKeepsOldCurrentAndRecoverableArchive()throws Exception{Map<String,String> originals=m4();BankRepository failing=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Crash");});try{failing.load(root,SnapshotFixtures.CHARACTER_A);fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));assertTrue(load().getFinancial().isImportedBaseline());}
	@Test public void brokenM4JournalIsNotMigratedOrZeroed()throws Exception{Map<String,String> originals=m4();BankRun old=codec.decode(originals.get("current.json"),SnapshotFixtures.CHARACTER_A);directory.joinSegment("bank-ledger-"+old.getRevision()+".json").write("corrupt");try{load();fail();}catch(SaveException expected){}assertEquals(originals.get("current.json"),BankStorageFiles.readText(current));}
	@Test public void damagedFinancialArchiveBlocksLoading()throws Exception{m4();BankRun run=load();directory.join(run.getFinancial().getImportArchive(),"current.json").write("corrupt");try{load();fail();}catch(SaveException expected){}}
	@Test public void incompleteFinancialPayloadNeverDefaultsToZero()throws Exception{gain(active(),2);JsonObject p=codec.payload(BankStorageFiles.readText(current),BankRunCodec.FORMAT);p.remove("financial");String invalid=codec.envelope(p,BankRunCodec.FORMAT);current.write(invalid);try{load();fail();}catch(SaveException expected){}assertEquals(invalid,BankStorageFiles.readText(current));}
	@Test public void interruptedFinancialPublicationDoesNotAdvanceHistoricalTotals()throws Exception{BankRun old=active();BankRun next=BankEngine.observe(old,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME,null,Collections.singletonMap(SHIELD,new PriceQuote(SHIELD,100L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false)));BankRepository failing=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Crash");});try{failing.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(old,load());assertEquals(next,repository.save(root,old,next));assertEquals(100,load().getFinancial().getGainedGp());}
	@Test public void repeatingSameFinancialSaveIsIdempotent()throws Exception{BankRun old=active();BankRun next=gain(old,2);assertEquals(next,repository.save(root,old,next));assertEquals(1,load().getFinancial().getEventCount());}
	@Test public void publishedFinancialStateRecoversAfterLostAcknowledgment()throws Exception
	{
		BankRun old=active();BankRun next=BankEngine.observe(old,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME,null,Collections.singletonMap(SHIELD,new PriceQuote(SHIELD,100L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false)));
		BankRepository lostAck=new BankRepository(codec,SnapshotFixtures.codec(),(pending,published)->{pending.moveTo(published,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);throw new IOException("Lost acknowledgment after publication");});
		try{lostAck.save(root,old,next);fail();}catch(SaveException expected){}assertEquals(next,load());assertEquals(next,repository.save(root,old,next));assertEquals(100,load().getFinancial().getGainedGp());assertEquals(1,load().getFinancial().getEventCount());
	}
	@Test public void inventedFinancialTotalsFailPersistenceValidation()throws Exception{BankRun before=active(),next=BankEngine.observe(before,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME);FinancialState f=next.getFinancial();BankRun invalid=new BankRun(next.getCharacter(),next.getRunId(),null,next.getStartedAt(),next.getRevision(),next.getSequence(),next.getInitialBank(),next.getPhysical(),next.getBalances(),true,null,null,next.getRecent(),next.getHostSummary(),new FinancialState(f.getMeasuredFrom(),false,f.getHighWater(),999,Collections.emptyMap(),0,0,1));try{repository.save(root,before,invalid);fail();}catch(SaveException expected){}assertEquals(before,load());}
	@Test public void explicitExpenseRoundTripPreservesBankAndReason()throws Exception{BankRun before=gain(active(),2);BankRun expense=repository.save(root,before,BankEngine.expense(before,1234,"Verified reclaim",UUID.randomUUID(),SnapshotFixtures.TIME));assertEquals(expense,load());assertEquals(before.getBalances(),expense.getBalances());assertEquals("Verified reclaim",load().getRecent().get(0).getValuations().get(0).getReason());}
	@Test public void compactRollingSummariesRetainFullImmutableJournalQuotes()throws Exception
	{
		BankRun first=gain(active(),3);Filepath segment=directory.joinSegment("bank-ledger-"+first.getRevision()+".json");String bytes=BankStorageFiles.readText(segment);BankRun next=gain(first,2);assertEquals(200,next.getFinancial().getGainedGp());assertTrue(next.getRecent().get(1).getValuations().isEmpty());assertEquals(bytes,BankStorageFiles.readText(segment));BankAudit original=codec.readAudit(codec.payload(bytes,1).getAsJsonObject("audit"));assertEquals(1,original.getValuations().size());assertEquals(Long.valueOf(100),original.getValuations().get(0).getQuote().getUnitGp());assertEquals(1,first.getRecent().get(0).getValuations().size());assertEquals(next,load());
	}
}
