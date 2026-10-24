package com.fromscratch.bank;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/** Persistent committed debt, including absent bank items and dose-normalized storage debt. */
final class OutstandingPanel extends JPanel
{
	private java.util.List<Overdrafts.Outstanding> shown=Collections.emptyList();
	private final BankIcons icons;
	private Color negativeColor=com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR;
	void color(Color color){Color next=color==null?com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR:color;if(!next.equals(negativeColor)){negativeColor=next;shown=null;}}
	OutstandingPanel(BankIcons icons)
	{this.icons=icons;setName("outstandingLegacyItems");setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setAlignmentX(LEFT_ALIGNMENT);setBackground(new Color(63,29,25));setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(205,85,55)),BorderFactory.createEmptyBorder(7,6,7,6)));setVisible(false);}
	void render(BankView view,BankActions actions)
	{
		java.util.List<Overdrafts.Outstanding> rows=Overdrafts.outstanding(view);setVisible(!rows.isEmpty());if(rows.equals(shown)){return;}shown=rows;removeAll();
		JLabel title=new JLabel("\u26a0 LEGACY ITEMS OUTSTANDING");title.setFont(new Font(Font.DIALOG,Font.BOLD,11));title.setForeground(new Color(255,179,104));add(title);
		JPanel list=new JPanel();list.setOpaque(false);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
		for(Overdrafts.Outstanding row:rows)
		{
			JButton entry=new JButton();entry.setName("outstanding-"+row.getItemId());entry.setLayout(new BorderLayout(5,0));entry.setOpaque(false);entry.setContentAreaFilled(false);entry.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));entry.setAlignmentX(LEFT_ALIGNMENT);
			JLabel sprite=new JLabel();icons.apply(sprite,row.getItemId());entry.add(sprite,BorderLayout.WEST);
			String amount=String.format(Locale.ENGLISH,"%,d",row.getQuantity());String unit=row.getUnit().equals("items")?"":" "+row.getUnit();
		JLabel label=new JLabel("<html>"+BankTooltips.escape(row.getName())+"<br><b><font color='#"+String.format(Locale.ENGLISH,"%06x",negativeColor.getRGB()&0xFFFFFF)+"'>"+amount+unit+"</font> outstanding</b></html>");label.setForeground(new Color(255,199,162));entry.add(label,BorderLayout.CENTER);
			entry.setToolTipText("Return or replace "+amount+unit+" to clear this balance. Click for recorded item history.");entry.addActionListener(e->{if(actions!=null){ItemHistoryDialog.show(this,actions,row.getItemId(),row.getName());}});list.add(entry);
		}
		JScrollPane scroll=new JScrollPane(list);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setPreferredSize(new Dimension(210,Math.min(180,rows.size()*46)));add(scroll);revalidate();repaint();
	}
	@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
}
