package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.wealth.BankValuation;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Narrow-sidebar layout and genuine UI actions, exercised without starting RuneScape. */
public class FinalCorrectionsUiTest
{
	private static BankView setup()
	{ return new BankView(null, new BankObservation(UUID.randomUUID(), 1, SnapshotFixtures.TIME, BankFixtures.bank(4), "Bank ready."), Collections.emptyMap(), false, false, "Bank ready."); }
	private static Component named(BankPanel panel, String name)
	{ return BankPolishTest.all(panel).stream().filter(c -> name.equals(c.getName())).findFirst().get(); }
	private static void layout(Container root)
	{ root.doLayout(); for (Component c : root.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
	@Test public void setupNumbersActionsAndRemovesRedundantStorageParagraph() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(); panel.render(setup(), null); panel.setSize(225, 960); layout(panel);
			assertFalse(BankPolishTest.all(panel).stream().anyMatch(c->"setupStorageNote".equals(c.getName())));
			assertTrue(((JLabel)named(panel,"setupInstructions")).getText().contains("1. Bank everything"));
			assertTrue(((JLabel)named(panel,"setupInstructions")).getText().contains("2. Unequip"));
			assertFalse(named(panel,"potionSetup").isVisible());
			assertTrue(((JLabel)named(panel,"setupBankStep")).getText().contains("3. Bank recorded"));
			for (Component c : BankPolishTest.all(panel)) { if (c instanceof JLabel) { assertFalse(((JLabel)c).getText().contains("Special storage is excluded.")); } }
			JCheckBox attested = (JCheckBox)BankPolishTest.all(panel).stream().filter(c -> c instanceof JCheckBox).findFirst().get();
			JButton start = (JButton)((JPanel)named(panel, "startActionRow")).getComponent(0);
			assertFalse(attested.isSelected()); assertFalse(start.isEnabled()); attested.doClick(0); assertTrue(start.isEnabled());
		});
	}
	@Test public void startHasFourteenPixelsAboveItAndCheckboxStaysNearStepTwo() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(); panel.render(setup(), null); panel.setSize(225, 960); layout(panel);
			Component step = named(panel, "setupBankStep"); JPanel row = (JPanel)named(panel, "startActionRow"); Component start = row.getComponent(0);
			int gap = row.getY() + start.getY() - step.getY() - step.getHeight(); assertTrue("Start gap=" + gap, gap >= 12 && gap <= 16);
			assertTrue(Math.abs(start.getX() * 2 + start.getWidth() - row.getWidth()) <= 1);
			Component intro = named(panel, "setupInstructions"); Component checkbox = BankPolishTest.all(panel).stream().filter(c -> c instanceof JCheckBox).findFirst().get();
			assertTrue(checkbox.getY() - intro.getY() - intro.getHeight() <= 4);
		});
	}
	@Test public void playedElsewhereStatesAutomaticUpdatesAndFitsCenteredEqualTiles() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(); panel.render(BankPolishTest.view(), null); panel.setSize(225, 960); layout(panel);
			JLabel help = (JLabel)named(panel, "elsewhereHelp"); assertTrue(help.getText().contains("Your progress updates automatically."));
			assertEquals(SwingConstants.CENTER, help.getHorizontalAlignment()); assertEquals(SwingConstants.CENTER, help.getVerticalAlignment());
			assertTrue(help.getPreferredSize().height <= help.getHeight()); assertTrue(help.getPreferredSize().width <= help.getWidth());
			JPanel row = (JPanel)named(panel, "playedElsewhereTiles"); assertEquals(2, ((GridLayout)row.getLayout()).getColumns());
			assertEquals(row.getComponent(0).getSize(), row.getComponent(1).getSize()); assertTrue(row.getComponent(1) instanceof JButton);
		});
	}
	private static void assertTileSprite(BankPanel panel, String title, int expectedId)
	{
		JButton tile = (JButton)BankPolishTest.all(panel).stream().filter(c -> c instanceof JButton
			&& title.equals(((JButton)c).getAccessibleContext().getAccessibleName())).findFirst().get();
		JLabel sprite = (JLabel)BankPolishTest.all(tile).stream().filter(c -> c instanceof JLabel
			&& ((JLabel)c).getClientProperty("fromscratch.itemId") != null).findFirst().get();
		assertEquals(expectedId, sprite.getClientProperty("fromscratch.itemId")); assertNotNull(sprite.getIcon());
	}
	@Test public void manualCorrectionsHistoryTileUsesVerifiedHorrorDiary() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Map<Integer, Integer> requested = new HashMap<>(); BankPanel panel = new BankPanel(new BankIcons(id -> { requested.merge(id, 1, Integer::sum); return null; }));
			assertEquals(3846, ItemID.HORROR_DIARY2); assertTileSprite(panel, "Manual Corrections", ItemID.HORROR_DIARY2);
			assertEquals(Integer.valueOf(1), requested.get(ItemID.HORROR_DIARY2));
		});
	}
	@Test public void manualBankCorrectionsActionTileKeepsVerifiedPurplePartyhat() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Map<Integer, Integer> requested = new HashMap<>(); BankPanel panel = new BankPanel(new BankIcons(id -> { requested.merge(id, 1, Integer::sum); return null; }));
			assertEquals(1046, ItemID.PURPLE_PARTYHAT); assertTileSprite(panel, "Manual Bank Corrections", ItemID.PURPLE_PARTYHAT);
			assertNotEquals(BankIcons.MANUAL_CORRECTIONS, BankIcons.BANK_CORRECTIONS);
			assertEquals(Integer.valueOf(1), requested.get(ItemID.PURPLE_PARTYHAT));
		});
	}
	@Test public void allFinancialTilesUseIntegerGpWhileTooltipsKeepExactDetails() throws Exception
	{
		BankRun run = BankEngine.start(SnapshotFixtures.CHARACTER_A, null, BankFixtures.bank(0), true, UUID.randomUUID(), SnapshotFixtures.TIME);
		run = BankEngine.observe(run, BankItems.aggregate(new Item[]{new Item(ItemID.COINS, 1000000)}, id -> false), UUID.randomUUID(), SnapshotFixtures.TIME);
		run = BankEngine.expense(run, 50840, "Confirmed expense", UUID.randomUUID(), SnapshotFixtures.TIME);
		BankView view = new BankView(run, setup().getObservation(), Collections.emptyMap(), false, false, "Bank progress saved.", null, BankValuation.calculate(run, Collections.emptyMap()));
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(); panel.render(view, null); panel.setSize(225, 960); layout(panel);
			int millionTiles = 0, expenseTiles = 0;
			for (Component c : BankPolishTest.all(panel))
			{
				if (c instanceof JLabel)
				{
					JLabel label = (JLabel)c;
					if ("1000K GP".equals(label.getText())) { millionTiles++; assertTrue(label.getToolTipText().contains("1,000,000 GP")); assertTrue(label.getPreferredSize().width <= label.getWidth()); }
					if ("50K GP".equals(label.getText())) { expenseTiles++; assertTrue(label.getToolTipText().contains("50,840 GP")); }
				}
			}
			// No persisted peak was supplied: only Bank GP and Current Bank Net Worth are 1M.
			assertEquals(2, millionTiles); assertEquals(1, expenseTiles);
			assertTrue(BankPolishTest.all(panel).stream().anyMatch(c -> c instanceof JLabel && "Unavailable".equals(((JLabel)c).getText())));
		});
	}
}
