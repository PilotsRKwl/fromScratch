package com.fromscratch.bank;

import com.fromscratch.buyback.*;
import com.fromscratch.ge.GeTrade;
import com.fromscratch.wealth.GpFormatter;
import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Recorded evidence for one exact item ID; no inferred consumption or offline trades. */
public final class ItemHistory
{
	private ItemHistory() { }
	@Value public static class Row { Instant at;String action;long quantity;String details; }
	@Value public static class Page
	{
		List<Row> rows;BankHistory.Cursor next;String message;
		public Page(List<Row> rows,BankHistory.Cursor next,String message)
		{ this.rows=Collections.unmodifiableList(new ArrayList<>(rows));this.next=next;this.message=message; }
	}
	public static List<Row> rows(BankAudit audit,int id)
	{
		List<Row> rows=new ArrayList<>();
		if(audit.getPotion()!=null){for(com.fromscratch.potion.PotionType type:audit.getPotion().getObservation().getTypes().values()){if(type.getItems().containsKey(id)){boolean review=audit.getPotion().getReview()!=null;long units=review?audit.getPotion().getReview().getOrDefault(type.getId(),0L):audit.getPotion().getObservation().getQuantities().getOrDefault(type.getId(),0L);rows.add(new Row(audit.getAt(),review?"Potion Storage manual review/correction":"Potion Storage observation",units,(review?"Recorded virtual ":"Verified physical ")+type.unit()+"; absolute amount, not additional income. "+(review?audit.getReason():"")));}}}
		Long physical=audit.getPhysicalChanges().get(id),balance=audit.getBalanceChanges().get(id);
		if(physical!=null&&physical!=0)
		{
			rows.add(new Row(audit.getAt(),physical>0?"Bank deposit":"Bank withdrawal",physical,"Physical bank change; progress change: "+(balance==null?0:balance)+". Not proof of income or spending."));
		}
		if(balance!=null&&(physical==null||!balance.equals(physical)))
		{
			boolean pouch=audit.getKind()==BankAudit.Kind.POUCH||audit.getBuyback()!=null&&audit.getBuyback().getLegacyRunes().containsKey(id);
			rows.add(new Row(audit.getAt(),pouch?(audit.getBuyback()!=null&&audit.getBuyback().getCatalogId().equals("looting_bag_family")?"Automatic — Legacy Looting Bag":"Legacy Rune Pouch offset"):audit.getKind()==BankAudit.Kind.CORRECTION?"Manual correction":"Automatic adjustment",balance,"Signed bank balance adjustment"));
		}
		BuybackPurchase purchase=audit.getBuyback();
		if(purchase!=null&&BuybackCatalog.entry(purchase.getCatalogId()).getRoles().keySet().stream().anyMatch(role->BuybackCatalog.variants(role).containsKey(id)))
		{
			rows.add(new Row(audit.getAt(),"Legacy buyback",1,BuybackCatalog.entry(purchase.getCatalogId()).getName()+" — "+GpFormatter.exact(purchase.getGp())+" (shared entitlement; not an extra physical copy)"));
		}
		for(GeTrade trade:audit.getExchange().getTrades())
		{
			if(trade.getItemId()==id&&trade.execution())
			{
				rows.add(new Row(trade.getAt(),trade.isBuy()?"GE purchase":"GE sale",trade.getQuantity(),GpFormatter.exact(trade.getReportedGp())+" reported execution; bank association unverified."));
			}
		}
		return rows;
	}
}
