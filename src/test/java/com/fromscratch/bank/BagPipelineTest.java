package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.persistence.*;
import com.fromscratch.wealth.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Native contents proof -> controller -> forced purchase -> bank deposits -> restart, using isolated saves. */
public class BagPipelineTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private static final int B=ItemID.LOOTING_BAG,O=ItemID.LOOTING_BAG_OPEN,N=ItemID.NATURERUNE,D=ItemID.DRAGON_BONES,C=ItemID.COINS;
	private final Source source=new Source();private final BankOverlayState overlay=new BankOverlayState();private BankView view;
	private Filepath root;private BankRepository repository;private BankController controller;private Worker worker;private PriceService prices;private boolean fail,visible=true,nativeEmpty;
	private Item[] contents=new Item[0],display=new Item[0];
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source.items=new Item[]{new Item(B,1)};prices=new PriceService(()->{Map<Integer,Long> q=new HashMap<>();q.put(N,100L);q.put(D,200L);return q;},()->SnapshotFixtures.TIME);prices.refresh();overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();bank(B,100,0,100000);}
	@After public void stop(){controller.stop();}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{view=v;overlay.updateBank(v);},prices);controller.start(()->{if(fail){throw new java.io.IOException("Synthetic interruption");}return root;},worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private void bank(int form,int nature,int bones,int coins){List<Item> items=new ArrayList<>();if(form>0){items.add(new Item(form,1));}if(nature>0){items.add(new Item(N,nature));}if(bones>0){items.add(new Item(D,bones));}items.add(new Item(C,coins));source.items=items.toArray(new Item[0]);source.observer.changed();settle();}
	private void proof(Item... items){contents=items;display=items.clone();source.bagSession=source.observe().getSession();source.bagAccess=new LootingBagReader.Access(){public boolean live(){return true;}public boolean visible(){return visible;}public Item[] contents(){return contents;}public Item[] displayed(){return display;}public String name(int id){return id==N?"Nature rune":id==D?"Dragon bones":"Coins";}};}
	private void buy(){BagObservation preview=source.bag();controller.buyBag(view.getRun().getRevision(),UUID.randomUUID(),preview,p->{});worker.runAll();}
	private LootingBagReader inspectionReader(){return new LootingBagReader(new LootingBagReader.Access(){
		public boolean live(){return true;}public boolean visible(){return visible;}
		public Item[] contents(){return contents;}public Item[] displayed(){return display;}
		public String name(int id){return id==N?"Nature rune":id==D?"Dragon bones":"Coins";}
		public boolean emptyMessage(){return nativeEmpty;}
	});}
	private LootingBagReader.Session inspection(){source.bagInspection=new LootingBagReader.Session(inspectionReader());return source.bagInspection;}
	private void request(LootingBagReader.Session proof){proof.request(source.character,source.observe(),B,source.bagTick);}
	private void inspectNextTick(LootingBagReader.Session proof){source.observer.tick();source.bagTick++;proof.tick(source.character,source.observe(),source.bagTick,source.open);}
	private BankRun saved() throws Exception{return repository.load(root,source.character).get();}
	@Test public void loadedBagCommitsOnePaymentAndAllOffsetsWithoutPhysicalChanges()throws Exception
	{proof(new Item(N,100),new Item(D,50),new Item(C,25000));BankRun old=saved();buy();assertEquals(0,saved().balance(N));assertEquals(-50,saved().balance(D));assertEquals(75000,saved().balance(C));assertEquals(65000,BuybackEngine.available(saved()));assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(old.getPhysical(),saved().getPhysical());assertEquals(old.getFinancial(),saved().getFinancial());assertEquals("1",overlay.getProjection().label(B));assertTrue(repository.history(root,saved(),null,false).getRows().stream().anyMatch(r->r.getCategory().equals("Automatic — Legacy Looting Bag")&&r.getPurchase()!=null));}
	@Test public void partialAndFullReturnsPreserveEarnedRunesAndNeverCreateGainOrPeak()throws Exception
	{proof(new Item(N,100),new Item(D,50),new Item(C,25000));long gain=saved().getFinancial().getGainedGp();Long peak=view.getValuation().getPeakGp();buy();bank(B,150,20,110000);assertEquals(50,saved().balance(N));assertEquals(-30,saved().balance(D));bank(B,200,50,125000);assertEquals(100,saved().balance(N));assertEquals(0,saved().balance(D));assertEquals(90000,BuybackEngine.available(saved()));assertEquals(gain,saved().getFinancial().getGainedGp());assertEquals(peak,view.getValuation().getPeakGp());}
	@Test public void emptyBagIsVerifiedOnlyWithCurrentVisibleMatchingServerContainer()throws Exception
	{proof();assertTrue(source.bag().fresh(Instant.now()));buy();assertTrue(saved().getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());assertEquals(90000,BuybackEngine.available(saved()));}
	@Test public void missingAndStalePacketsCannotSpendButFreshBankedDataNeedsNoInventoryPanel()throws Exception
	{buy();assertEquals(0,saved().getBuybacks().getCommittedGp());proof(new Item(N,100));source.bagSession=UUID.randomUUID();buy();assertEquals(0,saved().getBuybacks().getCommittedGp());source.bagSession=source.observe().getSession();visible=false;display=null;buy();assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(0,saved().balance(N));}
	@Test public void duplicateTypesAggregateAndCancellationPreviewDoesNotMutate()throws Exception
	{proof(new Item(D,1),new Item(D,1));BankRun old=saved();assertEquals(Long.valueOf(2),source.bag().getQuantities().get(D));controller.bagPreview(q->assertEquals(Long.valueOf(2),q.getQuantities().get(D)));assertEquals(old,saved());}
	@Test public void failedCommitAndChangedPreviewNeverPartiallySpendOrUnlock()throws Exception
	{proof(new Item(N,100));BankRun old=saved();BagObservation preview=source.bag();proof(new Item(N,101));controller.buyBag(old.getRevision(),UUID.randomUUID(),preview,p->fail("Unexpected acknowledgment"));worker.runAll();assertEquals(old,saved());proof(new Item(N,100));fail=true;buy();assertTrue(view.isError());assertEquals(old,saved());assertEquals(old,view.getRun());}
	@Test public void openClosedVariantsRestartAndRepeatPurchaseKeepOneEntitlementAndOffset()throws Exception
	{proof(new Item(N,500));buy();bank(O,100,0,100000);assertEquals("1",overlay.getProjection().label(O));assertEquals(-400,saved().balance(N));BankRun old=saved();controller.stop();source.observer.reset();launch();settle();proof(new Item(N,999));buy();assertEquals(old.getBuybacks().getPurchases(),saved().getBuybacks().getPurchases());assertEquals(-400,saved().balance(N));assertEquals(10000,saved().getBuybacks().getCommittedGp());}
	@Test public void naturallyEarnedOpenBagIsFreeAndDoesNotUnlockSeparateLegacyCopies()throws Exception
	{bank(O,100,0,100000);assertEquals("Available",BuybackEngine.status(saved(),BuybackCatalog.entry("looting_bag_family")));assertEquals("0",overlay.getProjection().label(O));bank(0,100,0,100000);bank(O,100,0,100000);assertEquals(0,BuybackEngine.balances(saved()).getOrDefault(O,0L).longValue());
	 source.items=new Item[]{new Item(B,1),new Item(O,1),new Item(N,100),new Item(C,100000)};source.observer.changed();settle();assertEquals(1,BuybackEngine.balances(saved()).values().stream().filter(n->n>0).count()-2);assertEquals(0,saved().getBuybacks().getCommittedGp());}
	@Test public void genuinelyEarnedBagNeedsNoPaymentAndUnavailableContentsCannotMasqueradeAsEmpty()
	{BankRun newRun=BankEngine.start(SnapshotFixtures.CHARACTER_B,null,new com.fromscratch.model.ContainerSnapshot(Collections.emptyMap(),0),true,UUID.randomUUID(),SnapshotFixtures.TIME);newRun=BankEngine.observe(newRun,new com.fromscratch.model.ContainerSnapshot(Collections.singletonMap(O,1L),1),UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals("Earned",BuybackEngine.status(newRun,BuybackCatalog.entry("looting_bag_family")));assertEquals(Long.valueOf(1),BuybackEngine.balances(newRun).get(O));proof();contents=null;assertFalse(source.bag().fresh(Instant.now()));}
	@Test public void bothBankedEmptyFormsWorkWithoutContentsWidgetsAndShowEmptyConfirmation()throws Exception
	{for(int form:new int[]{B,O}){bank(form,100,0,100000);proof();visible=false;display=null;BagObservation observed=source.bag();assertTrue(observed.fresh(Instant.now()));assertEquals(form,observed.getItemId());assertTrue(observed.getQuantities().isEmpty());String text=BuybackDialog.bagConfirmation(view.getRun(),observed);assertTrue(text.contains("Your Looting Bag is empty. No legacy item adjustments are needed."));assertFalse(text.contains("negative virtual"));}}
	@Test public void nativeInspectionAcceptsEmptyTextOrMatchingFreshPacketAndIgnoresDecorativeChild()
	{LootingBagReader.Access nativeView=new LootingBagReader.Access(){public boolean live(){return true;}public boolean visible(){return true;}public Item[] contents(){return contents;}public Item[] displayed(){return display;}public String name(int id){return "Nature rune";}public boolean emptyMessage(){return true;}};LootingBagReader reader=new LootingBagReader(nativeView);contents=null;display=new Item[]{new Item(-1,0)};assertTrue(reader.inspect(source.observe(),B,false).fresh(Instant.now()));contents=new Item[]{new Item(N,500)};display=new Item[]{new Item(N,500),new Item(-1,0)};assertFalse(reader.inspect(source.observe(),O,false).fresh(Instant.now()));assertEquals(Long.valueOf(500),reader.inspect(source.observe(),O,true).getQuantities().get(N));display=new Item[]{new Item(N,499)};assertFalse(reader.inspect(source.observe(),O,true).fresh(Instant.now()));}
	@Test public void explicitNativeEmptyMessageDoesNotRequireAnItemGridOrContainer()
	{
		LootingBagReader reader=new LootingBagReader(new LootingBagReader.Access(){
			public boolean live(){return true;}public boolean visible(){return true;}
			public Item[] contents(){return null;}public Item[] displayed(){return null;}
			public String name(int id){return "Item";}public boolean emptyMessage(){return true;}
		});
		assertTrue(reader.inspect(source.observe(),B,false).fresh(Instant.now()));
	}
	@Test public void missingContainerAndUnbuiltViewRemainUnknownButAReceivedZeroItemArrayIsVerifiedEmpty()
	{
		contents=null;display=null;assertEquals(BagObservation.State.UNKNOWN,inspectionReader().inspect(source.observe(),B,false).getState());
		contents=new Item[0];assertEquals(BagObservation.State.VERIFIED_EMPTY,inspectionReader().inspect(source.observe(),B,true).getState());
		contents=new Item[]{new Item(-1,0),new Item(-1,0)};assertEquals(BagObservation.State.VERIFIED_EMPTY,inspectionReader().inspect(source.observe(),O,true).getState());
	}
	@Test public void nativeViewProofSurvivesReturningTheBagAndAnUnavailableLateContainer() throws Exception
	{
		bank(0,100,0,100000);LootingBagReader.Session proof=inspection();request(proof);
		source.open=false;nativeEmpty=true;contents=null;display=null;inspectNextTick(proof);
		assertEquals(BagObservation.State.UNKNOWN,source.bag().getState()); // It is not banked yet.
		proof.bankClosed();source.open=true;visible=false;nativeEmpty=false;bank(B,100,0,100000);
		proof.changed(source.character,source.observe(),source.bagTick,true);
		BagObservation captured=source.bag();assertEquals(BagObservation.State.VERIFIED_EMPTY,captured.getState());
		buy();assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(1,saved().getBuybacks().getPurchases().size());
		assertTrue(saved().getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());assertEquals(90000,BuybackEngine.available(saved()));
	}
	@Test public void delayedViewBuildDoesNotMistakeMissingWidgetsForAnEmptyBag()
	{
		contents=null;display=null;LootingBagReader.Session proof=inspection();request(proof);inspectNextTick(proof);
		assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		nativeEmpty=true;inspectNextTick(proof);assertEquals(BagObservation.State.VERIFIED_EMPTY,source.bag().getState());
	}
	@Test public void lateUnrequestedEmptyTextAndContradictoryFreshContentsCannotProveEmptiness()
	{
		contents=null;display=null;LootingBagReader.Session proof=inspection();request(proof);source.bagTick+=6;nativeEmpty=true;
		proof.tick(source.character,source.observe(),source.bagTick,true);assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		contents=new Item[]{new Item(N,100)};display=new Item[0];assertEquals(BagObservation.State.UNKNOWN,inspectionReader().inspect(source.observe(),B,true).getState());
	}
	@Test public void oldSameTickPacketCannotAuthorizeANewViewButALaterPacketCan()
	{
		contents=new Item[]{new Item(N,100)};display=contents.clone();LootingBagReader.Session proof=inspection();BankObservation bank=source.observe();
		proof.changed(source.character,bank,source.bagTick,true);request(proof);inspectNextTick(proof);
		assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		proof.changed(source.character,source.observe(),source.bagTick,true);inspectNextTick(proof);
		assertEquals(BagObservation.State.VERIFIED_NONEMPTY,source.bag().getState());assertEquals(Long.valueOf(100),source.bag().getQuantities().get(N));
	}
	@Test public void matchingRepeatedPacketsAndOpenClosedFormsKeepTheSameCapturedContents()
	{
		contents=new Item[]{new Item(N,100),new Item(C,500)};display=contents.clone();LootingBagReader.Session proof=inspection();request(proof);
		proof.changed(source.character,source.observe(),source.bagTick,true);inspectNextTick(proof);BagObservation first=source.bag();
		visible=false;display=null;proof.changed(source.character,source.observe(),source.bagTick,true);proof.bankClosed();
		bank(O,100,0,100000);BagObservation returned=source.bag();assertEquals(O,returned.getItemId());
		assertEquals(first.getQuantities(),returned.getQuantities());assertEquals(first.getAt(),returned.getAt());
	}
	@Test public void characterSessionAndLogoutInvalidationNeverReuseNativeProof()
	{
		contents=null;display=null;nativeEmpty=true;LootingBagReader.Session proof=inspection();request(proof);inspectNextTick(proof);
		assertEquals(BagObservation.State.VERIFIED_EMPTY,source.bag().getState());
		assertEquals(BagObservation.State.UNKNOWN,proof.read(SnapshotFixtures.CHARACTER_B,source.observe(),source.bagTick).getState());
		request(proof);inspectNextTick(proof);source.observer.reset();source.observer.tick();assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		request(proof);inspectNextTick(proof);proof.clear();assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
	}
	@Test public void nativeProofExpiresWithoutRefreshingItsTimestampOnRead()
	{
		final Instant[] now={Instant.now().plusSeconds(10)};contents=null;display=null;nativeEmpty=true;
		LootingBagReader.Session proof=new LootingBagReader.Session(inspectionReader(),()->now[0]);source.bagInspection=proof;request(proof);inspectNextTick(proof);
		BagObservation first=source.bag();assertEquals(BagObservation.State.VERIFIED_EMPTY,first.getState());assertEquals(first.getAt(),source.bag().getAt());
		now[0]=now[0].plusSeconds(121);assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
	}
	@Test public void filteredNativeEmptyMessagesAreAcceptedButPlayerChatAndUnrequestedMessagesAreNot()
	{
		contents=null;display=null;visible=false;LootingBagReader.Session proof=inspection();BankObservation bank=source.observe();
		proof.message(source.character,bank,source.bagTick,net.runelite.api.ChatMessageType.SPAM,"Your looting bag is empty.");assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		request(proof);proof.message(source.character,bank,source.bagTick,net.runelite.api.ChatMessageType.PUBLICCHAT,"Your looting bag is empty.");assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		proof.message(source.character,bank,source.bagTick,net.runelite.api.ChatMessageType.SPAM,"<col=ff0000>Your looting bag is empty.</col>");assertEquals(BagObservation.State.VERIFIED_EMPTY,source.bag().getState());
		proof.invalidate();assertEquals(BagObservation.State.UNKNOWN,source.bag().getState());
		assertFalse(LootingBagReader.isEmptyMessage("I think your looting bag is empty."));
	}
	@Test public void changedContentsAfterViewCannotConfirmTheOldPreview() throws Exception
	{
		contents=null;display=null;nativeEmpty=true;LootingBagReader.Session proof=inspection();request(proof);inspectNextTick(proof);BagObservation preview=source.bag();BankRun old=saved();
		contents=new Item[]{new Item(D,50)};proof.changed(source.character,source.observe(),source.bagTick,true);
		controller.buyBag(old.getRevision(),UUID.randomUUID(),preview,p->fail("Stale confirmation accepted"));worker.runAll();assertEquals(old,saved());assertEquals(0,saved().getBuybacks().getCommittedGp());
	}
	@Test public void emptyNativeBuybackWithoutPotionStorageChargesOnceForRepeatedConfirmation() throws Exception
	{
		assertFalse(source.potionStorageUnlocked());contents=null;display=null;nativeEmpty=true;LootingBagReader.Session proof=inspection();request(proof);inspectNextTick(proof);
		UUID revision=view.getRun().getRevision(),purchase=UUID.randomUUID();BagObservation preview=source.bag();
		controller.buyBag(revision,purchase,preview,p->{});controller.buyBag(revision,purchase,preview,p->{});worker.runAll();
		controller.buyBag(revision,purchase,preview,p->{});worker.runAll();assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(1,saved().getBuybacks().getPurchases().size());
		assertTrue(saved().getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());
	}
	@Test public void unlockedPotionStorageDoesNotGateNativeEmptyBagBuyback() throws Exception
	{
		com.fromscratch.potion.PotionType prayer=new com.fromscratch.potion.PotionType(1,"Prayer potion",false,Collections.singletonMap(ItemID._4DOSEPRAYERRESTORE,4));
		source.storage=new com.fromscratch.potion.PotionObservation(true,Collections.singletonMap(1,prayer),Collections.singletonMap(1,12L),Instant.now());assertTrue(source.potionStorageUnlocked());
		com.fromscratch.potion.PotionState before=saved().getPotions();contents=null;display=null;nativeEmpty=true;LootingBagReader.Session proof=inspection();request(proof);inspectNextTick(proof);buy();
		assertEquals(10000,saved().getBuybacks().getCommittedGp());assertEquals(before,saved().getPotions());assertEquals(90000,BuybackEngine.available(saved()));
	}
	@Test public void filteredOutLegacyBagStillQualifiesAndOnlyCommittedPurchaseChangesMembership()throws Exception
	{
		final net.runelite.client.plugins.banktags.BankTag[] tag={null};VirtualBankMode filter=new VirtualBankMode(new VirtualBankMode.Access(){String active;public void register(String name,net.runelite.client.plugins.banktags.BankTag value){tag[0]=value;}public void unregister(String name){tag[0]=null;}public void open(String name){active=name;}public void close(){active=null;}public String active(){return active;}public void refresh(){}});filter.setEnabled(true);controller.informationActions(r->filter.update(r,source.open));controller.tick();worker.runAll();assertFalse(tag[0].contains(B));assertEquals(1,saved().physical(B));proof(new Item(N,100));controller.buyBag(view.getRun().getRevision(),UUID.randomUUID(),source.bag(),p->{});assertFalse(tag[0].contains(B));worker.runAll();assertTrue(tag[0].contains(B));assertFalse(tag[0].contains(N));assertEquals(1,saved().physical(B));filter.stop();
	}
}
