package com.fromscratch.bank;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankPolishTest
{
	static List<Component> all(Container root) { List<Component> result = new ArrayList<>(); for (Component c : root.getComponents()) { result.add(c); if (c instanceof Container) { result.addAll(all((Container) c)); } } return result; }
	static BankView view() { return new BankView(BankFixtures.run(1), null, Collections.singletonMap(BankFixtures.SHIELD, "Wooden shield"), false, false, "Bank progress saved."); }
	@Test public void sixEqualTilesUseExactlyTwoColumns() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel panel = new BankPanel(); panel.render(view(), null); JPanel grid = (JPanel) all(panel).stream().filter(c -> "statisticTiles".equals(c.getName())).findFirst().get(); GridLayout layout = (GridLayout) grid.getLayout(); assertEquals(2, layout.getColumns()); assertEquals(6, grid.getComponentCount()); grid.setSize(220, 348); grid.doLayout(); assertEquals(grid.getComponent(0).getWidth(), grid.getComponent(1).getWidth()); assertEquals(grid.getComponent(0).getHeight(), grid.getComponent(5).getHeight()); }); }
	@Test public void financialTilesUseImplementedValuesAndNotTrackedForUnobservedLosses() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel panel = new BankPanel(); panel.render(view(), null); long count = all(panel).stream().filter(c -> c instanceof JLabel && "Coming soon".equals(((JLabel) c).getText())).count(); assertEquals(0, count); JPanel tiles=(JPanel)all(panel).stream().filter(c->"statisticTiles".equals(c.getName())).findFirst().get();assertEquals(0,all(tiles).stream().filter(c->c instanceof JScrollPane).count());for(Component c:all(panel)){if(c instanceof JScrollPane){assertTrue(c.getPreferredSize().height<=200);}} }); }
	@Test public void ordinaryRefreshKeepsAllComponentIdentities() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel panel = new BankPanel(); panel.render(view(), null); List<Component> before = all(panel); for (int i = 0; i < 30; i++) { panel.render(view(), null); } assertEquals(before, all(panel)); }); }
	@Test public void historyAndCorrectionsAreInTheRequestedSections() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel panel = new BankPanel(); panel.render(view(), null); JPanel elsewhere = (JPanel) all(panel).stream().filter(c -> "playedElsewhere".equals(c.getName())).findFirst().get(); assertTrue(all(elsewhere).stream().anyMatch(c -> c instanceof JButton && "Manual Bank Corrections".equals(((JButton) c).getAccessibleContext().getAccessibleName()))); assertTrue(all(panel).stream().filter(c -> c instanceof JButton).map(c -> ((JButton)c).getAccessibleContext().getAccessibleName()).anyMatch("Automatic Changes"::equals)); }); }
	@Test public void historyTilesNavigateToSeparateReadonlyViews() throws Exception { SwingUtilities.invokeAndWait(() -> { List<Boolean> opened = new ArrayList<>(); BankPanel panel = new BankPanel(new BankIcons(id -> null), opened::add); panel.render(view(), null); for (String title : new String[]{"Automatic Changes", "Manual Corrections"}) { JButton button = (JButton) all(panel).stream().filter(c -> c instanceof JButton && title.equals(((JButton)c).getAccessibleContext().getAccessibleName())).findFirst().get(); button.doClick(); } assertEquals(Arrays.asList(false,true),opened); }); }
	@Test public void resetIsSeparateAndRequiresExactTypedConfirmation() { assertFalse(BankDialogs.resetConfirmed("")); assertFalse(BankDialogs.resetConfirmed("reset")); assertFalse(BankDialogs.resetConfirmed("RESET ")); assertTrue(BankDialogs.resetConfirmed("RESET")); }
	@Test public void missingSpriteHasFallbackAndIsNotRepeatedlyRequested() { AtomicInteger requests = new AtomicInteger(); BankIcons icons = new BankIcons(id -> { requests.incrementAndGet(); return null; }); JLabel a = new JLabel(), b = new JLabel(); icons.apply(a, 1); icons.apply(b, 1); assertNotNull(a.getIcon()); assertNotNull(b.getIcon()); assertEquals(1, requests.get()); }
	@Test public void spriteFailureStillShowsFallback() { BankIcons icons = new BankIcons(id -> { throw new IllegalStateException("Synthetic missing sprite"); }); JLabel label = new JLabel(); icons.apply(label, 1); assertNotNull(label.getIcon()); }
	@Test public void shortConfirmationWrapsWithoutScrollPane() { JTextArea text = BankDialogs.text(BankPanel.START_WARNING, 42); assertTrue(text.getLineWrap()); assertTrue(text.getWrapStyleWord()); assertTrue(text.getPreferredSize().height > text.getFontMetrics(text.getFont()).getHeight()); assertTrue(text.getPreferredSize().height < 110); assertNull(text.getParent()); }
	@Test public void textReflowsWhenDialogNarrows() { JTextArea text = BankDialogs.text(BankPanel.START_WARNING, 42); text.setSize(400, 200); int wide = text.getPreferredSize().height; text.setSize(220, 200); assertTrue(text.getPreferredSize().height > wide); }
	@Test public void dialogSizeRespectsSmallScreensAndMinimums() { assertEquals(new Dimension(360, 170), BankDialogs.fit(new Dimension(10, 10), new Dimension(1920, 1080))); assertEquals(new Dimension(300, 160), BankDialogs.fit(new Dimension(800, 800), new Dimension(300, 160))); }
	@Test public void firstFollowingTickCapturesAndUnchangedTicksDoNotRescan() { BankFixtures.Source source = new BankFixtures.Source(); source.observer.changed(); assertFalse(source.observe().isReady()); source.observer.tick(); BankObservation observation = source.observe(); assertTrue(observation.isReady()); source.observer.tick(); assertSame(observation, source.observe()); }
	@Test public void sourceWithMissingDataRetriesOnFollowingTick() { BankFixtures.Source source = new BankFixtures.Source(); source.items = null; assertFalse(source.observe().isReady()); source.items = new net.runelite.api.Item[0]; source.observer.tick(); assertTrue(source.observe().isReady()); }
	@Test public void monotonicTraceIncludesAllStagesAndReportsOnlyTwice() { List<String> lines = new ArrayList<>(); java.util.concurrent.atomic.AtomicLong now = new java.util.concurrent.atomic.AtomicLong(); BankTimings.Trace trace = new BankTimings.Trace(() -> now.addAndGet(1_000_000), lines::add); for (BankTimings.Phase phase : BankTimings.Phase.values()) { trace.mark(phase); } assertEquals(2, lines.size()); assertTrue(lines.get(1).contains("SWING=9.000")); trace.mark(BankTimings.Phase.OVERLAY); assertEquals(2, lines.size()); }
	@Test public void offscreenDashboardFitsRuneLiteSidebar() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(); panel.render(view(), null); panel.setSize(225, 960); layout(panel);
			JPanel grid = (JPanel) all(panel).stream().filter(c -> "statisticTiles".equals(c.getName())).findFirst().get();
			JPanel elsewhere = (JPanel) all(panel).stream().filter(c -> "playedElsewhere".equals(c.getName())).findFirst().get();
			assertEquals(grid.getX(), elsewhere.getX());
			for(Component c:all(elsewhere)){if(c instanceof JTextArea){assertTrue("Full help text must fit",((JTextArea)c).getUI().getPreferredSize((JTextArea)c).height<=c.getHeight());}}
			for (Component tile : grid.getComponents()) { assertTrue(tile.getWidth() >= 92); for (Component c : all((Container)tile)) { if (c instanceof JLabel && ((JLabel)c).getText() != null && !((JLabel)c).getText().startsWith("<html>")) { assertTrue("Tile value must fit", c.getPreferredSize().width <= c.getWidth()); } } }
			java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(225,960,java.awt.image.BufferedImage.TYPE_INT_RGB); Graphics2D graphics = image.createGraphics(); panel.paint(graphics); graphics.dispose();
			try { java.nio.file.Path output=java.nio.file.Paths.get("build/reports/dashboard-polish.png");java.nio.file.Files.createDirectories(output.getParent());javax.imageio.ImageIO.write(image,"png",output.toFile()); } catch(java.io.IOException ex){throw new AssertionError(ex);}
		});
	}
	private static void layout(Container c) { c.doLayout(); for(Component child:c.getComponents()){if(child instanceof Container){layout((Container)child);}} }
}
