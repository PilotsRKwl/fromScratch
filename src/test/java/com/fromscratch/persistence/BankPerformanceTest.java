package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.runelite.api.Item;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Synthetic benchmark distinguishes client capture, worker CPU/IO and EDT refresh; never launches the game. */
public class BankPerformanceTest
{
	@Rule public TemporaryFolder temp = new TemporaryFolder();
	private BankRunCodec codec() { return new BankRunCodec(SnapshotFixtures.gson()); }
	private Filepath root() { return Filepath.Unchecked.getRooted(temp.getRoot().toPath()); }
	private static ContainerSnapshot bank(int first)
	{
		Item[] items = new Item[1200]; for (int i = 0; i < items.length; i++) { items[i] = new Item(100000 + i, i == 0 ? first : 1); } return BankItems.aggregate(items, id -> false);
	}
	@Test public void resetLargeHistoryReportsDurationAndKeepsRecovery() throws Exception
	{
		for(int entries:new int[]{8,240,1200}){resetHistory(entries);}
	}
	private void resetHistory(int entries) throws Exception
	{
		Filepath benchmark=root().joinSegment("reset-"+entries);
		BankRepository repository=new BankRepository(codec(),SnapshotFixtures.codec());UUID session=UUID.randomUUID();BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,bank(1),true,session,SnapshotFixtures.TIME);repository.save(benchmark,null,run);
		for(int i=0;i<entries;i++){BankRun next=BankEngine.observe(run,bank(i+2),session,SnapshotFixtures.TIME.plusSeconds(i));repository.save(benchmark,run,next);run=next;}
		Filepath d=benchmark.join("characters",run.getCharacter());long files,bytes=0;try(java.util.stream.Stream<Filepath> walk=d.walk(1)){java.util.List<Filepath> all=walk.filter(f->f.getFileName().endsWith(".json")).collect(java.util.stream.Collectors.toList());files=all.size();for(Filepath f:all){bytes+=f.size();}}
		long started=System.nanoTime();BankRun candidate=BankEngine.reset(run,UUID.randomUUID(),SnapshotFixtures.TIME);long prepared=System.nanoTime();BankRun saved=repository.save(benchmark,run,candidate);long finished=System.nanoTime();assertFalse(saved.isActive());assertTrue(saved.getBalances().isEmpty());assertTrue(saved.getPotions().getTypes().isEmpty());assertEquals(saved,repository.load(benchmark,run.getCharacter()).get());
		System.out.printf(Locale.ROOT,"RESET_BENCH entries=%d bank_types=1200 json_files=%d bytes=%d prepare_ms=%.3f durable_ms=%.3f total_ms=%.3f%n",entries,files,bytes,(prepared-started)/1e6,(finished-prepared)/1e6,(finished-started)/1e6);
	}
	@Test public void fullBankAggregationAndLargeHistoryOrdinaryWritesAreBounded() throws Exception
	{
		long begin = System.nanoTime(); for (int i = 0; i < 100; i++) { assertEquals(1200, bank(1).getQuantities().size()); }
		double capture = (System.nanoTime() - begin) / 1e6;
		BankRepository repository = new BankRepository(codec(), SnapshotFixtures.codec()); UUID session = UUID.randomUUID();
		BankRun run = BankEngine.start(SnapshotFixtures.CHARACTER_A, null, bank(1), true, session, SnapshotFixtures.TIME); repository.save(root(), null, run);
		long cpu = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(); begin = System.nanoTime(); double early = 0, late = 0;
		for (int i = 0; i < 256; i++)
		{
			long start = System.nanoTime(); BankRun next = BankEngine.observe(run, bank(i + 2), session, SnapshotFixtures.TIME.plusSeconds(i)); repository.save(root(), run, next); run = next;
			if (i < 32) { early += (System.nanoTime() - start) / 1e6; } if (i >= 224) { late += (System.nanoTime() - start) / 1e6; }
		}
		double wall = (System.nanoTime() - begin) / 1e6, cpuMs = (ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpu) / 1e6;
		assertEquals(256, repository.load(root(), SnapshotFixtures.CHARACTER_A).get().balance(100000));
		BankHistory.Page page = repository.history(root(), run, null, false); assertEquals(25, page.getRows().size()); assertNotNull(page.getNext());
		System.out.printf(Locale.ROOT, "BANK_1200_ITEMS capture_100_ms=%.3f commits=256 worker_wall_ms=%.3f worker_cpu_ms=%.3f io_and_scheduling_ms=%.3f early_mean_ms=%.3f late_mean_ms=%.3f%n", capture, wall, cpuMs, Math.max(0, wall - cpuMs), early / 32, late / 32);
	}
	@Test public void rapidSnapshotsKeepOrderAndDiskWorkOffClientThread() throws Exception
	{
		long clientId = Thread.currentThread().getId(); BlockingQueue<Runnable> callbacks = new LinkedBlockingQueue<>(); List<String> diskThreads = new ArrayList<>();
		BankRepository repository = new BankRepository(codec(), SnapshotFixtures.codec(), (pending, current) -> { diskThreads.add(Thread.currentThread().getName()); pending.moveTo(current, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); });
		BankFixtures.Source fixture = new BankFixtures.Source(); fixture.items = new Item[1200]; for (int i=0;i<1200;i++){fixture.items[i]=new Item(100000+i,1);}
		BankSource source = new BankSource() { public String characterKey(){assertEquals(clientId,Thread.currentThread().getId());return fixture.characterKey();} public BankObservation observe(){assertEquals(clientId,Thread.currentThread().getId());return fixture.observe();} public String itemName(int id){assertEquals(clientId,Thread.currentThread().getId());return "Item "+id;} };
		List<BankView> views = new ArrayList<>(); ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r,"bank-test-storage")); BankController controller = new BankController(source,repository,callbacks::add,views::add);
		try
		{
			controller.start(() -> {assertNotEquals(clientId,Thread.currentThread().getId()); return root();},worker); pump(worker,callbacks,5); fixture.observer.tick(); controller.tick(); controller.startRun(true,null,true); pump(worker,callbacks,5); fixture.observer.tick(); controller.tick(); pump(worker,callbacks,5);
			long sequence = views.get(views.size()-1).getRun().getSequence();
			for(int i=0;i<100;i++){fixture.items[0]=new Item(100000,i<50?i+2:100-i);fixture.observer.changed();fixture.observer.tick();controller.tick();}
			pump(worker,callbacks,110); BankRun saved=repository.load(root(),SnapshotFixtures.CHARACTER_A).get(); assertEquals(0,saved.balance(100000)); assertEquals(sequence+100,saved.getSequence()); assertTrue(diskThreads.stream().allMatch("bank-test-storage"::equals));
		}
		finally{controller.stop();worker.shutdownNow();}
	}
	private static void pump(ExecutorService worker, BlockingQueue<Runnable> callbacks, int rounds) throws Exception
	{
		for(int i=0;i<rounds;i++){Runnable r;while((r=callbacks.poll())!=null){r.run();} CompletableFuture.runAsync(()->{},worker).get(20,TimeUnit.SECONDS);}
		Runnable r;while((r=callbacks.poll())!=null){r.run();}
	}
	@Test public void historyPagesSeparateAutomaticAndManualAndDoNotMutate() throws Exception
	{
		BankRepository repository=new BankRepository(codec(),SnapshotFixtures.codec());UUID session=UUID.randomUUID();BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,bank(1),true,session,SnapshotFixtures.TIME);repository.save(root(),null,run);
		for(int i=0;i<40;i++){BankRun next=BankEngine.observe(run,bank(i+2),session,SnapshotFixtures.TIME.plusSeconds(i));repository.save(root(),run,next);run=next;}
		BankRun next=BankEngine.correct(run,100000,-3,session,SnapshotFixtures.TIME.plusSeconds(50));repository.save(root(),run,next);run=next;
		BankHistory.Page first=repository.history(root(),run,null,false);BankHistory.Page second=repository.history(root(),run,first.getNext(),false);assertEquals(25,first.getRows().size());assertEquals(15,second.getRows().size());assertEquals(1,repository.history(root(),run,null,true).getRows().size());assertEquals(run,repository.load(root(),SnapshotFixtures.CHARACTER_A).get());
		Map<Integer,Long> changed=new TreeMap<>(run.getPhysical());changed.replaceAll((id,n)->n+1);
		next=BankEngine.observe(run,new ContainerSnapshot(changed,1200),session,SnapshotFixtures.TIME.plusSeconds(100));repository.save(root(),run,next);run=next;
		int rows=0,pages=0;BankHistory.Cursor cursor=null;
		do{BankHistory.Page page=repository.history(root(),run,cursor,false);assertTrue(page.getRows().size()<=25);rows+=page.getRows().size();cursor=page.getNext();assertTrue(++pages<=51);}while(cursor!=null);
		assertEquals(1240,rows);assertEquals(run,repository.load(root(),SnapshotFixtures.CHARACTER_A).get());
	}
}
