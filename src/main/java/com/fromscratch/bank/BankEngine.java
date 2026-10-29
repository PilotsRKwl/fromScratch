package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.time.Instant;
import java.util.*;
import com.fromscratch.wealth.*;
import com.fromscratch.potion.*;

/** Pure checked bank arithmetic and quoted high-water valuation; no acquisition provenance or personal-container inference. */
public final class BankEngine
{
	private BankEngine() { }
	public static BankRun start(String character, UUID previous, ContainerSnapshot bank, boolean attested, UUID session, Instant at)
	{
		if (!attested) { throw new IllegalArgumentException("Confirm that inventory and equipment have been banked first."); }
		BankAudit audit = audit(session, at, BankAudit.Kind.START, Collections.emptyMap(), Collections.emptyMap(), "Bank-only run started; initial progress is zero.");
		return new BankRun(character, UUID.randomUUID(), previous, at, audit.getId(), 1, bank, bank.getQuantities(), Collections.emptyMap(), true, null, null, Collections.singletonList(audit), null);
	}
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at)
	{ return observe(old, bank, session, at, null); }
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at, BankTimings.Trace trace)
	{ return observe(old,bank,session,at,trace,Collections.emptyMap()); }
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at, BankTimings.Trace trace, Map<Integer,PriceQuote> prices)
	{return observe(old,bank,session,at,trace,prices,null);}
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at, BankTimings.Trace trace, Map<Integer,PriceQuote> prices,PotionObservation storage)
	{
		requireAnchored(old);
		Map<Integer, Long> delta = differences(old.getPhysical(), bank.getQuantities());
		BankTimings.mark(trace, BankTimings.Phase.DIFFERENCE);
		boolean first = old.getHostSummary() == null || !old.getHostSummary().getSession().equals(session);
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances());
		Map<Integer,Long> held=com.fromscratch.pouch.PouchEngine.transfers(old.getBuybacks().getPouch(),delta);
		// P always advances by the full observed delta. Only B/financial attribution
		// exclude the explicit one-time legacy-source reservation, in this same commit.
		Map<Integer,Long> progress=new TreeMap<>(delta);for(Map.Entry<Integer,Long> e:held.entrySet()){progress.put(e.getKey(),Math.subtractExact(progress.get(e.getKey()),e.getValue()));}progress.values().removeIf(n->n==0);
		for(Map.Entry<Integer,Long> e:progress.entrySet()){balances.put(e.getKey(),Math.addExact(old.balance(e.getKey()),e.getValue()));}
		PotionUpdate input=storage!=null?new PotionUpdate(storage,null):null;
		PotionEngine.Result potions=PotionEngine.apply(old,bank.getQuantities(),balances,input,session);balances=new TreeMap<>(potions.getBank());
		if(delta.isEmpty()&&!first&&old.getPotions().equals(potions.getState())){return old;}
		progress=differences(old.getBalances(),balances);
		BankAudit change = audit(session, at, delta.isEmpty() ? (old.getPotions().equals(potions.getState())?BankAudit.Kind.SESSION_CHECK:BankAudit.Kind.STORAGE) : BankAudit.Kind.AUTOMATIC, delta, progress,
			first ? "Bank changes detected on return to this PC." : "Bank quantities updated.");
		if(!delta.isEmpty())
		{
			com.fromscratch.ge.GeUpdate attribution=com.fromscratch.ge.GeEngine.bank(old.getExchange(),change.getId(),progress,at).getEvidence();
			Map<Integer,Long> exclusions=new TreeMap<>(attribution.getGainExclusions());potions.getExclusions().forEach((id,q)->exclusions.merge(id,q,Math::addExact));
			change=new BankAudit(change.getId(),session,at,change.getKind(),delta,progress,change.getReason(),FinancialEngine.recognize(old.getFinancial(),balances,change.getId(),prices,at,exclusions),attribution,null,null,held,input==null?null:new PotionUpdate(storage,null,potions.getExclusions()));
		}
		else if(input!=null){change=new BankAudit(change.getId(),session,at,change.getKind(),delta,progress,"Potion Storage observation recorded.",Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),null,null,Collections.emptyMap(),new PotionUpdate(storage,null,potions.getExclusions()));}
		BankRun next = next(old, bank.getQuantities(), balances, true, change, first ? change : old.getHostSummary());
		BankTimings.mark(trace, BankTimings.Phase.PREPARED); return next;
	}
	public static BankRun correct(BankRun old, int id, long desired, UUID session, Instant at)
	{
		requireAnchored(old);
		if (id < 0 || desired == Long.MIN_VALUE || desired > old.physical(id)) { throw new IllegalArgumentException("A positive balance cannot exceed the physical bank quantity."); }
		long difference = Math.subtractExact(desired, old.balance(id));
		if (difference == 0) { return old; }
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances()); balances.put(id, desired);
		BankAudit change = audit(session, at, BankAudit.Kind.CORRECTION, Collections.emptyMap(), Collections.singletonMap(id, difference), "Player confirmed bank-count correction.");
		return next(old, old.getPhysical(), balances, true, change, old.getHostSummary());
	}
	/** Existing balances are kept; excess positive balances require explicit approval, never an implicit clamp. */
	public static BankRun anchor(BankRun old, ContainerSnapshot bank, boolean approveExcessReduction, UUID session, Instant at)
	{
		if (!old.isActive() || old.isAnchored()) { throw new IllegalArgumentException("No migration anchor is pending."); }
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances());
		balances.replaceAll((id, n) ->
		{
			long physical = bank.getQuantities().getOrDefault(id, 0L);
			if (n > physical && !approveExcessReduction) { throw new IllegalArgumentException("Some saved positive balances exceed this bank. Bank those items first, or explicitly approve the listed reductions."); }
			return Math.min(n, physical);
		});
		BankAudit change = audit(session, at, BankAudit.Kind.ANCHOR, Collections.emptyMap(), differences(old.getBalances(), balances), "Player confirmed one-time bank-only migration anchor; earlier changes were not replayed.");
		return next(old, bank.getQuantities(), balances, true, change, change);
	}
	public static BankRun reset(BankRun old, UUID session, Instant at)
	{
		BankAudit change = audit(session, at, BankAudit.Kind.RESET, Collections.emptyMap(), Collections.emptyMap(), "Player confirmed reset after verified archive.");
		return new BankRun(old.getCharacter(), null, old.getRunId(), null, change.getId(), Math.incrementExact(old.getSequence()), null,
			Collections.emptyMap(), Collections.emptyMap(), false, old.getLegacyDocument(), old.getMigrationArchive(), Collections.singletonList(change), null);
	}
	static BankRun next(BankRun old, Map<Integer, Long> physical, Map<Integer, Long> balances, boolean anchored, BankAudit change, BankAudit summary)
	{
		com.fromscratch.ge.GeState exchange=old.getExchange();
		if(change.getKind()==BankAudit.Kind.EXCHANGE){exchange=com.fromscratch.ge.GeEngine.observe(exchange,change.getExchange().getObservation(),old.getRunId()).getState();}
		else if(change.getKind()==BankAudit.Kind.AUTOMATIC){exchange=com.fromscratch.ge.GeEngine.bank(exchange,change.getId(),change.getBalanceChanges(),change.getAt()).getState();}
		com.fromscratch.pouch.PouchState pouch=old.getBuybacks().getPouch();
		if(change.getKind()==BankAudit.Kind.AUTOMATIC){pouch=com.fromscratch.pouch.PouchEngine.bank(pouch,change.getPhysicalChanges());}
		else if(change.getKind()==BankAudit.Kind.POUCH){pouch=com.fromscratch.pouch.PouchEngine.apply(pouch,change.getPouch(),change.getId());}
		else if(change.getBuyback()!=null&&change.getBuyback().getCatalogId().equals("rune_pouch_family")&&change.getBuyback().getPouchItemId()>0){pouch=com.fromscratch.pouch.PouchEngine.purchased(pouch,change.getBuyback().getLegacyRunes());}
		List<BankAudit> recent = new ArrayList<>();
		for(BankAudit previous:old.getRecent())
		{
			// Full quotes stay in that operation's immutable journal/checkpoint. The rolling current view
			// needs only older movement summaries; avoid rewriting a 1,200-item quote batch 30 times.
			recent.add(summary(previous));
		}
		recent.add(0, change); if (recent.size() > 30) { recent.remove(30); }
		return new BankRun(old.getCharacter(), old.getRunId(), old.getPreviousRunId(), old.getStartedAt(), change.getId(), Math.incrementExact(old.getSequence()), old.getInitialBank(), physical, balances, anchored,
			old.getLegacyDocument(), old.getMigrationArchive(), recent, summary(summary),FinancialEngine.advance(old.getFinancial(),balances,change),exchange,
			(change.getBuyback()==null?com.fromscratch.buyback.BuybackEngine.progress(old.getBuybacks(),balances):com.fromscratch.buyback.BuybackEngine.purchase(old,change.getBuyback())).withPouch(pouch),change.getPotion()==null?(change.getKind()==BankAudit.Kind.CORRECTION?old.getPotions().corrected(change.getBalanceChanges().keySet()):old.getPotions()):PotionEngine.apply(old,physical,balances,change.getPotion(),change.getSession()).getState());
	}
	private static BankAudit summary(BankAudit audit)
	{
		return audit!=null&&audit.getKind()==BankAudit.Kind.AUTOMATIC&&!audit.getValuations().isEmpty()
			?new BankAudit(audit.getId(),audit.getSession(),audit.getAt(),audit.getKind(),audit.getPhysicalChanges(),audit.getBalanceChanges(),audit.getReason(),Collections.emptyList(),audit.getExchange(),null,null,audit.getPouchTransfers(),audit.getPotion()):audit;
	}
	public static BankRun start(String character,UUID previous,ContainerSnapshot bank,boolean attested,UUID session,Instant at,PotionObservation storage)
	{return start(character,previous,bank,attested,session,at,storage,"Verified Potion Storage legacy baseline.");}
	public static BankRun start(String character,UUID previous,ContainerSnapshot bank,boolean attested,UUID session,Instant at,PotionObservation storage,String proof)
	{
		BankRun run=start(character,previous,bank,attested,session,at);PotionState state=PotionState.baseline(storage,session);
		BankAudit audit=new BankAudit(run.getRevision(),session,at,BankAudit.Kind.START,Collections.emptyMap(),Collections.emptyMap(),"Bank-only run started; initial progress is zero. "+proof);
		return new BankRun(run.getCharacter(),run.getRunId(),run.getPreviousRunId(),run.getStartedAt(),run.getRevision(),run.getSequence(),run.getInitialBank(),run.getPhysical(),run.getBalances(),true,null,null,Collections.singletonList(audit),null,run.getFinancial(),run.getExchange(),run.getBuybacks(),state);
	}
	public static BankRun reviewStorage(BankRun old,PotionObservation seen,Map<Integer,Long> reviewed,UUID session,Instant at)
	{return reviewStorage(old,seen,reviewed,session,at,"Player reviewed Potion Storage ownership; original bank baseline retained.");}
	private static BankRun reviewStorage(BankRun old,PotionObservation seen,Map<Integer,Long> reviewed,UUID session,Instant at,String reason)
	{
		requireAnchored(old);if(!seen.isVerified()){throw new IllegalArgumentException("Open Potion Storage to review verified quantities.");}
		reviewed.forEach((id,value)->Math.subtractExact(value,old.getPotions().balance(id)));
		PotionUpdate input=new PotionUpdate(seen,reviewed);PotionEngine.Result result=PotionEngine.apply(old,old.getPhysical(),old.getBalances(),input,session);
		BankAudit audit=new BankAudit(UUID.randomUUID(),session,at,BankAudit.Kind.STORAGE_REVIEW,Collections.emptyMap(),Collections.emptyMap(),reason,Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),null,null,Collections.emptyMap(),input);
		return next(old,old.getPhysical(),result.getBank(),true,audit,old.getHostSummary());
	}
	public static BankRun correctStorage(BankRun old,int id,long desired,Instant at)
	{
		PotionState state=old.getPotions();PotionType type=state.getTypes().get(id);
		if(type==null||!state.isReviewed()||state.getObservedAt()==null){throw new IllegalArgumentException("Review the verified Potion Storage baseline before individual corrections.");}
		long delta=Math.subtractExact(desired,state.balance(id));
		Map<Integer,Long> values=new TreeMap<>(state.getBalances());values.put(id,desired);
		PotionObservation seen=new PotionObservation(true,state.getTypes(),state.getPhysical(),state.getObservedAt());
		String reason="Manual Potion Storage correction: "+type.getName()+"; "+state.balance(id)+" -> "+desired+" "+type.unit()+"; adjustment "+delta+". Last verified physical observation and original baseline retained.";
		return reviewStorage(old,seen,values,state.getSession(),at,reason);
	}
	/** GE changes are observational audit operations; P/B and the immutable initial bank stay unchanged. */
	public static BankRun exchange(BankRun old,com.fromscratch.ge.GeObservation observation)
	{
		if(!old.isActive()){return old;}
		com.fromscratch.ge.GeEngine.Result result=com.fromscratch.ge.GeEngine.observe(old.getExchange(),observation,old.getRunId());
		if(result.getEvidence().emptyEvidence()){return old;}
		BankAudit audit=new BankAudit(com.fromscratch.ge.GeEngine.id(old.getRunId()+":"+observation.getEventId()),observation.getSession(),observation.getAt(),BankAudit.Kind.EXCHANGE,Collections.emptyMap(),Collections.emptyMap(),"Grand Exchange observation recorded.",Collections.emptyList(),result.getEvidence());
		return next(old,old.getPhysical(),old.getBalances(),old.isAnchored(),audit,old.getHostSummary());
	}
	/** One paid entitlement operation; raw bank observations and historical gain quotes are unchanged. */
	public static BankRun buyback(BankRun old,String catalogId,UUID purchaseId,UUID session,Instant at)
	{return buyback(old,catalogId,purchaseId,session,at,com.fromscratch.pouch.PouchObservation.unknown());}
	public static BankRun buyback(BankRun old,String catalogId,UUID purchaseId,UUID session,Instant at,com.fromscratch.pouch.PouchObservation contents)
	{
		requireAnchored(old);
		com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalogId);
		boolean pouch=catalogId.equals("rune_pouch_family");if(pouch&&(!contents.fresh(at)||old.physical(contents.getPouchId())!=1||com.fromscratch.buyback.BuybackCatalog.variants(catalogId).keySet().stream().mapToLong(old::physical).reduce(0L,Math::addExact)!=1)){throw new IllegalArgumentException("Rune Pouch data is not available yet. Keep the bank open, wait a game tick and retry.");}
		Map<Integer,Long> adjustments=new TreeMap<>(),balances=new TreeMap<>(old.getBalances());if(pouch){contents.getQuantities().forEach((id,n)->{adjustments.put(id,Math.negateExact(n));balances.put(id,Math.subtractExact(old.balance(id),n));});}
		com.fromscratch.buyback.BuybackPurchase purchase=new com.fromscratch.buyback.BuybackPurchase(purchaseId,catalogId,com.fromscratch.buyback.BuybackCatalog.cost(entry,old.getBuybacks().getEntitlements()),at,com.fromscratch.buyback.BuybackEngine.purchaseFloors(old,entry),pouch?contents.getPouchId():-1,pouch?contents.getQuantities():Collections.emptyMap());
		BankAudit change=new BankAudit(purchaseId,session,at,BankAudit.Kind.BUYBACK,Collections.emptyMap(),adjustments,"Player confirmed legacy buyback: "+entry.getName(),Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),purchase);
		return next(old,old.getPhysical(),balances,true,change,old.getHostSummary());
	}
	/** One exact paid copy, recorded in the existing payment/audit transaction without changing raw P/B. */
	public static BankRun buySelected(BankRun old,String catalogId,int itemId,UUID purchaseId,UUID session,Instant at)
	{
		requireAnchored(old);com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalogId);
		com.fromscratch.buyback.BuybackPurchase receipt=new com.fromscratch.buyback.BuybackPurchase(purchaseId,catalogId,entry.getPrice(),at,com.fromscratch.buyback.BuybackEngine.selectedFloors(old,itemId),itemId);
		BankAudit change=new BankAudit(purchaseId,session,at,BankAudit.Kind.BUYBACK,Collections.emptyMap(),Collections.emptyMap(),"Player confirmed one legacy copy: "+com.fromscratch.buyback.SelectedBuybacks.name(itemId),Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),receipt);
		return next(old,old.getPhysical(),old.getBalances(),true,change,old.getHostSummary());
	}
	/** Scoped explicit setup/review; restoring a misidentified deposit recognizes only current high-water progress. */
	public static BankRun buyBag(BankRun old,UUID purchaseId,UUID session,Instant at,com.fromscratch.buyback.BagObservation contents)
	{
		requireAnchored(old);String catalog="looting_bag_family";
		if(!contents.fresh(at)||old.physical(contents.getItemId())!=1||com.fromscratch.buyback.BuybackCatalog.variants(catalog).keySet().stream().mapToLong(old::physical).reduce(0L,Math::addExact)!=1){throw new IllegalArgumentException(com.fromscratch.buyback.BagObservation.INSTRUCTION);}
		Map<Integer,Long> adjustments=new TreeMap<>(),balances=new TreeMap<>(old.getBalances());contents.getQuantities().forEach((id,n)->{adjustments.put(id,Math.negateExact(n));balances.put(id,Math.subtractExact(old.balance(id),n));});
		com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalog);
		com.fromscratch.buyback.BuybackPurchase purchase=new com.fromscratch.buyback.BuybackPurchase(purchaseId,catalog,entry.getPrice(),at,com.fromscratch.buyback.BuybackEngine.purchaseFloors(old,entry),contents.getItemId(),contents.getQuantities(),contents.getState());
		BankAudit change=new BankAudit(purchaseId,session,at,BankAudit.Kind.BUYBACK,Collections.emptyMap(),adjustments,"Player confirmed legacy buyback: Looting Bag ("+contents.getState()+")",Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),purchase);
		return next(old,old.getPhysical(),balances,true,change,old.getHostSummary());
	}
	public static BankRun pouch(BankRun old,com.fromscratch.pouch.PouchAction action,UUID session,Instant at,Map<Integer,PriceQuote> prices)
	{
		requireAnchored(old);boolean present=com.fromscratch.buyback.BuybackCatalog.variants("rune_pouch_family").keySet().stream().anyMatch(id->old.physical(id)>0);
		if(!present){throw new IllegalArgumentException("Bank your Rune Pouch or Divine Rune Pouch before reviewing its contents.");}
		Map<Integer,Long> balances=new TreeMap<>(old.getBalances());UUID id=UUID.randomUUID();List<ValuationEvent> values=Collections.emptyList();
		if(action.getKind()==com.fromscratch.pouch.PouchAction.Kind.RESTORE_EARNED){action.getQuantities().forEach((r,n)->balances.put(r,Math.addExact(old.balance(r),n)));values=FinancialEngine.recognize(old.getFinancial(),balances,id,prices,at);}
		BankAudit change=new BankAudit(id,session,at,BankAudit.Kind.POUCH,Collections.emptyMap(),differences(old.getBalances(),balances),"Player confirmed legacy pouch transfer review: "+action.getReason(),values,com.fromscratch.ge.GeUpdate.empty(),null,action,Collections.emptyMap());
		return next(old,old.getPhysical(),balances,true,change,old.getHostSummary());
	}
	/** A recorded statistical expense never changes bank quantities or the bank coin balance. */
	public static BankRun expense(BankRun old,long gp,String reason,UUID session,Instant at)
	{
		requireAnchored(old);if(gp<=0){throw new IllegalArgumentException("Enter a positive loss/expense amount.");}
		UUID id=UUID.randomUUID();ValuationEvent event=new ValuationEvent(ValuationEvent.identity(id,ValuationEvent.Kind.EXPENSE,net.runelite.api.gameval.ItemID.COINS),ValuationEvent.Kind.EXPENSE,net.runelite.api.gameval.ItemID.COINS,gp,PriceQuote.coins(at),reason);
		BankAudit change=new BankAudit(id,session,at,BankAudit.Kind.EXPENSE,Collections.emptyMap(),Collections.emptyMap(),"Player confirmed recorded loss/expense: "+reason,Collections.singletonList(event));
		return next(old,old.getPhysical(),old.getBalances(),true,change,old.getHostSummary());
	}
	public static Map<Integer, Long> differences(Map<Integer, Long> before, Map<Integer, Long> after)
	{
		Map<Integer, Long> delta = new TreeMap<>(); Set<Integer> ids = new TreeSet<>(before.keySet()); ids.addAll(after.keySet());
		for (int id : ids) { long n = Math.subtractExact(after.getOrDefault(id, 0L), before.getOrDefault(id, 0L)); if (n != 0) { delta.put(id, n); } }
		return delta;
	}
	private static BankAudit audit(UUID session, Instant at, BankAudit.Kind kind, Map<Integer, Long> physical, Map<Integer, Long> balance, String reason)
	{ return new BankAudit(UUID.randomUUID(), session, at, kind, physical, balance, reason); }
	private static void requireAnchored(BankRun run) { if (!run.isActive() || !run.isAnchored()) { throw new IllegalArgumentException("Confirm the one-time bank migration first."); } }
}
