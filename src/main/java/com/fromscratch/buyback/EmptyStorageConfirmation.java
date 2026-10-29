package com.fromscratch.buyback;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Explicit player declaration, never a native contents observation or inferred empty store. */
@Value
public class EmptyStorageConfirmation
{
	private static final Set<String> CATALOGS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("fish_barrel","herb_sack","plank_sack","seed_box","tackle_box","quiver","blessed_quiver")));
	String catalogId;
	UUID revision;
	Instant at;
	public EmptyStorageConfirmation(String catalogId,UUID revision,Instant at,boolean allSharedStoresEmpty,boolean legacyContentsAccounted)
	{
		if(!required(catalogId)||!allSharedStoresEmpty||!legacyContentsAccounted){throw new IllegalArgumentException("Confirm that the shared store is empty and its legacy contents are already accounted for.");}
		this.catalogId=catalogId;this.revision=Objects.requireNonNull(revision);this.at=Objects.requireNonNull(at);
	}
	public static boolean required(String catalogId){return CATALOGS.contains(catalogId);}
	public boolean valid(String catalogId,UUID revision,Instant now){return this.catalogId.equals(catalogId)&&this.revision.equals(revision)&&!at.isAfter(now)&&now.isBefore(at.plusSeconds(120));}
}
