package com.fromscratch.persistence;

import com.fromscratch.buyback.*;
import com.google.gson.*;
import java.time.Instant;
import java.util.*;
import static com.fromscratch.persistence.BankRunCodec.*;

/** Explicit receipt trees: historical family encoding unchanged; format 11 adds selected item/quantity. */
final class BuybackCodec
{
	private BuybackCodec(){}
	static JsonObject purchase(BuybackPurchase p)
	{
		JsonObject j=new JsonObject(),floors=new JsonObject();p.getBankFloors().forEach(floors::addProperty);j.add("floors",floors);j.addProperty("id",p.getId().toString());j.addProperty("catalog",p.getCatalogId());j.addProperty("gp",p.getGp());j.addProperty("at",p.getAt().toString());if(p.getSelectedItemId()>0){JsonObject selected=new JsonObject();selected.addProperty("item",p.getSelectedItemId());selected.addProperty("quantity",1);j.add("selected",selected);}if(p.getPouchItemId()>0){JsonObject pouch=new JsonObject();pouch.addProperty("item",p.getPouchItemId());pouch.add("runes",map(p.getLegacyRunes()));if(p.getBagConfirmation()==BagObservation.State.PLAYER_CONFIRMED_EMPTY){pouch.addProperty("confirmation",p.getBagConfirmation().name());}j.add(p.getCatalogId().equals("looting_bag_family")?"bag":"pouch",pouch);}return j;
	}
	static BuybackPurchase purchase(JsonObject j)
	{
		if(j.has("selected")){keys(j,"id","catalog","gp","at","floors","selected");JsonObject selected=j.getAsJsonObject("selected");keys(selected,"item","quantity");if(number(selected.get("quantity"))!=1){throw new IllegalArgumentException("Each purchase must pay for one copy");}Map<String,Long> floors=new TreeMap<>();j.getAsJsonObject("floors").entrySet().forEach(e->floors.put(e.getKey(),number(e.getValue())));return new BuybackPurchase(UUID.fromString(str(j,"id")),str(j,"catalog"),number(j.get("gp")),Instant.parse(str(j,"at")),floors,Math.toIntExact(number(selected.get("item"))));}
		String content=j.has("bag")?"bag":"pouch";
		if(j.has(content)){keys(j,"id","catalog","gp","at","floors",content);JsonObject stored=j.getAsJsonObject(content);if(stored.has("confirmation")){keys(stored,"item","runes","confirmation");if(!content.equals("bag")||!str(stored,"confirmation").equals(BagObservation.State.PLAYER_CONFIRMED_EMPTY.name())){throw new IllegalArgumentException("Invalid bag declaration");}}else{keys(stored,"item","runes");}}else{keys(j,"id","catalog","gp","at","floors");}
		if(j.has("bag")&&!str(j,"catalog").equals("looting_bag_family")||j.has("pouch")&&str(j,"catalog").equals("looting_bag_family")){throw new IllegalArgumentException("Wrong storage receipt category");}
		Map<String,Long> floors=new TreeMap<>();j.getAsJsonObject("floors").entrySet().forEach(e->floors.put(e.getKey(),number(e.getValue())));
		BuybackPurchase purchase=new BuybackPurchase(UUID.fromString(str(j,"id")),str(j,"catalog"),number(j.get("gp")),Instant.parse(str(j,"at")),floors,j.has(content)?Math.toIntExact(number(j.getAsJsonObject(content).get("item"))):-1,j.has(content)?readMap(j.getAsJsonObject(content).getAsJsonObject("runes")):Collections.emptyMap());
		return j.has(content)&&j.getAsJsonObject(content).has("confirmation")?new BuybackPurchase(purchase.getId(),purchase.getCatalogId(),purchase.getGp(),purchase.getAt(),purchase.getBankFloors(),purchase.getPouchItemId(),purchase.getLegacyRunes(),BagObservation.State.PLAYER_CONFIRMED_EMPTY):purchase;
	}
	static JsonObject state(BuybackState s)
	{
		JsonObject j=new JsonObject(),roles=new JsonObject();s.getEntitlements().forEach((role,level)->{JsonObject value=new JsonObject();boolean copy=SelectedBuybacks.copyRole(role);value.addProperty("level",copy?1:level);value.addProperty("quantity",copy?level:BuybackState.COPIES_PER_ROLE);value.addProperty("bankFloor",s.getBankFloors().get(role));roles.add(role,value);});j.add("roles",roles);j.addProperty("catalogVersion",s.getCatalogVersion());j.addProperty("committedGp",s.getCommittedGp());j.addProperty("archive",s.getArchive());
		JsonArray entries=new JsonArray();s.getPurchases().forEach(p->entries.add(purchase(p)));j.add("purchases",entries);j.add("pouch",PouchCodec.state(s.getPouch()));return j;
	}
	static BuybackState state(JsonObject j)
	{return state(j,false);}
	static BuybackState state(JsonObject j,boolean historical)
	{
		if(historical){keys(j,"roles","catalogVersion","committedGp","archive","purchases");}else{keys(j,"roles","catalogVersion","committedGp","archive","purchases","pouch");}Map<String,Integer> roles=new TreeMap<>();Map<String,Long> floors=new TreeMap<>();j.getAsJsonObject("roles").entrySet().forEach(e->{JsonObject value=e.getValue().getAsJsonObject();keys(value,"level","quantity","bankFloor");boolean copy=SelectedBuybacks.copyRole(e.getKey());if(copy?number(value.get("level"))!=1||number(value.get("quantity"))<1:number(value.get("quantity"))!=BuybackState.COPIES_PER_ROLE){throw new IllegalArgumentException("Invalid entitlement copy limit");}roles.put(e.getKey(),Math.toIntExact(number(value.get(copy?"quantity":"level"))));floors.put(e.getKey(),number(value.get("bankFloor")));});
		List<BuybackPurchase> purchases=new ArrayList<>();for(JsonElement e:j.getAsJsonArray("purchases")){purchases.add(purchase(e.getAsJsonObject()));}
		return new BuybackState(Math.toIntExact(number(j.get("catalogVersion"))),roles,floors,number(j.get("committedGp")),purchases,nullable(j,"archive"),historical?com.fromscratch.pouch.PouchState.unreviewed():PouchCodec.state(j.getAsJsonObject("pouch")));
	}
}
