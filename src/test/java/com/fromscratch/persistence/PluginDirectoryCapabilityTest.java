package com.fromscratch.persistence;

import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Scoped storage cannot access unrelated directories. Unchecked roots are disposable fixtures only. */
public class PluginDirectoryCapabilityTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	@Test public void scopedPluginRootCannotReachSibling() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.newFolder("from-scratch").toPath());
		assertTrue(root.isRoot());
		try { root.getParent(); fail("Plugin roots must not expose their parent"); }
		catch (IllegalArgumentException expected) { }
		try { root.join("..", "outside"); fail("Plugin roots must not expose a sibling"); }
		catch (IllegalArgumentException expected) { }
	}
}
