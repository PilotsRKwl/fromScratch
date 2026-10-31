package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.potion.PotionObservation;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.*;

/** Guided setup and reset consent over real Swing components; no dialog or game client is opened. */
public class InitialSetupGuidanceTest
{
	@Test public void guidedSetupRequiresBankingAttestationAndOnlyThenConfirmsStart() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Probe actions = new Probe();
			int[] confirmations = {0};
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
			{
				confirmations[0]++;
				assertEquals("Start From Scratch?", title);
				assertEquals(BankPanel.START_WARNING, text);
				assertEquals("Start Run", action);
				assertFalse(typed);
				return true;
			});
			panel.render(setup(false, false, true), actions);
			assertTrue(find(panel, "setupPanel").isVisible());
			assertEquals(0, confirmations[0]);
			JButton start = button(panel, "Start From Scratch");
			assertFalse(start.isEnabled());
			((JCheckBox) find(panel, "setupBankingConfirmation")).doClick(0);
			assertTrue(start.isEnabled());
			start.doClick(0);
			assertEquals(1, confirmations[0]);
			assertEquals(1, actions.starts);
			assertTrue(actions.attested);
			assertFalse(start.isEnabled());
			assertEquals(0, actions.resets);
		});
	}

	@Test public void cancelingStartConfirmationDoesNotRequestAChallenge() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Probe actions = new Probe();
			int[] confirmations = {0};
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
			{
				confirmations[0]++;
				return false;
			});
			panel.render(setup(false, false, true), actions);
			((JCheckBox) find(panel, "setupBankingConfirmation")).doClick(0);
			button(panel, "Start From Scratch").doClick(0);
			assertEquals(1, confirmations[0]);
			assertEquals(0, actions.starts);
			assertEquals(0, actions.resets);
		});
	}

	@Test public void incompleteBusyOrFailedStorageCannotStartAChallenge() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Probe actions = new Probe();
			BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
			{
				fail("An unavailable setup must not request confirmation");
				return false;
			});
			for (BankView state : new BankView[]{setup(false, false, false), setup(true, false, true), setup(false, true, true)})
			{
				((JCheckBox) find(panel, "setupBankingConfirmation")).setSelected(true);
				panel.render(state, actions);
				JButton start = button(panel, "Start From Scratch");
				assertFalse(start.isEnabled());
				start.doClick(0);
			}
			assertEquals(0, actions.starts);
			assertEquals(0, actions.resets);
		});
	}

	@Test public void resetStillRequiresTypedConfirmationAndUsesCapturedRevision() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			for (boolean confirmed : new boolean[]{false, true})
			{
				Probe actions = new Probe();
				int[] confirmations = {0};
				BankRun run = BankFixtures.run(1);
				BankPanel panel = new BankPanel(new BankIcons(id -> null), (owner, title, text, action, typed) ->
				{
					confirmations[0]++;
					assertEquals("Reset Progress", title);
					assertEquals("Reset Progress", action);
					assertTrue(text.contains("recovery archive"));
					assertTrue(typed);
					return confirmed;
				});
				panel.render(new BankView(run, null, Collections.emptyMap(), false, false, "Bank progress saved."), actions);
				button(panel, "Reset Progress").doClick(0);
				assertEquals(1, confirmations[0]);
				assertEquals(confirmed ? 1 : 0, actions.resets);
				assertEquals(confirmed ? run.getRevision() : null, actions.resetRevision);
				assertEquals(0, actions.starts);
			}
		});
	}

	private static BankView setup(boolean busy, boolean error, boolean bankReady)
	{
		BankObservation bank = bankReady ? new BankObservation(UUID.randomUUID(), 1, SnapshotFixtures.TIME,
			BankFixtures.bank(1), "Bank ready.") : null;
		return new BankView(null, bank, Collections.emptyMap(), busy, error, "Open your bank.",
			null, null, PotionObservation.unavailableStore(), null, false, true);
	}
	private static Component find(Container root, String name)
	{
		for (Component component : BankPolishTest.all(root))
		{
			if (name.equals(component.getName())) { return component; }
		}
		throw new AssertionError("Missing component: " + name);
	}
	private static JButton button(Container root, String text)
	{
		for (Component component : BankPolishTest.all(root))
		{
			if (component instanceof JButton && text.equals(((JButton) component).getText())) { return (JButton) component; }
		}
		throw new AssertionError("Missing button: " + text);
	}
	private static final class Probe implements BankActions
	{
		int starts, resets;
		boolean attested;
		UUID resetRevision;
		@Override public void startRun(boolean value) { starts++; attested = value; }
		@Override public void anchor(UUID revision, ContainerSnapshot bank, boolean approve) { }
		@Override public void correct(UUID revision, int item, long desired) { }
		@Override public void reset(UUID revision) { resets++; resetRevision = revision; }
		@Override public void retry() { }
	}
}
