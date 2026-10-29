package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.fromscratch.model.ContainerSnapshot;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Real repository publication and portable archives, using only disposable synthetic progression. */
public class ExpandedBuybackPersistenceTest
{
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private final BankRunCodec codec=new BankRunCodec(SnapshotFixtures.gson());
 private final BankRepository repository=new BankRepository(codec,SnapshotFixtures.codec());
 private Filepath root(String name)throws Exception{return Filepath.Unchecked.getRooted(temp.newFolder(name).toPath());}
 private BankRun funded(Filepath root,long gp,int...legacy)throws Exception
 {BankRun start=repository.save(root,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BuybackModelTest.bank(legacy),true,UUID.randomUUID(),SnapshotFixtures.TIME));Map<Integer,Long> q=new TreeMap<>(start.getPhysical());q.put(ItemID.COINS,gp);return repository.save(root,start,BankEngine.observe(start,new ContainerSnapshot(q,q.size()),UUID.randomUUID(),SnapshotFixtures.TIME));}
 @Test public void everyNewBuybackSurvivesFailedSaveRetryRestartAndExportImport()throws Exception
 {
  BankRepository interrupted=new BankRepository(codec,SnapshotFixtures.codec(),(a,b)->{throw new IOException("Synthetic interrupted atomic publication");});
  for(Map.Entry<String,Long> entry:ExpandedBuybacksTest.PRICES.entrySet())
  {
   String id=entry.getKey();Filepath root=root(id);BankRun before=funded(root,entry.getValue(),ExpandedBuybacksTest.pieces(BuybackCatalog.entry(id))),after=ExpandedBuybacksTest.purchase(before,id);
   try{interrupted.save(root,before,after);fail(id);}catch(SaveException expected){}assertEquals(id,before,repository.load(root,before.getCharacter()).get());assertEquals(0,repository.load(root,before.getCharacter()).get().getBuybacks().getCommittedGp());
   assertEquals(after,repository.save(root,before,after));assertEquals(id,after,repository.load(root,after.getCharacter()).get());assertEquals(after,repository.save(root,before,after));
   Filepath backup=Filepath.Unchecked.getRooted(temp.getRoot().toPath().resolve(id+".fsp"));repository.backups().exportProgress(root,after,backup);
   BankRun changed=repository.save(root,after,BankEngine.correct(after,ItemID.COINS,entry.getValue()-1,UUID.randomUUID(),SnapshotFixtures.TIME));assertNotEquals(after,changed);
   ProgressBackup.Candidate preview=repository.backups().preview(root,after.getCharacter(),backup);assertEquals(after,preview.getRun());repository.backups().restore(root,preview);assertEquals(id,after,repository.load(root,after.getCharacter()).get());
   assertEquals(entry.getValue().longValue(),after.getBuybacks().getCommittedGp());assertEquals(after.getInitialBank(),repository.load(root,after.getCharacter()).get().getInitialBank());
  }
 }
 @Test public void historical107PurchasesLoadAsFormatTenWithoutRewritingTheCheckpoint()throws Exception
 {
  Filepath root=root("historical107");BankRun run=funded(root,3000000,ItemID.TZHAAR_CAPE_FIRE,ItemID.GRACEFUL_HOOD,ItemID.GRACEFUL_CAPE,ItemID.GRACEFUL_TOP,ItemID.GRACEFUL_LEGS,ItemID.GRACEFUL_GLOVES,ItemID.GRACEFUL_BOOTS);run=repository.save(root,run,BuybackModelTest.buy(run,"fire_cape"));run=repository.save(root,run,BuybackModelTest.buy(run,"graceful"));
  Filepath directory=root.join("characters",run.getCharacter()),current=directory.joinSegment("current.json"),ledger=directory.joinSegment("bank-ledger-"+run.getRevision()+".json");JsonObject payload=codec.payload(BankStorageFiles.readText(current),BankRunCodec.FORMAT);String old=codec.envelope(payload,10);current.write(old);JsonObject audit=codec.payload(BankStorageFiles.readText(ledger),1);audit.addProperty("result",SaveIntegrity.sha256(old));ledger.write(codec.envelope(audit,1));
  assertEquals(run,repository.load(root,run.getCharacter()).get());assertEquals(old,BankStorageFiles.readText(current));assertEquals(2000000,repository.load(root,run.getCharacter()).get().getBuybacks().getCommittedGp());
 }
 @Test public void historical108SelectedReceiptsRemainUnchangedWhenNewPurchasesAreAdded()throws Exception
 {
  Filepath root=root("historical108");BankRun run=funded(root,100000000,ItemID.HARMLESS_BLACK_MASK,ItemID.SLAYER_HELM_I,ItemID.ELITE_BLACK_FULL_HELM,ItemID.TZHAAR_CAPE_FIRE,ItemID.EMBERLIGHT);
  for(int id:new int[]{ItemID.HARMLESS_BLACK_MASK,ItemID.SLAYER_HELM_I,ItemID.ELITE_BLACK_FULL_HELM}){run=repository.save(root,run,BankEngine.buySelected(run,SelectedBuybacks.catalog(id),id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME));}run=repository.save(root,run,BuybackModelTest.buy(run,"fire_cape"));
  List<JsonObject> historical=new ArrayList<>();run.getBuybacks().getPurchases().forEach(p->historical.add(BuybackCodec.purchase(p)));BankRun after=repository.save(root,run,ExpandedBuybacksTest.purchase(run,"emberlight"));BankRun loaded=repository.load(root,run.getCharacter()).get();assertEquals(after,loaded);for(int i=0;i<historical.size();i++){assertEquals(historical.get(i),BuybackCodec.purchase(loaded.getBuybacks().getPurchases().get(i)));}assertEquals(48030000,loaded.getBuybacks().getCommittedGp());assertEquals(1L,(long)BuybackEngine.balances(loaded).get(ItemID.HARMLESS_BLACK_MASK));assertEquals(1L,(long)BuybackEngine.balances(loaded).get(ItemID.ELITE_BLACK_FULL_HELM));
 }
}
