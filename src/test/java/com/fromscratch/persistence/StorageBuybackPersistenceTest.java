package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.fromscratch.model.ContainerSnapshot;
import com.google.gson.*;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class StorageBuybackPersistenceTest
{
	private BankRun buy(BankRun run,String catalog){return BankEngine.buyEmptyStorage(run,catalog,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,new EmptyStorageConfirmation(catalog,run.getRevision(),SnapshotFixtures.TIME,true,true));}
	@Test public void everySupportedFormUsesOneSharedPaidCopyAndRoundTrips()throws Exception
	{
		BankRunCodec codec=new BankRunCodec(SnapshotFixtures.gson());
		for(String catalog:new String[]{"fish_barrel","herb_sack","plank_sack","seed_box","tackle_box"})
		{
			int[] ids=BuybackCatalog.variants(catalog).keySet().stream().mapToInt(i->i).toArray();BankRun before=BuybackModelTest.funded(4000000,ids),paid=buy(before,catalog);
			long projected=BuybackCatalog.variants(catalog).keySet().stream().mapToLong(id->BuybackEngine.balances(paid).getOrDefault(id,0L)).sum();assertEquals(1,projected);assertEquals(paid,codec.decode(codec.encode(paid),paid.getCharacter()));
			BuybackPurchase receipt=paid.getBuybacks().getPurchases().get(0);JsonObject encoded=BuybackCodec.purchase(receipt);assertEquals("PLAYER_CONFIRMED_EMPTY",encoded.getAsJsonObject("storage").get("confirmation").getAsString());assertFalse(encoded.has("pouch"));assertFalse(encoded.has("bag"));assertFalse(encoded.has("selected"));assertEquals(receipt,BuybackCodec.purchase(encoded));
		}
	}
	@Test public void strippedOrForgedDeclarationCannotLoad()throws Exception
	{
		BuybackPurchase receipt=buy(BuybackModelTest.funded(4000000,ItemID.FISH_BARREL_CLOSED),"fish_barrel").getBuybacks().getPurchases().get(0);JsonObject original=BuybackCodec.purchase(receipt);
		for(String fault:new String[]{"missing","notEmpty","unaccounted","otherCatalog","selected","extra"})
		{
			JsonObject bad=original.deepCopy();switch(fault){case "missing":bad.remove("storage");break;case "notEmpty":bad.getAsJsonObject("storage").addProperty("confirmation","VERIFIED_EMPTY");break;case "unaccounted":bad.getAsJsonObject("storage").addProperty("legacyContents","UNKNOWN");break;case "otherCatalog":bad.addProperty("catalog","fire_cape");break;case "selected":bad.add("selected",new JsonObject());break;case "extra":bad.getAsJsonObject("storage").addProperty("invented",true);break;}
			try{BuybackCodec.purchase(bad);fail(fault);}catch(IllegalArgumentException expected){}
		}
	}
	@Test public void correctedLegacyTransferThenPurchaseDoesNotCreditOrDoubleSubtractContents()
	{
		BankRun before=BuybackModelTest.funded(4000000,ItemID.FISH_BARREL_CLOSED);Map<Integer,Long> physical=new TreeMap<>(before.getPhysical());physical.put(ItemID.RAW_SHARK,28L);
		BankRun deposited=BankEngine.observe(before,new ContainerSnapshot(physical,physical.size()),UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals(28,deposited.balance(ItemID.RAW_SHARK));
		BankRun corrected=BankEngine.correct(deposited,ItemID.RAW_SHARK,0,UUID.randomUUID(),SnapshotFixtures.TIME);BankRun paid=buy(corrected,"fish_barrel");assertEquals(0,paid.balance(ItemID.RAW_SHARK));assertEquals(corrected.getBalances(),paid.getBalances());assertEquals(28,paid.physical(ItemID.RAW_SHARK));assertTrue(paid.getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());
	}
	@Test public void historicalTenSevenAndTenEightReceiptsStayByteEquivalent()throws Exception
	{
		BankRunCodec codec=new BankRunCodec(SnapshotFixtures.gson());BankRun funded=BuybackModelTest.funded(8000000,ItemID.TZHAAR_CAPE_FIRE,ItemID.SLAYER_HELM_I,ItemID.ELITE_BLACK_FULL_HELM,ItemID.FISH_BARREL_CLOSED);
		BankRun historical=BuybackModelTest.buy(funded,"fire_cape");historical=BankEngine.buySelected(historical,"slayer_gear",ItemID.SLAYER_HELM_I,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);historical=BankEngine.buySelected(historical,"elite_black_helm",ItemID.ELITE_BLACK_FULL_HELM,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME);
		List<JsonObject> old=new ArrayList<>();historical.getBuybacks().getPurchases().forEach(p->old.add(BuybackCodec.purchase(p)));BankRun paid=codec.decode(codec.encode(buy(historical,"fish_barrel")),historical.getCharacter());
		for(int i=0;i<old.size();i++){assertEquals(old.get(i),BuybackCodec.purchase(paid.getBuybacks().getPurchases().get(i)));}assertEquals(6030000,paid.getBuybacks().getCommittedGp());
	}
	@Test public void newStorageDeclarationCannotBeDisguisedAsFormatTen()throws Exception
	{
		BankRunCodec codec=new BankRunCodec(SnapshotFixtures.gson());BankRun paid=buy(BuybackModelTest.funded(4000000,ItemID.FISH_BARREL_CLOSED),"fish_barrel");JsonObject payload=codec.payload(codec.encode(paid),BankRunCodec.FORMAT);
		try{codec.decode(codec.envelope(payload,10),paid.getCharacter());fail();}catch(SaveException expected){}
	}
}
