package com.fromscratch.overlay;

import com.fromscratch.model.VirtualOwnership;
import com.fromscratch.model.VirtualLocation;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Paint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.FontManager;

/** Draw-only painting in client coordinates; never writes item/widget/sprite data. */
public final class BankItemPainter
{
	// A configurable dark neutral tint (default #333333 / 50%) mutes the whole item uniformly. Matching
	// the dark bank background more closely avoids a bright gray tile behind the sprite;
	// approximately 50% of original sprite contrast remains. Physical counters are drawn after tinting.
	// Do not redraw a generated item sprite: it can cover those glyphs or mismatch the
	// client's shadow/drag appearance. This fallback needs neither sprite copies nor a cache.
	private static final Font FONT = FontManager.getRunescapeSmallFont();
	private static final int PHYSICAL_INSET = 2;
	public static final Color PHYSICAL_COLOR = new Color(0xA0A0A0);
	private BankItemPainter() { }

	/** Native item-icon thresholds differ from From Scratch's compact virtual labels. */
	static String physicalLabel(int quantity)
	{
		if (quantity < 0) { throw new IllegalArgumentException("Invalid physical quantity"); }
		return quantity < 100_000 ? Integer.toString(quantity) : quantity < 10_000_000 ? quantity / 1_000 + "K" : quantity / 1_000_000 + "M";
	}
	/** Draw after the tint, only when the native counter was suppressed before rendering. No mask. */
	static void paintPhysical(Graphics2D graphics, Rectangle bounds, String label)
	{
		if (bounds.width <= PHYSICAL_INSET * 2 || bounds.height <= PHYSICAL_INSET * 2) { return; }
		Shape clip = graphics.getClip(); Font font = graphics.getFont(); Paint paint = graphics.getPaint();
		try
		{
			graphics.clipRect(bounds.x + PHYSICAL_INSET, bounds.y + PHYSICAL_INSET,
				bounds.width - PHYSICAL_INSET * 2, bounds.height - PHYSICAL_INSET * 2);
			graphics.setFont(FONT);
			FontMetrics metrics = graphics.getFontMetrics();
			int x = bounds.x + PHYSICAL_INSET, y = bounds.y + PHYSICAL_INSET + metrics.getAscent();
			// Reserve both insets and the one-pixel shadow. Native five-digit stacks can
			// otherwise reach the next slot even though their compact formatting is valid.
			double scale = Math.min(1d, (bounds.width - PHYSICAL_INSET * 2d) / Math.max(1d, metrics.stringWidth(label) + 1d));
			if (scale < 1d)
			{
				Graphics2D fitted = (Graphics2D) graphics.create();
				try
				{
					fitted.translate(x, y); fitted.scale(scale, 1d);
					fitted.setColor(Color.BLACK); fitted.drawString(label, 1, 1);
					fitted.setColor(PHYSICAL_COLOR); fitted.drawString(label, 0, 0);
				}
				finally { fitted.dispose(); }
			}
			else
			{
				graphics.setColor(Color.BLACK); graphics.drawString(label, x + 1, y + 1);
				graphics.setColor(PHYSICAL_COLOR); graphics.drawString(label, x, y);
			}
		}
		finally { graphics.setClip(clip); graphics.setFont(font); graphics.setPaint(paint); }
	}

	public static boolean eligible(int component, int itemId, int physicalQuantity)
	{
		return component == InterfaceID.Bankmain.ITEMS && itemId >= 0
			&& itemId != ItemID.BANK_FILLER && itemId != ItemID.BLANKOBJECT && physicalQuantity >= 0;
	}

	public static int textX(Rectangle bounds, FontMetrics metrics, String label)
	{
		return bounds.x + bounds.width - 2 - metrics.stringWidth(label);
	}
	public static int textY(Rectangle bounds, FontMetrics metrics)
	{
		return bounds.y + bounds.height - 2 - metrics.getDescent();
	}
	/** Fit exceptional long signed labels inside the slot without moving their baseline or right edge. */
	public static double textScale(Rectangle bounds, FontMetrics metrics, String label)
	{
		return Math.min(1d, Math.max(1, bounds.width - 4d) / Math.max(1, metrics.stringWidth(label) + 1d));
	}

	/** Placeholders represent no physical copies available in this bank location. */
	public static long quantity(VirtualOwnership ownership, int itemId, boolean placeholder)
	{
		return placeholder ? 0 : ownership.at(itemId, VirtualLocation.BANK);
	}

	/** Parent clip is supplied by RuneLite and further intersected by the caller; preserve it exactly. */
	public static void paint(Graphics2D graphics, Rectangle bounds, long quantity, String label, BankOverlayState.Settings settings)
	{
		if (bounds.width <= 0 || bounds.height <= 0 || !graphics.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)) { return; }
		Shape clip = graphics.getClip();
		Font font = graphics.getFont();
		Paint paint = graphics.getPaint();
		try
		{
			graphics.clip(bounds);
			if (settings.isDimLocked() && quantity <= 0)
			{
				graphics.setColor(settings.getLockedTint());
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
			if (settings.isQuantities())
			{
				graphics.setFont(FONT);
				FontMetrics metrics = graphics.getFontMetrics();
				double scale = textScale(bounds, metrics, label);
				if (scale < 1d)
				{
					// Keep the existing viewport/item clip. Only unusual oversized labels allocate a child graphics.
					Graphics2D fitted = (Graphics2D) graphics.create();
					try
					{
						fitted.translate(bounds.x + bounds.width - 2, textY(bounds, metrics)); fitted.scale(scale, 1d);
						int left = -metrics.stringWidth(label);
						fitted.setColor(Color.BLACK); fitted.drawString(label, left + 1, 1);
						fitted.setColor(settings.quantityColor(quantity)); fitted.drawString(label, left, 0);
					}
					finally { fitted.dispose(); }
					return;
				}
				int x = textX(bounds, metrics, label);
				int y = textY(bounds, metrics);
				graphics.setColor(Color.BLACK);
				graphics.drawString(label, x + 1, y + 1);
				graphics.setColor(settings.quantityColor(quantity));
				graphics.drawString(label, x, y);
			}
		}
		finally
		{
			graphics.setClip(clip);
			graphics.setFont(font);
			graphics.setPaint(paint);
		}
	}
}
