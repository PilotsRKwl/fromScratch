package com.fromscratch.potion;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Part of the same atomic bank checkpoint, never a separately saved quantity ledger. */
@Value
public class PotionState
{
	Map<Integer,PotionType> types;
	Map<Integer,Long> initial,physical,balances,remainder,bankOut,bankOwnedOut,storeOut,storeOwnedOut;
	Instant observedAt;
	UUID session;
	UUID observedSession;
	boolean reviewed;
	public PotionState(Map<Integer,PotionType> types,Map<Integer,Long> initial,Map<Integer,Long> physical,Map<Integer,Long> balances,Map<Integer,Long> remainder,Map<Integer,Long> bankOut,Map<Integer,Long> bankOwnedOut,Map<Integer,Long> storeOut,Map<Integer,Long> storeOwnedOut,Instant observedAt,UUID session,boolean reviewed)
	{this(types,initial,physical,balances,remainder,bankOut,bankOwnedOut,storeOut,storeOwnedOut,observedAt,session,session,reviewed);}
	public PotionState(Map<Integer,PotionType> types,Map<Integer,Long> initial,Map<Integer,Long> physical,Map<Integer,Long> balances,Map<Integer,Long> remainder,Map<Integer,Long> bankOut,Map<Integer,Long> bankOwnedOut,Map<Integer,Long> storeOut,Map<Integer,Long> storeOwnedOut,Instant observedAt,UUID session,UUID observedSession,boolean reviewed)
	{
		this.types=Collections.unmodifiableMap(new TreeMap<>(types));this.initial=numbers(initial,true);this.physical=numbers(physical,true);this.balances=numbers(balances,false);this.remainder=numbers(remainder,true);this.bankOut=numbers(bankOut,true);this.bankOwnedOut=numbers(bankOwnedOut,true);this.storeOut=numbers(storeOut,true);this.storeOwnedOut=numbers(storeOwnedOut,true);this.observedAt=observedAt;this.session=session;this.observedSession=observedSession;this.reviewed=reviewed;
		for(Map<Integer,Long> map:Arrays.asList(this.initial,this.physical,this.balances,this.remainder,this.bankOut,this.bankOwnedOut,this.storeOut,this.storeOwnedOut)){if(!types.keySet().containsAll(map.keySet())){throw new IllegalArgumentException("Unknown stored potion");}}
		for(int id:types.keySet()){if(reviewed&&this.balances.getOrDefault(id,0L)>this.physical.getOrDefault(id,0L)||this.bankOwnedOut.getOrDefault(id,0L)>this.bankOut.getOrDefault(id,0L)||this.storeOwnedOut.getOrDefault(id,0L)>this.storeOut.getOrDefault(id,0L)||this.remainder.getOrDefault(id,0L)>3){throw new IllegalArgumentException("Invalid stored potion balance");}}
		if(types.size()>256||observedAt!=null&&session==null||(observedAt==null)!=(observedSession==null)||reviewed&&observedAt==null){throw new IllegalArgumentException("Invalid potion checkpoint");}
	}
	public static Map<Integer,Long> numbers(Map<Integer,Long> source,boolean positive)
	{TreeMap<Integer,Long> result=new TreeMap<>();source.forEach((id,q)->{if(id<0||q==null||q==Long.MIN_VALUE||positive&&q<0){throw new IllegalArgumentException("Invalid potion quantity");}if(q!=0){result.put(id,q);}});return Collections.unmodifiableMap(result);}
	public static PotionState unknown(){return new PotionState(Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),null,null,false);}
	public static PotionState baseline(PotionObservation observation,UUID session)
	{if(!observation.isVerified()){throw new IllegalArgumentException("Open Potion Storage before starting, then return to your full bank.");}return new PotionState(observation.getTypes(),observation.getQuantities(),observation.getQuantities(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),observation.getAt(),session,true);}
	public long bankBalance(PotionType type,Map<Integer,Long> bank){return Math.addExact(type.doses(bank),remainder.getOrDefault(type.getId(),0L));}
	public long balance(int id){return balances.getOrDefault(id,0L);}
	public PotionState corrected(Set<Integer> itemIds)
	{
		Set<Integer> affected=new HashSet<>();types.forEach((id,type)->{if(!Collections.disjoint(type.getItems().keySet(),itemIds)){affected.add(id);}});if(affected.isEmpty()){return this;}
		List<Map<Integer,Long>> copies=new ArrayList<>();for(Map<Integer,Long> map:Arrays.asList(remainder,bankOut,bankOwnedOut,storeOut,storeOwnedOut)){Map<Integer,Long> copy=new TreeMap<>(map);affected.forEach(copy::remove);copies.add(copy);}
		return new PotionState(types,initial,physical,balances,copies.get(0),copies.get(1),copies.get(2),copies.get(3),copies.get(4),observedAt,session,observedSession,reviewed);
	}
}
