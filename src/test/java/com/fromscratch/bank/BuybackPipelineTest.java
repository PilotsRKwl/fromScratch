package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.ge.*;
import com.fromscratch.overlay.*;
import com.fromscratch.persistence.*;
import com.fromscratch.wealth.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Full observed bank -> controller -> forced local ledger -> overlay/dashboard projection. */
public class BuybackPipelineTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root;private BankRepository repository;private Worker worker;private BankController controller;
	private boolean storageFailure;
	private final Source source=new Source();private final BankOverlayState overlay=new BankOverlayState();private final List<BankView> views=new ArrayList<>();
	private static final int P=ItemID.BH_RUNE_POUCH,D=ItemID.DIVINE_RUNE_POUCH,F=ItemID.TZHAAR_CAPE_FIRE,C=ItemID.COINS;
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source.items=new Item[]{new Item(F,1),new Item(P,1)};overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();bank(3000000,F,P);source.pouchProof=new com.fromscratch.pouch.PouchObservation(true,Collections.emptyMap(),Instant.now(),P);}
	@After public void stop(){controller.stop();}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);});controller.start(()->{if(storageFailure){throw new java.io.IOException("Synthetic storage failure");}return root;},worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private void bank(int gp,int...ids){List<Item> items=new ArrayList<>();items.add(new Item(C,gp));for(int id:ids){items.add(new Item(id,1));}source.items=items.toArray(new Item[0]);source.observer.changed();settle();}
	private BankView view(){return views.get(views.size()-1);}
	private BankRun saved()throws Exception{return repository.load(root,source.character).get();}
	private void buy(String id){controller.buyback(view().getRun().getRevision(),id,UUID.randomUUID());worker.runAll();}
	@Test public void resetClearsPurchasedOwnershipAndSpendingForNextRun()throws Exception{buy("fire_cape");BankRun old=saved();assertEquals(1000000,old.getBuybacks().getCommittedGp());controller.reset(old.getRevision());worker.runAll();assertTrue(saved().getBuybacks().getPurchases().isEmpty());assertEquals(0,saved().getBuybacks().getCommittedGp());assertTrue(saved().getBalances().isEmpty());assertEquals(0,saved().getFinancial().getExpenseGp());assertEquals(0,saved().getExchange().getPurchases());assertEquals(0,saved().getExchange().getSales());assertEquals(0,saved().getFinancial().getGainedGp());controller.startRun(true,null,true);worker.runAll();assertNotEquals(old.getRunId(),saved().getRunId());assertEquals(0,saved().getBuybacks().getCommittedGp());}
	@Test public void purchasePublishesOnlyAfterDurableAcknowledgment()throws Exception{BankRun before=saved();controller.buyback(before.getRevision(),"fire_cape",UUID.randomUUID());assertEquals("0",overlay.getProjection().label(F));assertEquals(before,saved());worker.runAll();assertEquals("1",overlay.getProjection().label(F));assertEquals(2000000,BuybackEngine.available(saved()));assertEquals(before.getPhysical(),saved().getPhysical());}
	@Test public void separatePurchasesSpendOnceAndRefreshAvailableGpAndWorth()throws Exception{buy("rune_pouch_family");buy("fire_cape");assertEquals(1000000,BuybackEngine.available(saved()));assertEquals(Long.valueOf(1000000),view().getValuation().getSubtotalGp());assertEquals(2,saved().getBuybacks().getPurchases().size());assertEquals("1",overlay.getProjection().label(P));assertEquals("1",overlay.getProjection().label(F));assertEquals("1000K",overlay.getProjection().label(C));}
	@Test public void pouchUpgradePreservesOnePaymentInActualBankPipeline()throws Exception{buy("rune_pouch_family");bank(3000000,F,D);assertEquals("1",overlay.getProjection().label(D));assertEquals("0",overlay.getProjection().label(P));assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void pouchRevertDoesNotCreateAnotherEntitlement()throws Exception{buy("rune_pouch_family");bank(3000000,F,D);bank(3000000,F,P);assertEquals("1",overlay.getProjection().label(P));assertEquals(1000000,saved().getBuybacks().getCommittedGp());}
	@Test public void withdrawReturnRetainsPaidAccessWithoutPersonalTabs()throws Exception{buy("fire_cape");bank(3000000,P);assertEquals("0",overlay.getProjection().label(F));bank(3000000,F,P);assertEquals("1",overlay.getProjection().label(F));assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void physicalMultipleCopiesCannotDuplicatePouchAllowance()throws Exception{buy("rune_pouch_family");bank(3000000,F,P,D);assertEquals(2,BuybackEngine.balances(saved()).getOrDefault(P,0L)+BuybackEngine.balances(saved()).getOrDefault(D,0L));assertEquals(1,saved().getBuybacks().getEntitlements().size());}
	@Test public void duplicateConfirmationCannotPurchaseTwice()throws Exception{UUID revision=view().getRun().getRevision(),id=UUID.randomUUID();controller.buyback(revision,"fire_cape",id);controller.buyback(revision,"fire_cape",id);worker.runAll();controller.buyback(revision,"fire_cape",id);worker.runAll();assertEquals(1,saved().getBuybacks().getPurchases().size());assertEquals(1000000,saved().getBuybacks().getCommittedGp());}
	@Test public void closingBankRejectsPurchaseWithoutChangingProgress()throws Exception{BankRun before=saved();source.open(false);settle();buy("fire_cape");assertEquals(before,saved());assertTrue(view().getMessage().contains("bank"));}
	@Test public void changedPhysicalBankRejectsStaleShopIntent()throws Exception{BankRun before=saved();source.items=new Item[]{new Item(P,1),new Item(C,3000000)};source.observer.changed();source.observer.tick();controller.buyback(before.getRevision(),"fire_cape",UUID.randomUUID());worker.runAll();assertEquals(before,saved());assertTrue(view().getMessage().contains("bank changed"));}
	@Test public void insufficientBudgetAfterPriorBuybackRejectsSecond()throws Exception{bank(1000000,F,P);buy("rune_pouch_family");BankRun before=saved();buy("fire_cape");assertEquals(before,saved());assertEquals(0,BuybackEngine.available(saved()));}
	@Test public void restartRestoresEntitlementsAndCommittedExpenditure()throws Exception{buy("rune_pouch_family");BankRun before=saved();controller.stop();source.observer.reset();launch();settle();assertEquals(before.getBuybacks(),saved().getBuybacks());assertEquals("1",overlay.getProjection().label(P));assertEquals(2000000,BuybackEngine.available(saved()));}
	@Test public void restartWhilePouchOutsideBankRetainsUnlockOnReturn()throws Exception{buy("rune_pouch_family");bank(3000000,F);controller.stop();source.observer.reset();launch();settle();assertEquals("0",overlay.getProjection().label(P));bank(3000000,F,D);assertEquals("1",overlay.getProjection().label(D));assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void correctionDoesNotClearSpendingOrEntitlement()throws Exception{buy("fire_cape");controller.correct(view().getRun().getRevision(),C,1500000);worker.runAll();bank(3500000,F,P);assertEquals(1000000,BuybackEngine.available(saved()));assertEquals(1000000,saved().getBuybacks().getCommittedGp());assertEquals("1",overlay.getProjection().label(F));}
	@Test public void rawGpMayBecomeNegativeAfterPurchase()throws Exception{buy("fire_cape");bank(0,F,P);assertEquals(0,saved().balance(C));assertEquals(-1000000,BuybackEngine.available(saved()));assertEquals("1",overlay.getProjection().label(F));}
	@Test public void historicalGainsAndGeStateAreUnchangedByBuyback()throws Exception{BankRun before=saved();buy("fire_cape");assertEquals(before.getFinancial(),saved().getFinancial());assertEquals(before.getExchange(),saved().getExchange());assertEquals(before.getInitialBank(),saved().getInitialBank());}
	@Test public void geActivityAfterBuybackKeepsPouchAndBankMoneyIndependent()throws Exception{buy("rune_pouch_family");new GePackets().send(controller::exchange,0,SHIELD,GrandExchangeOfferState.SELLING,10,3,100,300);worker.runAll();assertEquals(3000000,saved().balance(C));assertEquals(2000000,BuybackEngine.available(saved()));assertEquals("1",overlay.getProjection().label(P));assertFalse(saved().getExchange().getRecent().isEmpty());}
	@Test public void charactersCannotShareEntitlements()throws Exception{buy("fire_cape");source.character=SnapshotFixtures.CHARACTER_B;source.observer.reset();settle();assertNull(view().getRun());assertEquals(1,repository.load(root,SnapshotFixtures.CHARACTER_A).get().getBuybacks().getPurchases().size());assertNull(overlay.getProjection());}
	@Test public void purchaseAndBankHistoryStaySeparate()throws Exception{buy("fire_cape");BankHistory.Page manual=repository.history(root,saved(),null,true);assertTrue(manual.getRows().isEmpty());assertEquals(BankAudit.Kind.BUYBACK,saved().getRecent().get(0).getKind());assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void unavailableOrTradeableDefinitionCannotChargeGp()throws Exception{BankRun before=saved();source.buybackDefinitions=false;buy("fire_cape");assertEquals(before,saved());assertTrue(view().getMessage().contains("No GP was spent"));}
	@Test public void failedStorageAcknowledgmentDoesNotPublishUnlockedItem()throws Exception{BankRun before=saved();storageFailure=true;buy("fire_cape");assertTrue(view().isError());assertEquals(before,view().getRun());assertEquals(before,saved());assertEquals("0",overlay.getProjection().label(F));assertEquals(before,controller.currentRun());storageFailure=false;controller.retry();worker.runAll();settle();buy("fire_cape");assertEquals(1,saved().getBuybacks().getPurchases().size());assertEquals("1",overlay.getProjection().label(F));}
	@Test public void completeGracefulSetUsesOneControllerCommitForSixOverlaySlots()throws Exception
	{
		controller.stop();root=Filepath.Unchecked.getRooted(temporary.newFolder().toPath());int[] ids={ItemID.GRACEFUL_HOOD,ItemID.GRACEFUL_CAPE,ItemID.GRACEFUL_TOP,ItemID.GRACEFUL_LEGS,ItemID.GRACEFUL_GLOVES,ItemID.GRACEFUL_BOOTS};
		source.items=Arrays.stream(ids).mapToObj(id->new Item(id,1)).toArray(Item[]::new);source.observer.reset();launch();settle();controller.startRun(true,null,true);worker.runAll();bank(1000000,ids);BankRun before=saved();
		controller.buyback(before.getRevision(),"graceful",UUID.randomUUID());for(int id:ids){assertEquals("0",overlay.getProjection().label(id));}worker.runAll();for(int id:ids){assertEquals("1",overlay.getProjection().label(id));}assertEquals(before.getSequence()+1,saved().getSequence());assertEquals(1,saved().getBuybacks().getPurchases().size());assertEquals(6,saved().getBuybacks().getEntitlements().size());
	}
	@Test public void paidPouchIsOneDurableUnlockAcrossBothForms()throws Exception{buy("rune_pouch_family");long count=saved().unlocked();bank(3000000,F,D);assertEquals(count,saved().unlocked());bank(3000000,F);assertEquals(count,saved().unlocked());assertEquals(1,saved().getBuybacks().getEntitlements().size());}
	@Test public void negativeBeforePurchaseUnlocksOneAndDoesNotMultiplyOnReturnsOrRestart()throws Exception
	{
		controller.stop();root=Filepath.Unchecked.getRooted(temporary.newFolder().toPath());source.items=new Item[]{new Item(F,1),new Item(F,1),new Item(F,1),new Item(F,1)};source.observer.reset();launch();settle();controller.startRun(true,null,true);worker.runAll();bank(1000000,F);assertEquals("-3",overlay.getProjection().label(F));buy("fire_cape");assertEquals("1",overlay.getProjection().label(F));assertEquals(-3,saved().balance(F));
		bank(1000000,F,F,F,F);assertEquals("1",overlay.getProjection().label(F));bank(1000000,F,F,F);assertEquals("0",overlay.getProjection().label(F));controller.stop();source.observer.reset();launch();settle();assertEquals("0",overlay.getProjection().label(F));bank(1000000,F,F,F,F);assertEquals("1",overlay.getProjection().label(F));assertEquals(1,saved().getBuybacks().getPurchases().size());assertEquals(1000000,saved().getBuybacks().getCommittedGp());
	}
}
