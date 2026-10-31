package com.fromscratch.persistence;

import com.fromscratch.bank.BankController;
import java.io.IOException;
import net.runelite.client.util.Filepath;

/** Shared worker-side storage for official challenges, confined to the plugin's scoped directory. */
public final class ProgressStorage implements BankController.DirectoryProvider
{
	private final BankController.DirectoryProvider directory;
	private final BankRepository repository;
	private Filepath verified;

	public ProgressStorage(BankController.DirectoryProvider directory, BankRepository repository)
	{ this.directory = directory; this.repository = repository; }

	@Override public synchronized Filepath get() throws IOException
	{
		if (verified != null) { return verified; }
		try
		{
			// This fixed child is the permanent home of official runs. Other files remain untouched.
			Filepath root = directory.get().joinSegment("progress");
			repository.verifyStorage(root);
			verified = root;
			return root;
		}
		catch (IOException | RuntimeException ex)
		{ throw new SaveException("Progress storage could not be verified. Preserve its saves and recovery files, close other clients and resolve the storage error. Nothing was reset."); }
	}
}
