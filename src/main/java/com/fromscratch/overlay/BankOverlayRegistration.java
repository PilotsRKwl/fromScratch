package com.fromscratch.overlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;

/** Exactly one overlay registration per enabled lifecycle, with immediate render-state cleanup. */
@Singleton
public final class BankOverlayRegistration
{
	private final Runnable add;
	private final Runnable remove;
	private final Runnable prepare;
	private final BankOverlayState state;
	private volatile boolean registered;

	@Inject
	public BankOverlayRegistration(OverlayManager manager, VirtualBankOverlay overlay, PotionStorageOverlay potions, BankOverlayState state, ClientThread clientThread)
	{
		this(() -> {manager.add(overlay);manager.add(potions);}, () -> { manager.remove(overlay);manager.remove(potions); clientThread.invoke(overlay::clearCache); }, overlay::preparePhysicalQuantities, state);
	}
	BankOverlayRegistration(Runnable add, Runnable remove, BankOverlayState state)
	{ this(add, remove, () -> {}, state); }
	private BankOverlayRegistration(Runnable add, Runnable remove, Runnable prepare, BankOverlayState state)
	{
		this.add = add; this.remove = remove; this.prepare = prepare; this.state = state;
	}
	public void beforeRender() { if (registered) { prepare.run(); } }
	public synchronized void start()
	{
		if (!registered) { state.clear(); add.run(); registered = true; }
		state.enable();
	}
	public synchronized void stop()
	{
		state.disable();
		if (registered) { remove.run(); registered = false; }
	}
}
