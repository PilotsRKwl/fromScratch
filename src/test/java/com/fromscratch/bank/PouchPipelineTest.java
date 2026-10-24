package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.persistence.*;
import com.fromscratch.pouch.*;
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

/** Focused automatic pouch purchase -> durable bank deficits -> ordinary deposit integration. */
public class PouchPipelineTest
{
 @Rule public TemporaryFolder temporary=new TemporaryFolder();
 private static final int P=ItemID.BH_RUNE_POUCH,D=ItemID.DIVINE_RUNE_POUCH,N=ItemID.NATURERUNE,L=ItemID.LAWRUNE,C=ItemID.COINS;
 private final Source source=new Source();private final BankOverlayState overlay=new BankOverlayState();private final List<BankView> views=new ArrayList<>();
 private Filepath root;private BankRepository repository;private BankController controller;private Worker worker;private PriceService prices;private boolean fail;
 @Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source.items=new Item[]{new Item(P,1)};prices=new PriceService(()->{Map<Integer,Long> q=new HashMap<>();q.put(N,100L);q.put(L,50L);return q;},()->SnapshotFixtures.TIME);prices.refresh();overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();bank(P,0,0);}
 @After public void stop(){controller.stop();}
 private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);},prices);controller.start(()->{if(fail){throw new java.io.IOException("synthetic interruption");}return root;},worker);worker.runAll();}
 private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
 private void bank(int pouch,int nature,int law){List<Item> q=new ArrayList<>();if(pouch>0){q.add(new Item(pouch,1));}if(nature>0){q.add(new Item(N,nature));}if(law>0){q.add(new Item(L,law));}q.add(new Item(C,2000000));source.items=q.toArray(new Item[0]);source.observer.changed();settle();}
 private BankView view(){return views.get(views.size()-1);}private BankRun saved()throws Exception{return repository.load(root,source.character).get();}
 private void readBankedPouch(int form,Map<Integer,Long> contents)throws Exception
 {
  int nature=Math.toIntExact(saved().physical(N)),law=Math.toIntExact(saved().physical(L));bank(form,nature,law);
  final Map<Integer,Integer> bits=new HashMap<>(),types=new HashMap<>();int slot=0;for(Map.Entry<Integer,Long> e:contents.entrySet()){bits.put(RuneLitePouchReader.TYPES[slot],slot+1);bits.put(RuneLitePouchReader.QUANTITIES[slot],Math.toIntExact(e.getValue()));types.put(slot+1,e.getKey());slot++;}
  source.pouchAccess=new RuneLitePouchReader.Access(){public boolean live(){return true;}public int varbit(int id){return bits.getOrDefault(id,0);}public int rune(int type){return types.getOrDefault(type,-1);}};
  assertTrue(source.open);assertTrue(source.pouch().verified());
 }
 private void buy(){controller.buyback(view().getRun().getRevision(),"rune_pouch_family",UUID.randomUUID());worker.runAll();}
 private Map<Integer,Long> loaded(){Map<Integer,Long> q=new TreeMap<>();q.put(N,500L);q.put(L,200L);return q;}
 @Test public void loadedPouchCommitsOnePaymentAllDeficitsAndAutomaticPurchaseHistory()throws Exception{readBankedPouch(P,loaded());BankRun before=saved();buy();assertEquals(-500,saved().balance(N));assertEquals(-200,saved().balance(L));assertEquals(1000000,BuybackEngine.available(saved()));assertEquals(before.getPhysical(),saved().getPhysical());assertEquals(before.getFinancial(),saved().getFinancial());assertEquals("1",overlay.getProjection().label(P));assertEquals("-500",overlay.getProjection().label(N));BankHistory.Page history=repository.history(root,saved(),null,false);assertTrue(history.getRows().stream().anyMatch(r->r.getItemId()==N&&r.getChange()==-500&&r.getCategory().equals("Automatic — Legacy Rune Pouch")&&r.getPurchase().equals(savedUnchecked().getBuybacks().getPurchases().get(0).getId())));}
 private BankRun savedUnchecked(){try{return saved();}catch(Exception ex){throw new AssertionError(ex);}}
 @Test public void earnedOneHundredReturnsAfterLegacyFiveHundredDepositWithoutExtraGains()throws Exception{bank(P,100,0);readBankedPouch(P,Collections.singletonMap(N,500L));long gain=saved().getFinancial().getGainedGp();buy();assertEquals(-400,saved().balance(N));bank(P,600,0);assertEquals(100,saved().balance(N));assertEquals(gain,saved().getFinancial().getGainedGp());assertEquals(Long.valueOf(1010000),view().getValuation().getSubtotalGp());}
 @Test public void partialThenCompleteDepositsClearOnlyActualBankedAmounts()throws Exception{readBankedPouch(P,loaded());buy();bank(P,100,0);assertEquals(-400,saved().balance(N));assertEquals(-200,saved().balance(L));bank(P,500,200);assertEquals(0,saved().balance(N));assertEquals(0,saved().balance(L));assertEquals(2000000,saved().getFinancial().getGainedGp());}
 @Test public void verifiedEmptyDivinePouchHasNoRuneAdjustments()throws Exception{readBankedPouch(D,Collections.emptyMap());buy();assertEquals(1,saved().getBuybacks().getPurchases().size());assertTrue(saved().getBuybacks().getPurchases().get(0).getLegacyRunes().isEmpty());assertEquals("1",overlay.getProjection().label(D));assertEquals(1000000,saved().getBuybacks().getCommittedGp());}
 @Test public void unavailableContentsNeverChargeOrUnlock()throws Exception{buy();assertEquals(0,saved().getBuybacks().getCommittedGp());assertTrue(view().getMessage().contains("not available"));source.pouchAccess=new RuneLitePouchReader.Access(){public boolean live(){return false;}public int varbit(int id){throw new IllegalStateException();}public int rune(int type){throw new IllegalStateException();}};buy();assertEquals(0,saved().getBuybacks().getCommittedGp());}
 @Test public void previewCancellationHasNoStateOrAdjustmentSideEffect()throws Exception{readBankedPouch(P,loaded());BankRun before=saved();controller.pouchPreview(proof->assertTrue(proof.verified()));assertEquals(before,saved());assertEquals(before,view().getRun());}
 @Test public void failedCommitPublishesNoPaymentUnlockOrRuneDebit()throws Exception{readBankedPouch(P,loaded());BankRun before=saved();fail=true;buy();assertTrue(view().isError());assertEquals(before,saved());assertEquals(before,view().getRun());assertEquals(0,saved().getBuybacks().getCommittedGp());}
 @Test public void duplicatesRestartAndLaterRefillsCannotCreateAnotherAdjustment()throws Exception{readBankedPouch(P,loaded());buy();BankRun before=saved();buy();controller.stop();source.observer.reset();source.pouchProof=PouchObservation.unknown();launch();settle();assertEquals(before.getBalances(),saved().getBalances());assertEquals(before.getBuybacks(),saved().getBuybacks());readBankedPouch(P,Collections.singletonMap(N,1000L));buy();assertEquals(-500,saved().balance(N));assertEquals(1,saved().getBuybacks().getPurchases().size());}
 @Test public void changedContentsAfterPreviewRejectPurchaseWithoutAnyWrite()throws Exception{readBankedPouch(P,loaded());PouchObservation preview=source.pouch();readBankedPouch(P,Collections.emptyMap());controller.buyback(view().getRun().getRevision(),"rune_pouch_family",UUID.randomUUID(),preview,p->fail("No acknowledgment"));worker.runAll();assertEquals(0,saved().getBuybacks().getCommittedGp());}
 @Test public void ordinaryDepositsAfterPurchaseStillTrackAndPouchUpgradeKeepsOnePayment()throws Exception{readBankedPouch(P,loaded());buy();bank(D,501,200);assertEquals(1,saved().balance(N));assertEquals("1",overlay.getProjection().label(D));assertEquals(1000000,saved().getBuybacks().getCommittedGp());assertEquals(2000100,saved().getFinancial().getGainedGp());}
}