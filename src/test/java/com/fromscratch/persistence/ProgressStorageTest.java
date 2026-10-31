package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.BankController;
import com.fromscratch.bank.BankEngine;
import com.fromscratch.bank.BankFixtures;
import com.fromscratch.bank.BankHistory;
import com.fromscratch.bank.BankRun;
import com.fromscratch.bank.BankView;
import com.fromscratch.buyback.BuybackEngine;
import com.fromscratch.buyback.BuybackModelTest;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.playtime.Playtime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Official storage isolation and read-only startup checks over disposable repository checkpoints. */
public class ProgressStorageTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath scoped;
	private Filepath root;
	private BankRepository repository;
	private PlaytimeRepository playtime;

	@Before public void setup() throws Exception
	{
		scoped = Filepath.Unchecked.getRooted(temporary.newFolder("from-scratch").toPath());
		root = scoped.joinSegment("progress");
		BankRunCodec codec = new BankRunCodec(SnapshotFixtures.gson());
		repository = new BankRepository(codec, SnapshotFixtures.codec());
		playtime = new PlaytimeRepository(codec);
	}

	@Test public void absentStorageNeedsNoAcknowledgmentAndCreatesNothing() throws Exception
	{
		Map<String, byte[]> before = files();
		assertEquals(root, storage().get());
		assertFalse(root.exists());
		assertFiles(before, files());
	}

	@Test public void outsideFilesAreNotLoadedReplacedOrCopied() throws Exception
	{
		BankRun outside = funded(scoped, SnapshotFixtures.CHARACTER_A);
		scoped.joinSegment("unrelated-record.txt").write("unrelated evidence");
		Map<String, byte[]> before = outsideFiles();
		Filepath official = storage().get();
		assertFalse(root.exists());
		assertFalse(repository.load(official, outside.getCharacter()).isPresent());
		BankRun fresh = repository.save(official, null, BankEngine.start(outside.getCharacter(), null,
			BuybackModelTest.bank(ItemID.DRAGON_BONES), true, UUID.randomUUID(), SnapshotFixtures.TIME));
		assertNotEquals(outside.getRunId(), fresh.getRunId());
		assertEquals(0, fresh.balance(ItemID.COINS));
		assertFiles(before, outsideFiles());
		assertEquals(fresh, repository.load(storage().get(), fresh.getCharacter()).get());
		assertFiles(before, outsideFiles());
	}

	@Test public void freshControllerReachesGuidedSetupWithoutLoadingOutsideRun() throws Exception
	{
		funded(scoped, SnapshotFixtures.CHARACTER_A);
		Map<String, byte[]> before = outsideFiles();
		BankFixtures.Source source = new BankFixtures.Source();
		BankFixtures.Worker worker = new BankFixtures.Worker();
		List<BankView> views = new ArrayList<>();
		BankController controller = new BankController(source, repository, Runnable::run, views::add);
		try
		{
			controller.start(storage(), worker);
			worker.runAll();
			assertFalse(views.get(views.size() - 1).isError());
			assertNotNull(views.get(views.size() - 1).getSetup());
			assertNull(controller.currentRun());
			assertFalse(root.join("characters", source.character, "current.json").exists());
			for (int i = 0; i < 4; i++) { source.observer.tick(); controller.tick(); worker.runAll(); }
			controller.startRun(true, null, true);
			worker.runAll();
			assertNotNull(controller.currentRun());
			assertTrue(controller.currentRun().isActive());
			assertEquals(controller.currentRun(), repository.load(storage().get(), source.character).get());
			assertFiles(before, outsideFiles());
		}
		finally { controller.stop(); }
	}

	@Test public void restartRestoresGpOwnershipBuybacksAndHistory() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		run = repository.save(root, run, BuybackModelTest.buy(run, "fire_cape"));
		Map<String, byte[]> before = files();
		Filepath verified = storage().get();
		assertFiles(before, files());
		BankRun loaded = repository.load(verified, run.getCharacter()).get();
		assertEquals(run, loaded);
		assertEquals(1500000L, BuybackEngine.available(loaded));
		assertEquals(1000000L, loaded.getBuybacks().getCommittedGp());
		assertEquals(1L, (long) BuybackEngine.balances(loaded).get(ItemID.TZHAAR_CAPE_FIRE));
		assertEquals(run.getBuybacks().getPurchases(), loaded.getBuybacks().getPurchases());
		BankHistory.Page history = repository.history(verified, loaded, null, false);
		assertFalse(history.getRows().isEmpty());
		assertTrue(history.getRows().stream().anyMatch(row -> row.getItemId() == ItemID.COINS && row.getChange() == 2500000));
	}

	@Test public void bankAndPlaytimeShareTheSameVerifiedProgressDirectory() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Playtime initial = playtime.load(root, run.getCharacter(), run.getRunId());
		Playtime stored = playtime.add(root, initial, 1234, UUID.randomUUID());
		ProgressStorage shared = storage();
		assertEquals(root, shared.get());
		assertEquals(run, repository.load(shared.get(), run.getCharacter()).get());
		assertEquals(stored, playtime.load(shared.get(), run.getCharacter(), run.getRunId()));
		assertEquals(1234, stored.getTicks());
	}

	@Test public void charactersRemainIsolated() throws Exception
	{
		BankRun first = funded(root, SnapshotFixtures.CHARACTER_A);
		BankRun second = repository.save(root, null, BankEngine.start(SnapshotFixtures.CHARACTER_B, null,
			BuybackModelTest.bank(ItemID.DRAGON_BONES), true, UUID.randomUUID(), SnapshotFixtures.TIME));
		Filepath verified = storage().get();
		assertEquals(first, repository.load(verified, first.getCharacter()).get());
		assertEquals(second, repository.load(verified, second.getCharacter()).get());
		assertEquals(0, second.balance(ItemID.COINS));
		assertTrue(second.getBuybacks().getPurchases().isEmpty());
	}

	@Test public void missingCurrentCheckpointBlocksStartupWithoutWrites() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath current = root.join("characters", run.getCharacter(), "current.json");
		current.delete();
		assertBlockedWithoutWrites();
		assertFalse(current.exists());
	}

	@Test public void missingJournalBlocksStartupWithoutWrites() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		root.join("characters", run.getCharacter(), "bank-ledger-" + run.getRevision() + ".json").delete();
		assertBlockedWithoutWrites();
	}

	@Test public void damagedCheckpointBlocksStartupWithoutWrites() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		root.join("characters", run.getCharacter(), "current.json").write("unreadable checkpoint");
		assertBlockedWithoutWrites();
	}

	@Test public void oneIncompleteCharacterBlocksTheEntireStartup() throws Exception
	{
		funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath broken = root.join("characters", SnapshotFixtures.CHARACTER_B);
		broken.createDirectories();
		broken.joinSegment("bank-pending-interrupted.json").write("retained incomplete evidence");
		assertBlockedWithoutWrites();
		assertFalse(broken.joinSegment("current.json").exists());
	}

	@Test public void repeatedStartupRetainsCommittedProgress() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		assertEquals(root, storage().get());
		Map<Integer, Long> physical = new TreeMap<>(run.getPhysical());
		physical.put(ItemID.COINS, 2600000L);
		BankRun next = repository.save(root, run, BankEngine.observe(run,
			new ContainerSnapshot(physical, physical.size()), UUID.randomUUID(), SnapshotFixtures.TIME));
		assertEquals(next, repository.load(storage().get(), next.getCharacter()).get());
		assertEquals(2600000L, next.balance(ItemID.COINS));
	}

	@Test public void missingPlaytimeCheckpointWithBackupDoesNotInitializeZero() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Playtime initial = playtime.load(root, run.getCharacter(), run.getRunId());
		playtime.add(root, initial, 500, UUID.randomUUID());
		Filepath current = root.join("characters", run.getCharacter(), "playtime", "playtime-" + run.getRunId() + ".json");
		current.delete();
		assertBlockedWithoutWrites();
		assertFalse(current.exists());
	}

	@Test public void damagedStorageCannotReachControllerChallengeCreation() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath current = root.join("characters", run.getCharacter(), "current.json");
		current.delete();
		Map<String, byte[]> before = files();
		BankFixtures.Source source = new BankFixtures.Source();
		BankFixtures.Worker worker = new BankFixtures.Worker();
		List<BankView> views = new ArrayList<>();
		BankController controller = new BankController(source, repository, Runnable::run, views::add);
		try
		{
			controller.start(storage(), worker);
			worker.runAll();
			assertTrue(views.get(views.size() - 1).isError());
			assertNull(controller.currentRun());
			controller.startRun(true, null, true);
			worker.runAll();
			assertNull(controller.currentRun());
			assertFiles(before, files());
			assertFalse(current.exists());
		}
		finally { controller.stop(); }
	}

	@Test public void unstartedCharacterWithOnlyRunLockSurvivesRestart() throws Exception
	{
		assertFalse(repository.load(storage().get(), SnapshotFixtures.CHARACTER_A).isPresent());
		Filepath character = root.join("characters", SnapshotFixtures.CHARACTER_A);
		assertTrue(character.joinSegment("run.lock").exists());
		assertEquals(0, character.joinSegment("run.lock").size());
		assertFalse(character.joinSegment("current.json").exists());
		Map<String, byte[]> before = files();
		assertEquals(root, storage().get());
		assertFiles(before, files());
	}

	@Test public void regularFileAtPlaytimeDirectoryBlocksStartupWithoutWrites() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath invalidFolder = root.join("characters", run.getCharacter(), "playtime");
		invalidFolder.write("unexpected regular-file recovery evidence");
		assertBlockedWithoutWrites();
		assertTrue(invalidFolder.isFile());
	}

	@Test public void regularFileAtProgressDirectoryIsPreserved() throws Exception
	{
		root.write("unrelated file");
		assertBlockedWithoutWrites();
	}

	@Test public void orphanedRecoveryEvidencePreventsNewStorage() throws Exception
	{
		root.createDirectories();
		root.joinSegment("pending-recovery.json").write("retained evidence");
		assertBlockedWithoutWrites();
	}

	@Test public void explicitResetArchivesBeforeFreshRunAndRestart() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath directory = root.join("characters", run.getCharacter());
		String original = BankStorageFiles.readText(directory.joinSegment("current.json"));
		BankRun reset = repository.save(root, run, BankEngine.reset(run, UUID.randomUUID(), SnapshotFixtures.TIME));
		try (Stream<Filepath> entries = directory.walk(1))
		{
			List<Filepath> archives = entries.filter(p -> p.getFileName().startsWith("bank-migration-")).collect(Collectors.toList());
			assertEquals(1, archives.size());
			assertEquals(original, BankStorageFiles.readText(archives.get(0).joinSegment("current.json")));
		}
		assertEquals(reset, repository.load(storage().get(), run.getCharacter()).get());
		BankRun fresh = BankEngine.start(run.getCharacter(), reset.getPreviousRunId(),
			BuybackModelTest.bank(ItemID.DRAGON_BONES), true, UUID.randomUUID(), SnapshotFixtures.TIME);
		fresh = new BankRun(fresh.getCharacter(), fresh.getRunId(), fresh.getPreviousRunId(), fresh.getStartedAt(),
			fresh.getRevision(), Math.incrementExact(reset.getSequence()), fresh.getInitialBank(), fresh.getPhysical(),
			fresh.getBalances(), fresh.isAnchored(), fresh.getLegacyDocument(), fresh.getMigrationArchive(),
			fresh.getRecent(), fresh.getHostSummary(), fresh.getFinancial(), fresh.getExchange(), fresh.getBuybacks(), fresh.getPotions());
		fresh = repository.save(root, reset, fresh);
		assertEquals(fresh, repository.load(storage().get(), run.getCharacter()).get());
		assertEquals(0, fresh.balance(ItemID.COINS));
	}

	private ProgressStorage storage() { return new ProgressStorage(() -> scoped, repository); }
	private BankRun funded(Filepath directory, String character) throws Exception
	{
		BankRun run = repository.save(directory, null, BankEngine.start(character, null,
			BuybackModelTest.bank(ItemID.TZHAAR_CAPE_FIRE), true, UUID.randomUUID(), SnapshotFixtures.TIME));
		Map<Integer, Long> physical = new TreeMap<>(run.getPhysical());
		physical.put(ItemID.COINS, 2500000L);
		return repository.save(directory, run, BankEngine.observe(run,
			new ContainerSnapshot(physical, physical.size()), UUID.randomUUID(), SnapshotFixtures.TIME));
	}
	private void assertBlockedWithoutWrites() throws Exception
	{
		Map<String, byte[]> before = files();
		try { storage().get(); fail("Unverified progression must remain blocked"); }
		catch (IOException expected) { assertNotNull(expected.getMessage()); }
		assertFiles(before, files());
	}
	private Map<String, byte[]> outsideFiles() throws Exception
	{
		Map<String, byte[]> result = files();
		result.keySet().removeIf(name -> name.startsWith("progress/"));
		return result;
	}
	private Map<String, byte[]> files() throws Exception
	{
		Map<String, byte[]> result = new TreeMap<>();
		try (Stream<Filepath> stream = scoped.walk())
		{
			for (Filepath file : stream.filter(Filepath::isFile).collect(Collectors.toList()))
			{
				String relative = file.toString().substring(scoped.toString().length() + 1).replace('\\', '/');
				try (java.io.InputStream input = file.openInputStream()) { result.put(relative, input.readAllBytes()); }
			}
		}
		return result;
	}
	private static void assertFiles(Map<String, byte[]> expected, Map<String, byte[]> actual)
	{
		assertEquals(expected.keySet(), actual.keySet());
		for (String name : expected.keySet()) { assertArrayEquals(name, expected.get(name), actual.get(name)); }
	}
}
