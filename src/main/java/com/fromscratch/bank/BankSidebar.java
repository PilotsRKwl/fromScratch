package com.fromscratch.bank;

import com.fromscratch.FromScratchPlugin;
import javax.inject.*;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.*;
import net.runelite.client.util.ImageUtil;

/** One EDT-owned bank-only navigation panel; no game objects cross this boundary. */
@Singleton
public final class BankSidebar
{
	private final java.util.function.Consumer<NavigationButton> add;
	private final java.util.function.Consumer<NavigationButton> remove;
	private NavigationButton navigation;
	private BankPanel panel;
	private BankActions actions;
	private BankIcons icons;
	private com.fromscratch.playtime.PlaytimeTracker.View playtime;
	private java.awt.Color negativeColor=com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR;
	private final java.util.concurrent.atomic.AtomicReference<BankView> latest = new java.util.concurrent.atomic.AtomicReference<>();
	private final java.util.concurrent.atomic.AtomicBoolean scheduled = new java.util.concurrent.atomic.AtomicBoolean();
	private BankView view = new BankView(null, null, java.util.Collections.emptyMap(), false, false, "Log in to load local progress.");
	@Inject public BankSidebar(ClientToolbar toolbar, BankIcons icons) { this(toolbar::addNavigation, toolbar::removeNavigation); this.icons = icons; }
	BankSidebar(java.util.function.Consumer<NavigationButton> add, java.util.function.Consumer<NavigationButton> remove) { this.add = add; this.remove = remove; }
	public void bind(BankActions actions) { this.actions = actions; }
	public void show()
	{
		if (navigation != null) { return; }
		panel = icons == null ? new BankPanel() : new BankPanel(icons);panel.negativeColor(negativeColor); panel.render(view, actions);panel.playtime(playtime);
		navigation = NavigationButton.builder().tooltip("From Scratch").icon(ImageUtil.loadImageResource(FromScratchPlugin.class, "icon.png")).priority(6).panel(panel).build(); add.accept(navigation);
	}
	public void update(BankView view)
	{
		latest.set(view); schedule();
	}
	private void schedule()
	{
		if (!scheduled.compareAndSet(false, true)) { return; }
		SwingUtilities.invokeLater(() ->
		{
			BankView next = latest.getAndSet(null); if (next != null) { view = next; if (panel != null) { panel.render(view, actions); BankTimings.mark(view.getTrace(), BankTimings.Phase.SWING); } }
			scheduled.set(false); if (latest.get() != null) { schedule(); }
		});
	}
	public void playtime(com.fromscratch.playtime.PlaytimeTracker.View time)
	{ SwingUtilities.invokeLater(()->{playtime=time;if(panel!=null) { panel.playtime(time); }}); }
	public void negativeColor(java.awt.Color color)
	{SwingUtilities.invokeLater(()->{negativeColor=color;if(panel!=null){panel.negativeColor(color);}});}
	public void hide() { latest.set(null);playtime=null; if (navigation != null) { remove.accept(navigation); navigation = null; panel = null; } view = new BankView(null, null, java.util.Collections.emptyMap(), false, false, "Log in to load local progress."); }
}
