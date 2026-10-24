package com.fromscratch.buyback;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** One on-request native bank-side contents observation, not ongoing personal ownership. */
@Value
public class BagObservation
{
	public static final String INSTRUCTION="Looting Bag contents are not verified. Withdraw your bag and choose View, then return it to the bank and retry within two minutes. Do not change its contents before confirming.";
	public enum State { UNKNOWN, VERIFIED_EMPTY, VERIFIED_NONEMPTY }
	int itemId; Map<Integer,Long> quantities; Map<Integer,String> names; Instant at;
	public BagObservation(int itemId,Map<Integer,Long> quantities,Map<Integer,String> names,Instant at)
	{
		if(itemId!=-1&&!BuybackCatalog.variants("looting_bag_family").containsKey(itemId)){throw new IllegalArgumentException("Invalid bag form");}
		this.itemId=itemId;this.quantities=quantities(quantities);this.names=Collections.unmodifiableMap(new TreeMap<>(names));this.at=Objects.requireNonNull(at);
		if(itemId==-1&&!this.quantities.isEmpty()){throw new IllegalArgumentException("Unknown bag contents");}
	}
	public boolean fresh(Instant now){return itemId>0&&!at.isAfter(now)&&now.isBefore(at.plusSeconds(120));}
	public State getState(){return itemId<0?State.UNKNOWN:quantities.isEmpty()?State.VERIFIED_EMPTY:State.VERIFIED_NONEMPTY;}
	public static BagObservation unknown(){return new BagObservation(-1,Collections.emptyMap(),Collections.emptyMap(),Instant.now());}
	public static Map<Integer,Long> quantities(Map<Integer,Long> values)
	{
		if(values.size()>28){throw new IllegalArgumentException("Bag capacity exceeded");}
		Map<Integer,Long> copy=new TreeMap<>();values.forEach((id,n)->{if(id<0||n==null||n<=0||n>Integer.MAX_VALUE){throw new IllegalArgumentException("Invalid bag quantity");}copy.put(id,n);});return Collections.unmodifiableMap(copy);
	}
}
