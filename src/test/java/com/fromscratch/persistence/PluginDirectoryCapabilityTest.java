package com.fromscratch.persistence;

import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Evidence for the blocked directory migration, not a migration implementation.
 * Unchecked roots are confined to disposable test directories. */
public class PluginDirectoryCapabilityTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	@Test public void scopedCanonicalRootCannotReachLegacySibling() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.newFolder("from-scratch").toPath());
		assertTrue(root.isRoot());
		try { root.getParent(); fail("Plugin roots must not expose their parent"); }
		catch (IllegalArgumentException expected) { }
		try { root.join("..", "from_scratch"); fail("Plugin roots must not expose a sibling"); }
		catch (IllegalArgumentException expected) { }
	}

	@Test public void legacyMigrationApiRejectsNestedPluginDataPath() throws Exception
	{
		for (String name : new String[]{"plugin-data/from_scratch", "plugin-data\\from_scratch", "plugin-data"})
		{
			try { Filepath.Unchecked.getLegacyPluginDirectory(temporary.getRoot().toPath(), name); fail("Unsupported legacy migration root: " + name); }
			catch (IllegalArgumentException expected) { }
		}
	}
}
