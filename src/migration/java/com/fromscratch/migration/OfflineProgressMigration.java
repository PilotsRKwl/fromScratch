package com.fromscratch.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Separately executed Java 11 tool. Never included in the RuneLite plugin.
 * Copies opaque progression bytes; does not validate, repair or merge save schemas. */
public final class OfflineProgressMigration
{
	private static final String HEADER = "FROM_SCRATCH_OFFLINE_MIGRATION_V1\n";
	private static final String CONFIRM = "MIGRATE FROM SCRATCH";
	private static final String STAGE = ".from-scratch-migration";
	private static final String COMPLETE = ".from-scratch-migration-complete";
	private static final LinkOption NOFOLLOW = LinkOption.NOFOLLOW_LINKS;
	private OfflineProgressMigration() { }

	public static void main(String[] args)
	{
		int status = run(args, new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
			System.out, System.err, OfflineProgressMigration::requireClientsClosed);
		if (status != 0) { System.exit(status); }
	}

	static int run(String[] args, BufferedReader input, PrintStream out, PrintStream error, OfflineCheck offline)
	{
		try
		{
			boolean dry = false;
			Path base = Paths.get(System.getProperty("user.home"), ".runelite", "plugin-data");
			for (int i = 0; i < args.length; i++)
			{
				if (args[i].equals("--dry-run")) { dry = true; }
				else if (args[i].equals("--plugin-data") && i + 1 < args.length) { base = Paths.get(args[++i]); }
				else if (args[i].equals("--help"))
				{
					out.println("Usage: java -jar from-scratch-migration.jar [--dry-run] [--plugin-data DIRECTORY]");
					out.println("Close all RuneLite/From Scratch clients. Copies from_scratch to from-scratch; never changes originals.");
					return 0;
				}
				else { throw fail("Unknown or incomplete argument. Use --help."); }
			}
			offline.check();
			if (!dry)
			{
				out.println("Close ALL RuneLite and From Scratch clients, including other accounts and launchers.");
				out.println("Source: " + base.toAbsolutePath().normalize().resolve("from_scratch"));
				out.println("Destination: " + base.toAbsolutePath().normalize().resolve("from-scratch"));
				out.println("Originals stay in place. Creates local migration staging/receipt files. No information is transmitted.");
				out.println("Type " + CONFIRM + " to confirm the clients are closed and authorize this copy:");
				if (!CONFIRM.equals(input.readLine())) { out.println("CANCELED: no migration authorized."); return 2; }
			}
			Result result = migrate(base, dry, (a, b) -> { }, offline);
			out.println(result.alreadyComplete ? "ALREADY COMPLETE: canonical progression is authoritative; nothing reimported."
				: dry ? "DRY RUN OK: " + result.copiedFiles + " files would be published. No files changed."
				: "SUCCESS: " + result.copiedFiles + " files published and the entire tree SHA-256 verified. Originals preserved.");
			return 0;
		}
		catch (IOException | SecurityException | IllegalArgumentException ex)
		{
			error.println("FAILED: " + (ex instanceof MigrationException ? ex.getMessage()
				: "Local filesystem/process verification failed (" + ex.getClass().getSimpleName() + "). Originals were not rewritten."));
			return 1;
		}
	}

	/** Core entry points are package-private for isolated tests; the executable requires confirmation. */
	static Result migrate(Path base, boolean dryRun) throws IOException
	{ return migrate(base, dryRun, (a, b) -> { }); }
	static Result migrate(Path base, boolean dryRun, Hook hook) throws IOException
	{ return migrate(base, dryRun, hook, () -> { }); }

