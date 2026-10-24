package com.fromscratch.potion;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** A complete, character-scoped value copy. Missing data is never an empty store. */
@Value
public class PotionObservation
{
	boolean verified;
	Map<Integer,PotionType> types;
	Map<Integer,Long> quantities;
	Instant at;
	public PotionObservation(boolean verified,Map<Integer,PotionType> types,Map<Integer,Long> quantities,Instant at)
	{
		this.verified=verified;this.types=Collections.unmodifiableMap(new TreeMap<>(types));this.quantities=PotionState.numbers(quantities,true);this.at=Objects.requireNonNull(at);
		if(types.size()>256||!types.keySet().containsAll(this.quantities.keySet())){throw new IllegalArgumentException("Incomplete potion observation");}
		Set<Integer> ids=new HashSet<>();for(PotionType type:types.values()){for(int id:type.getItems().keySet()){if(!ids.add(id)){throw new IllegalArgumentException("Ambiguous potion mapping");}}}
	}
	public static PotionObservation unknown(){return new PotionObservation(false,Collections.emptyMap(),Collections.emptyMap(),Instant.EPOCH);}
	public static PotionObservation unavailableStore(){return new PotionObservation(true,Collections.emptyMap(),Collections.emptyMap(),Instant.EPOCH);}
}
