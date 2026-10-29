package com.fromscratch.migration;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Opaque historical/recovery bytes in disposable trees; no RuneLite or account access. */
public class OfflineProgressMigrationTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Path base;
	private Path source;
	private Path destination;

	@Before public void setup() throws Exception
	{
		base = temporary.newFolder("plugin-data").toPath();
		source = Files.createDirectory(base.resolve("from_scratch"));
		destination = base.resolve("from-scratch");
		write(source, "characters/character-a/current.json", "{\"active\":true,\"virtualGp\":500000,\"buybacks\":[\"receipt-a\"]}");
		write(source, "characters/character-a/previous.json", "older checkpoint bytes");
		write(source, "characters/character-a/bank-ledger-revision.json", "journal bytes");
		write(source, "characters/character-a/bank-checkpoint-revision.json", "checkpoint bytes");
		write(source, "characters/character-a/history/item-evidence.json", "signed ownership and item history");
		write(source, "characters/character-a/playtime/playtime-run.json", "{\"ticks\":1500}");
		write(source, "characters/character-a/playtime/playtime-run-backup.json", "prior playtime");
		write(source, "characters/character-a/restored-directory.json", "restored-tree-pointer");
		write(source, "characters/character-a/restored-run/current.json", "restored challenge");
		write(source, "characters/character-a/bank-migration-old/manifest.json", "archive evidence");
		write(source, "characters/character-a/before-import-backup.fsp", "opaque local export bytes");
		write(source, "characters/character-a/run.lock", "");
		write(source, "characters/character-a/playtime/playtime.lock", "");
		write(source, "characters/character-b/current.json", "separate second-character progression");
		write(source, "characters/character-b/failed-save-pending.json", "incomplete recovery evidence is preserved verbatim");
		write(source, "progress-import-interrupted/recovery.bin", "unfinished import evidence");
		Files.createDirectories(source.resolve("characters/character-b/empty-recovery-directory"));
	}

	@Test public void successfulMigrationPreservesCompleteTreeAndCharacterIsolation() throws Exception
	{
		Map<String, byte[]> before = files(source, false);
		OfflineProgressMigration.Result result = OfflineProgressMigration.migrate(base, false);
		assertFalse(result.isDryRun());
		assertFalse(result.isAlreadyComplete());
		assertEquals(files(source, true).size(), result.getCopiedFiles());
		assertCopiedTree();
		assertFilesEqual(before, files(source, false));
		assertFalse(Files.exists(destination.resolve("characters/character-a/run.lock")));
		assertFalse(Files.exists(destination.resolve("characters/character-a/playtime/playtime.lock")));
		assertTrue(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFalse(java.util.Arrays.equals(Files.readAllBytes(destination.resolve("characters/character-a/current.json")),
			Files.readAllBytes(destination.resolve("characters/character-b/current.json"))));
	}

	@Test public void repeatedExecutionUsesCanonicalReceiptWithoutReimportingStaleBeta() throws Exception
	{
		OfflineProgressMigration.migrate(base, false);
		Map<String, byte[]> canonical = files(destination, false);
		write(source, "characters/character-a/current.json", "later play in the obsolete beta");
		write(source, "characters/character-c/current.json", "later character in the obsolete beta");
		OfflineProgressMigration.Result result = OfflineProgressMigration.migrate(base, false);
		assertTrue(result.isAlreadyComplete());
		assertEquals(0, result.getCopiedFiles());
		assertFilesEqual(canonical, files(destination, false));
		assertFalse(Files.exists(destination.resolve("characters/character-c")));
	}

	@Test public void conflictingDestinationIsNeverOverwritten() throws Exception
	{
		write(destination, "characters/character-a/current.json", "different existing challenge");
		Map<String, byte[]> originalSource = files(source, false);
		Map<String, byte[]> originalDestination = files(destination, false);
		expectFailure(() -> OfflineProgressMigration.migrate(base, false));
		assertFilesEqual(originalSource, files(source, false));
		assertFilesEqual(originalDestination, files(destination, false));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
	}

	@Test public void interruptedCopyCanBeRetriedWithoutLosingOriginals() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		AtomicInteger copied = new AtomicInteger();
		expectFailure(() -> OfflineProgressMigration.migrate(base, false, (from, to) ->
		{
			if (copied.incrementAndGet() == 2) { throw new IOException("Disposable interrupted-copy fixture"); }
		}));
		assertEquals(2, copied.get());
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFilesEqual(original, files(source, false));
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
		assertFilesEqual(original, files(source, false));
	}

	@Test public void interruptedFinalPublicationCanResume() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		AtomicInteger published = new AtomicInteger();
		expectFailure(() -> OfflineProgressMigration.migrate(base, false, new OfflineProgressMigration.Hook()
		{
			@Override public void afterCopy(Path from, Path to) { }
			@Override public void afterPublish(Path target) throws IOException
			{
				if (published.incrementAndGet() == 2) { throw new IOException("Disposable interrupted-publication fixture"); }
			}
		}));
		assertEquals(2, published.get());
		assertTrue(Files.isDirectory(destination));
		Map<String, byte[]> partial = files(destination, false);
		assertFalse(partial.isEmpty());
		assertTrue(partial.size() < files(source, true).size());
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFilesEqual(original, files(source, false));
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
		assertTrue(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFilesEqual(original, files(source, false));
	}

	@Test public void corruptedStagedBytesFailVerificationAndRetryRepairsTheCopy() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		expectFailure(() -> OfflineProgressMigration.migrate(base, false, (from, to) ->
		{
			byte[] bytes = Files.readAllBytes(to);
			if (bytes.length > 0) { bytes[0] ^= 1; Files.write(to, bytes); }
		}));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFilesEqual(original, files(source, false));
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
	}

	@Test public void truncatedStagedCopyIsNotDeclaredSuccessful() throws Exception
	{
		expectFailure(() -> OfflineProgressMigration.migrate(base, false, (from, to) -> Files.write(to, new byte[0])));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
	}

	@Test public void originalBytesAndModificationTimesRemainUntouched() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		Map<String, FileTime> modified = new TreeMap<>();
		for (String name : original.keySet()) { modified.put(name, Files.getLastModifiedTime(source.resolve(name))); }
		OfflineProgressMigration.migrate(base, false);
		assertFilesEqual(original, files(source, false));
		for (Map.Entry<String, FileTime> entry : modified.entrySet())
		{
			assertEquals("Source modified: " + entry.getKey(), entry.getValue(), Files.getLastModifiedTime(source.resolve(entry.getKey())));
		}
	}

	@Test public void preexistingIdenticalDestinationFileIsAccepted() throws Exception
	{
		String name = "characters/character-a/current.json";
		Files.createDirectories(destination.resolve(name).getParent());
		Files.write(destination.resolve(name), Files.readAllBytes(source.resolve(name)));
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
	}

	@Test public void emptyCanonicalDirectoryDoesNotPreventMigration() throws Exception
	{
		Files.createDirectory(destination);
		OfflineProgressMigration.migrate(base, false);
		assertCopiedTree();
	}

	@Test public void dryRunWritesNoDestinationStagingOrReceipt() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		Map<String, byte[]> pluginData = files(base, false);
		OfflineProgressMigration.Result result = OfflineProgressMigration.migrate(base, true);
		assertTrue(result.isDryRun());
		assertFalse(Files.exists(destination));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration")));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertFilesEqual(original, files(source, false));
		assertFilesEqual(pluginData, files(base, false));
	}

	@Test public void liveLegacyCharacterLockRefusesMigration() throws Exception
	{
		try (FileChannel channel = FileChannel.open(source.resolve("characters/character-a/run.lock"), StandardOpenOption.WRITE);
			 FileLock ignored = channel.lock())
		{
			expectFailure(() -> OfflineProgressMigration.migrate(base, false));
			assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		}
	}

	@Test public void canonicalChangesAfterCompletionAreNeverReplacedFromStaleBeta() throws Exception
	{
		OfflineProgressMigration.migrate(base, false);
		String name = "characters/character-a/current.json";
		write(destination, name, "canonical progression changed since the migration snapshot");
		Map<String, byte[]> updated = files(destination, false);
		OfflineProgressMigration.Result result = OfflineProgressMigration.migrate(base, false);
		assertTrue(result.isAlreadyComplete());
		assertFilesEqual(updated, files(destination, false));
	}

	@Test public void nonemptyReservedLockFileFailsWithoutDiscardingEvidence() throws Exception
	{
		write(source, "characters/character-a/run.lock", "unexpected nonempty recovery evidence");
		Map<String, byte[]> original = files(source, false);
		expectFailure(() -> OfflineProgressMigration.migrate(base, false));
		assertFilesEqual(original, files(source, false));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
	}

	@Test public void cliMissingOrWrongConfirmationWritesNothing() throws Exception
	{
		Map<String, byte[]> original = files(base, false);
		for (String answer : new String[]{"", "yes\n", "migrate from scratch\n"})
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ByteArrayOutputStream error = new ByteArrayOutputStream();
			int status = runCli(answer, output, error, () -> { });
			assertEquals(2, status);
			assertTrue(output.toString("UTF-8").contains("CANCELED"));
			assertEquals("", error.toString("UTF-8"));
			assertFilesEqual(original, files(base, false));
			assertFalse(Files.exists(destination));
			assertFalse(Files.exists(base.resolve(".from-scratch-migration")));
			assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
		}
	}

	@Test public void cliExactConfirmationAuthorizesVerifiedCopy() throws Exception
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ByteArrayOutputStream error = new ByteArrayOutputStream();
		AtomicInteger guardCalls = new AtomicInteger();
		int status = runCli("MIGRATE FROM SCRATCH\n", output, error, guardCalls::incrementAndGet);
		assertEquals(0, status);
		assertTrue(guardCalls.get() > 0);
		assertTrue(output.toString("UTF-8").contains("SUCCESS"));
		assertEquals("", error.toString("UTF-8"));
		assertTrue(Files.exists(base.resolve(".from-scratch-migration-complete")));
		assertCopiedTree();
	}

	@Test public void cliOpenClientGuardBlocksEvenWithExactConfirmation() throws Exception
	{
		Map<String, byte[]> original = files(base, false);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ByteArrayOutputStream error = new ByteArrayOutputStream();
		int status = runCli("MIGRATE FROM SCRATCH\n", output, error,
			() -> { throw new IOException("Disposable client-open fixture"); });
		assertEquals(1, status);
		assertTrue(error.toString("UTF-8").contains("FAILED"));
		assertFilesEqual(original, files(base, false));
		assertFalse(Files.exists(destination));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration")));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
	}

	@Test public void directoryLinkInsideLegacyTreeIsRejectedWithoutTouchingItsTarget() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		Path outside = temporary.newFolder("outside-link-target").toPath();
		write(outside, "private-evidence.bin", "disposable bytes that must never be imported or changed");
		Map<String, byte[]> outsideBytes = files(outside, false);
		Path link = source.resolve("recovery-link");
		try
		{
			createDirectoryLink(link, outside);
			expectFailure(() -> OfflineProgressMigration.migrate(base, false));
			assertFalse(Files.exists(destination));
			assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
			assertFilesEqual(outsideBytes, files(outside, false));
		}
		finally { Files.deleteIfExists(link); }
		assertFilesEqual(original, files(source, false));
	}

	@Test public void linkedPluginDataAncestorIsRejected() throws Exception
	{
		Map<String, byte[]> original = files(base, false);
		Path link = temporary.getRoot().toPath().resolve("plugin-data-link");
		try
		{
			createDirectoryLink(link, base);
			expectFailure(() -> OfflineProgressMigration.migrate(link, false));
			assertFilesEqual(original, files(base, false));
			assertFalse(Files.exists(destination));
		}
		finally { Files.deleteIfExists(link); }
	}

	@Test public void clientDetectionRecognizesLaunchersWithoutBlockingOrdinaryJava()
	{
		assertTrue(OfflineProgressMigration.isClient("FromScratchBeta.exe", ""));
		assertTrue(OfflineProgressMigration.isClient("C:\\Synthetic\\Programs\\RuneLite.exe", ""));
		assertTrue(OfflineProgressMigration.isClient("java", "-jar /synthetic/RuneLite.jar"));
		assertTrue(OfflineProgressMigration.isClient("java", "net.runelite.launcher.Launcher"));
		assertTrue(OfflineProgressMigration.isClient("javaw.exe", "net.runelite.client.RuneLite"));
		assertTrue(OfflineProgressMigration.isClient("java", "com.fromscratch.beta.BetaLauncher"));
		assertFalse(OfflineProgressMigration.isClient("java", "org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.10"));
		assertFalse(OfflineProgressMigration.isClient("C:\\Synthetic\\Java\\bin\\java.exe", "-version"));
	}

	@Test public void preexistingDestinationHardLinkToSourceIsRejected() throws Exception
	{
		Map<String, byte[]> original = files(source, false);
		String name = "characters/character-a/current.json";
		Path target = destination.resolve(name);
		Files.createDirectories(target.getParent());
		Files.createLink(target, source.resolve(name));
		assertTrue(Files.isSameFile(source.resolve(name), target));
		Map<String, byte[]> previousDestination = files(destination, false);
		expectFailure(() -> OfflineProgressMigration.migrate(base, false));
		assertFilesEqual(original, files(source, false));
		assertFilesEqual(previousDestination, files(destination, false));
		assertFalse(Files.exists(base.resolve(".from-scratch-migration-complete")));
	}

	private static void createDirectoryLink(Path link, Path target) throws Exception
	{
		if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win"))
		{
			// NTFS junctions need no Developer Mode. Both paths are disposable fixtures;
			// cleanup unlinks this exact entry and never recursively deletes through it.
			Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), target.toString())
				.redirectErrorStream(true).start();
			try (java.io.InputStream output = process.getInputStream()) { output.readAllBytes(); }
			assertEquals("Could not create the disposable junction fixture", 0, process.waitFor());
		}
		else { Files.createSymbolicLink(link, target); }
	}

	private int runCli(String answer, ByteArrayOutputStream output, ByteArrayOutputStream error,
		OfflineProgressMigration.OfflineCheck guard) throws IOException
	{
		try (BufferedReader input = new BufferedReader(new StringReader(answer));
			 PrintStream out = new PrintStream(output, true, "UTF-8");
			 PrintStream err = new PrintStream(error, true, "UTF-8"))
		{
			return OfflineProgressMigration.run(new String[]{"--plugin-data", base.toString()}, input, out, err, guard);
		}
	}

	private void assertCopiedTree() throws Exception
	{
		assertFilesEqual(files(source, true), files(destination, false));
		assertEquals(directories(source), directories(destination));
	}

	private static void write(Path root, String relative, String text) throws IOException
	{
		Path path = root.resolve(relative);
		Files.createDirectories(path.getParent());
		Files.write(path, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private static Map<String, byte[]> files(Path root, boolean omitLocks) throws IOException
	{
		Map<String, byte[]> result = new TreeMap<>();
		if (!Files.exists(root)) { return result; }
		try (Stream<Path> entries = Files.walk(root))
		{
			for (Path path : entries.filter(Files::isRegularFile).collect(Collectors.toList()))
			{
				String name = path.getFileName().toString();
				if (omitLocks && (name.equals("run.lock") || name.equals("playtime.lock"))) { continue; }
				result.put(root.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
			}
		}
		return result;
	}

	private static Set<String> directories(Path root) throws IOException
	{
		try (Stream<Path> entries = Files.walk(root))
		{
			return entries.filter(Files::isDirectory).map(root::relativize).map(Path::toString)
				.map(name -> name.replace('\\', '/')).collect(Collectors.toCollection(TreeSet::new));
		}
	}

	private static void assertFilesEqual(Map<String, byte[]> expected, Map<String, byte[]> actual)
	{
		assertEquals("File names differ", expected.keySet(), actual.keySet());
		for (String name : expected.keySet()) { assertArrayEquals("Bytes differ for " + name, expected.get(name), actual.get(name)); }
	}

	private static void expectFailure(Work work) throws Exception
	{
		try { work.run(); fail("Migration must fail safely"); }
		catch (IOException expected) { assertNotNull(expected.getMessage()); }
	}

	@FunctionalInterface private interface Work { void run() throws IOException; }
}
