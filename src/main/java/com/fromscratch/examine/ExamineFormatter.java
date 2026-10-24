package com.fromscratch.examine;

import com.fromscratch.bank.BankRun;
import com.fromscratch.buyback.*;
import com.fromscratch.overlay.VirtualQuantityFormatter;
import com.fromscratch.wealth.PriceQuote;
import java.awt.Color;
import java.util.*;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

/** One local chat line, using cached quotes and existing catalog/payment evidence. Never changes accounting. */
final class ExamineFormatter
{
	// Readability budget for OPTIONAL prices, not a claimed transport limit. Never slice the mandatory opening.
	static final int OPTIONAL_TEXT_BUDGET=160;
	private ExamineFormatter() { }
	static String format(BankRun run,int id,String name,long quantity,PriceQuote price,long ha,Color color)
	{return format(run,id,name,quantity,price,ha,color,com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR);}
	static String format(BankRun run,int id,String name,long quantity,PriceQuote price,long ha,Color color,Color negativeColor)
	{
		Color accent=color==null?new Color(0x1DFF00):color;
		Color signed=quantity<0?(negativeColor==null?com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR:negativeColor):accent;
		ChatMessageBuilder text=new ChatMessageBuilder();
		number(text,signed,"From Scratch: "+QuantityFormatter.formatNumber(quantity));
		// append(String) escapes Jagex markup, unlike append(Color,String). Only trusted numbers use the latter.
		// GAMEMESSAGE's native chat builder interprets a literal pipe as name|body (scripts 84/632).
		// This is a control delimiter even in a short message, not a character-limit/wrapping issue.
		text.append(" x ").append(name==null?"Item":name.replace('|','/'));
		Buyback info=buyback(run,id);String separator=": ";
		if(quantity<0) { text.append(" — Outstanding");separator="; "; }
		else if(quantity==0&&(info==null||!info.paid)) { text.append(" — Locked");separator="; "; }
		else if(quantity>0&&info!=null&&!info.paid&&BuybackCatalog.variants("looting_bag_family").containsKey(id)){text.append(" — Earned");separator="; ";}
		if(info!=null&&(info.paid||quantity<=0))
		{
			text.append(separator.equals(": ")?" — Buyback: ":"; Buyback: ");
			if(info.paid) { text.append("Unlocked ("); }
			number(text,accent,VirtualQuantityFormatter.signed(info.gp));text.append(" GP");
			if(info.paid) { text.append(info.set?" set paid)":" paid)"); }
			else if(info.set) { text.append(" (set)"); }
			separator="; ";
		}
		String message=text.build();
		// Zero-valued alchemy/market totals add no useful information beside a locked/paid buyback.
		if(info!=null&&quantity==0) { return message; }
		if(price!=null&&price.getUnitGp()!=null)
		{
			ChatMessageBuilder field=new ChatMessageBuilder().append(separator).append("GE ");total(field,signed,quantity,price.getUnitGp());
			if(quantity!=1) { each(field,accent,price.getUnitGp()); }
			if(price.isStale()) { field.append(" (stale)"); }
			String extended=optional(message,field.build());if(extended.equals(message)) { return message; }message=extended;separator="; ";
		}
		else if(info==null)
		{
			String extended=optional(message,separator+"GE unavailable");if(extended.equals(message)) { return message; }message=extended;separator="; ";
		}
		if(ha>0)
		{
			ChatMessageBuilder field=new ChatMessageBuilder().append(separator).append("HA ");total(field,signed,quantity,ha);
			if(quantity!=1) { each(field,accent,ha); }
			message=optional(message,field.build());
		}
		return message;
	}
	private static String optional(String message,String field)
	{ return Text.unescapeJagex(message).length()+Text.unescapeJagex(field).length()<=OPTIONAL_TEXT_BUDGET?message+field:message; }
	private static void number(ChatMessageBuilder text,Color color,String value)
	{
		// Balanced </col> restores the native normal color; ChatMessageManager also respects
		// configured server-message colors on these resets. No redundant/unresolved <colNORMAL> tokens.
		text.append(color,value);
	}
	private static void total(ChatMessageBuilder text,Color color,long quantity,long unit)
	{
		try { number(text,color,QuantityFormatter.formatNumber(Math.multiplyExact(quantity,unit))); }
		catch(ArithmeticException ex) { text.append("out of range"); }
	}
	private static void each(ChatMessageBuilder text,Color color,long unit)
	{ text.append(" (");number(text,color,QuantityFormatter.formatNumber(unit));text.append("ea)"); }
	private static Buyback buyback(BankRun run,int id)
	{
		for(BuybackEntry entry:BuybackCatalog.entries())
		{
			if(entry.getRoles().entrySet().stream().noneMatch(role->Objects.equals(BuybackCatalog.variants(role.getKey()).get(id),role.getValue()))) { continue; }
			boolean paid=covered(entry,run.getBuybacks().getEntitlements());
			if(!paid) { return new Buyback(false,BuybackCatalog.cost(entry,run.getBuybacks().getEntitlements()),entry.getRoles().size()>1); }
			// Use actual receipts. Base + Elite upgrades accumulate once; a whole-set receipt is not split per piece.
			Map<String,Integer> levels=new HashMap<>();long gp=0;
			for(BuybackPurchase receipt:run.getBuybacks().getPurchases())
			{
				BuybackEntry bought=BuybackCatalog.entry(receipt.getCatalogId());
				if(bought.getRoles().keySet().stream().anyMatch(entry.getRoles()::containsKey))
				{ gp=Math.addExact(gp,receipt.getGp());bought.getRoles().forEach((role,level)->levels.merge(role,level,Math::max)); }
				if(covered(entry,levels)) { return new Buyback(true,gp,entry.getRoles().size()>1); }
			}
			return null; // Inconsistent/unknown payment evidence must not suggest a repeat charge or invented price.
		}
		return null;
	}
	private static boolean covered(BuybackEntry entry,Map<String,Integer> levels)
	{ return entry.getRoles().entrySet().stream().allMatch(role->levels.getOrDefault(role.getKey(),0)>=role.getValue()); }
	private static final class Buyback
	{
		final boolean paid,set;final long gp;
		Buyback(boolean paid,long gp,boolean set) { this.paid=paid;this.gp=gp;this.set=set; }
	}
}
