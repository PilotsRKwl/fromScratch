package com.fromscratch.persistence;

import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import com.google.gson.*;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;

/** Explicit format-11 JSON with verified format-5 through 10 decoding; no reflective domain deserialization. */
public final class BankRunCodec
{
	public static final int FORMAT = 11;
	private final Gson gson;
	@Inject public BankRunCodec(Gson gson) { this.gson = gson.newBuilder().serializeNulls().create(); }
	public String encode(BankRun r)
	{
		JsonObject p = new JsonObject();
		p.addProperty("character", r.getCharacter()); p.addProperty("run", string(r.getRunId())); p.addProperty("previousRun", string(r.getPreviousRunId()));
		p.addProperty("started", string(r.getStartedAt())); p.addProperty("revision", r.getRevision().toString()); p.addProperty("sequence", r.getSequence());
		if (r.getInitialBank() == null) { p.add("initial", JsonNull.INSTANCE); }
		else { JsonObject initial = new JsonObject(); initial.addProperty("slots", r.getInitialBank().getOccupiedSlots()); initial.add("quantities", map(r.getInitialBank().getQuantities())); p.add("initial", initial); }
		p.add("physical", map(r.getPhysical())); p.add("balances", map(r.getBalances())); p.addProperty("anchored", r.isAnchored());
		p.addProperty("legacyDocument", r.getLegacyDocument()); p.addProperty("migrationArchive", r.getMigrationArchive());
		JsonArray recent = new JsonArray(); r.getRecent().forEach(a -> recent.add(audit(a))); p.add("recent", recent);
		p.add("hostSummary", r.getHostSummary() == null ? JsonNull.INSTANCE : audit(r.getHostSummary())); p.add("financial",FinancialCodec.state(r.getFinancial())); p.add("exchange",GeCodec.state(r.getExchange())); p.add("buybacks",BuybackCodec.state(r.getBuybacks())); p.add("potions",PotionCodec.state(r.getPotions())); return envelope(p, FORMAT);
	}
	public BankRun decode(String text, String character) throws SaveException
	{
		try
		{
			int format=Math.toIntExact(number(new JsonParser().parse(text).getAsJsonObject().get("formatVersion")));if(format!=5&&format!=6&&format!=7&&format!=8&&format!=9&&format!=10&&format!=FORMAT){throw new IllegalArgumentException("Unsupported bank schema");}
			JsonObject p = payload(text, format);
			List<String> expected=new ArrayList<>(Arrays.asList("character","run","previousRun","started","revision","sequence","initial","physical","balances","anchored","legacyDocument","migrationArchive","recent","hostSummary"));
			if(format>=6){expected.add("financial");}if(format>=7){expected.add("exchange");}if(format>=8){expected.add("buybacks");}if(format>=10){expected.add("potions");}keys(p,expected.toArray(new String[0]));
			if (!character.equals(str(p, "character"))) { throw new IllegalArgumentException("Character mismatch"); }
			ContainerSnapshot initial = null;
			if (!p.get("initial").isJsonNull()) { JsonObject i = p.getAsJsonObject("initial"); keys(i, "slots", "quantities"); initial = new ContainerSnapshot(readMap(i.getAsJsonObject("quantities")), Math.toIntExact(number(i.get("slots")))); }
			List<BankAudit> recent = new ArrayList<>(); for (JsonElement a : p.getAsJsonArray("recent")) { recent.add(readAudit(a.getAsJsonObject())); }
			JsonElement anchored = p.get("anchored"); if (!anchored.isJsonPrimitive() || !anchored.getAsJsonPrimitive().isBoolean()) { throw new IllegalArgumentException("Invalid bank anchor flag"); }
			BankRun decoded = new BankRun(character, uuid(nullable(p, "run")), uuid(nullable(p, "previousRun")), instant(nullable(p, "started")), UUID.fromString(str(p, "revision")), number(p.get("sequence")), initial,
				readMap(p.getAsJsonObject("physical")), readMap(p.getAsJsonObject("balances")), anchored.getAsBoolean(), nullable(p, "legacyDocument"), nullable(p, "migrationArchive"), recent,
				p.get("hostSummary").isJsonNull() ? null : readAudit(p.getAsJsonObject("hostSummary")),format==5?com.fromscratch.wealth.FinancialState.baseline(readMap(p.getAsJsonObject("balances")),recent.get(0).getAt(),true):FinancialCodec.state(p.getAsJsonObject("financial")),format>=7?GeCodec.state(p.getAsJsonObject("exchange")):com.fromscratch.ge.GeState.empty(),format>=8?BuybackCodec.state(p.getAsJsonObject("buybacks"),format==8):com.fromscratch.buyback.BuybackState.empty(),format>=10?PotionCodec.state(p.getAsJsonObject("potions")):com.fromscratch.potion.PotionState.unknown());
			if(format<11&&(decoded.getBuybacks().getEntitlements().keySet().stream().anyMatch(com.fromscratch.buyback.SelectedBuybacks::copyRole)||decoded.getBuybacks().getPurchases().stream().anyMatch(purchase->purchase.getSelectedItemId()>0||purchase.isStorageConfirmedEmpty())||decoded.getRecent().stream().anyMatch(a->a.getBuyback()!=null&&(a.getBuyback().getSelectedItemId()>0||a.getBuyback().isStorageConfirmedEmpty())))){throw new IllegalArgumentException("Selected-copy or empty-storage declaration evidence requires format 11");}
			return decoded;
		}
		catch (RuntimeException ex) { throw new SaveException("Bank save is corrupt or incompatible. Existing data and recovery files were preserved."); }
	}
	String envelope(JsonObject p, int version)
	{
		String data = gson.toJson(p); JsonObject e = new JsonObject(); e.addProperty("formatVersion", version); e.addProperty("sha256", SaveIntegrity.sha256(data)); e.addProperty("payload", data); return gson.toJson(e);
	}
	JsonObject payload(String text, int version)
	{
		JsonObject e = new JsonParser().parse(text).getAsJsonObject(); keys(e, "formatVersion", "sha256", "payload");
		if (number(e.get("formatVersion")) != version || !SaveIntegrity.sha256(str(e, "payload")).equals(str(e, "sha256"))) { throw new IllegalArgumentException("Invalid envelope"); }
		return new JsonParser().parse(str(e, "payload")).getAsJsonObject();
	}
	JsonObject audit(BankAudit a)
	{
		JsonObject p = new JsonObject(); p.addProperty("id", a.getId().toString()); p.addProperty("session", a.getSession().toString()); p.addProperty("at", a.getAt().toString()); p.addProperty("kind", a.getKind().name());
		p.add("physical", map(a.getPhysicalChanges())); p.add("balances", map(a.getBalanceChanges())); p.addProperty("reason", a.getReason()); p.add("valuations",FinancialCodec.events(a.getValuations())); if(!a.getExchange().emptyEvidence()){p.add("exchange",GeCodec.update(a.getExchange()));}if(a.getBuyback()!=null){p.add("buyback",BuybackCodec.purchase(a.getBuyback()));}if(a.getPouch()!=null){p.add("pouch",PouchCodec.action(a.getPouch()));}if(!a.getPouchTransfers().isEmpty()){p.add("pouchTransfers",map(a.getPouchTransfers()));}if(a.getPotion()!=null){p.add("potion",PotionCodec.update(a.getPotion()));}return p;
	}
	BankAudit readAudit(JsonObject p)
	{
		List<String> expected=new ArrayList<>(Arrays.asList("id","session","at","kind","physical","balances","reason"));if(p.has("valuations")){expected.add("valuations");}if(p.has("exchange")){expected.add("exchange");}if(p.has("buyback")){expected.add("buyback");}if(p.has("pouch")){expected.add("pouch");}if(p.has("pouchTransfers")){expected.add("pouchTransfers");}if(p.has("potion")){expected.add("potion");}keys(p,expected.toArray(new String[0]));
		return new BankAudit(UUID.fromString(str(p, "id")), UUID.fromString(str(p, "session")), Instant.parse(str(p, "at")), BankAudit.Kind.valueOf(str(p, "kind")), readMap(p.getAsJsonObject("physical")), readMap(p.getAsJsonObject("balances")), str(p, "reason"),p.has("valuations")?FinancialCodec.events(p.getAsJsonArray("valuations")):Collections.emptyList(),p.has("exchange")?GeCodec.update(p.getAsJsonObject("exchange")):com.fromscratch.ge.GeUpdate.empty(),p.has("buyback")?BuybackCodec.purchase(p.getAsJsonObject("buyback")):null,p.has("pouch")?PouchCodec.action(p.getAsJsonObject("pouch")):null,p.has("pouchTransfers")?readMap(p.getAsJsonObject("pouchTransfers")):Collections.emptyMap(),p.has("potion")?PotionCodec.update(p.getAsJsonObject("potion")):null);
	}
	static JsonObject map(Map<Integer, Long> map) { JsonObject p = new JsonObject(); new TreeMap<>(map).forEach((id, n) -> p.addProperty(id.toString(), n)); return p; }
	static Map<Integer, Long> readMap(JsonObject p)
	{
		Map<Integer, Long> result = new TreeMap<>(); for (Map.Entry<String, JsonElement> e : p.entrySet()) { if (!e.getKey().matches("0|[1-9][0-9]*")) { throw new IllegalArgumentException("Invalid item ID"); } result.put(Integer.parseInt(e.getKey()), number(e.getValue())); } return result;
	}
	static long number(JsonElement e) { if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || !e.getAsString().matches("-?(0|[1-9][0-9]*)")) { throw new IllegalArgumentException("Invalid integer"); } return Long.parseLong(e.getAsString()); }
	static String str(JsonObject p, String key) { JsonElement v = p.get(key); if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) { throw new IllegalArgumentException("Invalid field " + key); } return v.getAsString(); }
	static String nullable(JsonObject p, String key) { return p.get(key).isJsonNull() ? null : str(p, key); }
	static void keys(JsonObject p, String... expected) { if (!p.keySet().equals(new HashSet<>(Arrays.asList(expected)))) { throw new IllegalArgumentException("Unknown or missing fields"); } }
	private static String string(Object o) { return o == null ? null : o.toString(); }
	private static UUID uuid(String s) { return s == null ? null : UUID.fromString(s); }
	private static Instant instant(String s) { return s == null ? null : Instant.parse(s); }
}
