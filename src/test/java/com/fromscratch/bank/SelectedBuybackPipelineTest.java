package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.persistence.*;
import com.fromscratch.overlay.BankOverlayState;
import java.util.*;
import javax.swing.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real chooser -> confirmation -> controller -> repository -> renderer, with isolated synthetic saves. */
public class SelectedBuybackPipelineTest
{
	@Rule public TemporaryFolder temp=new TemporaryFolder();
	private static final int MASK=ItemID.HARMLESS_BLACK_MASK,HELM=ItemID.SLAYER_HELM_I,PURPLE=ItemID.SLAYER_HELM_I_PURPLE,BODY=ItemID.ELITE_BLACK_PLATEBODY;
	private final Source source=new Source();private final List<BankView> views=new ArrayList<>();private final BankOverlayState overlay=new BankOverlayState();
	private Worker worker;private BankController controller;private Filepath root;private BankRepository repository;private boolean failStorage;
	@Before public void setup()throws Exception{root=Filepath.Unchecked.getRooted(temp.getRoot().toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());source.items=items(0,3,1);overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();source.items=items(6000000,3,1);source.observer.changed();settle();}
	@After public void stop(){controller.stop();}
	private Item[] items(int gp,int masks,int helmets){return new Item[]{new Item(ItemID.COINS,gp),new Item(MASK,masks),new Item(HELM,helmets),new Item(PURPLE,1),new Item(BODY,2),new Item(ItemID.ELITE_BLACK_FULL_HELM,1),new Item(ItemID.ELITE_BLACK_PLATELEGS,1)};}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);});controller.start(()->{if(failStorage){throw new java.io.IOException("Synthetic storage failure");}return root;},worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private BankView view(){return views.get(views.size()-1);}
	private BankRun saved()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	private void buy(int id){controller.buySelected(view().getRun().getRevision(),SelectedBuybacks.catalog(id),id,UUID.randomUUID());worker.runAll();}
	private JButton tile(SelectedBuybackView v,int id){return (JButton)BankPolishTest.all(v).stream().filter(c->("selected-copy-"+id).equals(c.getName())).findFirst().get();}
	@Test public void chooserSelectsExactVariantAndFinalConfirmationCommitsOnlyOnce()throws Exception
	{
		BankRun before=saved();List<Integer> choices=new ArrayList<>();SwingUtilities.invokeAndWait(()->{
			SelectedBuybackView v=new SelectedBuybackView(view(),BuybackCatalog.entry("slayer_gear"),new BankIcons(id->null),choices::add);assertFalse(v.getContinueButton().isEnabled());assertEquals(3,BankPolishTest.all(v).stream().filter(c->c instanceof JButton&&c.getName()!=null&&c.getName().startsWith("selected-copy-")).count());
			tile(v,MASK).doClick();tile(v,PURPLE).doClick();assertEquals(PURPLE,v.selected());assertTrue(choices.isEmpty());v.setSize(420,350);v.doLayout();v.setSize(1000,600);v.doLayout();v.getContinueButton().doClick();v.getContinueButton().doClick();assertEquals(Collections.singletonList(PURPLE),choices);
			BuybackDialog.SelectedConfirmation confirmation=new BuybackDialog.SelectedConfirmation(before,BuybackCatalog.entry("slayer_gear"),PURPLE,new BankIcons(id->null),id->controller.buySelected(before.getRevision(),"slayer_gear",id,UUID.randomUUID()));confirmation.getConfirmButton().doClick();confirmation.getConfirmButton().doClick();
		});assertEquals(before,saved());worker.runAll();assertEquals(1000000,saved().getBuybacks().getCommittedGp());assertEquals("1",overlay.getProjection().label(PURPLE));assertEquals("0",overlay.getProjection().label(MASK));assertEquals("0",overlay.getProjection().label(HELM));
	}
	@Test public void cancelSelectionAndConfirmationDoNotPurchase()throws Exception{BankRun before=saved();SwingUtilities.invokeAndWait(()->{SelectedBuybackView v=new SelectedBuybackView(view(),BuybackCatalog.entry("slayer_gear"),new BankIcons(id->null),id->fail("Selection purchased"));tile(v,MASK).doClick();new BuybackDialog.SelectedConfirmation(before,BuybackCatalog.entry("slayer_gear"),MASK,new BankIcons(id->null),id->fail("Unconfirmed purchase"));});worker.runAll();assertEquals(before,saved());}
	@Test public void repeatedCopiesAreIndividuallyPaidAndProjected()throws Exception{buy(MASK);buy(MASK);assertEquals("2",overlay.getProjection().label(MASK));assertEquals(2000000,saved().getBuybacks().getCommittedGp());assertEquals(1,BuybackEngine.remaining(saved(),MASK));assertEquals("0",overlay.getProjection().label(HELM));}
	@Test public void samePurchaseIdAndStaleRevisionCannotDoubleCharge()throws Exception{BankRun before=saved();UUID id=UUID.randomUUID();controller.buySelected(before.getRevision(),"slayer_gear",MASK,id);controller.buySelected(before.getRevision(),"slayer_gear",MASK,id);worker.runAll();controller.buySelected(before.getRevision(),"slayer_gear",MASK,id);worker.runAll();assertEquals(1,saved().getBuybacks().getPurchases().size());controller.buySelected(saved().getRevision(),"slayer_gear",MASK,id);worker.runAll();assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void changedBankRejectsConfirmation()throws Exception{BankRun before=saved();source.items=items(6000000,2,1);source.observer.changed();source.observer.tick();controller.buySelected(before.getRevision(),"slayer_gear",MASK,UUID.randomUUID());worker.runAll();assertEquals(before,saved());}
	@Test public void differentCharacterRejectsOldEditor()throws Exception{BankRun before=saved();source.character=SnapshotFixtures.CHARACTER_B;controller.buySelected(before.getRevision(),"slayer_gear",MASK,UUID.randomUUID());worker.runAll();assertEquals(before,saved());}
	@Test public void unavailableDefinitionAndUnsupportedIdRejectWithoutPayment()throws Exception{BankRun before=saved();source.buybackDefinitions=false;buy(MASK);assertEquals(before,saved());source.buybackDefinitions=true;controller.buySelected(before.getRevision(),"slayer_gear",ItemID.WOODEN_SHIELD,UUID.randomUUID());worker.runAll();assertEquals(before,saved());}
	@Test public void saveFailureDoesNotPublishPaidOwnershipAndRetryChargesOnce()throws Exception{BankRun before=saved();failStorage=true;buy(MASK);assertEquals(before,saved());assertEquals("0",overlay.getProjection().label(MASK));controller.stop();failStorage=false;source.observer.reset();launch();settle();buy(MASK);assertEquals(1000000,saved().getBuybacks().getCommittedGp());}
	@Test public void restartRestoresTwoSelectedCopiesAndOtherVariantsStayLocked()throws Exception{buy(MASK);buy(MASK);BankRun before=saved();controller.stop();source.observer.reset();launch();settle();assertEquals(before.getBuybacks(),saved().getBuybacks());assertEquals("2",overlay.getProjection().label(MASK));assertEquals("0",overlay.getProjection().label(HELM));}
	@Test public void withdrawReturnAndOfflineNetChangesDoNotRefillOrDuplicate()throws Exception{buy(MASK);source.items=items(6000000,2,1);source.observer.changed();settle();assertEquals("0",overlay.getProjection().label(MASK));assertEquals(0,BuybackEngine.remaining(saved(),MASK));source.items=items(6000000,3,1);source.observer.changed();settle();assertEquals("1",overlay.getProjection().label(MASK));controller.stop();source.items=items(6000000,4,1);source.observer.reset();launch();settle();assertEquals("2",overlay.getProjection().label(MASK));assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void earnedAndPaidCopiesRemainSeparateFromLegacyCopies()throws Exception{source.items=items(6000000,4,1);source.observer.changed();settle();buy(MASK);assertEquals("2",overlay.getProjection().label(MASK));assertEquals(2,BuybackEngine.remaining(saved(),MASK));}
	@Test public void existingNegativeBalanceAnchorsOneCopyAndReturnsCannotMultiplyIt()throws Exception{source.items=items(6000000,1,1);source.observer.changed();settle();assertEquals(-2,saved().balance(MASK));buy(MASK);assertEquals("1",overlay.getProjection().label(MASK));source.items=items(6000000,3,1);source.observer.changed();settle();assertEquals("1",overlay.getProjection().label(MASK));assertEquals(1,saved().getBuybacks().getPurchases().size());}
	@Test public void eliteBlackDuplicatePiecesRequireSeparateThirtyThousandPayments()throws Exception{buy(BODY);assertEquals(30000,saved().getBuybacks().getCommittedGp());assertEquals("1",overlay.getProjection().label(BODY));buy(BODY);assertEquals(60000,saved().getBuybacks().getCommittedGp());assertEquals("2",overlay.getProjection().label(BODY));buy(BODY);assertEquals(60000,saved().getBuybacks().getCommittedGp());}
	@Test public void fullEliteBlackSetPurchasesAreIndependentAndVoidPriceIsUnchanged()throws Exception{buy(ItemID.ELITE_BLACK_FULL_HELM);assertEquals("0",overlay.getProjection().label(BODY));buy(BODY);buy(ItemID.ELITE_BLACK_PLATELEGS);assertEquals(90000,saved().getBuybacks().getCommittedGp());assertEquals(3,saved().getBuybacks().getPurchases().size());assertEquals("1",overlay.getProjection().label(ItemID.ELITE_BLACK_FULL_HELM));assertEquals("1",overlay.getProjection().label(BODY));assertEquals("1",overlay.getProjection().label(ItemID.ELITE_BLACK_PLATELEGS));assertEquals(1800000,BuybackCatalog.entry("elite_void_top").getPrice());}
	@Test public void purchasedAndEarnedTilesAreUnavailableAndFinalConfirmationRequiresFunds()throws Exception{buy(PURPLE);SwingUtilities.invokeAndWait(()->{SelectedBuybackView v=new SelectedBuybackView(view(),BuybackCatalog.entry("slayer_gear"),new BankIcons(id->null),id->fail());assertFalse(tile(v,PURPLE).isEnabled());tile(v,PURPLE).doClick();assertEquals(-1,v.selected());BankRun poor=BuybackModelTest.funded(999999,MASK);BuybackDialog.SelectedConfirmation confirmation=new BuybackDialog.SelectedConfirmation(poor,BuybackCatalog.entry("slayer_gear"),MASK,new BankIcons(id->null),id->fail());assertFalse(confirmation.getConfirmButton().isEnabled());confirmation.getConfirmButton().doClick();});}
	@Test public void historyIdentifiesSelectedItemAndOneCopy()throws Exception{buy(PURPLE);SwingUtilities.invokeAndWait(()->{JTable t=(JTable)BankPolishTest.all(BuybackDialog.history(view().getRun(),new BankIcons(id->null))).stream().filter(c->c instanceof JTable).findFirst().get();assertEquals("Purple slayer helmet (i)",t.getValueAt(0,1));assertEquals("1",t.getValueAt(0,3));});}
}
