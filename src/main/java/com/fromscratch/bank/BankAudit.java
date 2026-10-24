package com.fromscratch.bank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

/** One durable bank operation with optional high-water valuation/explicit-expense evidence, never inferred acquisition provenance. */
@Value
public class BankAudit
{
	public enum Kind { START, AUTOMATIC, SESSION_CHECK, CORRECTION, MIGRATION, ANCHOR, RESET, FINANCIAL_BASELINE, EXPENSE, EXCHANGE_BASELINE, EXCHANGE, BUYBACK_BASELINE, BUYBACK, POUCH_BASELINE, POUCH, STORAGE, STORAGE_REVIEW }
	UUID id;
	UUID session;
	Instant at;
	Kind kind;
	Map<Integer, Long> physicalChanges;
	Map<Integer, Long> balanceChanges;
	String reason;
	java.util.List<com.fromscratch.wealth.ValuationEvent> valuations;
	com.fromscratch.ge.GeUpdate exchange;
	com.fromscratch.buyback.BuybackPurchase buyback;
	com.fromscratch.pouch.PouchAction pouch;
	Map<Integer,Long> pouchTransfers;
	com.fromscratch.potion.PotionUpdate potion;
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason)
	{ this(id,session,at,kind,physicalChanges,balanceChanges,reason,java.util.Collections.emptyList()); }
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason, java.util.List<com.fromscratch.wealth.ValuationEvent> valuations)
	{this(id,session,at,kind,physicalChanges,balanceChanges,reason,valuations,com.fromscratch.ge.GeUpdate.empty());}
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason, java.util.List<com.fromscratch.wealth.ValuationEvent> valuations, com.fromscratch.ge.GeUpdate exchange)
	{this(id,session,at,kind,physicalChanges,balanceChanges,reason,valuations,exchange,null);}
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason, java.util.List<com.fromscratch.wealth.ValuationEvent> valuations,
		com.fromscratch.ge.GeUpdate exchange, com.fromscratch.buyback.BuybackPurchase buyback)
	{this(id,session,at,kind,physicalChanges,balanceChanges,reason,valuations,exchange,buyback,null,java.util.Collections.emptyMap());}
	public BankAudit(UUID id,UUID session,Instant at,Kind kind,Map<Integer,Long> physicalChanges,Map<Integer,Long> balanceChanges,String reason,
		java.util.List<com.fromscratch.wealth.ValuationEvent> valuations,com.fromscratch.ge.GeUpdate exchange,com.fromscratch.buyback.BuybackPurchase buyback,com.fromscratch.pouch.PouchAction pouch,Map<Integer,Long> pouchTransfers)
	{this(id,session,at,kind,physicalChanges,balanceChanges,reason,valuations,exchange,buyback,pouch,pouchTransfers,null);}
	public BankAudit(UUID id,UUID session,Instant at,Kind kind,Map<Integer,Long> physicalChanges,Map<Integer,Long> balanceChanges,String reason,
		java.util.List<com.fromscratch.wealth.ValuationEvent> valuations,com.fromscratch.ge.GeUpdate exchange,com.fromscratch.buyback.BuybackPurchase buyback,com.fromscratch.pouch.PouchAction pouch,Map<Integer,Long> pouchTransfers,com.fromscratch.potion.PotionUpdate potion)
	{
		this.id = java.util.Objects.requireNonNull(id);
		this.session = java.util.Objects.requireNonNull(session);
		this.at = java.util.Objects.requireNonNull(at);
		this.kind = java.util.Objects.requireNonNull(kind);
		this.physicalChanges = BankRun.quantities(physicalChanges, false);
		this.balanceChanges = BankRun.quantities(balanceChanges, false);
		if (reason == null || reason.length() > 500) { throw new IllegalArgumentException("Invalid bank change description"); }
		this.reason = reason;
		this.valuations=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(valuations));
		this.exchange=java.util.Objects.requireNonNull(exchange);this.buyback=buyback;
		if((kind==Kind.BUYBACK)!=(buyback!=null)||buyback!=null&&(!id.equals(buyback.getId())||!at.equals(buyback.getAt()))){throw new IllegalArgumentException("Invalid purchase audit evidence");}
		this.pouch=pouch;this.pouchTransfers=com.fromscratch.pouch.PouchState.quantities(pouchTransfers);
		this.potion=potion;
		if((kind==Kind.POUCH)!=(pouch!=null)||kind!=Kind.AUTOMATIC&&!this.pouchTransfers.isEmpty()){throw new IllegalArgumentException("Invalid pouch audit evidence");}
	}
}
