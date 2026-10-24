package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.overlay.*;
import com.fromscratch.persistence.*;
import com.fromscratch.wealth.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real observer/controller/Filepath commit/render projection/dashboard path, with a synthetic bulk price feed. */
public class WealthPipelineTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private Filepath root;private BankRepository repository;private Source source;private Worker worker;private BankController controller;
	private final BankOverlayState overlay=new BankOverlayState();private final List<BankView> views=new ArrayList<>();
	private PriceService prices;private final AtomicLong price=new AtomicLong(100);private final AtomicInteger requests=new AtomicInteger();
	@Before public void setup()
	{
		root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source=new Source();
		prices=new PriceService(()->{requests.incrementAndGet();return Collections.singletonMap(SHIELD,price.get());},()->SnapshotFixtures.TIME);prices.refresh();overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();settle();
	}
	@After public void stop(){controller.stop();}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);},prices);controller.start(()->root,worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private void change(long n){source.quantity(n);settle();}
	private BankView view(){return views.get(views.size()-1);}
	private BankRun saved()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	@Test public void automaticDepositCommitsHistoricalAndCurrentValueWithOverlay()throws Exception{change(3);assertEquals(200,saved().getFinancial().getGainedGp());assertEquals("2",overlay.getProjection().label(SHIELD));assertEquals(Long.valueOf(200),view().getValuation().getSubtotalGp());assertEquals(saved().getRevision(),view().getValuation().getRevision());}
	@Test public void withdrawalAndRedepositStayOneRecognizedGain()throws Exception{change(3);change(2);change(3);assertEquals(200,saved().getFinancial().getGainedGp());assertEquals(1,saved().getFinancial().getEventCount());assertEquals("2",overlay.getProjection().label(SHIELD));}
	@Test public void legacyReturnCannotManufactureGainOrExpense()throws Exception{change(0);assertEquals("-1",overlay.getProjection().label(SHIELD));change(1);assertEquals(0,saved().getFinancial().getGainedGp());assertEquals(0,saved().getFinancial().getExpenseRecords());}
	@Test public void finalWithdrawalKeepsNegativeMarketValueAcrossPlaceholderAndRestart()throws Exception
	{
		// A second synthetic run establishes four legacy copies, without resetting any real save.
		controller.stop(); source.character=SnapshotFixtures.CHARACTER_B; source.quantity(4); source.observer.reset(); launch(); settle(); controller.startRun(true,null,true); worker.runAll(); settle();
		source.quantity(1); settle(); assertEquals(Long.valueOf(-300),view().getValuation().getSubtotalGp());
		source.placeholders.add(net.runelite.api.gameval.ItemID.Placeholder.WOODEN_SHIELD);
		source.items=new net.runelite.api.Item[]{new net.runelite.api.Item(net.runelite.api.gameval.ItemID.Placeholder.WOODEN_SHIELD,0)};source.observer.changed();settle();
		assertEquals(-4,view().getRun().balance(SHIELD));assertEquals(Long.valueOf(-400),view().getValuation().getSubtotalGp());
		FinancialState financial=view().getRun().getFinancial();controller.stop();source.observer.reset();launch();settle();
		assertEquals(-4,view().getRun().balance(SHIELD));assertEquals(Long.valueOf(-400),view().getValuation().getSubtotalGp());assertEquals(financial,view().getRun().getFinancial());
	}
	@Test public void correctionUpdatesCounterWithoutManufacturingIncome()throws Exception{controller.correct(view().getRun().getRevision(),SHIELD,1);worker.runAll();assertEquals("1",overlay.getProjection().label(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());change(0);change(1);assertEquals(0,saved().getFinancial().getGainedGp());}
	@Test public void priceRefreshUpdatesDashboardWithoutChangingCheckpointOrHistory()throws Exception{change(2);BankRun before=saved();price.set(200);prices.refresh();settle();assertEquals(Long.valueOf(200),view().getValuation().getSubtotalGp());assertEquals(100,view().getRun().getFinancial().getGainedGp());assertEquals(before,saved());assertEquals(2,requests.get());}
	@Test public void bankPipelineNeverFetchesPriceFeedDuringRapidChanges()throws Exception{for(int i=0;i<30;i++){change(2);change(1);}assertEquals(1,requests.get());assertEquals(100,saved().getFinancial().getGainedGp());assertEquals(0,saved().balance(SHIELD));}
	@Test public void restartDoesNotReplayFinancialEvents()throws Exception{change(3);BankRun before=saved();controller.stop();source.observer.reset();launch();settle();assertEquals(before.getFinancial(),saved().getFinancial());assertEquals(before.getRunId(),saved().getRunId());assertEquals("2",overlay.getProjection().label(SHIELD));}
	@Test public void returningOfflineBankGainRecognizesOnlyNewHighWater()throws Exception{change(3);controller.stop();source.quantity(4);source.observer.reset();launch();settle();assertEquals(300,saved().getFinancial().getGainedGp());assertEquals("3",overlay.getProjection().label(SHIELD));}
	@Test public void noProgressPublishesBeforeDurabilityAcknowledgment()throws Exception{source.quantity(2);source.observer.tick();controller.tick();assertEquals("0",overlay.getProjection().label(SHIELD));assertEquals(0,saved().getFinancial().getGainedGp());worker.runAll();assertEquals("1",overlay.getProjection().label(SHIELD));assertEquals(100,saved().getFinancial().getGainedGp());}
	@Test public void failedSavePreservesFinancialAnchorThenRetryCreditsExactlyOnce()throws Exception
	{
		controller.stop();AtomicBoolean fail=new AtomicBoolean();worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);},prices);controller.start(()->{if(fail.get()){throw new java.io.IOException("Synthetic storage failure");}return root;},worker);worker.runAll();settle();BankRun before=saved();fail.set(true);change(2);assertTrue(view().isError());assertEquals(before,saved());fail.set(false);controller.retry();worker.runAll();settle();assertEquals(100,saved().getFinancial().getGainedGp());assertEquals("1",overlay.getProjection().label(SHIELD));
	}
	@Test public void explicitExpenseIsDurableWithoutChangingBankBalances()throws Exception{change(2);Map<Integer,Long> before=saved().getBalances();controller.recordExpense(view().getRun().getRevision(),300,"Verified death fee");worker.runAll();assertEquals(300,saved().getFinancial().getExpenseGp());assertEquals(before,saved().getBalances());assertEquals("1",overlay.getProjection().label(SHIELD));}
	@Test public void staleExpenseIntentCannotOverwriteNewerProgress()throws Exception{UUID old=view().getRun().getRevision();change(2);controller.recordExpense(old,300,"Fee");worker.runAll();assertEquals(0,saved().getFinancial().getExpenseGp());assertEquals(100,saved().getFinancial().getGainedGp());}
	@Test public void characterSwitchDoesNotMixFinancialState()throws Exception{change(2);source.character=SnapshotFixtures.CHARACTER_B;source.observer.reset();controller.tick();worker.runAll();settle();assertNull(view().getRun());assertNull(overlay.getProjection());assertEquals(100,repository.load(root,SnapshotFixtures.CHARACTER_A).get().getFinancial().getGainedGp());}
	@Test public void dashboardShowsCommittedWealthAndManualCorrectionPurpose()throws Exception
	{
		change(3);BankView committed=view();SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(committed,controller);JPanel grid=(JPanel)BankPolishTest.all(panel).stream().filter(c->"statisticTiles".equals(c.getName())).findFirst().get();assertEquals(2,((java.awt.GridLayout)grid.getLayout()).getColumns());assertEquals(6,grid.getComponentCount());java.util.List<String> labels=new ArrayList<>();for(java.awt.Component c:BankPolishTest.all(panel)){if(c instanceof JLabel){labels.add(((JLabel)c).getText());}}String text=String.join(" ",labels);assertTrue(text.contains("200 GP"));assertTrue(text.contains("Bank GP"));assertTrue(text.contains("0 GP"));assertFalse(text.contains("Coming soon"));assertTrue(text.contains("Manual<br>Bank<br>Corrections"));assertTrue(text.contains("Fix an incorrect"));assertEquals("Correct an incorrectly recorded virtual balance.",BankCorrectionDialog.PURPOSE);});
	}
	@Test public void pricedSubtotalFitsNarrowTilesAndExplainsUnknowns()throws Exception
	{
		change(2);BankView committed=view();BankValuation partial=new BankValuation(committed.getRun().getRevision(),-1_740_000L,3,1,SnapshotFixtures.TIME,1);BankView display=new BankView(committed.getRun(),committed.getObservation(),committed.getNames(),false,false,committed.getMessage(),null,partial);
		SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(display,controller);panel.setSize(225,960);layout(panel);JLabel subtotal=(JLabel)BankPolishTest.all(panel).stream().filter(c->c instanceof JLabel&&((JLabel)c).getText()!=null&&((JLabel)c).getText().contains("Priced subtotal")).findFirst().get();assertTrue(BankPolishTest.all(panel).stream().anyMatch(c->c instanceof JLabel&&"~-1740K GP*".equals(((JLabel)c).getText())));assertTrue(subtotal.getToolTipText().contains("3 item types have no available price"));assertTrue(subtotal.getToolTipText().contains("1 item type uses an older price"));assertTrue(subtotal.getPreferredSize().height<=subtotal.getHeight());assertTrue(subtotal.getPreferredSize().width<=subtotal.getWidth());try{java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,960,java.awt.image.BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();java.nio.file.Path file=java.nio.file.Paths.get("build/reports/dashboard-wealth.png");java.nio.file.Files.createDirectories(file.getParent());javax.imageio.ImageIO.write(image,"png",file.toFile());}catch(java.io.IOException ex){throw new AssertionError(ex);}});
	}
	private static void layout(java.awt.Container parent){parent.doLayout();for(java.awt.Component c:parent.getComponents()){if(c instanceof java.awt.Container){layout((java.awt.Container)c);}}}
}
