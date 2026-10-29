package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.potion.PotionObservation;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.*;

/** The real sidebar action uses explicit consent; no dialog or game client is opened. */
public class CanonicalStorageGuidanceTest
{
	@Test public void onlyFreshInstallationFailureExposesTheAcknowledgmentAction() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) -> false);
			Probe actions = new Probe();
			panel.render(view(false, false), actions);
			assertFalse(button(panel).isVisible());
			panel.render(view(true, false), actions);
			assertTrue(button(panel).isVisible());
			assertTrue(button(panel).isEnabled());
			panel.render(view(true, true), actions);
			assertFalse(button(panel).isEnabled());
		});
	}

	@Test public void cancelingFreshInstallationConfirmationMakesNoStorageOrStartRequest() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Probe actions = new Probe();
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
			{
				assertTrue(text.contains("offline migration utility"));
				assertTrue(text.contains("cannot inspect or detect your old beta directory"));
				return false;
			});
			panel.render(view(true, false), actions);
			button(panel).doClick(0);
			assertEquals(0, actions.confirmations);
			assertEquals(0, actions.starts);
		});
	}

	@Test public void explicitFreshConfirmationAcknowledgesOnlyAndDoesNotStartAChallenge() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Probe actions = new Probe();
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
			{
				assertEquals("I have no beta progression", action);
				assertTrue(text.contains("genuine new installation"));
				return true;
			});
			panel.render(view(true, false), actions);
			button(panel).doClick(0);
			assertEquals(1, actions.confirmations);
			assertEquals(0, actions.starts);
		});
	}

	private static BankView view(boolean fresh, boolean busy)
	{
		return new BankView(null, null, Collections.emptyMap(), busy, true, "Storage needs review.",
			null, null, PotionObservation.unavailableStore(), null, false, false, null, fresh);
	}
	private static JButton button(Container container)
	{
		JButton result = find(container);
		if (result == null) { throw new AssertionError("Fresh-installation action missing"); }
		return result;
	}
	private static JButton find(Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton && "confirmFreshInstallation".equals(component.getName())) { return (JButton) component; }
			if (component instanceof Container) { JButton nested = find((Container) component); if (nested != null) { return nested; } }
		}
		return null;
	}
	private static final class Probe implements BankActions
	{
		int confirmations, starts;
		@Override public void confirmFreshInstallation() { confirmations++; }
		@Override public void startRun(boolean attested) { starts++; }
		@Override public void anchor(UUID revision, ContainerSnapshot bank, boolean approve) { }
		@Override public void correct(UUID revision, int item, long desired) { }
		@Override public void reset(UUID revision) { }
		@Override public void retry() { }
	}
}
