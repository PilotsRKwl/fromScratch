package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.model.VirtualOwnershipTest;
import com.fromscratch.ui.SetupView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankOverlayStateTest
{
	private static SetupView active()
	{
		return new SetupView(SetupView.Mode.ACTIVE, "Active", SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100).withOwnership(VirtualOwnershipTest.fixture()), null, null);
	}
	@Test public void projectionUsesBankQuantityInsteadOfTotal()
	{
		BankOverlayState state = new BankOverlayState(); state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
		assertEquals("75", state.getProjection().label(ItemID.SHARK));
		assertEquals(100, active().getSaved().getOwnership().total(ItemID.SHARK));
		assertEquals(75, state.getProjection().balance(ItemID.SHARK, false));
		assertEquals("0", state.getProjection().label(ItemID.COINS));
	}
	@Test public void nonActiveLoadingErrorAndLogoutClearRenderData()
	{
		BankOverlayState state = new BankOverlayState(); state.enable();
		for (SetupView.Mode mode : SetupView.Mode.values())
		{
			com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
			if (mode != SetupView.Mode.ACTIVE) { com.fromscratch.overlay.LegacyOverlayFixture.update(state, new SetupView(mode, "state", active().getSaved(), null, null)); assertNull(state.getProjection()); }
		}
	}
	@Test public void disabledPluginRejectsLateCallbacksAndClearsOwnership()
	{
		BankOverlayState state = new BankOverlayState(); state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active()); state.disable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
		assertFalse(state.isEnabled()); assertNull(state.getProjection());
	}
	@Test public void defaultsEnableBothFeaturesWithBrightGreenColor()
	{
		FromScratchConfig config = new FromScratchConfig() { };
		assertTrue(config.bankVirtualQuantities()); assertTrue(config.lockedBankItems());
		assertEquals(new Color(0x1DFF00), config.virtualQuantityColor());
		assertEquals(new Color(0xFF4040),config.negativeVirtualQuantityColor());BankOverlayState state=new BankOverlayState();state.configure(config);assertEquals(config.negativeVirtualQuantityColor(),state.getSettings().quantityColor(-1));assertEquals(config.virtualQuantityColor(),state.getSettings().quantityColor(0));
	}
	@Test public void configurationChangesReplaceSettingsWithoutReloadingOwnership()
	{
		BankOverlayState state = new BankOverlayState(); state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
		BankOverlayState.Projection projection = state.getProjection();
		state.configure(new FromScratchConfig()
		{
			public boolean bankVirtualQuantities() { return false; }
			public boolean lockedBankItems() { return false; }
			public Color virtualQuantityColor() { return Color.CYAN; }
		});
		assertFalse(state.getSettings().isQuantities()); assertFalse(state.getSettings().isDimLocked());
		assertEquals(Color.CYAN, state.getSettings().getColor()); assertSame(projection, state.getProjection());
	}
	@Test public void repeatedEnableDisableHasExactlyOneRegistration()
	{
		BankOverlayState state = new BankOverlayState(); AtomicInteger adds = new AtomicInteger(); AtomicInteger removes = new AtomicInteger();
		BankOverlayRegistration registration = new BankOverlayRegistration(adds::incrementAndGet, removes::incrementAndGet, state);
		registration.start(); registration.start(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active()); registration.stop(); registration.stop();
		assertEquals(1, adds.get()); assertEquals(1, removes.get()); assertNull(state.getProjection());
		registration.start(); assertEquals(2, adds.get()); assertNull(state.getProjection()); registration.stop();
	}
	@Test public void onlyTheNormalBankLayerIsRegistered()
	{
		VirtualBankOverlay overlay = overlay(new BankOverlayState(), true, SnapshotFixtures.CHARACTER_A);
		assertEquals(1, overlay.getDrawHooks().size());
		assertTrue(overlay.getDrawHooks().contains(InterfaceID.Bankmain.ITEMS));
	}
	@Test public void closedBankAndWrongCharacterNeverReachRendering()
	{
		BankOverlayState state = new BankOverlayState(); state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
		Graphics2D graphics = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB).createGraphics();
		// The context throws if an inactive/closed/wrong-character pass reads widgets.
		assertNull(overlay(state, false, SnapshotFixtures.CHARACTER_A).render(graphics));
		assertNull(overlay(state, true, SnapshotFixtures.CHARACTER_B).render(graphics));
		state.disable(); assertNull(overlay(state, true, SnapshotFixtures.CHARACTER_A).render(graphics));
		graphics.dispose();
	}
	private static VirtualBankOverlay overlay(BankOverlayState state, boolean visible, String character)
	{
		return new VirtualBankOverlay(new VirtualBankOverlay.Context()
		{
			public String characterKey() { return character; }
			public boolean bankVisible() { return visible; }
			public boolean placeholder(int id) { return false; }
			public Rectangle bankViewport() { throw new AssertionError("Inactive render must not read widgets"); }
			public void forEachItem(Rectangle viewport, VirtualBankOverlay.ItemConsumer consumer) { throw new AssertionError("Inactive render must not visit items"); }
		}, state);
	}
}
