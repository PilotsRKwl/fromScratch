package com.fromscratch.buyback;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** One on-request native bank-side contents observation, not ongoing personal ownership. */
@Value
public class BagObservation
{
	public static final String INSTRUCTION="From Scratch couldn't read your Looting Bag contents. If it is empty, confirm that in Legacy Buybacks. Otherwise use the bag's native Check or View option, then return it to the bank and retry. Do not change its contents before confirming.";
	public enum State { UNKNOWN, VERIFIED_EMPTY, VERIFIED_NONEMPTY, PLAYER_CONFIRMED_EMPTY }
	int itemId; Map<Integer,Long> quantities; Map<Integer,String> names; Instant at; State state;
	public BagObservation(int itemId,Map<Integer,Long> quantities,Map<Integer,String> names,Instant at)
	{this(itemId,quantities,names,at,itemId<0?State.UNKNOWN:quantities.isEmpty()?State.VERIFIED_EMPTY:State.VERIFIED_NONEMPTY);}
	private BagObservation(int itemId,Map<Integer,Long> quantities,Map<Integer,String> names,Instant at,State state)
	{
		if(itemId!=-1&&!BuybackCatalog.variants("looting_bag_family").containsKey(itemId)){throw new IllegalArgumentException("Invalid bag form");}
		this.itemId=itemId;this.quantities=quantities(quantities);this.names=Collections.unmodifiableMap(new TreeMap<>(names));this.at=Objects.requireNonNull(at);
		this.state=Objects.requireNonNull(state);
		if(itemId==-1&&!this.quantities.isEmpty()||state==State.PLAYER_CONFIRMED_EMPTY&&(itemId<0||!this.quantities.isEmpty())){throw new IllegalArgumentException("Invalid bag contents evidence");}
	}
	public boolean fresh(Instant now){return itemId>0&&!at.isAfter(now)&&now.isBefore(at.plusSeconds(120));}
	/** A voluntary declaration, never a native observation. Final purchase revalidates it. */
	public static BagObservation playerConfirmedEmpty(int itemId){return new BagObservation(itemId,Collections.emptyMap(),Collections.emptyMap(),Instant.now(),State.PLAYER_CONFIRMED_EMPTY);}
	public static BagObservation unknown(){return new BagObservation(-1,Collections.emptyMap(),Collections.emptyMap(),Instant.now());}
	public static Map<Integer,Long> quantities(Map<Integer,Long> values)
	{
		if(values.size()>28){throw new IllegalArgumentException("Bag capacity exceeded");}
		Map<Integer,Long> copy=new TreeMap<>();values.forEach((id,n)->{if(id<0||n==null||n<=0||n>Integer.MAX_VALUE){throw new IllegalArgumentException("Invalid bag quantity");}copy.put(id,n);});return Collections.unmodifiableMap(copy);
	}
}
