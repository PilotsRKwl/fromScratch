package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.IntConsumer;
import javax.swing.*;

/** One exact physical variant per choice. Tiles are selection only, never purchase actions. */
public final class SelectedBuybackView extends JPanel
{
	private final JButton proceed=new JButton("Continue");
	private final Map<Integer,JButton> tiles=new LinkedHashMap<>();
	private int selected=-1;
	private boolean submitted;
	public JButton getContinueButton(){return proceed;}
	public int selected(){return selected;}
	public SelectedBuybackView(BankView view,BuybackEntry entry,BankIcons icons,IntConsumer accepted)
	{
		super(new BorderLayout(8,8));
		add(new JLabel("Choose one legacy copy. Price: "+GpFormatter.exact(entry.getPrice())),BorderLayout.NORTH);
		JPanel grid=new JPanel(new GridLayout(0,3,8,8));JPanel holder=new JPanel(new BorderLayout());holder.add(grid,BorderLayout.NORTH);
		JScrollPane scroll=new JScrollPane(holder,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(24);add(scroll,BorderLayout.CENTER);
		scroll.getViewport().addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){((GridLayout)grid.getLayout()).setColumns(Math.max(1,scroll.getViewport().getWidth()/210));grid.revalidate();}});
		BankRun run=view.getRun();Map<Integer,Long> balances=BuybackEngine.balances(run);
		for(int id:SelectedBuybacks.items(entry.getId()))
		{
			if(run.physical(id)<=0){continue;}
			long remaining=BuybackEngine.remaining(run,id);boolean eligible=view.ready()&&remaining>0&&BuybackEngine.available(run)>=entry.getPrice();
			JButton tile=new JButton();tile.setName("selected-copy-"+id);tile.setLayout(new BorderLayout(3,6));tile.setBackground(new Color(43,38,47));tile.setForeground(new Color(235,220,190));tile.setOpaque(true);
			JLabel icon=new JLabel();icons.apply(icon,id);tile.add(icon,BorderLayout.NORTH);
			String name=view.name(id);if(name.startsWith("Item #")){name=SelectedBuybacks.name(id);}
			tile.getAccessibleContext().setAccessibleName(name);
			tile.add(new JLabel("<html><div style='width:135pt;text-align:center'><b>"+escape(name)+"</b><br>In bank: "+run.physical(id)+"<br>Available: "+balances.getOrDefault(id,0L)+"<br>Purchased: "+run.getBuybacks().getEntitlements().getOrDefault(SelectedBuybacks.role(id),0)+"<br>Legacy copies available: "+remaining+"<br>"+GpFormatter.exact(entry.getPrice())+"</div></html>",SwingConstants.CENTER),BorderLayout.CENTER);
			tile.setToolTipText(BuybackEngine.selectedStatus(run,entry,id));tile.setEnabled(eligible);tile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(77,69,80),2),BorderFactory.createEmptyBorder(8,6,8,6)));tiles.put(id,tile);grid.add(tile);
			tile.addActionListener(e->{if(submitted||!tile.isEnabled()){return;}selected=id;for(Map.Entry<Integer,JButton> t:tiles.entrySet()){t.getValue().setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(t.getKey()==id?new Color(161,105,218):new Color(77,69,80),2),BorderFactory.createEmptyBorder(8,6,8,6)));}proceed.setEnabled(true);});
		}
		proceed.setEnabled(false);proceed.addActionListener(e->{if(!submitted&&proceed.isEnabled()&&selected>0){submitted=true;proceed.setEnabled(false);accepted.accept(selected);}});
	}
	private static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
