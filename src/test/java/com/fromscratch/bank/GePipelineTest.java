package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.ge.*;
import com.fromscratch.overlay.*;
import com.fromscratch.persistence.*;
import com.fromscratch.wealth.*;
import java.time.Instant;
import java.util.*;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real API-source copies -> FIFO controller -> forced Filepath records -> bank projection and financial UI. */
public class GePipelineTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root;private BankRepository repository;private Worker worker;private BankController controller;private PriceService prices;
	private final Source source=new Source();private final GePackets packets=new GePackets();private final BankOverlayState overlay=new BankOverlayState();private final List<BankView> views=new ArrayList<>();
	@Before public void setup()
	{
		root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());prices=new PriceService(()->new TreeMap<Integer,Long>(){{put(SHIELD,100L);put(ItemID.ASHES,50L);}},Instant::now);prices.refresh();
		source.items=new Item[]{new Item(SHIELD,1),new Item(ItemID.COINS,1000)};overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();settle();
	}
	@After public void stop(){controller.stop();}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);},prices);controller.start(()->root,worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private BankView view(){return views.get(views.size()-1);}
	private BankRun saved()throws Exception{return repository.load(root,source.character).get();}
	private void bank(int shields,int coins){source.items=new Item[]{new Item(SHIELD,shields),new Item(ItemID.COINS,coins)};source.observer.changed();settle();}
	private void offer(int slot,GrandExchangeOfferState state,int filled,long gp){packets.send(controller::exchange,slot,SHIELD,state,10,filled,100,gp);worker.runAll();settle();}
	@Test public void resetClearsFinalizedHistoryWithoutReplayingOfferAccounting()throws Exception{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);assertEquals(1,completed(true).size());BankRun before=saved();controller.reset(before.getRevision());worker.runAll();assertFalse(saved().isActive());assertEquals(0,saved().getExchange().getPurchases());assertTrue(saved().getBalances().isEmpty());assertEquals(1,saved().getRecent().size());}
	private List<GeTrade> completed(boolean buy)throws Exception
	{
		GeHistory.Page page=repository.exchangeHistory(root,saved(),null,buy);while(page.isScanning()){page=repository.exchangeHistory(root,saved(),page.getNext(),buy);}return page.getRows();
	}
	@Test public void pendingFillsStayHiddenAndCompletionUsesCumulativeExecutionWithoutReplay()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);assertTrue(completed(true).isEmpty());offer(0,GrandExchangeOfferState.BUYING,2,180);offer(0,GrandExchangeOfferState.BUYING,5,450);assertTrue(completed(true).isEmpty());bank(6,1000);
		offer(0,GrandExchangeOfferState.BOUGHT,10,910);offer(0,GrandExchangeOfferState.BOUGHT,10,910);bank(11,1000);BankRun before=saved();BankValuation wealth=view().getValuation();
		List<GeTrade> rows=completed(true);assertEquals(1,rows.size());assertEquals("Bought",rows.get(0).getStatus());assertEquals(10,rows.get(0).getQuantity());assertEquals(910,rows.get(0).getReportedGp());assertEquals(91,rows.get(0).getReportedGp()/rows.get(0).getQuantity());assertNull(rows.get(0).getEstimatedTax());
		assertEquals(before,saved());assertEquals(wealth,view().getValuation());assertEquals(10,before.balance(SHIELD));assertEquals(0,before.getFinancial().getGainedGp());
	}
	@Test public void canceledPartialsShowOnlyExecutedAmountsAndZeroCancellationsStayHidden()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BUYING,3,285);offer(0,GrandExchangeOfferState.CANCELLED_BUY,3,285);
		offer(1,GrandExchangeOfferState.SELLING,0,0);offer(1,GrandExchangeOfferState.SELLING,4,400);assertTrue(completed(false).isEmpty());offer(1,GrandExchangeOfferState.CANCELLED_SELL,4,400);
		offer(2,GrandExchangeOfferState.BUYING,0,0);offer(2,GrandExchangeOfferState.CANCELLED_BUY,0,0);offer(3,GrandExchangeOfferState.SELLING,0,0);offer(3,GrandExchangeOfferState.CANCELLED_SELL,0,0);BankRun before=saved();
		List<GeTrade> buys=completed(true),sales=completed(false);assertEquals(1,buys.size());assertEquals(1,sales.size());assertEquals("Bought (Partial)",buys.get(0).getStatus());assertEquals(3,buys.get(0).getQuantity());assertEquals(285,buys.get(0).getReportedGp());assertEquals("Sold (Partial)",sales.get(0).getStatus());assertEquals(4,sales.get(0).getQuantity());assertEquals(400,sales.get(0).getReportedGp());assertEquals(Long.valueOf(8),sales.get(0).getEstimatedTax());assertEquals(8,before.getExchange().getEstimatedTaxGp());assertEquals(before,saved());
	}
	@Test public void reusedSlotFinalizesIndependentOffersAndRestartPreservesDerivedIds()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);offer(0,GrandExchangeOfferState.EMPTY,0,0);offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,950);offer(0,GrandExchangeOfferState.BOUGHT,10,950);
		List<GeTrade> before=completed(true);assertEquals(2,before.size());assertNotEquals(before.get(0).getGeneration(),before.get(1).getGeneration());assertNotEquals(before.get(0).getId(),before.get(1).getId());assertEquals(20,before.stream().mapToLong(GeTrade::getQuantity).sum());
		controller.stop();source.observer.reset();packets.reset();launch();settle();offer(0,GrandExchangeOfferState.BOUGHT,10,950);assertEquals(before,completed(true));
	}
	@Test public void ordinaryPurchaseThenBankingNeverDuplicatesItemsOrGp()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);bank(1,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);bank(11,100);
		assertEquals(10,saved().balance(SHIELD));assertEquals(-900,saved().balance(ItemID.COINS));assertEquals("10",overlay.getProjection().label(SHIELD));
		assertEquals(0,saved().getFinancial().getGainedGp());assertEquals(1,saved().getExchange().getPurchases());
		GeTrade fill=saved().getExchange().getRecent().stream().filter(GeTrade::execution).findFirst().get();assertEquals(10,fill.getQuantity());assertEquals(900,fill.getReportedGp());assertNull(fill.getEstimatedTax());
	}
	@Test public void ordinarySaleDoesNotTreatGrossProceedsAsFreshGains()throws Exception
	{
		bank(0,1000);offer(0,GrandExchangeOfferState.SELLING,0,0);offer(0,GrandExchangeOfferState.SOLD,10,1000);bank(0,1980);
		assertEquals(980,saved().balance(ItemID.COINS));assertEquals(-1,saved().balance(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());assertEquals(20,saved().getExchange().getEstimatedTaxGp());assertEquals(0,saved().getFinancial().getExpenseGp());
	}
	@Test public void partialFillsUseCumulativeDifferencesAndBankCollectionsCanBePartial()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BUYING,3,285);bank(3,1000);offer(0,GrandExchangeOfferState.BUYING,7,660);bank(8,1000);
		assertEquals(7,saved().balance(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());assertEquals(2,saved().getExchange().getPurchases());
		assertEquals(4,saved().getExchange().getRecent().get(0).getQuantity());assertEquals(375,saved().getExchange().getRecent().get(0).getReportedGp());
	}
	@Test public void partiallyFilledCanceledOfferSurvivesRestartAndCollectionWithoutDuplicateCredits()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BUYING,3,285);offer(0,GrandExchangeOfferState.CANCELLED_BUY,3,285);
		assertEquals(1,saved().getExchange().getPurchases());assertEquals(3,saved().getExchange().getRecent().stream().filter(GeTrade::execution).mapToLong(GeTrade::getQuantity).sum());
		controller.stop();source.observer.reset();packets.reset();launch();settle();offer(0,GrandExchangeOfferState.CANCELLED_BUY,3,285);bank(4,1000);
		assertEquals(3,saved().balance(SHIELD));assertEquals("3",overlay.getProjection().label(SHIELD));assertEquals(1,saved().getExchange().getPurchases());assertEquals(0,saved().getFinancial().getGainedGp());
		assertEquals(1,completed(true).size());assertEquals("Bought (Partial)",completed(true).get(0).getStatus());assertEquals(3,completed(true).get(0).getQuantity());
	}
	@Test public void repeatedPacketsAndBankReopensDoNotDuplicateHistory()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);bank(11,1000);BankRun before=saved();offer(0,GrandExchangeOfferState.BOUGHT,10,900);source.open(false);settle();source.open(true);settle();assertEquals(before,saved());
	}
	@Test public void missingGeDataNeverBlocksOrdinaryBankUpdates()throws Exception{bank(3,1100);assertEquals(2,saved().balance(SHIELD));assertEquals(300,saved().getFinancial().getGainedGp());assertTrue(saved().getExchange().getOffers().isEmpty());}
	@Test public void unrelatedItemGainsRemainEligibleAlongsideMatchedTrade()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);source.items=new Item[]{new Item(SHIELD,11),new Item(ItemID.COINS,1000),new Item(ItemID.ASHES,1)};source.observer.changed();settle();assertEquals(50,saved().getFinancial().getGainedGp());assertEquals(10,saved().getFinancial().getHighWater().get(SHIELD).longValue());
	}
	@Test public void laterWithdrawAndRedepositOfBoughtItemsNeverCreatesGains()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);bank(11,1000);bank(6,1000);bank(11,1000);assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void concurrentOffersCannotAttributeOneDepositTwice()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(1,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);offer(1,GrandExchangeOfferState.BOUGHT,10,900);bank(11,1000);assertEquals(0,saved().getFinancial().getGainedGp());assertEquals(10,saved().getRecent().get(0).getExchange().getMatches().stream().filter(GeMatch::isDeposit).mapToLong(GeMatch::getQuantity).sum());bank(21,1000);assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void canceledBuyRefundOnlyMovesActualBankCoins()throws Exception
	{bank(1,0);offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.CANCELLED_BUY,0,0);bank(1,1000);assertEquals(0,saved().balance(ItemID.COINS));assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void canceledSellReturnDoesNotBecomeNewIncome()throws Exception
	{bank(0,1000);offer(0,GrandExchangeOfferState.SELLING,0,0);offer(0,GrandExchangeOfferState.CANCELLED_SELL,0,0);bank(1,1000);assertEquals(0,saved().balance(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void restartRestoresOfferIdentityAndDoesNotReplayFills()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BUYING,3,285);GeState before=saved().getExchange();assertTrue(completed(true).isEmpty());controller.stop();source.observer.reset();packets.reset();launch();settle();offer(0,GrandExchangeOfferState.BUYING,3,285);assertEquals(before.getRecent(),saved().getExchange().getRecent());assertEquals(before.getCredits(),saved().getExchange().getCredits());assertEquals(before.getOffers().get(0).getGeneration(),saved().getExchange().getOffers().get(0).getGeneration());offer(0,GrandExchangeOfferState.BUYING,7,660);assertEquals(4,saved().getExchange().getRecent().get(0).getQuantity());assertTrue(completed(true).isEmpty());offer(0,GrandExchangeOfferState.CANCELLED_BUY,7,660);assertEquals(1,completed(true).size());assertEquals(7,completed(true).get(0).getQuantity());assertEquals(660,completed(true).get(0).getReportedGp());
	}
	@Test public void offlineNetFillHasExplicitUncertaintyAndNoReplay()throws Exception
	{
		offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BUYING,3,285);controller.stop();source.observer.reset();packets.reset();launch();settle();offer(0,GrandExchangeOfferState.BUYING,7,660);
		assertEquals(GeTrade.Evidence.COMPATIBLE_NET_FILL,saved().getExchange().getRecent().get(0).getEvidence());assertEquals(4,saved().getExchange().getRecent().get(0).getQuantity());
	}
	@Test public void fullyOfflineUnobservedTradesAreNotFabricated()throws Exception{controller.stop();source.quantity(3);source.observer.reset();launch();settle();assertTrue(saved().getExchange().getRecent().isEmpty());assertEquals(2,saved().balance(SHIELD));}
	@Test public void noGeProgressPublishesBeforePersistenceAcknowledgment()throws Exception
	{
		GeObservation o=new GeObservation(UUID.randomUUID(),UUID.randomUUID(),Instant.now(),0,SHIELD,"Wooden shield",GrandExchangeOfferState.BUYING,10,0,100,0,false);
		controller.exchange(o);assertTrue(view().getRun().getExchange().getOffers().isEmpty());assertTrue(saved().getExchange().getOffers().isEmpty());worker.runAll();assertEquals(1,view().getRun().getExchange().getOffers().size());
	}
	@Test public void currentWorthRemainsBankOnlyWhenOffersAreUncollected()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);assertEquals(0,saved().balance(SHIELD));assertEquals(Long.valueOf(0),view().getValuation().getSubtotalGp());}
	@Test public void taxIsRecordedOnceAndNeverDebitsBankGp()throws Exception
	{offer(0,GrandExchangeOfferState.SELLING,0,0);offer(0,GrandExchangeOfferState.SELLING,3,300);offer(0,GrandExchangeOfferState.SOLD,10,1000);offer(0,GrandExchangeOfferState.SOLD,10,1000);BankRun before=saved();assertEquals(20,before.getExchange().getEstimatedTaxGp());assertEquals(0,before.balance(ItemID.COINS));assertEquals(1,completed(false).size());assertEquals("Sold",completed(false).get(0).getStatus());assertEquals(10,completed(false).get(0).getQuantity());assertEquals(Long.valueOf(20),completed(false).get(0).getEstimatedTax());assertEquals(before,saved());}
	@Test public void historyReadsSeparatePurchaseAndSalePages()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);offer(1,GrandExchangeOfferState.SELLING,0,0);offer(1,GrandExchangeOfferState.SOLD,10,1000);assertTrue(repository.exchangeHistory(root,saved(),null,true).getRows().stream().allMatch(GeTrade::isBuy));assertTrue(repository.exchangeHistory(root,saved(),null,false).getRows().stream().noneMatch(GeTrade::isBuy));}
	@Test public void slotClearStaysDurableButDisappearsFromPurchaseQueryWithoutLosingFill()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,900);offer(0,GrandExchangeOfferState.EMPTY,0,0);assertTrue(saved().getExchange().getRecent().stream().anyMatch(t->t.getStatus().startsWith("Slot cleared")));java.util.List<GeTrade> rows=repository.exchangeHistory(root,saved(),null,true).getRows();assertTrue(rows.stream().noneMatch(t->t.getStatus().startsWith("Slot cleared")));assertEquals(10,rows.stream().filter(GeTrade::execution).mapToLong(GeTrade::getQuantity).sum());}
	@Test public void perCharacterIsolationIncludesOfferGenerationsAndFees()throws Exception
	{offer(0,GrandExchangeOfferState.SELLING,0,0);offer(0,GrandExchangeOfferState.SOLD,10,1000);source.character=SnapshotFixtures.CHARACTER_B;source.observer.reset();settle();assertNull(view().getRun());assertEquals(20,repository.load(root,SnapshotFixtures.CHARACTER_A).get().getExchange().getEstimatedTaxGp());}
	@Test public void onlyAmountBeyondCompatiblePurchaseRemainsANewGain()throws Exception
	{offer(0,GrandExchangeOfferState.BUYING,0,0);offer(0,GrandExchangeOfferState.BOUGHT,10,1000);bank(16,1000);assertEquals(15,saved().balance(SHIELD));assertEquals(500,saved().getFinancial().getGainedGp());bank(18,1000);assertEquals(700,saved().getFinancial().getGainedGp());}
	@Test public void offersObservedBeforeStartArePreexistingWithoutGeSetup()throws Exception
	{
		controller.stop();source.character=SnapshotFixtures.CHARACTER_B;source.observer.reset();packets.reset();launch();settle();assertNull(view().getRun());
		offer(0,GrandExchangeOfferState.BOUGHT,10,900);controller.startRun(true,null,true);worker.runAll();settle();assertEquals(GeOffer.Origin.PREEXISTING,saved().getExchange().getOffers().get(0).getOrigin());
		bank(11,1000);assertEquals(10,saved().balance(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());assertTrue(view().getMessage().contains("preexisting GE offer"));
	}
	@Test public void firstSeenCompletedOfferHasUnknownOriginInsteadOfInventedPriorHistory()throws Exception
	{offer(0,GrandExchangeOfferState.BOUGHT,10,900);assertEquals(GeOffer.Origin.UNKNOWN,saved().getExchange().getRecent().get(0).getOrigin());assertEquals(GeTrade.Evidence.EXISTING_SNAPSHOT,saved().getExchange().getRecent().get(0).getEvidence());assertEquals(0,saved().getExchange().getPurchases());assertEquals(1,completed(true).size());assertEquals(GeTrade.Evidence.EXISTING_SNAPSHOT,completed(true).get(0).getEvidence());assertEquals(900,completed(true).get(0).getReportedGp());bank(11,1000);assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void rapidGeEventsAndBankSnapshotCommitInOrder()throws Exception
	{
		packets.send(controller::exchange,0,SHIELD,GrandExchangeOfferState.BUYING,100,0,100,0);worker.runAll();
		for(int q=1;q<=80;q++){packets.send(controller::exchange,0,SHIELD,GrandExchangeOfferState.BUYING,100,q,100,q*100L);}
		source.items=new Item[]{new Item(SHIELD,81),new Item(ItemID.COINS,1000)};source.observer.changed();settle();assertEquals(80,saved().getExchange().getPurchases());assertEquals(80,saved().balance(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());assertEquals("80",overlay.getProjection().label(SHIELD));
		List<GeHistory.Page> pages=new ArrayList<>();controller.exchangeHistory(true,null,pages::add);worker.execute(()->assertTrue("Bank worker must get a turn before long history scanning finishes",pages.isEmpty()));worker.runAll();assertEquals(1,pages.size());assertTrue(pages.get(0).getRows().isEmpty());assertNull(pages.get(0).getNext());assertEquals(80,saved().getExchange().getPurchases());
	}
	@Test public void platinumProceedsConsumeTheSameGpBudgetOnce()throws Exception
	{
		offer(0,GrandExchangeOfferState.SELLING,0,0);offer(0,GrandExchangeOfferState.SOLD,10,10000);
		source.items=new Item[]{new Item(SHIELD,1),new Item(ItemID.COINS,1800),new Item(ItemID.PLATINUM,9)};source.observer.changed();settle();
		assertEquals(800,saved().balance(ItemID.COINS));assertEquals(9,saved().balance(ItemID.PLATINUM));assertEquals(0,saved().getFinancial().getGainedGp());
		assertEquals(200,saved().getExchange().getCredits().stream().filter(c->c.getKind()==GeCredit.Kind.SALE_PROCEEDS).mapToLong(GeCredit::getRemaining).sum());
	}
	@Test public void geOnlyCommitReusesImmutableBankLabelCache()throws Exception
	{Map<Integer,String> labels=overlay.getProjection().getLabels();offer(0,GrandExchangeOfferState.BUYING,0,0);assertSame(labels,overlay.getProjection().getLabels());assertEquals(saved().getRevision(),overlay.getProjection().getRevision());}
	@Test public void extremeOfferBurstRetainsLatestTotalsWithoutFillingTheBankQueue()throws Exception
	{
		packets.send(controller::exchange,0,SHIELD,GrandExchangeOfferState.BUYING,1000,0,100,0);worker.runAll();
		long queuedAt=System.nanoTime();for(int q=1;q<=400;q++){packets.send(controller::exchange,0,SHIELD,GrandExchangeOfferState.BUYING,1000,q,100,q*100L);}System.out.printf(Locale.ROOT,"GE_BURST_400 source_and_client_queue_ms=%.3f%n",(System.nanoTime()-queuedAt)/1e6);
		worker.runAll();settle();assertEquals(400,saved().getExchange().getOffers().get(0).getLast().getFilled());assertEquals(40000,saved().getExchange().getOffers().get(0).getLast().getReportedGp());assertFalse(view().isError());
		bank(401,1000);assertEquals("400",overlay.getProjection().label(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());
	}
}
