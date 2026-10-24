package com.fromscratch.potion;

import com.fromscratch.bank.BankRun;
import com.fromscratch.wealth.PriceQuote;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

/** Same cached per-dose quote for bank bottles, remainder doses and storage; no double valuation. */
public final class PotionWealth
{
	private PotionWealth() { }
	@lombok.Value public static class Value { BigInteger gp;int missing;int stale;int priced;Instant oldest; }
	public static Set<Integer> pooledItems(BankRun run)
	{Set<Integer> result=new HashSet<>();if(run.getPotions().isReviewed()){run.getPotions().getTypes().values().forEach(t->result.addAll(t.getItems().keySet()));}return result;}
	public static Value calculate(BankRun run,Map<Integer,PriceQuote> prices)
	{
		PotionState state=run.getPotions();if(!state.isReviewed()){return new Value(BigInteger.ZERO,state.getObservedAt()==null?0:1,0,0,null);}
		BigInteger value=BigInteger.ZERO;int missing=0,stale=0,priced=0;Instant oldest=null;
		for(PotionType type:state.getTypes().values())
		{
			long units=Math.addExact(state.bankBalance(type,run.getBalances()),state.balance(type.getId()));if(units==0){continue;}
			PriceQuote quote=prices.get(type.displayId());if(quote==null||quote.getUnitGp()==null){missing++;continue;}priced++;if(quote.isStale()){stale++;}if(oldest==null||quote.getReceivedAt().isBefore(oldest)){oldest=quote.getReceivedAt();}
			BigInteger amount=BigInteger.valueOf(units).multiply(BigInteger.valueOf(quote.getUnitGp())),divisor=BigInteger.valueOf(type.getItems().get(type.displayId()));BigInteger[] division=amount.divideAndRemainder(divisor);if(amount.signum()<0&&division[1].signum()!=0){division[0]=division[0].subtract(BigInteger.ONE);}value=value.add(division[0]);
		}
		return new Value(value,missing,stale,priced,oldest);
	}
}
