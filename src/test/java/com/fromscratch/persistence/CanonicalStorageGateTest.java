package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.BankEngine;
import com.fromscratch.bank.BankHistory;
import com.fromscratch.bank.BankRun;
import com.fromscratch.buyback.BuybackEngine;
import com.fromscratch.buyback.BuybackModelTest;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.playtime.Playtime;
import java.io.IOException;
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

/** Canonical startup checks over disposable, genuine repository checkpoints only. */
public class CanonicalStorageGateTest
{
	private static final String RECEIPT = ".from-scratch-migration-complete";
	private static final String HEADER = "FROM_SCRATCH_OFFLINE_MIGRATION_V1\n";
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private BankRunCodec codec;
	private BankRepository repository;
	private PlaytimeRepository playtime;

	@Before public void setup() throws Exception
	{
		root = Filepath.Unchecked.getRooted(temporary.newFolder("from-scratch").toPath());
		codec = new BankRunCodec(SnapshotFixtures.gson());
		repository = new BankRepository(codec, SnapshotFixtures.codec());
		playtime = new PlaytimeRepository(codec);
	}

	@Test public void freshInstallationRequiresExplicitDecisionWithoutCreatingProgression() throws Exception
	{
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertFiles(before, files());
		assertFalse(root.joinSegment("characters").exists());
	}

	@Test public void confirmedFreshInstallationSurvivesRestartAndCanStartNormally() throws Exception
	{
		CanonicalStorageGate first = gate();
		first.confirmFreshInstallation();
		assertEquals(root, first.get());
		BankRun run = funded(first.get(), SnapshotFixtures.CHARACTER_A);
		assertTrue(run.isActive());
		assertEquals(root, gate().get());
		assertEquals(run, repository.load(gate().get(), run.getCharacter()).get());
	}

