package com.fromscratch.wealth;

import com.fromscratch.bank.BankRun;
import java.time.Instant;
import java.util.*;
import java.math.BigInteger;
import lombok.Value;

/** Derived worker-calculated current market subtotal, never a persisted or speculative bank operation. */
@Value
public class BankValuation
{
	UUID revision;
	Long subtotalGp;
	int missingTypes;
	int staleTypes;
	Instant oldestReceivedAt;
	int pricedTypes;
	Long peakGp;
	public BankValuation(UUID revision,Long subtotalGp,int missingTypes,int staleTypes,Instant oldestReceivedAt,int pricedTypes){this(revision,subtotalGp,missingTypes,staleTypes,oldestReceivedAt,pricedTypes,null);}
	public BankValuation(UUID revision,Long subtotalGp,int missingTypes,int staleTypes,Instant oldestReceivedAt,int pricedTypes,Long peakGp){this.revision=revision;this.subtotalGp=subtotalGp;this.missingTypes=missingTypes;this.staleTypes=staleTypes;this.oldestReceivedAt=oldestReceivedAt;this.pricedTypes=pricedTypes;this.peakGp=peakGp;}
	public BankValuation withPeak(Long peak){return new BankValuation(revision,subtotalGp,missingTypes,staleTypes,oldestReceivedAt,pricedTypes,peak);}
	public static BankValuation calculate(BankRun run,Map<Integer,PriceQuote> prices)
	{
		// Effective coins already subtract committed buybacks. Never deduct them a second time.
		BigInteger total=BigInteger.ZERO;int missing=0,stale=0,priced=0;Instant oldest=null;
		Set<Integer> pooled=com.fromscratch.potion.PotionWealth.pooledItems(run);
		for(Map.Entry<Integer,Long> entry:com.fromscratch.buyback.BuybackEngine.balances(run).entrySet())
		{
			if(entry.getValue()==0||pooled.contains(entry.getKey())){continue;}PriceQuote quote=prices.get(entry.getKey());
			if(entry.getKey()==net.runelite.api.gameval.ItemID.COINS){quote=PriceQuote.coins(Instant.EPOCH);}
			if(quote==null||quote.getUnitGp()==null){missing++;continue;}
			priced++;
			if(quote.isStale()){stale++;}if(quote.getSource()!=PriceQuote.Source.COINS&&(oldest==null||quote.getReceivedAt().isBefore(oldest))){oldest=quote.getReceivedAt();}
			total=total.add(BigInteger.valueOf(entry.getValue()).multiply(BigInteger.valueOf(quote.getUnitGp())));
		}
		com.fromscratch.potion.PotionWealth.Value potions=com.fromscratch.potion.PotionWealth.calculate(run,prices);total=total.add(potions.getGp());missing+=potions.getMissing();stale+=potions.getStale();priced+=potions.getPriced();if(potions.getOldest()!=null&&(oldest==null||potions.getOldest().isBefore(oldest))){oldest=potions.getOldest();}
		Long value;try{value=total.longValueExact();}catch(ArithmeticException ex){value=null;}
		return new BankValuation(run.getRevision(),value,missing,stale,oldest,priced);
	}
	public String label() { return subtotalGp==null?"Out of range":pricedTypes==0&&missingTypes>0?"Unavailable":(staleTypes>0?"~":"")+GpFormatter.compact(subtotalGp)+(missingTypes>0?"*":""); }
	public String explanation()
	{
		return (subtotalGp==null?"Valuation exceeds signed long range.":pricedTypes==0&&missingTypes>0?"No verified current price records available.":GpFormatter.exact(subtotalGp)+"; "+(missingTypes>0?"priced subtotal":"signed bank market estimate"))+". "+missingTypes+" unpriced types; "+staleTypes+" stale types. RuneLite GE guide; oldest received: "+(oldestReceivedAt==null?"N/A":oldestReceivedAt)+". Coins included once. Not a spendable wallet.";
	}
}
