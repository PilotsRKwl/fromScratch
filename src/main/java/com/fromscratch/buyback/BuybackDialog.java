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
		else{content=new BuybackView(view,icons,entry->{if(entry.getId().equals("looting_bag_family")){actions.bagPreview(contents->SwingUtilities.invokeLater(()->{
			if(!dialog[0].isDisplayable()){return;}
			BagObservation chosen=confirmBag(owner,view.getRun(),icons,contents);
			if(chosen!=null){dialog[0].dispose();actions.buyBag(view.getRun().getRevision(),UUID.randomUUID(),chosen,purchase->SwingUtilities.invokeLater(()->message(owner,"Looting Bag Unlocked",bagSuccess(purchase,chosen.getNames()))));}
		}));}else if(entry.getId().equals("rune_pouch_family")){actions.pouchPreview(contents->SwingUtilities.invokeLater(()->{if(!dialog[0].isDisplayable()){return;}if(!contents.fresh(java.time.Instant.now())){message(owner,"Rune Pouch Contents",contents.getProblem());return;}if(confirm(owner,view.getRun(),entry,icons,contents)){dialog[0].dispose();actions.buyback(view.getRun().getRevision(),entry.getId(),UUID.randomUUID(),contents,purchase->SwingUtilities.invokeLater(()->message(owner,"Rune Pouch Unlocked",success(purchase))));}}));}else if(confirm(owner,view.getRun(),entry,icons,null)){dialog[0].dispose();actions.buyback(view.getRun().getRevision(),entry.getId(),UUID.randomUUID());}});}
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
	private static BagObservation confirmBag(Component owner,BankRun run,BankIcons icons,BagObservation contents)
	{
		BagObservation[] approved={null};JDialog[] dialog={null};
		BagConfirmation content=new BagConfirmation(run,icons,contents,choice->{approved[0]=choice;dialog[0].dispose();});
		JButton cancel=new JButton("Cancel");JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(cancel);buttons.add(content.getConfirmButton());
		dialog[0]=BankDialogs.dialog(owner,"Unlock Looting Bag?",content,buttons,null);cancel.addActionListener(e->dialog[0].dispose());dialog[0].getRootPane().setDefaultButton(content.getConfirmButton());dialog[0].setVisible(true);return approved[0];
	}
	/** The real confirmation component is also exercised headlessly by purchase pipeline tests. */
	public static final class BagConfirmation extends JPanel
	{
		private final JButton confirm=new JButton("Confirm Buyback");
		private boolean submitted;
		public JButton getConfirmButton(){return confirm;}
		public BagConfirmation(BankRun run,BankIcons icons,BagObservation observed,java.util.function.Consumer<BagObservation> accepted)
		{
			super(new BorderLayout(8,8));
			BagObservation contents=observed!=null&&observed.fresh(java.time.Instant.now())?observed:BagObservation.unknown();
			boolean unknown=contents.getState()==BagObservation.State.UNKNOWN;
			JLabel icon=new JLabel();icons.apply(icon,BuybackCatalog.entry("looting_bag_family").getIcon());add(icon,BorderLayout.WEST);
			add(BankDialogs.text(bagConfirmation(run,contents),40),BorderLayout.CENTER);
			JCheckBox empty=new JCheckBox("I confirm my Looting Bag is empty.");empty.setName("bagEmptyAttestation");if(unknown){add(empty,BorderLayout.SOUTH);}
			long count=0;int form=-1;for(int id:BuybackCatalog.variants("looting_bag_family").keySet()){count=Math.addExact(count,run.physical(id));if(run.physical(id)>0){form=id;}}
			boolean eligible=count==1&&"Available".equals(BuybackEngine.status(run,BuybackCatalog.entry("looting_bag_family")));int bagId=form;
			confirm.setEnabled(eligible&&!unknown);empty.addActionListener(e->confirm.setEnabled(eligible&&empty.isSelected()&&!submitted));
			confirm.addActionListener(e->{if(!submitted&&confirm.isEnabled()){submitted=true;confirm.setEnabled(false);accepted.accept(unknown?BagObservation.playerConfirmedEmpty(bagId):contents);}});
		}
	}
	public static String bagConfirmation(BankRun run,BagObservation contents)
	{return "Looting Bag: 10,000 GP\nAvailable GP: "+GpFormatter.compact(BuybackEngine.available(run))+"\nRemaining: "+GpFormatter.compact(Math.subtractExact(BuybackEngine.available(run),10000))+(contents.getState()==BagObservation.State.UNKNOWN?"\n\nFrom Scratch couldn't read the contents of your Looting Bag.\n\nAny items already inside your Looting Bag are legacy items and must be accounted for separately.\n\nIf it contains items, use its Check or View option, return it to the bank and retry.":contents.getQuantities().isEmpty()?"\n\nYour Looting Bag is empty. No legacy item adjustments are needed.":"\n\nThis unlocks your Looting Bag, but not the old items inside it.\n\nFrom Scratch will record the existing contents as negative virtual bank balances. Empty your bag and bank the items afterward to clear those balances."+bagAdjustments(contents.getQuantities(),contents.getNames()));}
	private static String bagAdjustments(java.util.Map<Integer,Long> contents,java.util.Map<Integer,String> names)
	{StringBuilder text=new StringBuilder();if(!contents.isEmpty()){text.append("\n\nAdjustments added:");contents.forEach((id,n)->text.append('\n').append(names.getOrDefault(id,"Item")).append(": -").append(net.runelite.client.util.QuantityFormatter.formatNumber(n)));}return text.toString();}
	public static String bagSuccess(BuybackPurchase purchase,java.util.Map<Integer,String> names)
	{return purchase.getLegacyRunes().isEmpty()?"Your Looting Bag is unlocked.":"Your Looting Bag is unlocked. Its previous contents have been recorded as legacy adjustments."+bagAdjustments(purchase.getLegacyRunes(),names)+"\n\nEmpty your Looting Bag and deposit the items into your bank to clear the negative balances.";}
	public static JComponent history(BankRun run,BankIcons icons)
	{
		DefaultTableModel model=new DefaultTableModel(new String[]{"","Item / Family","Price paid","Pieces","Purchased","Contents confirmation"},0){public boolean isCellEditable(int r,int c){return false;}public Class<?> getColumnClass(int c){return c==0?JLabel.class:String.class;}};
		DateTimeFormatter dates=DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
		java.util.List<BuybackPurchase> rows=run.getBuybacks().getPurchases();
		JTable table=new JTable(model);BuybackView.iconRenderer(table);
		for(int i=rows.size()-1;i>=0;i--){BuybackPurchase p=rows.get(i);BuybackEntry entry=BuybackCatalog.entry(p.getCatalogId());JLabel icon=new JLabel();icons.apply(icon,entry.getIcon());icon.addPropertyChangeListener("icon",e->table.repaint());String proof=p.getCatalogId().equals("looting_bag_family")?(p.getBagConfirmation()==BagObservation.State.PLAYER_CONFIRMED_EMPTY?"Player confirmed empty":p.getBagConfirmation()==BagObservation.State.VERIFIED_EMPTY?"Verified empty":p.getBagConfirmation()==BagObservation.State.VERIFIED_NONEMPTY?"Verified contents":"Not recorded"):"";model.addRow(new Object[]{icon,entry.getName(),GpFormatter.compact(p.getGp()),Integer.toString(entry.getRoles().size()),dates.format(p.getAt()),proof});}
		table.setRowHeight(38);table.setAutoCreateRowSorter(true);table.getColumnModel().getColumn(0).setMaxWidth(42);table.getColumnModel().getColumn(1).setPreferredWidth(300);table.getColumnModel().getColumn(4).setPreferredWidth(160);return new JScrollPane(table);
	}
}
