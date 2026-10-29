package com.fromscratch.bank;

import com.fromscratch.playtime.Playtime;
import com.fromscratch.playtime.PlaytimeTracker;
import com.fromscratch.wealth.*;
import java.awt.*;
import java.util.Map;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;

/** Persistent two-column dashboard. Ordinary updates change labels/buttons, never rebuild the component tree. */
public final class BankPanel extends PluginPanel
{
	public static final String START_WARNING = "Your current bank will become your legacy baseline. All virtual bank balances will start at zero.";
	public static final String FRESH_INSTALLATION_WARNING = "Have you used the Friends Beta? Close all RuneLite and From Scratch clients and run the offline migration utility before beginning here.\n\nThis version cannot inspect or detect your old beta directory. Continue only if this is a genuine new installation and you have no beta progression to migrate. This does not migrate or reset any progression.";
	private static final Color PURPLE = new Color(0xB450FF);
	private static final Color BACKGROUND = new Color(30, 28, 25);
	private final CardLayout cards = new CardLayout();
	private final JPanel body = new JPanel(cards)
	{
		@Override public Dimension getPreferredSize()
		{
			// CardLayout normally reserves space for the hidden dashboard, making a short setup scroll.
			for(Component child:getComponents()){if(child.isVisible()){return child.getPreferredSize();}}
			return super.getPreferredSize();
		}
		@Override public Dimension getMinimumSize()
		{
			for(Component child:getComponents()){if(child.isVisible()){return child.getMinimumSize();}}
			return super.getMinimumSize();
		}
	};
	private final JLabel notice = new JLabel();
	private final JCheckBox attested = new BankAttestation();
	private final JLabel bankStep=setupCopy("3. Open your bank.");
	private final JButton start = new BankPrimaryButton("Start From Scratch"), corrections = new JButton(), reset = new BankDangerButton("Reset Progress"), retry = new JButton("Retry Loading"), anchor = new JButton("Review Bank Anchor");
	private final JButton freshInstallation = new JButton("New installation");
	private final JPanel upgrade = vertical();
	private final JPanel stats = tileRow(0, 98);
	private final OutstandingPanel outstanding;
	private final BankIcons icons;
	private final PotionSetupPanel potionSetup=new PotionSetupPanel(this::updateControls);
	private final BankTile unlocked;
	private final BankTile duration;
	private final BankTile gained;
	private final BankTile netWorth;
	private final BankTile bankGp;
	private final BankTile lost;
	private final JLabel automaticSummary = new JLabel("No changes yet", SwingConstants.CENTER), manualSummary = new JLabel("No corrections yet", SwingConstants.CENTER);
	private final JLabel purchasesSummary=new JLabel("0 fills",SwingConstants.CENTER),salesSummary=new JLabel("0 fills",SwingConstants.CENTER);
	private PlaytimeTracker.View playtime;
	private BankView view;
	private BankActions actions;
	private String card;
	private java.util.function.Consumer<Boolean> historyNavigation;
	private Runnable correctionsNavigation;
	private java.util.function.Consumer<Boolean> exchangeNavigation;
	private Confirmation confirmation = BankDialogs::confirm;
	@FunctionalInterface interface Confirmation { boolean show(Component owner,String title,String text,String action,boolean typedReset); }
	public BankPanel() { this(new BankIcons(id -> null)); }
	BankPanel(BankIcons icons, java.util.function.Consumer<Boolean> navigation) { this(icons); historyNavigation = navigation; }
	BankPanel(BankIcons icons, java.util.function.Consumer<Boolean> navigation, Runnable correctionsNavigation) { this(icons, navigation); this.correctionsNavigation = correctionsNavigation; }
	BankPanel(BankIcons icons,java.util.function.Consumer<Boolean> navigation,Runnable correctionsNavigation,java.util.function.Consumer<Boolean> exchangeNavigation){this(icons,navigation,correctionsNavigation);this.exchangeNavigation=exchangeNavigation;}
	BankPanel(BankIcons icons, Confirmation confirmation) { this(icons); this.confirmation = confirmation; }
	public BankPanel(BankIcons icons)
	{
		this.icons=icons;outstanding=new OutstandingPanel(icons);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setBackground(BACKGROUND); body.setOpaque(false); stats.setOpaque(false); stats.setAlignmentX(LEFT_ALIGNMENT);
		for (JButton button : new JButton[]{retry, anchor, freshInstallation}) { button.setAlignmentX(LEFT_ALIGNMENT); button.setBackground(new Color(55, 46, 63)); button.setForeground(new Color(235, 220, 190)); button.setOpaque(true); button.setContentAreaFilled(false); }
		JLabel title = new JLabel("From Scratch"); title.setFont(FontManager.getRunescapeBoldFont()); title.setForeground(new Color(0xFFD54F));
		JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);header.setAlignmentX(LEFT_ALIGNMENT);header.add(title,BorderLayout.CENTER);
		JButton files=new JButton("⋮");files.setToolTipText("Progress files");files.getAccessibleContext().setAccessibleName("Progress files");files.setMargin(new Insets(0,6,0,6));files.addActionListener(e->ProgressDialogs.menu(this,()->actions,()->view).show(files,0,files.getHeight()));header.add(files,BorderLayout.EAST);header.setMaximumSize(new Dimension(Integer.MAX_VALUE,header.getPreferredSize().height));add(header);
		notice.setForeground(new Color(0xFFD54F)); notice.setVisible(false); add(notice); retry.setVisible(false); add(retry);
		freshInstallation.setName("confirmFreshInstallation"); freshInstallation.setVisible(false); add(freshInstallation);
		JPanel setup = vertical(); setup.setName("setupPanel"); setup.add(heading("Before Starting"));
		JLabel intro = setupCopy("1. Bank everything from your inventory.\n2. Unequip and bank all your gear."); intro.setName("setupInstructions"); intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0)); setup.add(intro);
		attested.setName("setupBankingConfirmation");setup.add(attested);
		bankStep.setName("setupBankStep");setup.add(bankStep);setup.add(potionSetup);
		JPanel startRow = centeredAction(start); startRow.setName("startActionRow"); startRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); setup.add(startRow);
		body.add(setup, "setup");
		JPanel active = vertical();
		stats.setName("statisticTiles");
		gained=new BankTile("Peak Progress Value", "Peak Progress<br>Value", ItemID.PLATINUM, icons);stats.add(gained);
		netWorth=new BankTile("Current Bank Net Worth", "Current Bank<br>Net Worth", ItemID.CASKET, icons);stats.add(netWorth);
		bankGp=new BankTile("Bank GP", "Bank GP", ItemID.COINS, icons);stats.add(bankGp);
		unlocked = new BankTile("Items Unlocked", "Items Unlocked", ItemID.CRYSTAL_KEY, icons); stats.add(unlocked);
		duration = new BankTile("Time Played", "Time Played", ItemID.GIANT_STOPWATCH, icons); stats.add(duration);
		lost=new BankTile("Total Value Lost / Spent", "Total Value<br>Lost / Spent", ItemID.ASHES, icons);stats.add(lost);
		lost.onActivate(this::recordExpense);
		active.add(outstanding);active.add(stats);
		upgrade.add(heading("Finish Bank Upgrade")); upgrade.add(description("Open your bank and confirm its comparison point. Your previous save and history are archived.")); upgrade.add(anchor); active.add(upgrade);
		active.add(heading("Legacy Buybacks")); JPanel buybacks=tileRow(1,90);buybacks.setName("legacyBuybackTiles");
		buybacks.add(historyTile("Buyback Shop",ItemID.TZHAAR_CAPE_FIRE,new JLabel("Legacy unlocks",SwingConstants.CENTER),icons,()->openBuybacks(false,icons)));
		buybacks.add(historyTile("Buyback History",ItemID.BH_RUNE_POUCH,new JLabel("Purchases",SwingConstants.CENTER),icons,()->openBuybacks(true,icons)));active.add(buybacks);
		JPanel elsewhere = vertical(); elsewhere.setName("playedElsewhere"); elsewhere.add(heading("Played Elsewhere?"));
		JPanel returnTiles = tileRow(1,116); returnTiles.setName("playedElsewhereTiles");
		JPanel instructions = new JPanel(new BorderLayout()); instructions.setName("elsewhereInstructions"); decorateTile(instructions);
		JLabel help = new JLabel("<html><div style='width:90pt;text-align:center'>Played on mobile or another PC? Bank your items and open your bank here. Your progress updates automatically.</div></html>", SwingConstants.CENTER);
		help.setName("elsewhereHelp"); help.setFont(new Font(Font.DIALOG, Font.PLAIN, 11)); help.setForeground(new Color(216, 205, 181)); help.setVerticalAlignment(SwingConstants.CENTER); instructions.add(help, BorderLayout.CENTER); returnTiles.add(instructions);
		fillTile(corrections, "Manual Bank Corrections", BankIcons.BANK_CORRECTIONS, new JLabel("<html><center>Fix an incorrect<br>count</center></html>", SwingConstants.CENTER), icons); returnTiles.add(corrections); elsewhere.add(returnTiles); active.add(elsewhere);
		active.add(heading("Grand Exchange History"));JPanel exchange=tileRow(1,90);exchange.setName("grandExchangeTiles");
		exchange.add(historyTile("Purchases",ItemID.COINS,purchasesSummary,icons,()->openExchange(true,icons)));
		exchange.add(historyTile("Sales",ItemID.PLATINUM,salesSummary,icons,()->openExchange(false,icons)));active.add(exchange);
		active.add(heading("Bank History")); JPanel history = tileRow(1, 90);
		history.add(historyTile("Automatic Changes", BankIcons.AUTOMATIC_CHANGES, automaticSummary, icons, () -> openHistory(false)));
		history.add(historyTile("Manual Corrections", BankIcons.MANUAL_CORRECTIONS, manualSummary, icons, () -> openHistory(true))); active.add(history);
		JPanel danger = vertical(); danger.setName("resetDangerArea"); danger.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0), BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(121, 55, 55)), BorderFactory.createEmptyBorder(8, 8, 8, 8))));
		JLabel dangerTitle = heading("Reset Progress"); dangerTitle.setForeground(new Color(235, 116, 116)); danger.add(dangerTitle); danger.add(description("Permanently restart your From Scratch challenge.")); Dimension resetSize=reset.getPreferredSize(); reset.setPreferredSize(new Dimension(Math.max(170,resetSize.width),resetSize.height)); danger.add(centeredAction(reset)); active.add(danger);
		body.add(active, "active"); body.setAlignmentX(LEFT_ALIGNMENT); add(body);
		attested.addActionListener(e -> {if(actions!=null&&view!=null&&view.getSetup()!=null){actions.setupAttested(view.getSetup().getAttempt(),attested.isSelected());}updateControls();});
		start.addActionListener(e -> { if (actions != null && potionSetup.ready() && confirmation.show(this, "Start From Scratch?", START_WARNING, "Start Run", false)) { start.setEnabled(false); actions.startRun(attested.isSelected(),potionSetup.selectedCapture(),potionSetup.notUnlocked()); } });
		corrections.addActionListener(e -> { if (correctionsNavigation != null) { correctionsNavigation.run(); } else if (view != null && actions != null) { BankCorrectionDialog.show(this, view, actions,icons); } });
		reset.addActionListener(e -> { BankView captured = view; if (actions != null && captured != null && captured.getRun() != null && confirmation.show(this, "Reset Progress", "This ends your current challenge and clears its active progress. A recovery archive will be kept. Your real OSRS items are untouched.", "Reset Progress", true)) { reset.setEnabled(false);notice.setText("Resetting progress...");notice.setVisible(true); actions.reset(captured.getRun().getRevision()); } });
		retry.addActionListener(e -> { if (actions != null) { actions.retry(); } }); anchor.addActionListener(e -> anchor());
		freshInstallation.addActionListener(e ->
		{
			if (actions != null && view != null && view.isFreshInstallationRequired() && !view.isBusy()
				&& confirmation.show(this, "Confirm a new installation", FRESH_INSTALLATION_WARNING, "I have no beta progression", false))
			{ actions.confirmFreshInstallation(); }
		});
	}
	public void render(BankView updated, BankActions actions)
	{
		if(updated.getSetup()!=null){attested.setSelected(updated.getSetup().isAttested());}
		else if(view!=null&&view.getRun()!=null&&view.getRun().isActive()&&(updated.getRun()==null||!updated.getRun().isActive())){attested.setSelected(false);}
		this.view = updated; this.actions = actions; BankRun run = updated.getRun(); boolean active = run != null && run.isActive();
		attested.setEnabled(!updated.isBusy()&&!updated.isError()&&(updated.getSetup()==null||updated.getSetup().getCharacter()!=null));
		outstanding.render(updated,actions);potionSetup.render(updated,actions);
		boolean recorded=updated.getSetup()==null?updated.getObservation()!=null&&updated.getObservation().isReady():updated.getSetup().getBank()!=null;
		bankStep.setText(setupCopy(recorded?"3. Bank recorded.":"3. Open your bank.").getText());
		String wanted = active ? "active" : "setup"; if (!wanted.equals(card)) { cards.show(body, wanted); card = wanted; body.revalidate(); invalidate(); revalidate(); }
		String message = updated.getMessage(); boolean important = message.equals("Resetting progress...") || message.startsWith("Progress reset.") || updated.isError() || (!active && !updated.ready()) || (active && !updated.isBusy() && !message.equals("Bank progress saved.") && !message.equals("Bank progress updates automatically.") && run.isAnchored());
		notice.setVisible(important); notice.setText(important ? "<html>" + escape(message) + "</html>" : ""); retry.setVisible(updated.isError());
		freshInstallation.setVisible(updated.isFreshInstallationRequired()); freshInstallation.setEnabled(!updated.isBusy());
		if (active)
		{
			unlocked.value(Long.toString(run.unlocked()), "Positive bank item types plus durable purchased equipment slots; shared families count once."); refreshDuration();
			FinancialState financial=run.getFinancial();
			gained.value(updated.getValuation()==null||updated.getValuation().getPeakGp()==null?"Unavailable":"~"+GpFormatter.compact(updated.getValuation().getPeakGp()),BankTooltips.html("Peak Progress Value","Highest recorded virtual wealth plus confirmed permanent progression spending. Based on estimated item prices.","Measured since this feature began. Unpriced or stale checkpoints cannot raise the peak."));
			bankGp.value(GpFormatter.compact(com.fromscratch.buyback.BuybackEngine.available(run)),BankTooltips.bankGp(run));
			BankValuation market=updated.getValuation();
			if(market!=null&&market.getRevision().equals(run.getRevision())){netWorth.value(market.label(),BankTooltips.netWorth(market),market.getMissingTypes()>0&&market.getPricedTypes()>0?"Priced subtotal":null);}
			else{netWorth.value(run.isAnchored()?"Updating…":"Unavailable",BankTooltips.netWorth(null));}
			long geTax=run.getExchange().getEstimatedTaxGp();String loss;
			try{loss=financial.isImportedBaseline()&&financial.getExpenseRecords()==0&&geTax==0&&run.getBuybacks().getCommittedGp()==0?"History incomplete":(geTax>0?"~":"")+GpFormatter.compact(Math.addExact(Math.addExact(financial.getExpenseGp(),geTax),run.getBuybacks().getCommittedGp()));}catch(ArithmeticException ex){loss="Out of range";}
			lost.value(loss,BankTooltips.expenses(run));
			automaticSummary.setText(count(run, false) + " recent"); manualSummary.setText(count(run, true) + " recent");
			purchasesSummary.setText(run.getExchange().getPurchases()+" fills");salesSummary.setText(run.getExchange().getSales()+" fills");
		}
		upgrade.setVisible(active && !run.isAnchored()); updateControls();
	}
	private void updateControls()
	{
		if (view == null) { return; } boolean usable = !view.isBusy() && !view.isError();
		start.setEnabled(view.setupBankReady() && attested.isSelected() && potionSetup.ready()); corrections.setEnabled(usable && view.getRun() != null && view.getRun().isAnchored()); reset.setEnabled(usable && view.getRun() != null && view.getRun().isActive()); anchor.setEnabled(view.ready());
	}
	private void openHistory(boolean manual) { if (historyNavigation != null) { historyNavigation.accept(manual); } else if (actions != null) { BankHistoryDialog.show(this, actions, manual); } }
	private void openExchange(boolean buy,BankIcons icons){if(exchangeNavigation!=null){exchangeNavigation.accept(buy);}else if(actions!=null){com.fromscratch.ge.GeHistoryDialog.show(this,actions,buy,icons);}}
	private void openBuybacks(boolean history,BankIcons icons){if(view!=null&&actions!=null&&!view.isBusy()&&!view.isError()){com.fromscratch.buyback.BuybackDialog.show(this,view,actions,icons,history);}}
	private void recordExpense()
	{
		if(view!=null&&actions!=null&&!view.isBusy()&&!view.isError()&&view.getRun()!=null&&view.getRun().isAnchored()){BankExpenseDialog.show(this,view,actions);}
	}
	public void playtime(PlaytimeTracker.View time) { playtime=time;refreshDuration(); }
	void negativeColor(Color color){outstanding.color(color);if(view!=null){outstanding.render(view,actions);}}
	private void refreshDuration()
	{
		if(view==null||view.getRun()==null||!view.getRun().isActive()) { return; }
		boolean matches=playtime!=null&&playtime.getRun().equals(view.getRun().getRunId())&&playtime.getCharacter().equals(view.getRun().getCharacter());
		duration.value(!matches?"Loading…":playtime.getError()!=null?"Unavailable":Playtime.format(playtime.getTicks()),BankTooltips.html("Time Played",matches&&playtime.getError()!=null?playtime.getError():"Time played using RuneLite on this computer since tracking began. Mobile gameplay is not included."));
	}
	private void anchor()
	{
		BankView captured = view; if (captured == null || !captured.ready()) { return; }
		StringBuilder text = new StringBuilder("Keep saved balances and use this bank as the comparison point? Earlier changes will not be replayed.\n"); boolean excess = false;
		for (Map.Entry<Integer, Long> e : captured.getRun().getBalances().entrySet()) { long p = captured.getObservation().getBank().quantity(e.getKey()); if (e.getValue() > p) { excess = true; text.append(captured.name(e.getKey())).append(": ").append(e.getValue()).append(" → ").append(p).append('\n'); } }
		if (excess) { text.append("Bank missing copies first, or approve the listed reductions."); }
		if (BankDialogs.confirm(this, excess ? "Approve listed reductions and anchor?" : "Confirm bank anchor?", text.toString(), "Confirm Anchor", false)) { actions.anchor(captured.getRun().getRevision(), captured.getObservation().getBank(), excess); }
	}
	private static long count(BankRun run, boolean manual) { return run.getRecent().stream().filter(a -> manual ? a.getKind() == BankAudit.Kind.CORRECTION || a.getKind() == BankAudit.Kind.ANCHOR || a.getKind() == BankAudit.Kind.STORAGE_REVIEW : a.getKind() == BankAudit.Kind.AUTOMATIC).mapToLong(a -> a.getKind()==BankAudit.Kind.STORAGE_REVIEW?1:a.getBalanceChanges().size()).sum(); }
	private static JPanel tileRow(int rows,int height)
	{
		JPanel panel=new JPanel(new GridLayout(rows,2,6,6))
		{
			@Override public Dimension getPreferredSize(){Dimension d=super.getPreferredSize();int count=((GridLayout)getLayout()).getRows()==0?(getComponentCount()+1)/2:1;return new Dimension(d.width,Math.max(d.height,count*height+Math.max(0,count-1)*6));}
			@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
		};panel.setOpaque(false);panel.setAlignmentX(LEFT_ALIGNMENT);return panel;
	}
	private static JPanel vertical() { JPanel p = new JPanel(); p.setBackground(BACKGROUND); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setAlignmentX(LEFT_ALIGNMENT); return p; }
	private static JLabel setupCopy(String text)
	{
		JLabel label=new JLabel("<html><div style='width:200pt'>"+BankTooltips.escape(text).replace("\n","<br>")+"</div></html>");
		label.setFont(new Font(Font.DIALOG,Font.PLAIN,12));label.setForeground(new Color(216,205,181));label.setAlignmentX(LEFT_ALIGNMENT);return label;
	}
	private static JPanel centeredAction(JButton button)
	{
		JPanel row=new JPanel(new FlowLayout(FlowLayout.CENTER,0,4))
		{
			@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
		};row.setOpaque(false);row.setAlignmentX(LEFT_ALIGNMENT);row.add(button);return row;
	}
	private static JLabel heading(String text) { JLabel label = new JLabel(text); label.setForeground(PURPLE); label.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0)); label.setAlignmentX(LEFT_ALIGNMENT); return label; }
	private static JTextArea description(String text) { JTextArea area = new JTextArea(text); area.setFont(new Font(Font.DIALOG, Font.PLAIN, 12)); area.setForeground(new Color(216, 205, 181)); area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false); area.setEditable(false); area.setFocusable(false); area.setSize(170, Short.MAX_VALUE); area.setMaximumSize(new Dimension(Integer.MAX_VALUE, area.getPreferredSize().height)); area.setAlignmentX(LEFT_ALIGNMENT); return area; }
	private static JButton historyTile(String title, int id, JLabel summary, BankIcons icons, Runnable action)
	{
		JButton button = new JButton(); fillTile(button, title, id, summary, icons); button.addActionListener(e -> action.run()); return button;
	}
	private static void decorateTile(JComponent tile)
	{
		tile.setBackground(new Color(43, 39, 33)); tile.setOpaque(true);
		tile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(75, 62, 85)), BorderFactory.createEmptyBorder(4, 3, 4, 3)));
	}
	private static void fillTile(JButton button, String title, int id, JLabel summary, BankIcons icons)
	{
		button.setUI(new javax.swing.plaf.basic.BasicButtonUI()); button.setLayout(new BorderLayout(0, 2)); decorateTile(button); button.setContentAreaFilled(false); button.setRolloverEnabled(true); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setToolTipText(title); button.getAccessibleContext().setAccessibleName(title);
		button.getModel().addChangeListener(e -> { button.setBackground(button.getModel().isRollover() || button.hasFocus() ? new Color(55, 45, 63) : new Color(43, 39, 33)); });
		JLabel icon = new JLabel(); icons.apply(icon, id); button.add(icon, BorderLayout.NORTH);
		JLabel label = new JLabel("<html><center>" + title.replace(" ", "<br>") + "</center></html>", SwingConstants.CENTER); label.setFont(FontManager.getRunescapeSmallFont()); label.setForeground(new Color(216, 205, 181)); button.add(label, BorderLayout.CENTER);
		summary.setFont(FontManager.getRunescapeSmallFont()); summary.setForeground(new Color(0xFFD54F)); button.add(summary, BorderLayout.SOUTH);
	}
	private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
