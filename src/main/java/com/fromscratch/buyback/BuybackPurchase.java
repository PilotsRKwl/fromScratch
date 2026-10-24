package com.fromscratch.buyback;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Immutable evidence. One ID pays once for all roles, including a complete outfit or pouch family. */
@Value
public class BuybackPurchase
{
	UUID id;
	String catalogId;
	long gp;
	Instant at;
	Map<String,Long> bankFloors;
	int pouchItemId;
	Map<Integer,Long> legacyRunes;
	BagObservation.State bagConfirmation;
	public BuybackPurchase(UUID id,String catalogId,long gp,Instant at,Map<String,Long> bankFloors)
	{this(id,catalogId,gp,at,bankFloors,-1,Collections.emptyMap());}
	public BuybackPurchase(UUID id,String catalogId,long gp,Instant at,Map<String,Long> bankFloors,int pouchItemId,Map<Integer,Long> legacyRunes)
	{this(id,catalogId,gp,at,bankFloors,pouchItemId,legacyRunes,"looting_bag_family".equals(catalogId)&&pouchItemId>0?(legacyRunes.isEmpty()?BagObservation.State.VERIFIED_EMPTY:BagObservation.State.VERIFIED_NONEMPTY):BagObservation.State.UNKNOWN);}
	public BuybackPurchase(UUID id,String catalogId,long gp,Instant at,Map<String,Long> bankFloors,int pouchItemId,Map<Integer,Long> legacyRunes,BagObservation.State bagConfirmation)
	{
		this.id=Objects.requireNonNull(id);this.catalogId=Objects.requireNonNull(catalogId);
		if(gp<=0){throw new IllegalArgumentException("Invalid buyback expenditure");}
		this.gp=gp;this.at=Objects.requireNonNull(at);
		if(!bankFloors.keySet().equals(BuybackCatalog.entry(catalogId).getRoles().keySet())||bankFloors.values().stream().anyMatch(n->n==null||n>0||n==Long.MIN_VALUE)){throw new IllegalArgumentException("Invalid paid-copy bank anchor");}
		this.bankFloors=Collections.unmodifiableMap(new TreeMap<>(bankFloors));
		// Retain the historical accessor/receipt representation for pouch compatibility.
		this.pouchItemId=pouchItemId;this.legacyRunes=catalogId.equals("looting_bag_family")?BagObservation.quantities(legacyRunes):com.fromscratch.pouch.PouchState.quantities(legacyRunes);
		if(pouchItemId!=-1&&(!(catalogId.equals("rune_pouch_family")||catalogId.equals("looting_bag_family"))||!BuybackCatalog.variants(catalogId).containsKey(pouchItemId))||pouchItemId==-1&&!this.legacyRunes.isEmpty()){throw new IllegalArgumentException("Invalid storage purchase evidence");}
		this.bagConfirmation=Objects.requireNonNull(bagConfirmation);
		BagObservation.State expected=catalogId.equals("looting_bag_family")&&pouchItemId>0?(this.legacyRunes.isEmpty()?BagObservation.State.VERIFIED_EMPTY:BagObservation.State.VERIFIED_NONEMPTY):BagObservation.State.UNKNOWN;
		if(bagConfirmation!=expected&&!(expected==BagObservation.State.VERIFIED_EMPTY&&bagConfirmation==BagObservation.State.PLAYER_CONFIRMED_EMPTY)){throw new IllegalArgumentException("Invalid Looting Bag confirmation");}
	}
}
