package com.fromscratch.bank;

import java.awt.*;
import javax.swing.*;

/** High-contrast player attestation. Standard checkbox label hit-testing and Space activation remain intact. */
final class BankAttestation extends JCheckBox
{
	BankAttestation()
	{this("<html>I confirm my inventory<br>and equipped items<br>are banked.</html>","I confirm my inventory and equipped items are banked.");}
	BankAttestation(String text,String accessible)
	{
		super(text);
		setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		setForeground(new Color(240, 231, 216));
		setBackground(new Color(42, 35, 49));
		setOpaque(true); setRolloverEnabled(true); setFocusPainted(false);
		setBorderPainted(true);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(100, 78, 119)), BorderFactory.createEmptyBorder(8, 6, 8, 6)));
		setIconTextGap(8); setAlignmentX(LEFT_ALIGNMENT);
		getAccessibleContext().setAccessibleName(accessible);
		Icon indicator = new Indicator();
		setIcon(indicator); setSelectedIcon(indicator); setRolloverIcon(indicator);
		setRolloverSelectedIcon(indicator); setPressedIcon(indicator); setDisabledIcon(indicator); setDisabledSelectedIcon(indicator);
	}
	@Override public Dimension getMaximumSize()
	{
		// Respect actual font/HTML height instead of stretching the confirmation row or clipping scaled text.
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	/** Draw only our Swing control, never any game widget or sprite. */
	private static final class Indicator implements Icon
	{
		public int getIconWidth() { return 22; }
		public int getIconHeight() { return 22; }
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			AbstractButton button = (AbstractButton) component;
			Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				boolean selected = button.isSelected();
				g.setColor(selected ? new Color(94, 43, 128) : new Color(27, 24, 31)); g.fillRect(x + 2, y + 2, 18, 18);
				g.setColor(selected ? new Color(0xB450FF) : button.getModel().isRollover() ? Color.WHITE : new Color(201, 190, 213));
				g.setStroke(new BasicStroke(2)); g.drawRect(x + 2, y + 2, 18, 18);
				if (selected)
				{
					g.setColor(Color.WHITE); g.setStroke(new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
					g.drawPolyline(new int[]{x + 6, x + 10, x + 17}, new int[]{y + 11, y + 15, y + 7}, 3);
				}
				if (button.hasFocus()) { g.setColor(new Color(0xFFD54F)); g.setStroke(new BasicStroke(1)); g.drawRect(x, y, 21, 21); }
			}
			finally { g.dispose(); }
		}
	}
}
