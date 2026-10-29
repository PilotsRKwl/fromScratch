package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.fromscratch.model.ContainerSnapshot;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Synthetic bounded buyback ledger plus 1,200-ID bank workload; never a live latency claim. */
public class BuybackPerformanceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	@Test public void fullCatalogDoesNotRequireFullJournalScansDuringBanking()throws Exception
	{
		Map<Integer,Long> physical=new TreeMap<>();for(int n=0;n<1200;n++){physical.put(100000+n,1L);}
		for(BuybackEntry entry:BuybackCatalog.entries()){if(SelectedBuybacks.selected(entry.getId())){physical.put(SelectedBuybacks.items(entry.getId()).iterator().next(),1L);}entry.getRoles().forEach((role,level)->physical.put(BuybackCatalog.variants(role).entrySet().stream().filter(e->e.getValue().equals(level)).findFirst().get().getKey(),1L));}
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());UUID session=UUID.randomUUID();
		BankRun run=repo.save(root,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(physical,physical.size()),true,session,Instant.now()));for(int n=0;n<1200;n++){physical.put(100000+n,2L);}physical.put(ItemID.COINS,1000000000L);run=repo.save(root,run,BankEngine.observe(run,new ContainerSnapshot(physical,physical.size()),session,Instant.now()));
		run=repo.save(root,run,BuybackModelTest.emptyPouch(run));
 // Exercise base purchases before upgrades independently of the player-facing alphabetical order.
 List<BuybackEntry> purchases=new ArrayList<>(BuybackCatalog.entries());purchases.sort(Comparator.comparingInt(e->e.getRoles().values().stream().mapToInt(Integer::intValue).max().orElse(1)));
 for(BuybackEntry entry:purchases){run=repo.save(root,run,SelectedBuybacks.selected(entry.getId())?BankEngine.buySelected(run,entry.getId(),SelectedBuybacks.items(entry.getId()).iterator().next(),UUID.randomUUID(),session,Instant.now()):ExpandedBuybacksTest.purchase(run,entry.getId()));}
		assertEquals(BuybackCatalog.entries().size(),run.getBuybacks().getPurchases().size());
		long wall=System.nanoTime(),cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int n=0;n<250;n++){assertTrue(BuybackEngine.balances(run).size()>=1200);}
		double projectionMs=(System.nanoTime()-wall)/1e6;wall=System.nanoTime();cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int n=0;n<64;n++){physical.put(100000,1L+(n%2==0?1:0));run=repo.save(root,run,BankEngine.observe(run,new ContainerSnapshot(physical,physical.size()),session,Instant.now()));}
		double wallMs=(System.nanoTime()-wall)/1e6,cpuMs=(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6;
		assertEquals(run,repo.load(root,run.getCharacter()).get());assertEquals(30,run.getRecent().size());assertEquals(0,run.balance(100000));
		System.out.printf(Locale.ROOT,"BUYBACK_1200_ITEMS purchases=%d projection_250_ms=%.3f commits=64 worker_wall_ms=%.3f worker_cpu_ms=%.3f io_scheduling_ms=%.3f%n",run.getBuybacks().getPurchases().size(),projectionMs,wallMs,cpuMs,wallMs-cpuMs);
	}
}
