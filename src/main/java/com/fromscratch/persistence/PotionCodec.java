package com.fromscratch.persistence;

import com.fromscratch.potion.*;
import com.google.gson.*;
import java.time.Instant;
import java.util.*;

/** Explicit version-10 checkpoint extension; old checkpoints decode to unknown storage. */
final class PotionCodec
{
	private PotionCodec() { }
	static JsonArray types(Map<Integer,PotionType> types)
	{JsonArray a=new JsonArray();types.values().forEach(t->{JsonObject p=new JsonObject();p.addProperty("id",t.getId());p.addProperty("name",t.getName());p.addProperty("unfinished",t.isUnfinished());Map<Integer,Long> items=new TreeMap<>();t.getItems().forEach((id,q)->items.put(id,q.longValue()));p.add("items",BankRunCodec.map(items));a.add(p);});return a;}
	static Map<Integer,PotionType> types(JsonArray a)
	{
		if(a.size()>256){throw new IllegalArgumentException("Too many potion types");}Map<Integer,PotionType> types=new TreeMap<>();
		for(JsonElement e:a){JsonObject p=e.getAsJsonObject();BankRunCodec.keys(p,"id","name","unfinished","items");Map<Integer,Integer> items=new TreeMap<>();BankRunCodec.readMap(p.getAsJsonObject("items")).forEach((id,q)->items.put(id,Math.toIntExact(q)));int id=Math.toIntExact(BankRunCodec.number(p.get("id")));if(!p.get("unfinished").isJsonPrimitive()||!p.get("unfinished").getAsJsonPrimitive().isBoolean()||types.put(id,new PotionType(id,BankRunCodec.str(p,"name"),p.get("unfinished").getAsBoolean(),items))!=null){throw new IllegalArgumentException("Invalid potion type");}}
		return types;
	}
	static JsonObject observation(PotionObservation o)
	{JsonObject p=new JsonObject();p.addProperty("verified",o.isVerified());p.add("types",types(o.getTypes()));p.add("physical",BankRunCodec.map(o.getQuantities()));p.addProperty("at",o.getAt().toString());return p;}
	static PotionObservation observation(JsonObject p)
	{BankRunCodec.keys(p,"verified","types","physical","at");if(!p.get("verified").getAsJsonPrimitive().isBoolean()){throw new IllegalArgumentException("Invalid verification flag");}return new PotionObservation(p.get("verified").getAsBoolean(),types(p.getAsJsonArray("types")),BankRunCodec.readMap(p.getAsJsonObject("physical")),Instant.parse(BankRunCodec.str(p,"at")));}
	static JsonObject state(PotionState s)
	{
		JsonObject p=new JsonObject();p.add("types",types(s.getTypes()));String[] names={"initial","physical","balances","remainder","bankOut","bankOwnedOut","storeOut","storeOwnedOut"};List<Map<Integer,Long>> maps=Arrays.asList(s.getInitial(),s.getPhysical(),s.getBalances(),s.getRemainder(),s.getBankOut(),s.getBankOwnedOut(),s.getStoreOut(),s.getStoreOwnedOut());for(int i=0;i<names.length;i++){p.add(names[i],BankRunCodec.map(maps.get(i)));}p.addProperty("at",s.getObservedAt()==null?null:s.getObservedAt().toString());p.addProperty("session",s.getSession()==null?null:s.getSession().toString());p.addProperty("observedSession",s.getObservedSession()==null?null:s.getObservedSession().toString());p.addProperty("reviewed",s.isReviewed());return p;
	}
	static PotionState state(JsonObject p)
	{
		BankRunCodec.keys(p,"types","initial","physical","balances","remainder","bankOut","bankOwnedOut","storeOut","storeOwnedOut","at","session","observedSession","reviewed");if(!p.get("reviewed").getAsJsonPrimitive().isBoolean()){throw new IllegalArgumentException("Invalid review flag");}String at=BankRunCodec.nullable(p,"at"),session=BankRunCodec.nullable(p,"session");return new PotionState(types(p.getAsJsonArray("types")),BankRunCodec.readMap(p.getAsJsonObject("initial")),BankRunCodec.readMap(p.getAsJsonObject("physical")),BankRunCodec.readMap(p.getAsJsonObject("balances")),BankRunCodec.readMap(p.getAsJsonObject("remainder")),BankRunCodec.readMap(p.getAsJsonObject("bankOut")),BankRunCodec.readMap(p.getAsJsonObject("bankOwnedOut")),BankRunCodec.readMap(p.getAsJsonObject("storeOut")),BankRunCodec.readMap(p.getAsJsonObject("storeOwnedOut")),at==null?null:Instant.parse(at),session==null?null:UUID.fromString(session),p.get("observedSession").isJsonNull()?null:UUID.fromString(BankRunCodec.str(p,"observedSession")),p.get("reviewed").getAsBoolean());
	}
	static JsonObject update(PotionUpdate u)
	{JsonObject p=new JsonObject();p.add("observation",observation(u.getObservation()));p.add("review",u.getReview()==null?JsonNull.INSTANCE:BankRunCodec.map(u.getReview()));p.add("exclusions",BankRunCodec.map(u.getExclusions()));return p;}
	static PotionUpdate update(JsonObject p)
	{BankRunCodec.keys(p,"observation","review","exclusions");return new PotionUpdate(observation(p.getAsJsonObject("observation")),p.get("review").isJsonNull()?null:BankRunCodec.readMap(p.getAsJsonObject("review")),BankRunCodec.readMap(p.getAsJsonObject("exclusions")));}
}
