package com.fromscratch.examine;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.*;
import com.fromscratch.buyback.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.pouch.PouchObservation;
import java.awt.Color;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.util.Text;
import org.junit.*;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Native request -> native response -> queued supplement, with cached quotes and committed run fixtures. */
public class NativeExamineTest
{
	private BankRun run=BankFixtures.run(1);
	private final List<String> messages=new ArrayList<>();
	private int widget=SHIELD;
	private Color color=new Color(0x1DFF00);
	private final NativeExamine.Items items=new NativeExamine.Items()
	{
		public int widget(int component,int child) { return widget; }
		public int normalize(int id) { return id==999999?SHIELD:id; }
		public int highAlchemy(int id) { return 4; }
		public String name(int id) { return id==SHIELD?"Wooden shield":"Coins"; }
	};
	private final PriceService prices=new PriceService(()->Collections.singletonMap(SHIELD,180L),java.time.Instant::now);
	private NativeExamine examine;
	@Before public void setup() { prices.refresh();examine=new NativeExamine(items,prices,messages::add,()->color);examine.start(()->run); }
	private void request(int id) { examine.requested("Examine",MenuAction.EXAMINE_ITEM_GROUND,id,0,0); }
	private void response(ChatMessageType type,String text) { ChatMessage e=new ChatMessage();e.setType(type);e.setMessage(text);examine.response(e); }
	@Test public void negativeExamineUsesLiveNegativeColorWithoutRecoloringWordsOrPositivePrices()
	{
		run=BankEngine.observe(run,bank(0),UUID.randomUUID(),java.time.Instant.now());Color[] negative={Color.RED};examine=new NativeExamine(items,prices,messages::add,()->Color.GREEN,()->negative[0]);examine.start(()->run);
		for(Color selected:new Color[]{Color.RED,Color.MAGENTA}){negative[0]=selected;request(SHIELD);response(ChatMessageType.ITEM_EXAMINE,"A shield.");String message=messages.get(messages.size()-1);assertTrue(message.startsWith("<col="+(selected==Color.RED?"ff0000":"ff00ff")+">From Scratch: -1</col> x Wooden shield"));assertTrue(message.contains("<col=00ff00>180</col>ea)"));assertFalse(message.contains("<colNORMAL>"));}
	}
	@Test public void nativeThenCorePriceThenExactlyOneSupplementAndLiveSegmentColors()
	{
		run=BankEngine.observe(run,bank(126),UUID.randomUUID(),java.time.Instant.now());request(SHIELD);assertTrue(messages.isEmpty());
		EventBus bus=new EventBus();
		// Same priorities and FIFO enqueue behavior as core Examine + the production plugin.
		bus.register(ChatMessage.class,e->{if(e.getType()==ChatMessageType.ITEM_EXAMINE&&!e.getMessage().startsWith("Price of ")) { messages.add("Price of Wooden shield: GE average 180"); }},0);
		bus.register(ChatMessage.class,examine::response,NativeExamine.RESPONSE_PRIORITY);
		messages.add("Native examine");ChatMessage nativeLine=new ChatMessage();nativeLine.setType(ChatMessageType.ITEM_EXAMINE);nativeLine.setMessage("A shield.");bus.post(nativeLine);
		assertEquals("Native examine",messages.get(0));assertTrue(messages.get(1).startsWith("Price of "));
		assertEquals("From Scratch: 125 x Wooden shield: GE 22,500 (180ea); HA 500 (4ea)",displayed(messages.get(2)));
		assertTrue(messages.get(2).contains("<col=1dff00>From Scratch: 125</col> x Wooden shield"));
		assertTrue(messages.get(2).contains("GE <col=1dff00>22,500</col>"));
		assertTrue(messages.get(2).contains("<col=1dff00>180</col>ea)"));
		assertFalse(messages.get(2).contains("<colNORMAL>"));
		// RuneLite's optional normal server-message color replaces </col> resets, without changing content.
		assertEquals(displayed(messages.get(2)),displayed("<col=ffffff>"+messages.get(2).replace("</col>","<col=ffffff>")+"</col>"));
		response(ChatMessageType.ITEM_EXAMINE,"A shield.");assertEquals(3,messages.size());
		color=Color.MAGENTA;request(SHIELD);response(ChatMessageType.ITEM_EXAMINE,"A shield.");assertTrue(messages.get(3).contains("<col=ff00ff>From Scratch:"));assertFalse(messages.get(3).contains("<col=1dff00>"));
	}
	@Test public void rapidRequestsStayFifoAndCorePriceLinesDoNotConsumeNext()
	{
		request(SHIELD);request(OTHER);response(ChatMessageType.ITEM_EXAMINE,"A shield.");
		response(ChatMessageType.ITEM_EXAMINE,"<col=ff0000>Price of Wooden shield: GE average 180</col>");assertEquals(1,messages.size());
		response(ChatMessageType.ITEM_EXAMINE,"Lovely money!");assertEquals(2,messages.size());assertTrue(displayed(messages.get(1)).contains("GE 0 (1ea)"));
	}
	@Test public void unknownWidgetsConsumeTheirOwnResponseAndNpcAndOwnMessagesAreIgnored()
	{
		examine.requested("Examine",MenuAction.EXAMINE_NPC,SHIELD,0,0);widget=-1;
		examine.requested("Examine",MenuAction.CC_OP_LOW_PRIORITY,0,0,0);request(SHIELD);
		response(ChatMessageType.ITEM_EXAMINE,"Unknown item");assertTrue(messages.isEmpty());
		response(ChatMessageType.GAMEMESSAGE,"From Scratch: test");assertTrue(messages.isEmpty());
		response(ChatMessageType.ITEM_EXAMINE,"A shield.");assertEquals(1,messages.size());
	}
	@Test public void staleAndChangedRunRequestsAreDiscarded()
	{
		examine.remember(SHIELD,run,0);assertEquals(-1,examine.take(run,10_000_000_001L));
		request(SHIELD);run=BankFixtures.run(1);response(ChatMessageType.ITEM_EXAMINE,"A shield.");assertTrue(messages.isEmpty());
		request(SHIELD);examine.clear();response(ChatMessageType.ITEM_EXAMINE,"A shield.");assertTrue(messages.isEmpty());
	}
	@Test public void placeholderOrNoteIdentityUsesSavedOriginalBalance()
	{
		run=BankEngine.observe(run,bank(0),UUID.randomUUID(),java.time.Instant.now());request(999999);
		response(ChatMessageType.ITEM_EXAMINE,"A placeholder.");assertEquals(1,messages.size());assertTrue(displayed(messages.get(0)).contains("-1 x Wooden shield — Outstanding; GE -180 (180ea); HA -4 (4ea)"));
	}
	@Test public void missingStaleAndOverflowPricesRemainExplicit()
	{
		String missing=format(run,SHIELD,"Item",0,PriceQuote.unknown(SHIELD),0);assertTrue(displayed(missing).contains("GE unavailable"));assertFalse(missing.contains("HA "));
		PriceQuote q=new PriceQuote(SHIELD,2L,java.time.Instant.now(),PriceQuote.Source.RUNELITE_GE_GUIDE,true);
		assertTrue(displayed(format(run,SHIELD,"Item",Long.MAX_VALUE,q,0)).contains("GE out of range (2ea) (stale)"));
		PriceQuote three=new PriceQuote(SHIELD,3L,java.time.Instant.now(),PriceQuote.Source.RUNELITE_GE_GUIDE,false);
		assertEquals("From Scratch: 0 x Mind rune — Locked; GE 0 (3ea); HA 0 (1ea)",displayed(format(run,SHIELD,"Mind rune",0,three,1)));
		PriceQuote five=new PriceQuote(SHIELD,5L,java.time.Instant.now(),PriceQuote.Source.RUNELITE_GE_GUIDE,false);
		assertEquals("From Scratch: -1,400 x Body rune — Outstanding; GE -7,000 (5ea); HA -1,400 (1ea)",displayed(format(run,SHIELD,"Body rune",-1400,five,1)));
		String escaped=format(run,SHIELD,"<col=ff0000><img=1>",1,three,1);assertFalse(escaped.contains("<col=ff0000>"));assertFalse(escaped.contains("<img=1>"));assertTrue(escaped.contains("<lt>"));
	}
	private static String format(BankRun run,int id,String name,long n,PriceQuote q,long ha)
	{ return ExamineFormatter.format(run,id,name,n,q,ha,new Color(0x1DFF00)); }
	@Test public void bagExamineDistinguishesLockedPaidSharedVariantAndEarnedWithoutNewMenu()
	{
		BankRun locked=legacy(ItemID.LOOTING_BAG);assertEquals("From Scratch: 0 x Looting bag — Locked; Buyback: 10K GP",displayed(format(locked,ItemID.LOOTING_BAG,"Looting bag",0,PriceQuote.unknown(ItemID.LOOTING_BAG),0)));
		BankRun paid=BuybackModelTest.buy(locked,"looting_bag_family");assertTrue(displayed(format(paid,ItemID.LOOTING_BAG_OPEN,"Looting bag",1,null,0)).contains("Buyback: Unlocked (10K GP paid)"));assertTrue(displayed(format(locked,ItemID.LOOTING_BAG_OPEN,"Looting bag",1,null,0)).contains("1 x Looting bag — Earned"));
	}
	/** Native BUILD_CHATBOX -> script 84 (1639-1654) -> 632: first pipe separates name from body.
	 * This frozen native-script rule deliberately runs AFTER color formatting, not just on formatter inputs.
	 * Text.unescapeJagex then models the visible text; actual canvas wrapping still requires user acceptance.
	 */
	private static String displayed(String payload)
	{
		int delimiter=payload.indexOf('|');
		return Text.unescapeJagex(delimiter<0?payload:payload.substring(delimiter+1)).trim();
	}
	private static PriceQuote quote(int id,long gp)
	{ return new PriceQuote(id,gp,com.fromscratch.SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false); }
	private static BankRun legacy(int...ids)
	{
		Map<Integer,Long> initial=new TreeMap<>();for(int id:ids) { initial.put(id,1L); }
		BankRun run=BankEngine.start(com.fromscratch.SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(initial,initial.size()),true,UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME);
		Map<Integer,Long> bank=new TreeMap<>(initial);bank.put(ItemID.COINS,30000000L);
		return BankEngine.observe(run,new ContainerSnapshot(bank,bank.size()),UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME);
	}
	private static BankRun buy(BankRun run,String entry)
	{ return BankEngine.buyback(run,entry,UUID.randomUUID(),UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME); }
	@Test public void lockedFireCapeUsesCatalogPriceButArbitraryUntradeablesDoNot()
	{
		String cape=displayed(format(run,ItemID.TZHAAR_CAPE_FIRE,"Fire cape",0,null,39004));
		assertEquals("From Scratch: 0 x Fire cape — Locked; Buyback: 1000K GP",cape);
		assertFalse(displayed(format(run,SHIELD,"Wooden shield",0,null,0)).contains("Buyback"));
	}
	@Test public void sharedPouchReceiptShowsPurchasedOnBothFormsWithoutRepeatCharge()
	{
		BankRun paid=BankEngine.buyback(legacy(ItemID.BH_RUNE_POUCH),"rune_pouch_family",UUID.randomUUID(),UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME,new PouchObservation(true,Collections.emptyMap(),com.fromscratch.SnapshotFixtures.TIME,ItemID.BH_RUNE_POUCH));
		for(int id:new int[]{ItemID.BH_RUNE_POUCH,ItemID.DIVINE_RUNE_POUCH,ItemID.BH_RUNE_POUCH_TROUVER})
		{
			String line=displayed(format(paid,id,"Rune pouch",BuybackEngine.balances(paid).getOrDefault(id,0L),null,6000));
			assertTrue(line.contains("Buyback: Unlocked (1000K GP paid)"));assertFalse(line.contains("— Locked"));assertEquals(1,line.split("1000K",-1).length-1);
		}
		assertEquals(1000000,paid.getBuybacks().getCommittedGp());assertEquals(1,paid.getBuybacks().getPurchases().size());
	}
	@Test public void purchasedFireCapeStatusAndPriceSurviveItsAbsenceFromBank()
	{
		BankRun paid=buy(legacy(ItemID.TZHAAR_CAPE_FIRE),"fire_cape");
		assertEquals("From Scratch: 1 x Fire cape — Buyback: Unlocked (1000K GP paid); HA 6,000",displayed(format(paid,ItemID.TZHAAR_CAPE_FIRE,"Fire cape",1,null,6000)));
		Map<Integer,Long> bank=new TreeMap<>(paid.getPhysical());bank.remove(ItemID.TZHAAR_CAPE_FIRE);paid=BankEngine.observe(paid,new ContainerSnapshot(bank,bank.size()),UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME);
		assertTrue(displayed(format(paid,ItemID.TZHAAR_CAPE_FIRE,"Fire cape",BuybackEngine.balances(paid).getOrDefault(ItemID.TZHAAR_CAPE_FIRE,0L),null,0)).contains("0 x Fire cape — Buyback: Unlocked (1000K GP paid)"));
	}
	@Test public void outfitAndVoidUseExistingSharedSetReceiptsAndUpgradeCosts()
	{
		BuybackEntry graceful=BuybackCatalog.entry("graceful");int[] pieces=graceful.getRoles().entrySet().stream().mapToInt(role->BuybackCatalog.variants(role.getKey()).entrySet().stream().filter(v->v.getValue().equals(role.getValue())).findFirst().get().getKey()).toArray();
		BankRun outfit=buy(legacy(pieces),"graceful");for(int id:pieces) { assertTrue(displayed(format(outfit,id,"Graceful",1,null,0)).contains("Unlocked (1000K GP set paid)")); }
		BankRun regular=buy(legacy(ItemID.PEST_VOID_KNIGHT_TOP,ItemID.ELITE_VOID_KNIGHT_TOP),"void_top");
		assertTrue(displayed(format(regular,ItemID.ELITE_VOID_KNIGHT_TOP,"Elite Void top",0,null,0)).contains("Buyback: 1000K GP"));
		BankRun elite=buy(regular,"elite_void_top");
		assertTrue(displayed(format(elite,ItemID.ELITE_VOID_KNIGHT_TOP,"Elite Void top",1,null,0)).contains("Unlocked (1800K GP paid)"));
		assertTrue(displayed(format(elite,ItemID.PEST_VOID_KNIGHT_TOP,"Void top",0,null,0)).contains("Unlocked (800K GP paid)"));
	}
	@Test public void nativeChatDelimiterReproducesLiveFailuresAndAllFiveFixedPayloadsKeepTheirOpening()
	{
		// Even a SHORT message loses its prefix: this is a native control delimiter, not a length limit.
		assertEquals("HA 1",displayed("<col=1dff00>From Scratch: 1</col> x X | HA 1"));
		String[] previous={
			"From Scratch: 9000K x Coins: GE average 9,000,000 (1ea)",
			"From Scratch: 199 x Law rune: GE average 23,283 (117ea) | HA value 28,656 (144ea)",
			"From Scratch: -10 x Cosmic rune — Outstanding | GE average -1,080 (108ea) | HA value -300 (30ea)",
			"From Scratch: 1 x Rune pouch — Buyback: Unlocked (1000K GP paid) | HA value 6,000",
			"From Scratch: 0 x Fire cape — Legacy locked | Buyback: 1000K GP | HA value 0 (39,004ea)"};
		String[] broken={previous[0],"HA value 28,656 (144ea)","GE average -1,080 (108ea) | HA value -300 (30ea)","HA value 6,000","Buyback: 1000K GP | HA value 0 (39,004ea)"};
		for(int i=0;i<previous.length;i++) { assertEquals(broken[i],displayed("<colNORMAL><col=1dff00>"+previous[i]+"</col><colNORMAL>")); }
		BankRun paid=BankEngine.buyback(legacy(ItemID.BH_RUNE_POUCH),"rune_pouch_family",UUID.randomUUID(),UUID.randomUUID(),com.fromscratch.SnapshotFixtures.TIME,new PouchObservation(true,Collections.emptyMap(),com.fromscratch.SnapshotFixtures.TIME,ItemID.BH_RUNE_POUCH));
		String[] payloads={
			format(run,ItemID.COINS,"Coins",9000000,PriceQuote.coins(com.fromscratch.SnapshotFixtures.TIME),0),
			format(run,ItemID.LAWRUNE,"Law rune",199,quote(ItemID.LAWRUNE,117),144),
			format(run,ItemID.COSMICRUNE,"Cosmic rune",-10,quote(ItemID.COSMICRUNE,108),30),
			format(paid,ItemID.BH_RUNE_POUCH,"Rune pouch",BuybackEngine.balances(paid).get(ItemID.BH_RUNE_POUCH),null,6000),
			format(run,ItemID.TZHAAR_CAPE_FIRE,"Fire cape",0,null,39004)};
		String[] expected={
			"From Scratch: 9,000,000 x Coins: GE 9,000,000 (1ea)",
			"From Scratch: 199 x Law rune: GE 23,283 (117ea); HA 28,656 (144ea)",
			"From Scratch: -10 x Cosmic rune — Outstanding; GE -1,080 (108ea); HA -300 (30ea)",
			"From Scratch: 1 x Rune pouch — Buyback: Unlocked (1000K GP paid); HA 6,000",
			"From Scratch: 0 x Fire cape — Locked; Buyback: 1000K GP"};
		for(int i=0;i<payloads.length;i++)
		{
			assertFalse(payloads[i].contains("|"));assertFalse(payloads[i].contains("<colNORMAL>"));
			assertEquals(expected[i],displayed(payloads[i]));
			assertEquals(payloads[i].split("<col=",-1).length,payloads[i].split("</col>",-1).length);
		}
	}
	@Test public void optionalPricingBudgetNeverSlicesEssentialTextAndNamesCannotInjectNativeDelimiters()
	{
		String header="From Scratch: 199 x ",ge=": GE 23,283 (117ea)",ha="; HA 28,656 (144ea)";
		String name="X".repeat(ExamineFormatter.OPTIONAL_TEXT_BUDGET-header.length()-ge.length()-ha.length());
		String full=format(run,SHIELD,name,199,quote(SHIELD,117),144);
		assertEquals(ExamineFormatter.OPTIONAL_TEXT_BUDGET,displayed(full).length());
		assertEquals(header+name+ge+ha,displayed(full));
		assertEquals(header+name+"X"+ge,displayed(format(run,SHIELD,name+"X",199,quote(SHIELD,117),144)));
		String tooLongForGe="X".repeat(ExamineFormatter.OPTIONAL_TEXT_BUDGET-header.length()-ge.length()+1);
		assertEquals(header+tooLongForGe,displayed(format(run,SHIELD,tooLongForGe,199,quote(SHIELD,117),1)));
		String longName="A very long item name ".repeat(10);
		assertEquals("From Scratch: 0 x "+longName+" — Locked; Buyback: 1000K GP",displayed(format(run,ItemID.TZHAAR_CAPE_FIRE,longName,0,null,39004)));
		String escaped=format(run,SHIELD,"Rune | <col=ff0000>name</col>",-10,quote(SHIELD,108),30);
		assertEquals("From Scratch: -10 x Rune / <col=ff0000>name</col> — Outstanding; GE -1,080 (108ea); HA -300 (30ea)",displayed(escaped));
		assertFalse(escaped.contains("<col=ff0000>"));
	}
	@Test public void examineAlwaysShowsExactSignedQuantityWhileOverlayKeepsCompactFormatting()
	{
		for(long n:new long[]{0,125,9999,119388,9000000,-1400,-125750,Long.MAX_VALUE,Long.MIN_VALUE})
		{
			String expected=java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(n);
			String message=format(run,ItemID.COINS,"Coins",n,PriceQuote.coins(com.fromscratch.SnapshotFixtures.TIME),0);
			assertTrue(displayed(message).startsWith("From Scratch: "+expected+" x Coins"));
		}
		assertEquals("119K",com.fromscratch.overlay.VirtualQuantityFormatter.signed(119388));
	}
}
