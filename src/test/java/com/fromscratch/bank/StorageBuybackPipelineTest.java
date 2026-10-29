package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.persistence.*;
import com.fromscratch.overlay.BankOverlayState;
import java.time.Instant;
import java.util.*;
import javax.swing.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real unchecked confirmations -> controller -> atomic repository -> acknowledged projection. */
public class StorageBuybackPipelineTest
{
	@Rule public TemporaryFolder temp=new TemporaryFolder();
	private static final String[] CATALOGS={"fish_barrel","herb_sack","plank_sack","seed_box","tackle_box"};
	private final Source source=new Source();private final List<BankView> views=new ArrayList<>();private final BankOverlayState overlay=new BankOverlayState();
	private Worker worker;private BankController controller;private Filepath root;private BankRepository repository;private boolean failStorage;
	@Before public void setup()throws Exception
	{
		root=Filepath.Unchecked.getRooted(temp.newFolder("store").toPath());repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		source.items=items(0);overlay.enable();launch();settle();controller.startRun(true,null,true);worker.runAll();source.items=items(30000000);source.observer.changed();settle();
	}
	@After public void stop(){controller.stop();}
	private Item[] items(int gp){List<Item> q=new ArrayList<>();q.add(new Item(ItemID.COINS,gp));for(String catalog:CATALOGS){q.add(new Item(BuybackCatalog.entry(catalog).getIcon(),2));}return q.toArray(new Item[0]);}
	private void launch(){worker=new Worker();controller=new BankController(source,repository,Runnable::run,v->{views.add(v);overlay.updateBank(v);});controller.start(()->{if(failStorage){throw new java.io.IOException("Synthetic storage failure");}return root;},worker);worker.runAll();}
	private void settle(){source.observer.tick();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();}
	private BankRun saved()throws Exception{return repository.load(root,SnapshotFixtures.CHARACTER_A).get();}
	private BankView view(){return views.get(views.size()-1);}
	private EmptyStorageConfirmation declaration(String catalog){return new EmptyStorageConfirmation(catalog,view().getRun().getRevision(),Instant.now(),true,true);}
	private void buy(String catalog){controller.buyEmptyStorage(view().getRun().getRevision(),catalog,UUID.randomUUID(),declaration(catalog));worker.runAll();}
	private JCheckBox check(JComponent c,String name){return (JCheckBox)BankPolishTest.all(c).stream().filter(w->name.equals(w.getName())).findFirst().get();}
	@Test public void everyStoragePurchaseChargesExactPriceAndOnlyOneCopyAfterAcknowledgment()throws Exception
	{
		long spent=0;long[] prices={4000000,2500000,1500000,2500000,4000000};
		for(int i=0;i<CATALOGS.length;i++)
		{
			String catalog=CATALOGS[i];BankRun before=saved();int id=BuybackCatalog.entry(catalog).getIcon();assertEquals(prices[i],BuybackCatalog.entry(catalog).getPrice());
			controller.buyEmptyStorage(before.getRevision(),catalog,UUID.randomUUID(),declaration(catalog));assertEquals(before,saved());assertEquals("0",overlay.getProjection().label(id));worker.runAll();spent+=prices[i];
			assertEquals(spent,saved().getBuybacks().getCommittedGp());assertEquals("1",overlay.getProjection().label(id));assertEquals(2,saved().physical(id));assertEquals(before.getBalances(),saved().getBalances());assertEquals(before.getPhysical(),saved().getPhysical());assertEquals(before.getInitialBank(),saved().getInitialBank());assertTrue(saved().getBuybacks().getPurchases().get(i).isStorageConfirmedEmpty());
			buy(catalog);assertEquals(spent,saved().getBuybacks().getCommittedGp());
		}
	}
	@Test public void bothUncheckedDeclarationsRequiredAndCancelSendsNothing()throws Exception
	{
		BankRun before=saved();List<EmptyStorageConfirmation> accepted=new ArrayList<>();SwingUtilities.invokeAndWait(()->{
			BuybackDialog.StorageConfirmation c=new BuybackDialog.StorageConfirmation(before,BuybackCatalog.entry("fish_barrel"),new BankIcons(id->null),accepted::add);
			assertFalse(c.getConfirmButton().isEnabled());assertFalse(check(c,"storageEmptyAttestation").isSelected());assertFalse(check(c,"storageLegacyAccountedAttestation").isSelected());
			check(c,"storageEmptyAttestation").doClick();assertFalse(c.getConfirmButton().isEnabled());check(c,"storageLegacyAccountedAttestation").doClick();assertTrue(c.getConfirmButton().isEnabled());assertTrue(accepted.isEmpty());
			c.getConfirmButton().doClick();c.getConfirmButton().doClick();assertEquals(1,accepted.size());
		});assertEquals(before,saved());
	}
	@Test public void missingOrStaleOrWrongRevisionDeclarationCannotSpend()throws Exception
	{
		BankRun before=saved();for(EmptyStorageConfirmation proof:Arrays.asList(null,new EmptyStorageConfirmation("fish_barrel",before.getRevision(),Instant.now().minusSeconds(121),true,true),new EmptyStorageConfirmation("fish_barrel",UUID.randomUUID(),Instant.now(),true,true),declaration("seed_box")))
		{controller.buyEmptyStorage(before.getRevision(),"fish_barrel",UUID.randomUUID(),proof);worker.runAll();assertEquals(before,saved());}
	}
	@Test public void nonemptyEvidenceOverridesDeclarationForPartialAndFullStores()throws Exception{source.storageHasContents=true;BankRun before=saved();for(String catalog:CATALOGS){buy(catalog);assertEquals(before,saved());assertTrue(view().getMessage().contains("Native data shows contents"));}}
	@Test public void ordinaryBuybackCannotBypassContentsConfirmation()throws Exception{BankRun before=saved();for(String catalog:CATALOGS){controller.buyback(before.getRevision(),catalog,UUID.randomUUID());worker.runAll();assertEquals(before,saved());}}
	@Test public void insufficientFundsMissingItemAndUnverifiedDefinitionsCannotSpend()throws Exception
	{
		BankRun before=saved();source.buybackDefinitions=false;buy("fish_barrel");assertEquals(before,saved());source.buybackDefinitions=true;
		source.items=new Item[]{new Item(ItemID.COINS,30000000)};source.observer.changed();settle();before=saved();buy("fish_barrel");assertEquals(before,saved());
		source.items=items(0);source.observer.changed();settle();before=saved();buy("fish_barrel");assertEquals(before,saved());
	}
	@Test public void failedSaveDoesNotDebitOrPublishAndRetryChargesOnce()throws Exception{BankRun before=saved();failStorage=true;buy("fish_barrel");assertEquals(before,saved());assertEquals(before,view().getRun());controller.stop();failStorage=false;source.observer.reset();launch();settle();buy("fish_barrel");assertEquals(4000000,saved().getBuybacks().getCommittedGp());}
	@Test public void duplicatedIntentStaleEditorAndChangedCharacterCannotDoubleCharge()throws Exception{BankRun before=saved();UUID id=UUID.randomUUID();EmptyStorageConfirmation proof=declaration("fish_barrel");controller.buyEmptyStorage(before.getRevision(),"fish_barrel",id,proof);controller.buyEmptyStorage(before.getRevision(),"fish_barrel",id,proof);worker.runAll();BankRun paid=saved();controller.buyEmptyStorage(before.getRevision(),"fish_barrel",id,proof);worker.runAll();assertEquals(paid,saved());source.character=SnapshotFixtures.CHARACTER_B;controller.buyEmptyStorage(paid.getRevision(),"seed_box",UUID.randomUUID(),declaration("seed_box"));worker.runAll();assertEquals(paid,saved());}
	@Test public void paidStorageSurvivesRestartAndExportImport()throws Exception
	{
		buy("herb_sack");BankRun paid=saved();Filepath export=Filepath.Unchecked.getRooted(temp.newFolder("exports").toPath()).joinSegment("synthetic.fsp");repository.backups().exportProgress(root,paid,export);
		controller.stop();source.observer.reset();launch();settle();assertEquals(paid.getBuybacks(),saved().getBuybacks());
		ProgressBackup.Candidate candidate=repository.backups().preview(root,paid.getCharacter(),export);assertEquals(paid,candidate.getRun());repository.backups().restore(root,candidate);assertEquals(paid,saved());
	}
	@Test public void paidHistoryDisclosesDeclaration()throws Exception{buy("plank_sack");SwingUtilities.invokeAndWait(()->{JTable table=(JTable)BankPolishTest.all(BuybackDialog.history(view().getRun(),new BankIcons(id->null))).stream().filter(c->c instanceof JTable).findFirst().get();assertEquals("Player confirmed empty; legacy contents accounted",table.getValueAt(0,5));});}
}
