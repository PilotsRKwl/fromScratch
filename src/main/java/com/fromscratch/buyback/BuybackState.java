package com.fromscratch.buyback;

import java.util.*;
import lombok.Value;

/** Bounded catalog ledger. Historical values are role levels; copy_ID values count exact paid copies. */
@Value
public class BuybackState
{
	public static final int COPIES_PER_ROLE=1;
	int catalogVersion;
	Map<String,Integer> entitlements;
	Map<String,Long> bankFloors;
	long committedGp;
	List<BuybackPurchase> purchases;
	String archive;
	com.fromscratch.pouch.PouchState pouch;
	public BuybackState(int catalogVersion,Map<String,Integer> entitlements,Map<String,Long> bankFloors,long committedGp,List<BuybackPurchase> purchases,String archive)
	{this(catalogVersion,entitlements,bankFloors,committedGp,purchases,archive,com.fromscratch.pouch.PouchState.unreviewed());}
	public BuybackState(int catalogVersion,Map<String,Integer> entitlements,Map<String,Long> bankFloors,long committedGp,List<BuybackPurchase> purchases,String archive,com.fromscratch.pouch.PouchState pouch)
	{
		if(catalogVersion!=BuybackCatalog.VERSION||purchases.size()>4096||committedGp<0||archive!=null&&!archive.matches("bank-migration-[a-f0-9-]{36}")){throw new IllegalArgumentException("Invalid buyback state");}
		Map<String,Integer> verified=new TreeMap<>();Map<String,Long> anchors=new TreeMap<>();Set<UUID> ids=new HashSet<>();long sum=0;
		for(BuybackPurchase p:purchases)
		{
			if(!ids.add(p.getId())){throw new IllegalArgumentException("Duplicate buyback payment");}
			BuybackEntry entry=BuybackCatalog.entry(p.getCatalogId());long cost=BuybackCatalog.cost(entry,verified);
			if(cost!=p.getGp()||p.getSelectedItemId()<0&&entry.getRoles().entrySet().stream().allMatch(e->verified.getOrDefault(e.getKey(),0)>=e.getValue())){throw new IllegalArgumentException("Mismatched buyback evidence");}
			p.getBankFloors().forEach((role,floor)->{if(anchors.containsKey(role)&&floor<anchors.get(role)){throw new IllegalArgumentException("Paid-copy anchor regressed");}anchors.put(role,floor);});
			if(p.getSelectedItemId()>0){verified.merge(SelectedBuybacks.role(p.getSelectedItemId()),1,Math::addExact);}else{verified.putAll(entry.getRoles());}sum=Math.addExact(sum,p.getGp());
		}
		if(sum!=committedGp||!verified.equals(entitlements)||!verified.keySet().equals(bankFloors.keySet())){throw new IllegalArgumentException("Buyback entitlements must match payments");}
		bankFloors.forEach((role,floor)->{if(floor==null||floor>0||floor<anchors.get(role)){throw new IllegalArgumentException("Invalid paid-copy anchor");}});
		this.catalogVersion=catalogVersion;this.entitlements=Collections.unmodifiableMap(verified);this.committedGp=committedGp;
		this.bankFloors=Collections.unmodifiableMap(new TreeMap<>(bankFloors));
		this.purchases=Collections.unmodifiableList(new ArrayList<>(purchases));this.archive=archive;
		this.pouch=Objects.requireNonNull(pouch);
	}
	public static BuybackState empty(){return empty(null);}
	public static BuybackState empty(String archive){return new BuybackState(BuybackCatalog.VERSION,Collections.emptyMap(),Collections.emptyMap(),0,Collections.emptyList(),archive);}
	public BuybackState add(BuybackPurchase purchase)
	{
		List<BuybackPurchase> next=new ArrayList<>(purchases);next.add(purchase);Map<String,Integer> roles=new TreeMap<>(entitlements);if(purchase.getSelectedItemId()>0){roles.merge(SelectedBuybacks.role(purchase.getSelectedItemId()),1,Math::addExact);}else{roles.putAll(BuybackCatalog.entry(purchase.getCatalogId()).getRoles());}
		Map<String,Long> floors=new TreeMap<>(bankFloors);floors.putAll(purchase.getBankFloors());
		return new BuybackState(BuybackCatalog.VERSION,roles,floors,Math.addExact(committedGp,purchase.getGp()),next,archive,pouch);
	}
	public BuybackState withPouch(com.fromscratch.pouch.PouchState next){return new BuybackState(catalogVersion,entitlements,bankFloors,committedGp,purchases,archive,next);}
}
