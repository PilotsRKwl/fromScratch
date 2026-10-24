package com.fromscratch.persistence;

import com.fromscratch.bank.*;
import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.tracking.*;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

/** Format-1–4 compatibility, exact-byte archives, new chain integrity and durable failure boundaries. */
public class BankPersistenceTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private BankRepository repository;
	private BankRunCodec codec;
	@Before public void setup() { root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); codec = new BankRunCodec(SnapshotFixtures.gson()); repository = new BankRepository(codec, SnapshotFixtures.codec()); }
	private Filepath directory() { return root.join("characters", SnapshotFixtures.CHARACTER_A); }
	private Filepath current() { return directory().joinSegment("current.json"); }
	private BankRun load() throws Exception { return repository.load(root, SnapshotFixtures.CHARACTER_A).get(); }
	private void old(String text) throws Exception { directory().createDirectories(); current().write(text); }
	private BankRun active() throws Exception { BankRun run = BankFixtures.run(1); return repository.save(root, null, run); }
	private BankRun observe(BankRun before, long n) throws Exception { return repository.save(root, before, BankEngine.observe(before, bank(n), UUID.randomUUID(), TIME)); }
	private BankRepository failing() { return new BankRepository(codec, SnapshotFixtures.codec(), (pending, current) -> { throw new IOException("Synthetic publication failure"); }); }
	private List<Filepath> files(String prefix) throws Exception { try (Stream<Filepath> files = directory().walk(1)) { return files.filter(f -> f.getFileName().startsWith(prefix)).collect(Collectors.toList()); } }
	@Test public void formatNineLoadsWithoutRewritingAndNextCommitUpgradesAtomically()throws Exception
	{
		BankRun original=active();JsonObject payload=codec.payload(BankStorageFiles.readText(current()),BankRunCodec.FORMAT);payload.remove("potions");String old=codec.envelope(payload,9);current().write(old);directory().joinSegment("bank-checkpoint-"+original.getRevision()+".json").write(old);
		Filepath ledger=directory().joinSegment("bank-ledger-"+original.getRevision()+".json");JsonObject batch=codec.payload(BankStorageFiles.readText(ledger),1);batch.addProperty("result",SaveIntegrity.sha256(old));String originalLedger=codec.envelope(batch,1);ledger.write(originalLedger);
		assertEquals(original,load());assertEquals(old,BankStorageFiles.readText(current()));assertEquals(originalLedger,BankStorageFiles.readText(ledger));
		BankRun advanced=observe(original,2);assertEquals(advanced,load());assertEquals(original.getInitialBank(),advanced.getInitialBank());assertEquals(originalLedger,BankStorageFiles.readText(ledger));assertEquals(10,new JsonParser().parse(BankStorageFiles.readText(current())).getAsJsonObject().get("formatVersion").getAsInt());
	}
	@Test public void signedBalancesAndCorrectionsRoundTrip() throws Exception { BankRun run = observe(active(), 0); assertEquals(-1, load().balance(SHIELD)); assertEquals(run, load()); }
	@Test public void missingSaveDoesNotWriteBlankState() throws Exception { assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_A).isPresent()); assertFalse(current().exists()); }
	@Test public void characterIsolationRejectsCopiedCheckpoint() throws Exception { active(); Filepath other = root.join("characters", SnapshotFixtures.CHARACTER_B); other.createDirectories(); current().copyTo(other.joinSegment("current.json")); try { repository.load(root, SnapshotFixtures.CHARACTER_B); fail(); } catch (SaveException expected) { } }
	@Test public void failedPublicationDoesNotAdvanceCursorAndOrphansAreNotReplayed() throws Exception
	{
		BankRun before = active(); BankRun proposed = BankEngine.observe(before, bank(3), UUID.randomUUID(), TIME);
		try { failing().save(root, before, proposed); fail(); } catch (SaveException expected) { }
		assertEquals(before, load()); assertEquals(2, files("bank-checkpoint-").size());
		BankRun committed = observe(before, 3); assertEquals(2, committed.balance(SHIELD)); assertEquals(committed, load());
	}
	@Test public void acknowledgedCandidateCanBeRetriedIdempotently() throws Exception { BankRun before = active(); BankRun after = observe(before, 2); assertEquals(after, repository.save(root, before, after)); }
	@Test public void staleSaveCannotOverwriteNewerProgress() throws Exception { BankRun before = active(); BankRun newer = observe(before, 2); try { observe(before, 3); fail(); } catch (SaveException expected) { } assertEquals(newer, load()); }
	@Test public void corruptCurrentPreservedNotReset() throws Exception { active(); current().write("{corrupt"); try { load(); fail(); } catch (SaveException expected) { } assertEquals("{corrupt", RunRepository.readText(current())); }
	@Test public void newerFormatPreservedWithoutMigration() throws Exception { active(); JsonObject e = new JsonParser().parse(RunRepository.readText(current())).getAsJsonObject(); e.addProperty("formatVersion", 99); String text = e.toString(); current().write(text); try { load(); fail(); } catch (SaveException expected) { } assertEquals(text, RunRepository.readText(current())); assertTrue(files("bank-migration-").isEmpty()); }
	@Test public void missingCurrentWithRecoveryEvidenceBlocksNewRun() throws Exception { active(); current().delete(); try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { } try { active(); fail(); } catch (SaveException expected) { } }
	@Test public void missingJournalBlocksLoad() throws Exception { BankRun run = active(); directory().joinSegment("bank-ledger-" + run.getRevision() + ".json").delete(); try { load(); fail(); } catch (SaveException expected) { } }
	@Test public void corruptEarlierJournalBlocksLoad() throws Exception { BankRun first = active(); observe(first, 2); directory().joinSegment("bank-ledger-" + first.getRevision() + ".json").write("corrupt"); try { load(); fail(); } catch (SaveException expected) { } }
	@Test public void resetArchivesCompleteHistoryBeforePublishingTombstone() throws Exception { BankRun run = observe(active(), 2); BankRun reset = repository.save(root, run, BankEngine.reset(run, UUID.randomUUID(), TIME)); assertFalse(reset.isActive()); Filepath archive = files("bank-migration-").get(0); assertTrue(archive.joinSegment("current.json").exists()); assertTrue(archive.joinSegment("bank-ledger-" + run.getRevision() + ".json").exists()); assertEquals(reset, load()); }
	@Test public void failedResetPublicationNeverAcknowledgesReset() throws Exception { BankRun before = active(); try { failing().save(root, before, BankEngine.reset(before, UUID.randomUUID(), TIME)); fail(); } catch (SaveException expected) { } assertEquals(before, load()); assertFalse(files("bank-migration-").isEmpty()); }
	@Test public void resetArchiveManifestMatchesEveryRetainedByte() throws Exception
	{
		BankRun before=observe(active(),2);repository.save(root,before,BankEngine.reset(before,UUID.randomUUID(),TIME));Filepath archive=files("bank-migration-").get(0);com.google.gson.JsonObject manifest=codec.payload(BankStorageFiles.readText(archive.joinSegment("manifest.json")),1);assertTrue(manifest.size()>=4);for(Map.Entry<String,com.google.gson.JsonElement> entry:manifest.entrySet()){assertEquals(entry.getValue().getAsString(),com.fromscratch.persistence.SaveIntegrity.sha256(BankStorageFiles.readText(archive.joinSegment(entry.getKey()))));}
	}
	@Test public void failedArchiveLeavesCurrentAndOriginalsUntouched() throws Exception
	{
		BankRun before = observe(active(), 2);
		String original = BankStorageFiles.readText(current());
		// A directory cannot be read as a JSON file on either supported OS.
		Filepath unreadable = directory().joinSegment("unreadable.json"); unreadable.createDirectories();
		try { repository.save(root, before, BankEngine.reset(before, UUID.randomUUID(), TIME)); fail(); }
		catch (SaveException expected) { }
		assertEquals(original, BankStorageFiles.readText(current()));
		assertEquals(before, load());
		for (Filepath archive : files("bank-migration-")) { assertFalse(archive.joinSegment("manifest.json").exists()); }
		unreadable.delete();
		assertFalse(repository.save(root, before, BankEngine.reset(before, UUID.randomUUID(), TIME)).isActive());
	}
	@Test public void resetAllowsFreshRunAfterReloadWithoutChangingOtherCharacter() throws Exception
	{
		BankRun before = observe(active(), 0);
		BankRun other = BankEngine.start(SnapshotFixtures.CHARACTER_B, null, bank(3), true, UUID.randomUUID(), TIME);
		repository.save(root, null, other);
		BankRun reset = repository.save(root, before, BankEngine.reset(before, UUID.randomUUID(), TIME));
		assertEquals(reset, load()); assertEquals(other, repository.load(root, other.getCharacter()).get());
		assertTrue(reset.getBalances().isEmpty()); assertNull(reset.getRunId());
		BankRun fresh = BankEngine.start(before.getCharacter(), reset.getPreviousRunId(), bank(4), true, UUID.randomUUID(), TIME);
		fresh = new BankRun(fresh.getCharacter(), fresh.getRunId(), fresh.getPreviousRunId(), fresh.getStartedAt(), fresh.getRevision(), Math.incrementExact(reset.getSequence()), fresh.getInitialBank(), fresh.getPhysical(), fresh.getBalances(), true, null, null, fresh.getRecent(), null, fresh.getFinancial(), fresh.getExchange(), fresh.getBuybacks(), fresh.getPotions());
		repository.save(root, reset, fresh); assertEquals(fresh, load());
		assertNotEquals(before.getRunId(), fresh.getRunId()); assertTrue(fresh.getBalances().isEmpty());
	}
	@Test public void milestoneTwoMigrationPreservesRunAndOriginalDocument() throws Exception
	{
		RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100); String bytes = OwnershipPersistenceTest.versionOne(old); old(bytes);
		BankRun migrated = load(); assertEquals(old.getState().getRunId(), migrated.getRunId()); assertEquals(old.getState().getStartedAt(), migrated.getStartedAt()); assertEquals(old.getSnapshot().at(SnapshotLocation.BANK), migrated.getInitialBank()); assertEquals(bytes, migrated.getLegacyDocument()); assertFalse(migrated.isAnchored()); assertTrue(migrated.getBalances().isEmpty());
		assertEquals(bytes, RunRepository.readText(directory().join(migrated.getMigrationArchive(), "current.json"))); assertEquals(migrated, load());
	}
	@Test public void milestoneThreePositiveBankBalancesKeptOutsideHoldingsArchived() throws Exception
	{
		RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 10).withOwnership(VirtualOwnership.empty().withItem(SHIELD, new VirtualItemOwnership(3, Collections.singletonMap(VirtualLocation.BANK, 2L))));
		JsonObject envelope = new JsonParser().parse(SnapshotFixtures.codec().encode(old)).getAsJsonObject(); JsonObject p = new JsonParser().parse(envelope.get("payload").getAsString()).getAsJsonObject(); p.remove("itemProgress"); envelope.addProperty("formatVersion", 2); envelope.addProperty("payload", p.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(p.toString())); old(envelope.toString());
		BankRun migrated = load(); assertEquals(2, migrated.balance(SHIELD)); assertFalse(migrated.isAnchored()); assertTrue(migrated.getLegacyDocument().contains("ownership"));
		try { BankEngine.anchor(migrated, bank(1), false, UUID.randomUUID(), TIME); fail(); } catch (IllegalArgumentException expected) { }
		BankRun anchored = repository.save(root, migrated, BankEngine.anchor(migrated, bank(1), true, UUID.randomUUID(), TIME)); assertEquals(1, anchored.balance(SHIELD)); assertEquals(Long.valueOf(-1), anchored.getRecent().get(0).getBalanceChanges().get(SHIELD)); assertEquals(anchored, load());
	}
	@Test public void milestoneFourDeficitsAndAllLedgerBytesArchivedBeforeAnchor() throws Exception
	{
		RunRepository oldRepository = new RunRepository(SnapshotFixtures.codec()); RunRecord first = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(10, 0, 0)), TIME); oldRepository.activate(root, first);
		RunRecord established = oldRepository.saveItems(root, first, ENGINE.plan(first, observation(holdings(10, 0, 0), 1), id -> id, true, false, Collections.emptyMap(), ""));
		RunRecord withdrawn = oldRepository.saveItems(root, established, plan(established, holdings(9, 1, 0), 2));
		Map<String, String> originals = new TreeMap<>(); for (Filepath file : files("ledger-")) { originals.put(file.getFileName(), RunRepository.readText(file)); }
		BankRun migrated = load(); assertEquals(-1, migrated.balance(ItemID.SHARK)); assertEquals(withdrawn.getState().getRunId(), migrated.getRunId());
		for (Map.Entry<String, String> e : originals.entrySet()) { assertEquals(e.getValue(), RunRepository.readText(directory().join(migrated.getMigrationArchive(), e.getKey()))); assertEquals(e.getValue(), RunRepository.readText(directory().joinSegment(e.getKey()))); }
		BankRun anchored = repository.save(root, migrated, BankEngine.anchor(migrated, new ContainerSnapshot(Collections.singletonMap(ItemID.SHARK, 9L), 1), false, UUID.randomUUID(), TIME));
		BankRun same = BankEngine.observe(anchored, new ContainerSnapshot(Collections.singletonMap(ItemID.SHARK, 9L), 1), anchored.getHostSummary().getSession(), TIME); assertSame(anchored, same); assertEquals(anchored, load());
	}
	@Test public void invalidOldLedgerNeverMigratesOrResets() throws Exception
	{
		RunRepository oldRepository = new RunRepository(SnapshotFixtures.codec()); RunRecord first = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(1, 0, 0)), TIME); oldRepository.activate(root, first);
		RunRecord established = oldRepository.saveItems(root, first, ENGINE.plan(first, observation(holdings(1, 0, 0), 1), id -> id, true, false, Collections.emptyMap(), "")); String original = RunRepository.readText(current());
		directory().joinSegment("ledger-" + established.getState().getRunId() + "-" + established.getRevision() + ".json").write("corrupt"); try { load(); fail(); } catch (SaveException expected) { } assertEquals(original, RunRepository.readText(current())); assertTrue(files("bank-migration-").isEmpty());
	}
	@Test public void interruptedMigrationPreservesOldCurrentAndCanRetry() throws Exception { String original = OwnershipPersistenceTest.versionOne(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1)); old(original); try { failing().load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { } assertEquals(original, RunRepository.readText(current())); assertFalse(files("bank-migration-").isEmpty()); assertFalse(load().isAnchored()); }
	@Test public void damagedArchiveBlocksLoadRatherThanIgnoringLegacyData() throws Exception { old(OwnershipPersistenceTest.versionOne(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1))); BankRun migrated = load(); directory().join(migrated.getMigrationArchive(), "current.json").write("corrupt"); try { load(); fail(); } catch (SaveException expected) { } }
	@Test public void missingRequiredBalanceMapIsCorruptionNotZero() throws Exception { active(); JsonObject p = codec.payload(RunRepository.readText(current()), BankRunCodec.FORMAT); p.remove("balances"); current().write(codec.envelope(p, BankRunCodec.FORMAT)); try { load(); fail(); } catch (SaveException expected) { } }
	@Test public void oldResetDoesNotResurrectActiveRun() throws Exception { RunRecord reset = RunRecord.reset(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1)); old(OwnershipPersistenceTest.versionOne(reset)); BankRun migrated = load(); assertFalse(migrated.isActive()); assertEquals(reset.getPreviousRunId(), migrated.getPreviousRunId()); }
	@Test public void verifiedFormatThreeChainMigratesWithoutRewritingItsSegments() throws Exception
	{
		RunRecordCodec oldCodec = SnapshotFixtures.codec(); RunRepository oldRepository = new RunRepository(oldCodec);
		RunRecord initial = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(1, 0, 0)), TIME); oldRepository.activate(root, initial);
		RunRecord established = oldRepository.saveItems(root, initial, ENGINE.plan(initial, observation(holdings(1, 0, 0), 1), id -> id, true, false, Collections.emptyMap(), ""));
		String oldCheckpoint = oldCodec.encodeVersion(established, 3); current().write(oldCheckpoint);
		Filepath segment = files("ledger-").get(0); JsonObject e = new JsonParser().parse(RunRepository.readText(segment)).getAsJsonObject(); JsonObject p = new JsonParser().parse(e.get("payload").getAsString()).getAsJsonObject();
		for (JsonElement a : p.getAsJsonArray("entries")) { a.getAsJsonObject().remove("legacyWithdrawalChange"); }
		p.addProperty("resultSha256", SaveIntegrity.sha256(oldCheckpoint)); e.addProperty("version", 1); e.addProperty("payload", p.toString()); e.addProperty("sha256", SaveIntegrity.sha256(p.toString())); String oldSegment = e.toString(); segment.write(oldSegment);
		BankRun migrated = load(); assertEquals(oldCheckpoint, migrated.getLegacyDocument()); assertEquals(oldSegment, RunRepository.readText(segment)); assertEquals(oldSegment, RunRepository.readText(directory().join(migrated.getMigrationArchive(), segment.getFileName())));
	}
	@Test public void nonzeroInitialBalanceCannotBypassPersistenceBoundary() throws Exception
	{
		BankRun r = BankFixtures.run(1); BankRun invalid = new BankRun(r.getCharacter(), r.getRunId(), null, r.getStartedAt(), r.getRevision(), 1, r.getInitialBank(), r.getPhysical(), Collections.singletonMap(SHIELD, 1L), true, null, null, r.getRecent(), null);
		try { repository.save(root, null, invalid); fail(); } catch (SaveException expected) { } assertFalse(current().exists());
	}
	@Test public void ordinaryTransactionCannotDiscardTheTrustedAnchor() throws Exception
	{
		BankRun before = active(); BankRun next = BankEngine.observe(before, bank(2), UUID.randomUUID(), TIME);
		BankRun invalid = new BankRun(next.getCharacter(), next.getRunId(), next.getPreviousRunId(), next.getStartedAt(), next.getRevision(), next.getSequence(), next.getInitialBank(), next.getPhysical(), next.getBalances(), false, null, null, next.getRecent(), next.getHostSummary());
		try { repository.save(root, before, invalid); fail(); } catch (SaveException expected) { } assertEquals(before, load());
	}
}
