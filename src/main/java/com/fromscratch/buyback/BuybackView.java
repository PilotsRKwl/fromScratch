package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** Curated responsive cards. Selecting a card never purchases it; confirmation remains a separate action. */
public final class BuybackView extends JPanel
{
	private final BankView view;
	private final BankIcons icons;
	private final JTextField search=new JTextField();
	private final JComboBox<String> category=new JComboBox<>();
	private final JPanel catalog=new JPanel();
	private final java.util.List<BuybackEntry> entries=new ArrayList<>();
	private final java.util.List<JPanel> grids=new ArrayList<>();
	private final JButton purchase=new JButton("Buyback");
	private final JLabel detail=new JLabel("Select an item to view its availability.");
	private BuybackEntry selected;
	private int columns=3;
	public BuybackView(BankView view,BankIcons icons,java.util.function.Consumer<BuybackEntry> confirm)
	{
		super(new BorderLayout(8,8));this.view=view;this.icons=icons;category.addItem("All categories");BuybackCatalog.CATEGORIES.forEach(category::addItem);
		JPanel top=new JPanel(new BorderLayout(4,6));top.add(new JLabel("Available GP: "+GpFormatter.compact(BuybackEngine.available(view.getRun()))),BorderLayout.NORTH);
		JPanel controls=new JPanel(new GridLayout(1,2,8,0));search.setToolTipText("Search item or family name");controls.add(search);controls.add(category);top.add(controls,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
		catalog.setLayout(new BoxLayout(catalog,BoxLayout.Y_AXIS));JScrollPane scroll=new JScrollPane(catalog,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(24);add(scroll,BorderLayout.CENTER);
		scroll.getViewport().addComponentListener(new ComponentAdapter(){@Override public void componentResized(ComponentEvent e){int next=Math.max(1,scroll.getViewport().getWidth()/190);if(next!=columns){columns=next;for(JPanel grid:grids){((GridLayout)grid.getLayout()).setColumns(columns);grid.setMaximumSize(new Dimension(Integer.MAX_VALUE,grid.getPreferredSize().height));}catalog.revalidate();}}});
		JPanel bottom=new JPanel(new BorderLayout(8,8));bottom.add(detail,BorderLayout.CENTER);bottom.add(purchase,BorderLayout.EAST);purchase.setEnabled(false);add(bottom,BorderLayout.SOUTH);
		search.getDocument().addDocumentListener(BankDialogs.changes(this::refresh));category.addActionListener(e->refresh());
		purchase.addActionListener(e->{if(selected!=null&&purchase.isEnabled()){confirm.accept(selected);}});refresh();
	}
	public void filter(String query,String category){search.setText(query);this.category.setSelectedItem(category);refresh();}
	public int rows(){return entries.size();}
	public BuybackEntry selected(){return selected;}
	private void select(BuybackEntry entry)
	{
		selected=entry;String state=displayStatus(entry);detail.setText("<html>"+escape(entry.getName())+"<br>"+escape(state)+"</html>");
		purchase.setEnabled(view.ready()&&"Available".equals(BuybackEngine.status(view.getRun(),entry)));
		for(Component group:catalog.getComponents()){if(group instanceof JPanel){for(Component child:((JPanel)group).getComponents()){if(child instanceof JButton){((JButton)child).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(entry.getId().equals(child.getName())?new Color(0xB450FF):new Color(77,69,80),2),BorderFactory.createEmptyBorder(8,6,8,6)));}}}}
	}
	private String displayStatus(BuybackEntry entry)
	{
		if(!view.ready()){return "Open your bank";}
		return BuybackEngine.catalogStatus(view.getRun(),entry);
	}
	private void refresh()
	{
		catalog.removeAll();grids.clear();entries.clear();selected=null;purchase.setEnabled(false);detail.setText("Select an item to view its availability.");
		String chosen=(String)category.getSelectedItem();
		for(String group:BuybackCatalog.CATEGORIES)
		{
			if(!"All categories".equals(chosen)&&!group.equals(chosen)){continue;}
			JPanel grid=new JPanel(new GridLayout(0,columns,8,8));grid.setAlignmentX(LEFT_ALIGNMENT);
			for(BuybackEntry entry:BuybackCatalog.search(search.getText()))
			{
				if(!group.equals(category(entry))){continue;}entries.add(entry);
				JButton card=new JButton();card.setLayout(new BorderLayout(3,6));card.setName(entry.getId());card.getAccessibleContext().setAccessibleName(entry.getName());card.setBackground(new Color(43,38,47));card.setOpaque(true);card.setForeground(new Color(235,220,190));card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(77,69,80),2),BorderFactory.createEmptyBorder(8,6,8,6)));
				JLabel icon=new JLabel("",SwingConstants.CENTER);icons.apply(icon,entry.getIcon());card.add(icon,BorderLayout.NORTH);
				JLabel text=new JLabel("<html><div style='width:120pt;text-align:center'><b>"+escape(entry.getName())+"</b><br>"+GpFormatter.compact(BuybackCatalog.cost(entry,view.getRun().getBuybacks().getEntitlements()))+"<br>"+escape(displayStatus(entry))+"</div></html>",SwingConstants.CENTER);text.setForeground(card.getForeground());card.add(text,BorderLayout.CENTER);
				card.setToolTipText(displayStatus(entry));card.addActionListener(e->select(entry));grid.add(card);
			}
			if(grid.getComponentCount()>0){JLabel heading=new JLabel(group);heading.setBorder(BorderFactory.createEmptyBorder(10,0,6,0));heading.setAlignmentX(LEFT_ALIGNMENT);catalog.add(heading);catalog.add(grid);grids.add(grid);grid.setMaximumSize(new Dimension(Integer.MAX_VALUE,grid.getPreferredSize().height));}
		}
		catalog.revalidate();catalog.repaint();
	}
	public static String category(BuybackEntry entry){return entry.getCategory();}
	private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
	static void iconRenderer(JTable table)
	{table.setDefaultRenderer(JLabel.class,(t,value,selected,focus,row,column)->{JLabel label=(JLabel)value;label.setOpaque(true);label.setBackground(selected?t.getSelectionBackground():t.getBackground());return label;});}
	public static String confirmation(BankRun run,BuybackEntry entry)
	{
		long price=BuybackCatalog.cost(entry,run.getBuybacks().getEntitlements()),available=BuybackEngine.available(run);
		String explanation=entry.getId().equals("rune_pouch_family")?"Unlocks both Rune Pouch and Divine Rune Pouch for 1M GP. Pay once, even if you upgrade or revert your pouch.":"Unlocks one qualifying legacy copy per listed equipment piece. Real items and coins stay untouched.";
		return entry.getName()+"\nPrice: "+GpFormatter.exact(price)+"\nAvailable GP: "+GpFormatter.exact(available)+"\nRemaining: "+GpFormatter.exact(Math.subtractExact(available,price))+"\n\n"+explanation;
	}
}
