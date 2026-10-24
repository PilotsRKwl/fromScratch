package com.fromscratch.potion;

import java.util.*;
import lombok.Value;

/** Immutable observation/review evidence carried by an ordinary bank audit operation. */
@Value
public class PotionUpdate
{
	PotionObservation observation;
	Map<Integer,Long> review;
	Map<Integer,Long> exclusions;
	public PotionUpdate(PotionObservation observation,Map<Integer,Long> review)
	{this(observation,review,Collections.emptyMap());}
	public PotionUpdate(PotionObservation observation,Map<Integer,Long> review,Map<Integer,Long> exclusions)
	{this.observation=observation;this.review=review==null?null:PotionState.numbers(review,false);this.exclusions=PotionState.numbers(exclusions,true);if(observation==null&&review==null){throw new IllegalArgumentException("Empty storage evidence");}}
}
