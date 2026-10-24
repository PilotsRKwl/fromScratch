package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Explicit local purchase confirmation. Closing/canceling either dialog sends no intent. */
public final class BuybackDialog
{
	private BuybackDialog(){}
	public static final String POUCH_INSTRUCTION="Rune Pouch data is not available yet. Keep the bank open, wait a game tick and retry.";
	public static void show(Component owner,BankView view,BankActions actions,BankIcons icons,boolean history)
	{
		if(view.getRun()==null||!view.getRun().isActive()){return;}
		JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton close=new JButton("Close");buttons.add(close);
		JDialog[] dialog=new JDialog[1];JComponent content;
		if(history){content=history(view.getRun(),icons);}
		else{content=new BuybackView(view,icons,entry->{if(entry.getId().equals("looting_bag_family")){actions.bagPreview(contents->SwingUtilities.invokeLater(()->{if(!dialog[0].isDisplayable()){return;}if(!contents.fresh(java.time.Instant.now())){message(owner,"Looting Bag Contents",BagObservation.INSTRUCTION);return;}if(confirmBag(owner,view.getRun(),entry,icons,contents)){dialog[0].dispose();actions.buyBag(view.getRun().getRevision(),UUID.randomUUID(),contents,purchase->SwingUtilities.invokeLater(()->message(owner,"Looting Bag Unlocked",bagSuccess(purchase,contents.getNames()))));}}));}else if(entry.getId().equals("rune_pouch_family")){actions.pouchPreview(contents->SwingUtilities.invokeLater(()->{if(!dialog[0].isDisplayable()){return;}if(!contents.fresh(java.time.Instant.now())){message(owner,"Rune Pouch Contents",contents.getProblem());return;}if(confirm(owner,view.getRun(),entry,icons,contents)){dialog[0].dispose();actions.buyback(view.getRun().getRevision(),entry.getId(),UUID.randomUUID(),contents,purchase->SwingUtilities.invokeLater(()->message(owner,"Rune Pouch Unlocked",success(purchase))));}}));}else if(confirm(owner,view.getRun(),entry,icons,null)){dialog[0].dispose();actions.buyback(view.getRun().getRevision(),entry.getId(),UUID.randomUUID());}});}
		dialog[0]=BankDialogs.dialog(owner,history?"Buyback History":"Legacy Buybacks",content,buttons,new Dimension(700,520));close.addActionListener(e->dialog[0].dispose());dialog[0].setVisible(true);
	}
	private static boolean confirm(Component owner,BankRun run,BuybackEntry entry,BankIcons icons,com.fromscratch.pouch.PouchObservation contents)
	{
		JPanel content=new JPanel(new BorderLayout(8,8));JLabel icon=new JLabel();icons.apply(icon,entry.getIcon());content.add(icon,BorderLayout.WEST);content.add(BankDialogs.text(contents==null?BuybackView.confirmation(run,entry):pouchConfirmation(run,contents),40),BorderLayout.CENTER);
		JButton confirm=new JButton("Confirm Buyback"),cancel=new JButton("Cancel");JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(cancel);buttons.add(confirm);
		JDialog dialog=BankDialogs.dialog(owner,entry.getId().equals("rune_pouch_family")?"Unlock Rune Pouch / Divine Rune Pouch?":"Confirm Legacy Buyback",content,buttons,null);boolean[] approved={false};
		confirm.addActionListener(e->{if(!approved[0]){approved[0]=true;confirm.setEnabled(false);dialog.dispose();}});cancel.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(confirm);dialog.setVisible(true);return approved[0];
	}
	public static String pouchConfirmation(BankRun run,com.fromscratch.pouch.PouchObservation contents)
	{
		return "Cost: 1000K GP\nAvailable GP: "+GpFormatter.compact(BuybackEngine.available(run))+"\nRemaining: "+GpFormatter.compact(Math.subtractExact(BuybackEngine.available(run),1000000))+"\n\nThis unlocks your Rune Pouch, but not the old runes inside it.\n\nFrom Scratch will record those runes as negative bank balances. Empty the pouch and bank its runes afterward to clear them."+adjustments(contents.getQuantities());
	}
	private static String adjustments(java.util.Map<Integer,Long> runes){if(runes.isEmpty()){return "";}StringBuilder text=new StringBuilder("\n\nAdjustments added:");runes.forEach((id,n)->text.append('\n').append(com.fromscratch.pouch.PouchRunes.ids().get(id)).append(": -").append(n));return text.toString();}
	public static String success(BuybackPurchase purchase){return purchase.getLegacyRunes().isEmpty()?"Your Rune Pouch is unlocked.":"Your Rune Pouch is unlocked. Its previous runes have been recorded as legacy adjustments."+adjustments(purchase.getLegacyRunes())+"\n\nNext: Empty your pouch and deposit the runes into your bank. Their negative balances will clear automatically.";}
	private static void message(Component owner,String title,String text){JButton close=new JButton("Close");JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(close);JDialog dialog=BankDialogs.dialog(owner,title,BankDialogs.text(text,40),buttons,null);close.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(close);dialog.setVisible(true);}
	private static boolean confirmBag(Component owner,BankRun run,BuybackEntry entry,BankIcons icons,BagObservation contents)
	{
		JPanel content=new JPanel(new BorderLayout(8,8));JLabel icon=new JLabel();icons.apply(icon,entry.getIcon());content.add(icon,BorderLayout.WEST);content.add(BankDialogs.text(bagConfirmation(run,contents),40),BorderLayout.CENTER);
		JButton confirm=new JButton("Confirm Buyback"),cancel=new JButton("Cancel");JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(cancel);buttons.add(confirm);boolean[] approved={false};
		JDialog dialog=BankDialogs.dialog(owner,"Unlock Looting Bag?",content,buttons,null);confirm.addActionListener(e->{if(!approved[0]){approved[0]=true;confirm.setEnabled(false);dialog.dispose();}});cancel.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(confirm);dialog.setVisible(true);return approved[0];
	}
	public static String bagConfirmation(BankRun run,BagObservation contents)
	{return "Cost: 10K GP\nAvailable GP: "+GpFormatter.compact(BuybackEngine.available(run))+"\nRemaining: "+GpFormatter.compact(Math.subtractExact(BuybackEngine.available(run),10000))+(contents.getQuantities().isEmpty()?"\n\nYour Looting Bag is empty. No legacy item adjustments are needed.":"\n\nThis unlocks your Looting Bag, but not the old items inside it.\n\nFrom Scratch will record the existing contents as negative virtual bank balances. Empty your bag and bank the items afterward to clear those balances."+bagAdjustments(contents.getQuantities(),contents.getNames()));}
	private static String bagAdjustments(java.util.Map<Integer,Long> contents,java.util.Map<Integer,String> names)
	{StringBuilder text=new StringBuilder();if(!contents.isEmpty()){text.append("\n\nAdjustments added:");contents.forEach((id,n)->text.append('\n').append(names.getOrDefault(id,"Item")).append(": -").append(net.runelite.client.util.QuantityFormatter.formatNumber(n)));}return text.toString();}
	public static String bagSuccess(BuybackPurchase purchase,java.util.Map<Integer,String> names)
	{return purchase.getLegacyRunes().isEmpty()?"Your Looting Bag is unlocked.":"Your Looting Bag is unlocked. Its previous contents have been recorded as legacy adjustments."+bagAdjustments(purchase.getLegacyRunes(),names)+"\n\nEmpty your Looting Bag and deposit the items into your bank to clear the negative balances.";}
	public static JComponent history(BankRun run,BankIcons icons)
	{
		DefaultTableModel model=new DefaultTableModel(new String[]{"","Item / Family","Price paid","Pieces","Purchased"},0){public boolean isCellEditable(int r,int c){return false;}public Class<?> getColumnClass(int c){return c==0?JLabel.class:String.class;}};
		DateTimeFormatter dates=DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
		java.util.List<BuybackPurchase> rows=run.getBuybacks().getPurchases();
		JTable table=new JTable(model);BuybackView.iconRenderer(table);
		for(int i=rows.size()-1;i>=0;i--){BuybackPurchase p=rows.get(i);BuybackEntry entry=BuybackCatalog.entry(p.getCatalogId());JLabel icon=new JLabel();icons.apply(icon,entry.getIcon());icon.addPropertyChangeListener("icon",e->table.repaint());model.addRow(new Object[]{icon,entry.getName(),GpFormatter.compact(p.getGp()),Integer.toString(entry.getRoles().size()),dates.format(p.getAt())});}
		table.setRowHeight(38);table.setAutoCreateRowSorter(true);table.getColumnModel().getColumn(0).setMaxWidth(42);table.getColumnModel().getColumn(1).setPreferredWidth(300);table.getColumnModel().getColumn(4).setPreferredWidth(160);return new JScrollPane(table);
	}
}
