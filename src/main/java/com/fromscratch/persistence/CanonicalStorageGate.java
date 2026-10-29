package com.fromscratch.persistence;

import com.fromscratch.bank.BankController;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;
import net.runelite.client.util.Filepath;

/** One worker-side admission gate shared by economic storage and playtime.
 * Uses only the normal canonical plugin directory; never probes beta siblings. */
public final class CanonicalStorageGate implements BankController.DirectoryProvider
{
	public static final String MIGRATION_RECEIPT = ".from-scratch-migration-complete";
	public static final String INSTALLATION_RECEIPT = ".from-scratch-installation";
	private static final String INSTALLATION = "FROM_SCRATCH_CANONICAL_INSTALL_V1\n";
	private static final String MIGRATION = "FROM_SCRATCH_OFFLINE_MIGRATION_V1\n[0-9a-f]{64}\n";
	private final BankController.DirectoryProvider canonical;
	private final BankRepository repository;
	private Filepath verified;

	public CanonicalStorageGate(BankController.DirectoryProvider canonical, BankRepository repository)
	{ this.canonical = canonical; this.repository = repository; }

	@Override public synchronized Filepath get() throws IOException
	{
		if (verified != null) { return verified; }
		try
		{
			Filepath root = canonical.get();
			Filepath migration = root.joinSegment(MIGRATION_RECEIPT), installation = root.joinSegment(INSTALLATION_RECEIPT);
			if (migration.exists() && installation.exists()) { throw blocked("Conflicting installation and migration evidence exists. Preserve all files and resolve the conflict before continuing."); }
			if (migration.exists())
			{
				if (!migration.isFile() || migration.size() > 256 || !BankStorageFiles.readText(migration).matches(MIGRATION))
				{ throw blocked("Migration completion evidence is invalid. Preserve all files, close every client and resolve the migration evidence before continuing."); }
			}
			else if (installation.exists())
			{
				if (!installation.isFile() || installation.size() > 256 || !INSTALLATION.equals(BankStorageFiles.readText(installation)))
				{ throw blocked("New-installation evidence is invalid. Existing files were preserved; no new challenge was created."); }
			}
			else
			{
				if (empty(root)) { throw new FreshInstallationRequired(); }
				throw blocked("Canonical data exists without migration or installation evidence. Close all clients and complete the offline migration; no new challenge was created.");
			}
			repository.verifyCutover(root);
			verified = root;
			return root;
		}
		catch (GuidanceException ex) { throw ex; }
		catch (IOException | RuntimeException ex)
		{ throw blocked("Canonical progression could not be verified. Preserve its saves and recovery files, close other clients and resolve the storage error. Nothing was reset."); }
	}

	/** Called on the storage worker only after the player's explicit UI confirmation. */
	public synchronized void confirmFreshInstallation() throws IOException
	{
		Filepath root = canonical.get();
		if (root.joinSegment(INSTALLATION_RECEIPT).exists()) { get(); return; }
		if (!empty(root)) { throw blocked("Existing canonical files prevent fresh-install confirmation. Complete migration or resolve recovery evidence first."); }
		root.createDirectories();
		Filepath pending = root.joinSegment(".from-scratch-installation-pending-" + UUID.randomUUID());
		BankStorageFiles.writeNew(pending, INSTALLATION);
		pending.moveTo(root.joinSegment(INSTALLATION_RECEIPT), StandardCopyOption.ATOMIC_MOVE);
		get();
	}

	private static boolean empty(Filepath root) throws IOException
	{
		try (Stream<Filepath> entries = root.walk(1))
		{ return root.isDirectory() && entries.allMatch(root::equals); }
		catch (NoSuchFileException absent) { return true; }
	}
	private static GuidanceException blocked(String message) { return new GuidanceException(message); }
	public static class GuidanceException extends SaveException
	{ public GuidanceException(String message) { super(message); } }
	public static final class FreshInstallationRequired extends GuidanceException
	{
		public FreshInstallationRequired()
		{ super("Before starting: existing Friends Beta players must close all clients and run the offline migration utility. This version cannot detect old beta files. Genuine new players may choose New installation."); }
	}
}
