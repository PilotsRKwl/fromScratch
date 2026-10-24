package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Getter;

/** Immutable render projections. No game containers, disk access or accounting is used by painters. */
@Singleton
public final class BankOverlayState
{
	public static final Color DEFAULT_COLOR = new Color(0x1DFF00);
	public static final Color DEFAULT_NEGATIVE_COLOR = new Color(0xFF4040);
	@Getter private volatile boolean enabled;
	@Getter private volatile Projection projection;
	@Getter private volatile boolean potionCurrent;
	@Getter private volatile Settings settings = new Settings(true, true, DEFAULT_COLOR);

	public void enable() { enabled = true; }
	public void disable() { enabled = false; projection = null; potionCurrent=false; }
	public void clear() { projection = null; potionCurrent=false; }
	public void configure(FromScratchConfig config)
	{
		settings = new Settings(config.bankVirtualQuantities(), config.lockedBankItems(), config.virtualQuantityColor(), config.lockedItemBackgroundColor(), config.lockedItemBackgroundOpacity(),config.negativeVirtualQuantityColor());
	}

	/** Live bank-only path. Unanchored migrations are hidden until the player confirms the anchor. */
	public void updateBank(com.fromscratch.bank.BankView view)
	{
		com.fromscratch.bank.BankRun run = view.getRun();
		potionCurrent=enabled&&!view.isError()&&run!=null&&run.isActive()&&run.isAnchored()&&run.getPotions().isReviewed()&&view.getPotions().isVerified()&&run.getPotions().getPhysical().equals(view.getPotions().getQuantities())&&run.getPotions().getTypes().equals(view.getPotions().getTypes());
		// A rejected update is not a rejected last acknowledged checkpoint.
		if (!enabled || run == null || !run.isActive() || !run.isAnchored()) { projection = null; }
		else if (projection == null || !projection.revision.equals(run.getRevision())) { projection = new Projection(run, view.getTrace(),projection); }
	}

	@Getter
	public static final class Projection
	{
		private final String characterKey;
		private final Map<Integer, String> labels;
		private final Map<Integer, Long> bankBalances;
		private final java.util.UUID revision;
		private final com.fromscratch.bank.BankTimings.Trace trace;
		private final Map<Integer,PotionLabel> potionLabels;
		private Projection(com.fromscratch.bank.BankRun run, com.fromscratch.bank.BankTimings.Trace trace,Projection previous)
		{
			this.revision = run.getRevision(); this.trace = trace;
			characterKey = run.getCharacter(); bankBalances = com.fromscratch.buyback.BuybackEngine.balances(run);
			Map<Integer,PotionLabel> potionText=new HashMap<>();run.getPotions().getTypes().values().forEach(type->{long n=run.getPotions().balance(type.getId());PotionLabel label=new PotionLabel("FS: "+String.format(java.util.Locale.ENGLISH,"%,d",n),"FS: "+VirtualQuantityFormatter.signed(n),n<0,type.unit());type.getItems().keySet().forEach(item->potionText.put(item,label));});potionLabels=Collections.unmodifiableMap(potionText);
			// A GE/history-only commit changes revision, not bank quantities. Reuse the
			// immutable labels rather than reformatting a large bank on the client thread.
			if(previous!=null&&characterKey.equals(previous.characterKey)&&bankBalances.equals(previous.bankBalances)){labels=previous.labels;return;}
			Map<Integer, String> formatted = new HashMap<>(); bankBalances.forEach((id, n) -> formatted.put(id, VirtualQuantityFormatter.signed(n)));
			labels = Collections.unmodifiableMap(formatted);
		}
		public String label(int id) { return labels.getOrDefault(id, "0"); }
		public long balance(int id, boolean placeholder)
		{
			long n = bankBalances.getOrDefault(id, 0L); return placeholder ? Math.min(0, n) : n;
		}
	}
	@lombok.Value public static class PotionLabel { String text;String compactText;boolean negative;String unit; }

	@Getter
	public static final class Settings
	{
		private final boolean quantities;
		private final boolean dimLocked;
		private final Color color;
		private final Color negativeColor;
		private final Color lockedTint;
		public Settings(boolean quantities, boolean dimLocked, Color color)
		{ this(quantities, dimLocked, color, new Color(0x333333), 50); }
		public Settings(boolean quantities, boolean dimLocked, Color color, Color lockedColor, int opacity)
		{this(quantities,dimLocked,color,lockedColor,opacity,DEFAULT_NEGATIVE_COLOR);}
		public Settings(boolean quantities, boolean dimLocked, Color color, Color lockedColor, int opacity,Color negativeColor)
		{
			this.quantities = quantities;
			this.dimLocked = dimLocked;
			this.color = color == null ? DEFAULT_COLOR : new Color(color.getRGB());
			this.negativeColor=negativeColor==null?DEFAULT_NEGATIVE_COLOR:new Color(negativeColor.getRGB());
			Color tint = lockedColor == null ? new Color(0x333333) : lockedColor;
			this.lockedTint = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), Math.round(Math.max(0, Math.min(100, opacity)) * 255f / 100));
		}
		public Color quantityColor(long amount){return amount<0?negativeColor:color;}
	}
}
