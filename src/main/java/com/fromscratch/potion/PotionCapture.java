package com.fromscratch.potion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Value;

/** Runtime-only setup proof. Never reconstructed from a saved run or a previous login. */
@Value
public class PotionCapture
{
	String character;
	UUID attempt;
	PotionObservation observation;
	public PotionCapture(String character,UUID attempt,PotionObservation observation)
	{
		if(character==null||attempt==null||!observation.isVerified()||observation.getTypes().isEmpty()||!observation.getAt().isAfter(Instant.EPOCH)){throw new IllegalArgumentException("A complete native Potion Storage capture is required.");}
		this.character=character;this.attempt=attempt;this.observation=observation;
	}
	public boolean belongsTo(String character,UUID attempt){return Objects.equals(this.character,character)&&this.attempt.equals(attempt);}
}
