package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.pouch.PouchObservation;
import com.fromscratch.playtime.Playtime;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.SnapshotFixtures.*;

/** Real isolated Filepath trees; no account files, game client or external IO. */
public class ProgressBackupTest
{
	@Rule public TemporaryFolder temp=new TemporaryFolder();
	private Filepath root,export;
	private BankRunCodec codec;
	private BankRepository repository;
	@Before public void setup()throws Exception
	{
		root=Filepath.Unchecked.getRooted(temp.newFolder("store").toPath());export=Filepath.Unchecked.getRooted(temp.getRoot().toPath().resolve("backup.fsp")); // Actual Filepath.Chooser capability scope.
		codec=new BankRunCodec(gson());repository=new BankRepository(codec,SnapshotFixtures.codec());
	}
	private BankRun run()throws Exception{return repository.save(root,null,BankFixtures.run(4));}
	private BankRun move(BankRun run,long n)throws Exception{return repository.save(root,run,BankEngine.observe(run,BankFixtures.bank(n),UUID.randomUUID(),TIME));}
	private BankRun load()throws Exception{return repository.load(root,CHARACTER_A).get();}
	private Filepath base(){return root.join("characters",CHARACTER_A);}
	private void exported(BankRun run)throws Exception{repository.backups().exportProgress(root,run,export);}
	@Test public void resetPreservesExportOtherCharacterAndHistoricalPlaytime() throws Exception
	{
		BankRun run=run(),other=BankEngine.start(CHARACTER_B,null,BankFixtures.bank(8),true,UUID.randomUUID(),TIME);repository.save(root,null,other);PlaytimeRepository timer=new PlaytimeRepository(codec);Playtime old=timer.load(root,CHARACTER_A,run.getRunId());timer.add(root,old,900,UUID.randomUUID());exported(run);byte[] original;try(InputStream in=export.openInputStream()){original=in.readAllBytes();}BankRun reset=repository.save(root,run,BankEngine.reset(run,UUID.randomUUID(),TIME));assertFalse(reset.isActive());assertEquals(other,repository.load(root,CHARACTER_B).get());try(InputStream in=export.openInputStream()){assertArrayEquals(original,in.readAllBytes());}assertEquals(900,timer.load(root,CHARACTER_A,run.getRunId()).getTicks());assertEquals(0,timer.load(root,CHARACTER_A,UUID.randomUUID()).getTicks());
	}
	@Test public void completePortableRoundTripPreservesPurchaseOffsetsFinancesHistoryAndPlaytime()throws Exception
	{
		Map<Integer,Long> initial=Collections.singletonMap(ItemID.BH_RUNE_POUCH,1L);
		BankRun run=repository.save(root,null,BankEngine.start(CHARACTER_A,null,new ContainerSnapshot(initial,1),true,UUID.randomUUID(),TIME));
		Map<Integer,Long> bank=new TreeMap<>(initial);bank.put(ItemID.COINS,5000000L);bank.put(ItemID.NATURERUNE,100L);
		run=repository.save(root,run,BankEngine.observe(run,new ContainerSnapshot(bank,3),UUID.randomUUID(),TIME));
		run=repository.save(root,run,BankEngine.buyback(run,"rune_pouch_family",UUID.randomUUID(),UUID.randomUUID(),TIME,new PouchObservation(true,Collections.singletonMap(ItemID.NATURERUNE,500L),TIME,ItemID.BH_RUNE_POUCH)));
		PlaytimeRepository time=new PlaytimeRepository(codec);Playtime measured=time.load(root,CHARACTER_A,run.getRunId());time.add(root,measured,1500,UUID.randomUUID());
		exported(run);BankRun archived=run;bank.put(ItemID.NATURERUNE,600L);run=repository.save(root,run,BankEngine.observe(run,new ContainerSnapshot(bank,3),UUID.randomUUID(),TIME));
		ProgressBackup.Candidate candidate=repository.backups().preview(root,CHARACTER_A,export);assertEquals(archived,candidate.getRun());assertEquals(100,load().balance(ItemID.NATURERUNE));
		repository.backups().restore(root,candidate);assertEquals(archived,load());assertEquals(-400,load().balance(ItemID.NATURERUNE));assertEquals(1000000,load().getBuybacks().getCommittedGp());assertEquals(1500,time.load(root,CHARACTER_A,run.getRunId()).getTicks());
		assertTrue(base().joinSegment("current.json").exists());assertTrue(base().joinSegment("restored-directory.json").exists());
		try(java.util.stream.Stream<Filepath> files=base().walk(1)){assertTrue(files.anyMatch(f->f.getFileName().startsWith("before-import-")&&f.getFileName().endsWith(".fsp")));}
		bank.put(ItemID.NATURERUNE,600L);BankRun next=repository.save(root,archived,BankEngine.observe(archived,new ContainerSnapshot(bank,3),UUID.randomUUID(),TIME));assertEquals(100,next.balance(ItemID.NATURERUNE));assertEquals(next,load());
	}
	@Test public void previewCancellationDoesNotMutateCurrentAndStaleComparisonIsRejected()throws Exception
	{
		BankRun run=run();exported(run);ProgressBackup.Candidate canceled=repository.backups().preview(root,CHARACTER_A,export);repository.backups().discard(canceled);assertEquals(run,load());assertFalse(base().joinSegment("restored-directory.json").exists());
		ProgressBackup.Candidate stale=repository.backups().preview(root,CHARACTER_A,export);BankRun next=move(run,2);
		try{repository.backups().restore(root,stale);fail();}catch(SaveException expected){assertTrue(expected.getMessage().contains("changed"));}assertEquals(next,load());
	}
	@Test public void wrongCharacterMalformedAndTraversalBackupsNeverReplaceProgress()throws Exception
	{
		BankRun run=run();exported(run);
		try{repository.backups().preview(root,CHARACTER_B,export);fail();}catch(SaveException expected){}
		export.write("not a zip");try{repository.backups().preview(root,CHARACTER_A,export);fail();}catch(SaveException expected){}
		try(ZipOutputStream zip=new ZipOutputStream(export.openOutputStream())){zip.putNextEntry(new ZipEntry("../current.json"));zip.write("bad".getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}
		try{repository.backups().preview(root,CHARACTER_A,export);fail();}catch(SaveException expected){}assertEquals(run,load());
	}
	@Test public void checksumAndMissingLedgerAreRejectedBeforeAnyCurrentWrites()throws Exception
	{
		BankRun run=run();exported(run);Filepath changed=Filepath.Unchecked.getRooted(temp.getRoot().toPath().resolve("changed.fsp"));
		try(ZipInputStream input=new ZipInputStream(export.openInputStream());ZipOutputStream output=new ZipOutputStream(changed.openOutputStream()))
		{
			ZipEntry entry;while((entry=input.getNextEntry())!=null){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){bytes.write(buffer,0,n);}output.putNextEntry(new ZipEntry(entry.getName()));output.write(entry.getName().equals("current.json")?"corrupt".getBytes(java.nio.charset.StandardCharsets.UTF_8):bytes.toByteArray());output.closeEntry();}
		}
		try{repository.backups().preview(root,CHARACTER_A,changed);fail();}catch(SaveException expected){}assertEquals(run,load());
		PlaytimeRepository timer=new PlaytimeRepository(codec);Playtime time=timer.load(root,CHARACTER_A,run.getRunId());timer.add(root,time,10,UUID.randomUUID());
		Filepath counter=base().join("playtime","playtime-"+run.getRunId()+".json");counter.delete();
		try{repository.backups().exportProgress(root,run,Filepath.Unchecked.getRooted(temp.getRoot().toPath().resolve("incomplete.fsp")));fail();}catch(SaveException expected){assertTrue(expected.getMessage().contains("Playtime"));}
		base().joinSegment("bank-ledger-"+run.getRevision()+".json").delete();try{exported(run);fail();}catch(IOException expected){}
	}
	@Test public void interruptedRestoreDoesNotPublishAnyPartialProgress()throws Exception
	{
		BankRun run=run();exported(run);BankRun latest=move(run,1);ProgressBackup.Candidate candidate=repository.backups().preview(root,CHARACTER_A,export);
		try{Thread.currentThread().interrupt();repository.backups().restore(root,candidate);fail();}catch(IOException expected){}finally{Thread.interrupted();}
		assertEquals(latest,load());assertFalse(base().joinSegment("restored-directory.json").exists());
	}
	@Test public void previousPublishedCheckpointCanBeExplicitlyRecoveredWithoutReplayingOrphans()throws Exception
	{
		BankRun before=run();BankRun last=move(before,1);assertEquals(before,codec.decode(BankStorageFiles.readText(base().joinSegment("previous.json")),CHARACTER_A));
		base().joinSegment("current.json").write("broken");try{load();fail();}catch(SaveException expected){}
		ProgressBackup.Candidate candidate=repository.backups().previous(root,CHARACTER_A);assertEquals(before,candidate.getRun());repository.backups().restore(root,candidate);assertEquals(before,load());
		assertTrue(base().joinSegment("bank-checkpoint-"+last.getRevision()+".json").exists());
	}
	@Test public void validCurrentIsNeverAutomaticallyReplacedAndExportCannotTargetOwnSave()throws Exception
	{
		BankRun run=run();move(run,3);try{repository.backups().previous(root,CHARACTER_A);fail();}catch(IllegalArgumentException expected){}
		try{repository.backups().exportProgress(root,load(),base().joinSegment("current.json"));fail();}catch(SaveException expected){}assertEquals(-1,load().balance(BankFixtures.SHIELD));
		exported(load());byte[] original;try(InputStream input=export.openInputStream()){original=input.readAllBytes();}
		try{exported(load());fail();}catch(SaveException expected){assertTrue(expected.getMessage().contains("already exists"));}
		try(InputStream input=export.openInputStream()){assertArrayEquals(original,input.readAllBytes());}
	}
	@Test public void controllerPublishesRestoredOverlayOnlyAfterCommitAndNextBankDeltaRemainsAutomatic()throws Exception
	{
		BankRun initial=run();BankFixtures.Source source=new BankFixtures.Source();source.quantity(4);BankFixtures.Worker worker=new BankFixtures.Worker();com.fromscratch.overlay.BankOverlayState overlay=new com.fromscratch.overlay.BankOverlayState();overlay.enable();
		BankController controller=new BankController(source,repository,Runnable::run,overlay::updateBank);controller.start(()->root,worker);worker.runAll();source.observer.tick();controller.tick();worker.runAll();
		java.util.List<String> results=new ArrayList<>();controller.exportProgress(export,results::add);assertFalse(export.exists());worker.runAll();assertTrue(export.exists());
		source.quantity(1);source.observer.tick();controller.tick();worker.runAll();assertEquals("-3",overlay.getProjection().label(BankFixtures.SHIELD));
		java.util.List<ProgressBackup.Candidate> candidates=new ArrayList<>();controller.previewImport(export,candidates::add,results::add);worker.runAll();assertEquals(1,candidates.size());controller.importProgress(candidates.get(0),results::add);
		assertEquals("-3",overlay.getProjection().label(BankFixtures.SHIELD));worker.runAll();assertEquals("0",overlay.getProjection().label(BankFixtures.SHIELD));assertEquals(initial.getRunId(),load().getRunId());
		source.observer.changed();source.observer.tick();controller.tick();worker.runAll();assertEquals(-3,load().balance(BankFixtures.SHIELD));assertEquals("-3",overlay.getProjection().label(BankFixtures.SHIELD));controller.stop();
	}
}
