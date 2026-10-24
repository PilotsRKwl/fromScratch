package com.fromscratch.potion;

import com.fromscratch.bank.*;
import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Checked dose reconciliation inside the bank transaction. No inventory inference or game actions. */
public final class PotionEngine
{
	private PotionEngine() { }
	@Value public static class Result { PotionState state;Map<Integer,Long> bank;Map<Integer,Long> exclusions; }
	public static Result apply(BankRun run,Map<Integer,Long> physical,Map<Integer,Long> ordinary,PotionUpdate input,UUID session)
	{
		PotionState old=run.getPotions();PotionObservation seen=input==null?null:input.getObservation();
		if(seen==null||!seen.isVerified()&&old.getTypes().isEmpty()&&seen.getTypes().isEmpty()){return new Result(old,ordinary,Collections.emptyMap());}
		boolean fresh=seen.isVerified();if(!fresh){Map<Integer,PotionType> definitions=new TreeMap<>(old.getTypes());definitions.putAll(seen.getTypes());seen=new PotionObservation(true,definitions,old.getPhysical(),old.getObservedAt()==null?Instant.EPOCH:old.getObservedAt());}
		boolean first=old.getObservedAt()==null;boolean same=Objects.equals(session,old.getSession());
		Map<Integer,PotionType> types=new TreeMap<>(old.getTypes());types.putAll(seen.getTypes());
		boolean reviewed=old.isReviewed()&&!first&&old.getTypes().equals(seen.getTypes());
		if(fresh&&!Objects.equals(session,old.getObservedSession())&&!first&&!old.getPhysical().equals(seen.getQuantities())){reviewed=false;}
		if(!fresh&&!Objects.equals(session,old.getObservedSession())&&types.values().stream().anyMatch(type->type.doses(physical)!=type.doses(run.getPhysical()))){reviewed=false;}
		Map<Integer,Long> stored=new TreeMap<>(old.getBalances()),remainder=new TreeMap<>(old.getRemainder());
		Map<Integer,Long> bo=new TreeMap<>(same?old.getBankOut():Collections.emptyMap()),bvout=new TreeMap<>(same?old.getBankOwnedOut():Collections.emptyMap()),so=new TreeMap<>(same?old.getStoreOut():Collections.emptyMap()),svout=new TreeMap<>(same?old.getStoreOwnedOut():Collections.emptyMap());
		// A failed native read is different from a closed, unchanged same-login store.
		// Unmatched bank additions could be legacy withdrawals from that unreadable store.
		if(!fresh&&input.getObservation().getTypes().isEmpty())
		{
			for(PotionType type:types.values()){long added=Math.subtractExact(type.doses(physical),type.doses(run.getPhysical()));long returned=Math.addExact(bo.getOrDefault(type.getId(),0L),so.getOrDefault(type.getId(),0L));if(added>returned){reviewed=false;}}
		}
		Map<Integer,Long> bank=new TreeMap<>(ordinary),exclusions=new TreeMap<>();
		if(input.getReview()!=null)
		{
			if(!fresh){throw new IllegalArgumentException("Open Potion Storage for a current review.");}
			if(!seen.getTypes().keySet().containsAll(input.getReview().keySet())){throw new IllegalArgumentException("Unknown potion in review");}
			for(Map.Entry<Integer,Long> e:input.getReview().entrySet()){if(e.getValue()>seen.getQuantities().getOrDefault(e.getKey(),0L)){throw new IllegalArgumentException("Available storage units cannot exceed verified physical stock.");}}
			if(old.isReviewed())
			{
				for(int id:types.keySet()){if(old.balance(id)!=input.getReview().getOrDefault(id,0L)){bo.remove(id);bvout.remove(id);so.remove(id);svout.remove(id);}}
			}
			else{bo.clear();bvout.clear();so.clear();svout.clear();}
			stored=new TreeMap<>(input.getReview());reviewed=true;
		}
		else if(reviewed)
		{
			for(PotionType type:types.values())
			{
				int id=type.getId();long b=Math.subtractExact(type.doses(physical),type.doses(run.getPhysical())),s=Math.subtractExact(seen.getQuantities().getOrDefault(id,0L),old.getPhysical().getOrDefault(id,0L));
				long vb=old.bankBalance(type,run.getBalances()),vs=old.balance(id);boolean conversion=bo.getOrDefault(id,0L)>0||so.getOrDefault(id,0L)>0;
				// Opposite changes copied at the same GameTick are a single cross-container movement.
				if(b<0&&s>0){long q=Math.min(Math.negateExact(b),s),owned=Math.min(Math.max(0,vb),q);vb=Math.subtractExact(vb,owned);vs=Math.addExact(vs,owned);b=Math.addExact(b,q);s-=q;conversion=true;}
				else if(s<0&&b>0){long q=Math.min(Math.negateExact(s),b),owned=Math.min(Math.max(0,vs),q);vs=Math.subtractExact(vs,owned);vb=Math.addExact(vb,owned);s=Math.addExact(s,q);b-=q;conversion=true;}
				// Withdrawal budgets are once-consumed and valid only in this continuous host bank session.
				// Two possible source containers make attribution ambiguous: retain evidence for review.
				if((b>0||s>0)&&bo.getOrDefault(id,0L)>0&&so.getOrDefault(id,0L)>0){reviewed=false;break;}
				if(b>0)
				{
					long q=Math.min(b,so.getOrDefault(id,0L)),owned=Math.min(q,svout.getOrDefault(id,0L));
					if(q>0){vb=Math.addExact(vb,owned);vs=Math.addExact(vs,q-owned);take(so,svout,id,q,owned);b-=q;conversion=true;}
					if(b>0){long returned=Math.min(b,bo.getOrDefault(id,0L));take(bo,bvout,id,returned,Math.min(returned,bvout.getOrDefault(id,0L)));vb=Math.addExact(vb,b);}
				}
				if(s>0)
				{
					long q=Math.min(s,bo.getOrDefault(id,0L)),owned=Math.min(q,bvout.getOrDefault(id,0L));
					if(q>0){vs=Math.addExact(vs,owned);vb=Math.addExact(vb,q-owned);take(bo,bvout,id,q,owned);s-=q;conversion=true;}
					if(s>0){long returned=Math.min(s,so.getOrDefault(id,0L));take(so,svout,id,returned,Math.min(returned,svout.getOrDefault(id,0L)));vs=Math.addExact(vs,s);}
				}
				if(b<0){long q=Math.negateExact(b);add(bo,id,q);add(bvout,id,Math.min(Math.max(0,vb),q));vb=Math.addExact(vb,b);}
				if(s<0){long q=Math.negateExact(s);add(so,id,q);add(svout,id,Math.min(Math.max(0,vs),q));vs=Math.addExact(vs,s);}
				long changedIds=type.getItems().keySet().stream().filter(item->!Objects.equals(run.getPhysical().get(item),physical.get(item))).count();
				allocate(type,vb,physical,bank,remainder);stored.put(id,vs);
				if(conversion||changedIds>1){for(int item:type.getItems().keySet()){long change=Math.subtractExact(bank.getOrDefault(item,0L),run.balance(item));if(change>0){exclusions.put(item,change);}}}
			}
			if(!reviewed){bank=new TreeMap<>(ordinary);stored=new TreeMap<>(old.getBalances());remainder=new TreeMap<>(old.getRemainder());bo.clear();bvout.clear();so.clear();svout.clear();exclusions.clear();}
		}
		if(!reviewed&&input.getReview()==null)
		{
			// Unknown store stock must not turn a deposit from that store into earned bank bottles.
			// Preserve existing bank doses and once-consumed same-session returns; other gains await review.
			for(PotionType type:types.values())
			{
				int id=type.getId();long delta=Math.subtractExact(type.doses(physical),type.doses(run.getPhysical())),virtual=old.bankBalance(type,run.getBalances());
				if(delta<0){long q=Math.negateExact(delta);add(bo,id,q);add(bvout,id,Math.min(Math.max(0,virtual),q));virtual=Math.addExact(virtual,delta);}
				else if(delta>0){long q=Math.min(delta,bo.getOrDefault(id,0L));virtual=Math.addExact(virtual,q);take(bo,bvout,id,q,Math.min(q,bvout.getOrDefault(id,0L)));}
				allocate(type,virtual,physical,bank,remainder);for(int item:type.getItems().keySet()){long change=Math.subtractExact(bank.getOrDefault(item,0L),run.balance(item));if(change>0){exclusions.put(item,change);}}
			}
		}
		PotionState state=new PotionState(types,fresh&&first?seen.getQuantities():old.getInitial(),seen.getQuantities(),stored,remainder,bo,bvout,so,svout,fresh?seen.getAt():old.getObservedAt(),session,fresh?session:old.getObservedSession(),reviewed);
		return new Result(state,bank,exclusions);
	}
	private static void add(Map<Integer,Long> map,int id,long n){map.put(id,Math.addExact(map.getOrDefault(id,0L),n));}
	private static void take(Map<Integer,Long> units,Map<Integer,Long> owned,int id,long q,long v){units.put(id,units.getOrDefault(id,0L)-q);owned.put(id,owned.getOrDefault(id,0L)-v);}
	private static void allocate(PotionType type,long target,Map<Integer,Long> physical,Map<Integer,Long> bank,Map<Integer,Long> remainder)
	{
		if(type.doses(bank)==target&&remainder.getOrDefault(type.getId(),0L)==0){return;}
		if(target>type.doses(physical)){throw new IllegalArgumentException("Virtual potion doses exceed the physical bank.");}
		Map<Integer,Long> prior=new TreeMap<>(bank);for(int id:type.getItems().keySet()){bank.remove(id);}
		List<Integer> ids=new ArrayList<>(type.getItems().keySet());ids.sort(Comparator.<Integer>comparingInt(type.getItems()::get).reversed());long left=target;
		if(left>=0)
		{
			for(int pass=0;pass<2;pass++){for(int id:ids){int dose=type.getItems().get(id);long cap=pass==0?Math.max(0,prior.getOrDefault(id,0L)):physical.getOrDefault(id,0L)-bank.getOrDefault(id,0L);long q=Math.min(Math.min(cap,physical.getOrDefault(id,0L)-bank.getOrDefault(id,0L)),left/dose);if(q>0){add(bank,id,q);left-=q*dose;}}}
		}
		else{int id=ids.get(ids.size()-1),dose=type.getItems().get(id);long q=Math.floorDiv(left,dose);bank.put(id,q);left=Math.subtractExact(left,Math.multiplyExact(q,dose));}
		remainder.put(type.getId(),left);
	}
}