	private static Result migrate(Path directory, boolean dryRun, Hook hook, OfflineCheck offline) throws IOException
	{
		Path base = directory.toAbsolutePath().normalize();
		safeAncestors(base);
		requireDirectory(base);
		Path source = base.resolve("from_scratch"), target = base.resolve("from-scratch");
		// The future Hub plugin can read this through its own scoped Filepath root.
		Path receipt = target.resolve(COMPLETE);
		if (completed(receipt, target)) { return new Result(0, true, dryRun); }
		requireDirectory(source);
		Snapshot original = snapshot(source);
		if (original.entries.containsKey(COMPLETE)) { throw fail("Legacy root contains reserved migration metadata. Preserve it and resolve the conflict first."); }
		Snapshot existing = Files.exists(target, NOFOLLOW) ? snapshot(target) : new Snapshot();
		checkSubset(original, existing);
		checkIndependent(source, target, existing);
		if (dryRun)
		{
			try (Locks locks = new Locks())
			{
				locks.acquire(original.locks); locks.acquire(existing.locks);
				offline.check();
				return new Result(original.fileCount() - existing.fileCount(), false, true);
			}
		}
		Path mutex = base.resolve(".from-scratch-migration.lock");
		safeAncestors(mutex);
		try (FileChannel channel = FileChannel.open(mutex, StandardOpenOption.CREATE, StandardOpenOption.WRITE, NOFOLLOW);
			 FileLock lock = tryLock(channel))
		{
			if (completed(receipt, target)) { return new Result(0, true, false); }
			Snapshot currentDestination = Files.exists(target, NOFOLLOW) ? snapshot(target) : new Snapshot();
			checkSubset(original, currentDestination);
			checkIndependent(source, target, currentDestination);
			try (Locks sourceLocks = new Locks())
			{
				sourceLocks.acquire(original.locks); sourceLocks.acquire(currentDestination.locks);
				offline.check();
				if (!original.sameSource(snapshot(source))) { throw fail("Legacy tree changed while acquiring locks. Close every client and retry."); }
				if (!currentDestination.sameSource(Files.exists(target, NOFOLLOW) ? snapshot(target) : new Snapshot()))
				{ throw fail("Destination changed while acquiring locks. Close every client and retry."); }
				Path stage = base.resolve(STAGE), parts = stage.resolve("parts"), payload = stage.resolve("files");
				ensureDirectory(stage); ensureDirectory(parts);
				byte[] manifest = original.manifest();
				Path journal = stage.resolve("manifest");
				if (Files.exists(journal, NOFOLLOW))
				{
					if (!Arrays.equals(readBounded(journal, 64 * 1024 * 1024), manifest))
					{ throw fail("Interrupted transaction belongs to a different legacy snapshot. Preserve staging and resolve before retrying."); }
				}
				else
				{
					if (Files.exists(payload, NOFOLLOW)) { throw fail("Staging payload has no verified manifest. Preserve it for recovery."); }
					publishBytes(manifest, journal, parts);
				}
				ensureDirectory(payload);
				for (Entry entry : original.entries.values())
				{
					Path staged = resolve(payload, entry.name);
					if (entry.directory) { ensureDirectory(staged); continue; }
					ensureDirectory(staged.getParent());
					// Refresh into a new inode even on retry: an existing stage file must
					// never cause a source hard link to become canonical progression.
					Path part = parts.resolve("copy-" + UUID.randomUUID());
					copyFile(resolve(source, entry.name), part);
					hook.afterCopy(resolve(source, entry.name), part);
					if (!matches(part, entry)) { throw fail("Copied file failed SHA-256 verification. Originals preserved; retry the interrupted copy."); }
					// Replace only an owned staging name. Never truncate an existing inode.
					safeAncestors(staged);
					if (Files.isDirectory(staged, NOFOLLOW)) { throw fail("Staging file path is occupied by a directory; preserve the transaction for recovery."); }
					Files.move(part, staged, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				}
				if (!original.same(snapshot(payload)) || !original.sameSource(snapshot(source)))
				{ throw fail("Legacy/staged tree changed or is incomplete. No completion receipt was written."); }
				offline.check();
				ensureDirectory(target);
				int copied = 0;
				for (Entry entry : original.entries.values())
				{
					Path destination = resolve(target, entry.name);
					if (entry.directory) { ensureDirectory(destination); continue; }
					ensureDirectory(destination.getParent());
					if (Files.exists(destination, NOFOLLOW))
					{
						if (!matches(destination, entry)) { throw fail("CONFLICT: destination data differs. Nothing was overwritten."); }
						continue;
					}
					publishLink(resolve(payload, entry.name), destination);
					if (!matches(destination, entry)) { throw fail("Published file failed SHA-256 verification. No completion receipt was written."); }
					hook.afterPublish(destination);
					copied++;
				}
				if (!original.same(snapshot(target)) || !original.sameSource(snapshot(source)))
				{ throw fail("Final tree verification failed. Preserve both trees and retry only with all clients closed."); }
				offline.check();
				publishBytes((HEADER + hex(hash().digest(manifest)) + "\n").getBytes(StandardCharsets.UTF_8), receipt, parts);
				if (!completed(receipt, target)) { throw fail("Completion receipt could not be verified."); }
				return new Result(copied, false, false);
			}
		}
	}

	private static boolean completed(Path receipt, Path target) throws IOException
	{
		if (!Files.exists(receipt, NOFOLLOW)) { return false; }
		String text = new String(readBounded(receipt, 256), StandardCharsets.UTF_8);
		if (!text.matches(HEADER + "[0-9a-f]{64}\\n")) { throw fail("Completion receipt is invalid. Preserve all migration evidence; no reimport was attempted."); }
		requireDirectory(target);
		// Genesis hashes must NOT reject legitimate later canonical progression.
		return true;
	}

	private static Snapshot snapshot(Path root) throws IOException
	{
		requireDirectory(root);
		Snapshot result = new Snapshot();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		{
			@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				checkType(attrs); safeAncestors(dir);
				if (!dir.equals(root)) { result.add(new Entry(relative(root, dir), true, 0, "")); }
				return FileVisitResult.CONTINUE;
			}
			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				checkType(attrs); safeAncestors(file);
				if (!attrs.isRegularFile()) { throw fail("Unsupported non-regular file. Nothing was replaced."); }
				String name = file.getFileName().toString();
				if (name.equals("run.lock") || name.equals("playtime.lock"))
				{
					if (attrs.size() != 0) { throw fail("Nonempty operational lock file contains unexpected evidence; migration refused."); }
					result.locks.add(file); return FileVisitResult.CONTINUE;
				}
				result.add(new Entry(relative(root, file), false, attrs.size(), digest(file)));
				return FileVisitResult.CONTINUE;
			}
		});
		return result;
	}

	private static void checkSubset(Snapshot source, Snapshot destination) throws IOException
	{
		for (Entry entry : destination.entries.values())
		{
			if (!entry.equals(source.entries.get(entry.name)))
			{ throw fail("CONFLICT: destination contains different or additional data. Nothing was overwritten."); }
		}
	}
	private static void checkIndependent(Path source, Path target, Snapshot destination) throws IOException
	{
		for (Entry entry : destination.entries.values())
		{
			if (!entry.directory && Files.isSameFile(resolve(source, entry.name), resolve(target, entry.name)))
			{ throw fail("CONFLICT: an existing destination file aliases the original. An independent copy is required."); }
		}
	}

	private static void safeAncestors(Path path) throws IOException
	{
		Path absolute = path.toAbsolutePath().normalize(), current = absolute.getRoot();
		checkType(Files.readAttributes(current, BasicFileAttributes.class, NOFOLLOW));
		for (Path segment : absolute)
		{
			current = current.resolve(segment);
			try { checkType(Files.readAttributes(current, BasicFileAttributes.class, NOFOLLOW)); }
			catch (NoSuchFileException absent) { return; }
		}
	}
	private static void checkType(BasicFileAttributes attributes) throws IOException
	{
		// Windows junctions can report isDirectory=true AND isOther=true.
		if (attributes.isSymbolicLink() || attributes.isOther()) { throw fail("Symlink, reparse point or special file rejected. Originals preserved."); }
	}
	private static void requireDirectory(Path path) throws IOException
	{
		safeAncestors(path);
		if (!Files.isDirectory(path, NOFOLLOW)) { throw fail("Required progression directory is missing or is not a normal directory."); }
	}
	private static void ensureDirectory(Path path) throws IOException
	{
		safeAncestors(path);
		if (!Files.exists(path, NOFOLLOW))
		{
			if (path.getParent() != null) { ensureDirectory(path.getParent()); }
			try { Files.createDirectory(path); } catch (FileAlreadyExistsException raced) { /* Verify below. */ }
		}
		requireDirectory(path);
	}
	private static Path resolve(Path root, String name) throws IOException
	{
		Path relative = Paths.get(name);
		Path value = root.resolve(relative).normalize();
		if (relative.isAbsolute() || !value.startsWith(root) || value.equals(root)) { throw fail("Unsafe relative migration path rejected."); }
		safeAncestors(value);
		return value;
	}
	private static String relative(Path root, Path path) { return root.relativize(path).toString().replace('\\', '/'); }
	private static boolean matches(Path file, Entry expected) throws IOException
	{
		safeAncestors(file);
		if (!Files.exists(file, NOFOLLOW)) { return false; }
		BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class, NOFOLLOW);
		checkType(attrs);
		return attrs.isRegularFile() && attrs.size() == expected.size && digest(file).equals(expected.sha);
	}
	private static void copyFile(Path source, Path target) throws IOException
	{
		safeAncestors(source); safeAncestors(target);
		try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ, NOFOLLOW);
			 FileChannel out = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, NOFOLLOW))
		{
			ByteBuffer buffer = ByteBuffer.allocate(65536);
			while (in.read(buffer) != -1) { buffer.flip(); while (buffer.hasRemaining()) { out.write(buffer); } buffer.clear(); }
			out.force(true);
		}
	}
	private static void publishBytes(byte[] bytes, Path target, Path parts) throws IOException
	{
		Path temporary = parts.resolve("record-" + UUID.randomUUID());
		safeAncestors(temporary);
		try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, NOFOLLOW))
		{
			ByteBuffer data = ByteBuffer.wrap(bytes);
			while (data.hasRemaining()) { channel.write(data); }
			channel.force(true);
		}
		if (!Arrays.equals(readBounded(temporary, bytes.length), bytes)) { throw fail("Migration record verification failed."); }
		publishLink(temporary, target);
	}
	private static void publishLink(Path verified, Path target) throws IOException
	{
		safeAncestors(verified); safeAncestors(target);
		try { Files.createLink(target, verified); }
		catch (FileAlreadyExistsException conflict) { throw fail("CONFLICT: destination appeared during publication. Nothing was overwritten; retry."); }
		catch (UnsupportedOperationException unsupported) { throw fail("Filesystem does not support atomic hard-link publication. Originals preserved."); }
	}
	private static byte[] readBounded(Path path, int maximum) throws IOException
	{
		safeAncestors(path);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, NOFOLLOW))
		{
			if (channel.size() > maximum) { throw fail("Migration record exceeds its safe size limit."); }
			ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(channel.size()));
			while (buffer.hasRemaining()) { if (channel.read(buffer) == -1) { throw fail("Migration record is incomplete."); } }
			return buffer.array();
		}
	}
	private static String digest(Path path) throws IOException
	{
		MessageDigest sha = hash();
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, NOFOLLOW))
		{
			ByteBuffer buffer = ByteBuffer.allocate(65536);
			while (channel.read(buffer) != -1) { buffer.flip(); sha.update(buffer); buffer.clear(); }
		}
		return hex(sha.digest());
	}
	private static MessageDigest hash()
	{ try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); } }
	private static String hex(byte[] value)
	{
		StringBuilder text = new StringBuilder(value.length * 2);
		for (byte b : value) { text.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16)); }
		return text.toString();
	}
	private static FileLock tryLock(FileChannel channel) throws IOException
	{
		try { FileLock lock = channel.tryLock(); if (lock == null) { throw fail("Progression is locked by another process. Close every client and retry."); } return lock; }
		catch (OverlappingFileLockException busy) { throw fail("Progression is locked by another operation. Close every client and retry."); }
	}

	private static void requireClientsClosed() throws IOException
	{
		try (Stream<ProcessHandle> processes = ProcessHandle.allProcesses())
		{
			for (ProcessHandle process : processes.filter(p -> p.pid() != ProcessHandle.current().pid()).collect(Collectors.toList()))
			{
				ProcessHandle.Info info = process.info();
				String command = info.command().orElse("").toLowerCase(Locale.ROOT);
				String arguments = info.arguments().map(a -> String.join(" ", a).toLowerCase(Locale.ROOT)).orElse("");
				String executable = command.replace('\\', '/');
				String basename = executable.substring(executable.lastIndexOf('/') + 1);
				if (isClient(executable, arguments))
				{ throw fail("A RuneLite/From Scratch client or launcher is running. Close all clients before continuing."); }
				if ((basename.equals("java.exe") || basename.equals("javaw.exe") || basename.equals("java"))
					&& !info.arguments().isPresent() && process.isAlive())
				{ throw fail("A Java process cannot be inspected. Close it before migrating; offline status cannot be verified."); }
			}
		}
	}
	static boolean isClient(String command, String arguments)
	{
		String executable = command.toLowerCase(Locale.ROOT).replace('\\', '/');
		String basename = executable.substring(executable.lastIndexOf('/') + 1);
		String args = arguments.toLowerCase(Locale.ROOT);
		return executable.contains("runelite") || basename.equals("fromscratchbeta.exe")
			|| args.contains("net.runelite.client.runelite") || args.contains("net.runelite.launcher.launcher")
			|| args.contains("runelite.jar") || args.contains("com.fromscratch.beta.betalauncher")
			|| args.contains("com.fromscratch.fromscratchplugintest");
	}

	@FunctionalInterface interface Hook
	{
		void afterCopy(Path source, Path target) throws IOException;
		default void afterPublish(Path target) throws IOException { }
	}
	@FunctionalInterface interface OfflineCheck { void check() throws IOException; }
	private static MigrationException fail(String message) { return new MigrationException(message); }
	private static final class MigrationException extends IOException
	{ MigrationException(String message) { super(message); } }
	private static final class Locks implements AutoCloseable
	{
		private final List<FileChannel> channels = new ArrayList<>();
		private final List<FileLock> locks = new ArrayList<>();
		void acquire(List<Path> paths) throws IOException
		{
			for (Path path : paths)
			{
				safeAncestors(path);
				FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, NOFOLLOW);
				channels.add(channel); locks.add(tryLock(channel));
			}
		}
		@Override public void close() throws IOException
		{
			IOException failure = null;
			for (FileChannel channel : channels) { try { channel.close(); } catch (IOException ex) { failure = ex; } }
			if (failure != null) { throw failure; }
		}
	}
	private static final class Snapshot
	{
		private final SortedMap<String, Entry> entries = new TreeMap<>();
		private final List<Path> locks = new ArrayList<>();
		void add(Entry entry) { entries.put(entry.name, entry); }
		int fileCount() { return (int) entries.values().stream().filter(e -> !e.directory).count(); }
		boolean same(Snapshot other) { return entries.equals(other.entries); }
		boolean sameSource(Snapshot other) { return same(other) && new TreeSet<>(locks).equals(new TreeSet<>(other.locks)); }
		byte[] manifest()
		{
			StringBuilder value = new StringBuilder(HEADER);
			for (Entry entry : entries.values())
			{
				value.append(entry.directory ? 'D' : 'F').append('\t')
					.append(Base64.getUrlEncoder().encodeToString(entry.name.getBytes(StandardCharsets.UTF_8))).append('\t')
					.append(entry.size).append('\t').append(entry.sha).append('\n');
			}
			return value.toString().getBytes(StandardCharsets.UTF_8);
		}
	}
	private static final class Entry
	{
		final String name, sha; final boolean directory; final long size;
		Entry(String name, boolean directory, long size, String sha) { this.name = name; this.directory = directory; this.size = size; this.sha = sha; }
		@Override public boolean equals(Object value)
		{
			if (!(value instanceof Entry)) { return false; }
			Entry other = (Entry) value;
			return name.equals(other.name) && directory == other.directory && size == other.size && sha.equals(other.sha);
		}
		@Override public int hashCode() { return Objects.hash(name, directory, size, sha); }
	}
	public static final class Result
	{
		private final int copiedFiles; private final boolean alreadyComplete, dryRun;
		Result(int copiedFiles, boolean alreadyComplete, boolean dryRun) { this.copiedFiles = copiedFiles; this.alreadyComplete = alreadyComplete; this.dryRun = dryRun; }
		public int getCopiedFiles() { return copiedFiles; }
		public boolean isAlreadyComplete() { return alreadyComplete; }
		public boolean isDryRun() { return dryRun; }
	}
}
