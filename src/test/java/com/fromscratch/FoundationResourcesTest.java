package com.fromscratch;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.*;

/** Publication assets and the development launcher must agree with plugin identity. */
public class FoundationResourcesTest
{
	@Test
	public void metadataUsesFromScratchIdentityAndStandardBuild() throws Exception
	{
		Properties metadata = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/runelite-plugin.properties"))
		{
			assertNotNull(in);
			metadata.load(in);
		}
		assertEquals("From Scratch", metadata.getProperty("displayName"));
		assertEquals("Pilots r kwl", metadata.getProperty("author"));
		assertEquals(FromScratchPlugin.class.getName(), metadata.getProperty("plugins"));
		assertEquals("standard", metadata.getProperty("build"));
		// Hub derives standard-build versions from the source commit. The optional
		// beta installer independently uses Gradle/toolchain package versions.
		assertNull(metadata.getProperty("version"));
		net.runelite.client.plugins.PluginDescriptor descriptor = FromScratchPlugin.class.getAnnotation(net.runelite.client.plugins.PluginDescriptor.class);
		assertEquals(descriptor.name(), metadata.getProperty("displayName"));
		assertEquals(descriptor.description(), metadata.getProperty("description"));
		assertArrayEquals(descriptor.tags(), metadata.getProperty("tags").split(","));
		for (String tag : descriptor.tags()) { assertTrue("Invalid discovery tag: " + tag, tag.matches("[a-z0-9-]+")); }
	}

	@Test
	public void bundledSidebarIconIsASmallTransparentPng() throws Exception
	{
		BufferedImage icon = readPng("/com/fromscratch/icon.png");
		assertEquals(24, icon.getWidth());
		assertEquals(24, icon.getHeight());
		assertTransparentArtwork(icon);
	}

	@Test
	public void hubIconFitsTheOfficialMaximumDimensions() throws Exception
	{
		BufferedImage icon = readPng("/hub-icon.png");
		assertTrue(icon.getWidth() <= 48);
		assertTrue(icon.getHeight() <= 72);
		assertTransparentArtwork(icon);
	}

	@Test
	public void productionBytecodeTargetsJava11() throws Exception
	{
		try (DataInputStream in = new DataInputStream(getClass().getResourceAsStream("/com/fromscratch/FromScratchPlugin.class")))
		{
			assertEquals(0xCAFEBABE, in.readInt());
			in.readUnsignedShort();
			assertEquals(55, in.readUnsignedShort());
		}
	}

	@Test
	public void presentationHelpDefaultsToVisible()
	{
		assertTrue(new FromScratchConfig() {}.showFoundationHelp());
	}

	private BufferedImage readPng(String resource) throws Exception
	{
		try (DataInputStream in = new DataInputStream(getClass().getResourceAsStream(resource)))
		{
			assertEquals(0x89504E470D0A1A0AL, in.readLong());
		}
		try (InputStream in = getClass().getResourceAsStream(resource))
		{
			BufferedImage image = ImageIO.read(in);
			assertNotNull(image);
			return image;
		}
	}

	private static void assertTransparentArtwork(BufferedImage image)
	{
		assertTrue(image.getColorModel().hasAlpha());
		boolean transparent = false;
		boolean visible = false;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int alpha = image.getRGB(x, y) >>> 24;
				transparent |= alpha == 0;
				visible |= alpha > 0;
			}
		}
		assertTrue("Transparent padding required", transparent);
		assertTrue("Icon cannot be empty", visible);
	}
}
