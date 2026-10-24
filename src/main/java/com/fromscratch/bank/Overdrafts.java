package com.fromscratch.bank;

import com.fromscratch.potion.*;
import java.util.*;
import lombok.Value;

/** Derived from acknowledged signed balances and actual physical deltas, never menu clicks. */
public final class Overdrafts
{
	private Overdrafts() { }
	@Value public static class Warning { int itemId;String name;String source;String unit;long withdrawn;long available;long outstanding;long increase;
		public String message(){String item=name.replace("<","").replace(">","").replace('|','/');if(item.length()>60){item=item.substring(0,60);}return String.format(java.util.Locale.ENGLISH,"%s: withdrew %,d %s %s; %,d available. %,d outstanding%s.",source,withdrawn,item,unit,available,outstanding,increase==outstanding?"":" (increased by "+String.format(java.util.Locale.ENGLISH,"%,d",increase)+")");}
	}
	@Value public static class Outstanding { int itemId;String name;String unit;long quantity; }
	public static List<Warning> between(BankRun before,BankRun after,boolean continuous,java.util.function.IntFunction<String> names)
	{
		if(!continuous||before==null||after==before||!before.getRunId().equals(after.getRunId())){return Collections.emptyList();}
		BankAudit audit=after.getRecent().get(0);if(audit.getKind()!=BankAudit.Kind.AUTOMATIC&&audit.getKind()!=BankAudit.Kind.STORAGE){return Collections.emptyList();}
		if(before.getHostSummary()!=null&&!before.getHostSummary().getSession().equals(audit.getSession())){return Collections.emptyList();}
		List<Warning> result=new ArrayList<>();Set<Integer> potionItems=new HashSet<>();
		for(PotionType type:after.getPotions().getTypes().values())
		{
			potionItems.addAll(type.getItems().keySet());
			if(!before.getPotions().isReviewed()||!after.getPotions().isReviewed()||!Objects.equals(before.getPotions().getSession(),after.getPotions().getSession())){continue;}
			long bankDelta=Math.subtractExact(type.doses(after.getPhysical()),type.doses(before.getPhysical()));
			add(result,type.displayId(),type.getName(),"Bank",type.unit(),bankDelta,before.getPotions().bankBalance(type,before.getBalances()),after.getPotions().bankBalance(type,after.getBalances()));
			long storeDelta=Math.subtractExact(after.getPotions().getPhysical().getOrDefault(type.getId(),0L),before.getPotions().getPhysical().getOrDefault(type.getId(),0L));
			add(result,type.displayId(),type.getName(),"Potion Storage",type.unit(),storeDelta,before.getPotions().balance(type.getId()),after.getPotions().balance(type.getId()));
		}
		Map<Integer,Long> old=com.fromscratch.buyback.BuybackEngine.balances(before),now=com.fromscratch.buyback.BuybackEngine.balances(after);
		audit.getPhysicalChanges().forEach((id,delta)->{if(!potionItems.contains(id)){add(result,id,names.apply(id),"Bank","items",delta,old.getOrDefault(id,0L),now.getOrDefault(id,0L));}});
		return result;
	}
	private static void add(List<Warning> result,int id,String name,String source,String unit,long delta,long before,long after)
	{if(delta<0&&after<Math.min(0,before)){long total=Math.negateExact(after),increase=Math.subtractExact(total,Math.max(0,Math.negateExact(before)));result.add(new Warning(id,name,source,unit,Math.negateExact(delta),Math.max(0,before),total,increase));}}
	public static List<Outstanding> outstanding(BankView view)
	{
		BankRun run=view.getRun();if(run==null||!run.isActive()){return Collections.emptyList();}List<Outstanding> result=new ArrayList<>();Set<Integer> potionItems=new HashSet<>();
		for(PotionType type:run.getPotions().getTypes().values())
		{
			potionItems.addAll(type.getItems().keySet());long bank=run.getPotions().bankBalance(type,run.getBalances()),stored=run.getPotions().balance(type.getId());long q=Math.addExact(bank<0?Math.negateExact(bank):0,stored<0?Math.negateExact(stored):0);if(q>0){result.add(new Outstanding(type.displayId(),type.getName(),type.unit(),q));}
		}
		com.fromscratch.buyback.BuybackEngine.balances(run).forEach((id,q)->{if(q<0&&!potionItems.contains(id)){result.add(new Outstanding(id,view.name(id),"items",Math.negateExact(q)));}});
		return Collections.unmodifiableList(result);
	}
}