	@Test public void validReceiptRestoresActiveRunGpOwnershipBuybacksAndHistory() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		run = repository.save(root, run, BuybackModelTest.buy(run, "fire_cape"));
		receipt();
		Map<String, byte[]> before = files();
		Filepath verified = gate().get();
		assertEquals(root, verified);
		assertFiles(before, files());
		BankRun loaded = repository.load(verified, run.getCharacter()).get();
		assertEquals(run, loaded);
		assertTrue(loaded.isActive());
		assertEquals(1500000L, BuybackEngine.available(loaded));
		assertEquals(1000000L, loaded.getBuybacks().getCommittedGp());
		assertEquals(1L, (long) BuybackEngine.balances(loaded).get(ItemID.TZHAAR_CAPE_FIRE));
		assertEquals(run.getBuybacks().getPurchases(), loaded.getBuybacks().getPurchases());
		BankHistory.Page history = repository.history(verified, loaded, null, false);
		assertFalse(history.getRows().isEmpty());
		assertTrue(history.getRows().stream().anyMatch(row -> row.getItemId() == ItemID.COINS && row.getChange() == 2500000));
	}

	@Test public void existingPlaytimeUsesTheSameVerifiedCanonicalRoot() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Playtime initial = playtime.load(root, run.getCharacter(), run.getRunId());
		Playtime stored = playtime.add(root, initial, 1234, UUID.randomUUID());
		receipt();
		CanonicalStorageGate shared = gate();
		Filepath economicRoot = shared.get();
		Filepath timeRoot = shared.get();
		assertEquals(economicRoot, timeRoot);
		assertEquals(run, repository.load(economicRoot, run.getCharacter()).get());
		assertEquals(stored, playtime.load(timeRoot, run.getCharacter(), run.getRunId()));
		assertEquals(1234, stored.getTicks());
	}

	@Test public void migratedCharactersRemainIsolated() throws Exception
	{
		BankRun first = funded(root, SnapshotFixtures.CHARACTER_A);
		BankRun second = repository.save(root, null, BankEngine.start(SnapshotFixtures.CHARACTER_B, null,
			BuybackModelTest.bank(ItemID.DRAGON_BONES), true, UUID.randomUUID(), SnapshotFixtures.TIME));
		receipt();
		Filepath verified = gate().get();
		assertEquals(first, repository.load(verified, first.getCharacter()).get());
		assertEquals(second, repository.load(verified, second.getCharacter()).get());
		assertEquals(0, second.balance(ItemID.COINS));
		assertTrue(second.getBuybacks().getPurchases().isEmpty());
	}

	@Test public void existingProgressionWithoutReceiptFailsWithoutWrites() throws Exception
	{
		funded(root, SnapshotFixtures.CHARACTER_A);
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		expectFailure(() -> gate().confirmFreshInstallation());
		assertFiles(before, files());
	}

	@Test public void malformedMigrationReceiptFailsWithoutChangingEvidence() throws Exception
	{
		funded(root, SnapshotFixtures.CHARACTER_A);
		for (String invalid : new String[]{"", "partial", HEADER + "0\n", HEADER + repeat('A', 64) + "\n", HEADER + repeat('0', 64)})
		{
			root.joinSegment(RECEIPT).write(invalid);
			Map<String, byte[]> before = files();
			expectFailure(() -> gate().get());
			assertFiles(before, files());
		}
	}

	@Test public void validReceiptDoesNotHideMissingCurrentCheckpoint() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		receipt();
		root.join("characters", run.getCharacter(), "current.json").delete();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		expectFailure(() -> gate().confirmFreshInstallation());
		assertFiles(before, files());
		assertFalse(root.join("characters", run.getCharacter(), "current.json").exists());
	}

	@Test public void validReceiptDoesNotHideMissingJournal() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		receipt();
		root.join("characters", run.getCharacter(), "bank-ledger-" + run.getRevision() + ".json").delete();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertFiles(before, files());
	}

	@Test public void oneIncompleteCharacterBlocksTheEntireStartup() throws Exception
	{
		funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath broken = root.join("characters", SnapshotFixtures.CHARACTER_B);
		broken.createDirectories();
		broken.joinSegment("bank-pending-interrupted.json").write("retained incomplete evidence");
		receipt();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertFiles(before, files());
		assertFalse(broken.joinSegment("current.json").exists());
	}

	@Test public void repeatedStartupAcceptsLegitimateCanonicalProgressAfterMigration() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		receipt();
		assertEquals(root, gate().get());
		Map<Integer, Long> physical = new TreeMap<>(run.getPhysical());
		physical.put(ItemID.COINS, 2600000L);
		BankRun next = repository.save(root, run, BankEngine.observe(run,
			new ContainerSnapshot(physical, physical.size()), UUID.randomUUID(), SnapshotFixtures.TIME));
		assertEquals(next, repository.load(gate().get(), next.getCharacter()).get());
		assertEquals(2600000L, next.balance(ItemID.COINS));
	}

	@Test public void conflictingFreshAndMigrationMarkersFailSafely() throws Exception
	{
		gate().confirmFreshInstallation();
		receipt();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertFiles(before, files());
	}

	@Test public void missingPlaytimeCheckpointWithBackupBlocksStartupWithoutInitializingZero() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Playtime initial = playtime.load(root, run.getCharacter(), run.getRunId());
		playtime.add(root, initial, 500, UUID.randomUUID());
		receipt();
		Filepath current = root.join("characters", run.getCharacter(), "playtime", "playtime-" + run.getRunId() + ".json");
		current.delete();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertFiles(before, files());
		assertFalse(current.exists());
	}

	@Test public void blockedMigrationCannotReachControllerFreshChallengeCreation() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		receipt();
		Filepath current = root.join("characters", run.getCharacter(), "current.json");
		current.delete();
		Map<String, byte[]> before = files();
		com.fromscratch.bank.BankFixtures.Source source = new com.fromscratch.bank.BankFixtures.Source();
		com.fromscratch.bank.BankFixtures.Worker worker = new com.fromscratch.bank.BankFixtures.Worker();
		java.util.List<com.fromscratch.bank.BankView> views = new java.util.ArrayList<>();
		com.fromscratch.bank.BankController controller = new com.fromscratch.bank.BankController(source, repository, Runnable::run, views::add);
		try
		{
			controller.start(gate(), worker);
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

	@Test public void freshControllerConfirmationUsesWorkerAndDoesNotAutomaticallyStartChallenge() throws Exception
	{
		com.fromscratch.bank.BankFixtures.Source source = new com.fromscratch.bank.BankFixtures.Source();
		com.fromscratch.bank.BankFixtures.Worker worker = new com.fromscratch.bank.BankFixtures.Worker();
		java.util.List<com.fromscratch.bank.BankView> views = new java.util.ArrayList<>();
		com.fromscratch.bank.BankController controller = new com.fromscratch.bank.BankController(source, repository, Runnable::run, views::add);
		try
		{
			controller.start(gate(), worker);
			worker.runAll();
			assertTrue(views.get(views.size() - 1).isFreshInstallationRequired());
			assertTrue(views.get(views.size() - 1).isError());
			assertFalse(root.joinSegment("characters").exists());
			controller.confirmFreshInstallation();
			assertFalse(root.joinSegment(".from-scratch-installation").exists());
			worker.runAll();
			assertFalse(views.get(views.size() - 1).isError());
			assertFalse(views.get(views.size() - 1).isFreshInstallationRequired());
			assertNull(controller.currentRun());
			assertFalse(root.join("characters", source.character, "current.json").exists());
			for (int i = 0; i < 4; i++) { source.observer.tick(); controller.tick(); worker.runAll(); }
			controller.startRun(true, null, true);
			worker.runAll();
			assertNotNull(controller.currentRun());
			assertTrue(controller.currentRun().isActive());
		}
		finally { controller.stop(); }
	}

	@Test public void acknowledgedUnstartedCharacterWithOnlyRunLockSurvivesRestart() throws Exception
	{
		CanonicalStorageGate first = gate();
		first.confirmFreshInstallation();
		assertFalse(repository.load(first.get(), SnapshotFixtures.CHARACTER_A).isPresent());
		Filepath character = root.join("characters", SnapshotFixtures.CHARACTER_A);
		assertTrue(character.joinSegment("run.lock").exists());
		assertEquals(0, character.joinSegment("run.lock").size());
		assertFalse(character.joinSegment("current.json").exists());
		Map<String, byte[]> before = files();
		assertEquals(root, gate().get());
		assertFiles(before, files());
	}

	@Test public void regularFileAtPlaytimeDirectoryBlocksStartupWithoutChangingEvidence() throws Exception
	{
		BankRun run = funded(root, SnapshotFixtures.CHARACTER_A);
		Filepath invalidFolder = root.join("characters", run.getCharacter(), "playtime");
		invalidFolder.write("unexpected regular-file recovery evidence");
		receipt();
		Map<String, byte[]> before = files();
		expectFailure(() -> gate().get());
		assertTrue(invalidFolder.isFile());
		assertFiles(before, files());
	}

	private CanonicalStorageGate gate() { return new CanonicalStorageGate(() -> root, repository); }

	private BankRun funded(Filepath directory, String character) throws Exception
	{
		BankRun run = repository.save(directory, null, BankEngine.start(character, null,
			BuybackModelTest.bank(ItemID.TZHAAR_CAPE_FIRE), true, UUID.randomUUID(), SnapshotFixtures.TIME));
		Map<Integer, Long> physical = new TreeMap<>(run.getPhysical());
		physical.put(ItemID.COINS, 2500000L);
		return repository.save(directory, run, BankEngine.observe(run,
			new ContainerSnapshot(physical, physical.size()), UUID.randomUUID(), SnapshotFixtures.TIME));
	}
	private void receipt() throws Exception { root.joinSegment(RECEIPT).write(HEADER + repeat('0', 64) + "\n"); }
	private static String repeat(char character, int count)
	{ char[] values = new char[count]; java.util.Arrays.fill(values, character); return new String(values); }
	private Map<String, byte[]> files() throws Exception
	{
		Map<String, byte[]> result = new TreeMap<>();
		try (Stream<Filepath> stream = root.walk())
		{
			for (Filepath file : stream.filter(Filepath::isFile).collect(Collectors.toList()))
			{
				String relative = file.toString().substring(root.toString().length() + 1).replace('\\', '/');
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
	private static void expectFailure(Work action) throws Exception
	{
		try { action.run(); fail("Unverified canonical storage must remain blocked"); }
		catch (IOException expected) { assertNotNull(expected.getMessage()); }
	}
	@FunctionalInterface private interface Work { void run() throws Exception; }
}
