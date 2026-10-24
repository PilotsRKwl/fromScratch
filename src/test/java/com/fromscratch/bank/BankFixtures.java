package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.ContainerSnapshot;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;

/** Only synthetic data; no account, client UI, reflection or credentials. */
public final class BankFixtures
{
	public static final int SHIELD = ItemID.WOODEN_SHIELD;
	public static final int OTHER = ItemID.COINS; // exact-ID independence, not GP accounting
	public static ContainerSnapshot bank(long shields) { return BankItems.aggregate(shields == 0 ? new Item[0] : new Item[]{new Item(SHIELD, Math.toIntExact(shields))}, id -> false); }
	public static BankRun run(long shields) { return BankEngine.start(SnapshotFixtures.CHARACTER_A, null, bank(shields), true, UUID.randomUUID(), SnapshotFixtures.TIME); }
	public static final class Source implements BankSource, BankObserver.Access
	{
		public String character = SnapshotFixtures.CHARACTER_A;
		public boolean open = true;
		public boolean pin;
		public boolean buybackDefinitions=true;
		public com.fromscratch.potion.PotionObservation storage=com.fromscratch.potion.PotionObservation.unavailableStore();
		public com.fromscratch.potion.PotionObservation potions(){return storage;}
		public boolean fullBank=true,storageOpen,captureValid=true;
		public boolean fullBankView(){return fullBank;}
		public boolean potionStorageOpen(){return storageOpen;}
		public boolean potionStorageUnlocked(){return !storage.getTypes().isEmpty();}
		public boolean unlockKnown=true;
		public boolean potionStorageUnlockKnown(){return unlockKnown;}
		public boolean validPotionCapture(com.fromscratch.potion.PotionObservation captured){return captureValid&&(!storage.isVerified()||captured.equals(storage));}
		public com.fromscratch.buyback.LootingBagReader.Access bagAccess; public UUID bagSession;
		public com.fromscratch.buyback.LootingBagReader.Session bagInspection;
		public long bagTick;
		public com.fromscratch.buyback.BagObservation bag(){BankObservation bank=observer.observe();return bagInspection!=null?bagInspection.read(character,bank,bagTick):bagAccess==null?com.fromscratch.buyback.BagObservation.unknown():new com.fromscratch.buyback.LootingBagReader(bagAccess).read(bank,bagSession);}
		public com.fromscratch.pouch.PouchObservation pouchProof=com.fromscratch.pouch.PouchObservation.unknown();
		public com.fromscratch.pouch.RuneLitePouchReader.Access pouchAccess;
		public com.fromscratch.pouch.PouchObservation pouch(){return pouchAccess==null?pouchProof:new com.fromscratch.pouch.RuneLitePouchReader(pouchAccess).banked(observer.observe());}
		public Item[] items = new Item[]{new Item(SHIELD, 1)};
		public final Set<Integer> placeholders = new HashSet<>();
		public final BankObserver observer = new BankObserver(this);
		public boolean normalBankOpen() { return open && !pin; }
		public ContainerSnapshot readBank() { return BankItems.aggregate(items, placeholders::contains); }
		public String characterKey() { return character; }
		public boolean buybackDefinition(int id){return buybackDefinitions;}
		public BankObservation observe() { return observer.observe(); }
		public String itemName(int id) { return id == SHIELD ? "Wooden shield" : id == OTHER ? "Coins" : "Variant #" + id; }
		public void quantity(long n) { items = n == 0 ? new Item[0] : new Item[]{new Item(SHIELD, Math.toIntExact(n))}; observer.changed(); }
		public void open(boolean value) { open = value; observer.changed(); }
	}
	public static final class Worker extends AbstractExecutorService
	{
		public final Queue<Runnable> tasks = new ArrayDeque<>(); private boolean stopped;
		public void runAll() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
		public void execute(Runnable task) { if (stopped) { throw new RejectedExecutionException(); } tasks.add(task); }
		public void shutdown() { stopped = true; }
		public List<Runnable> shutdownNow() { stopped = true; List<Runnable> abandoned = new ArrayList<>(tasks); tasks.clear(); return abandoned; }
		public boolean isShutdown() { return stopped; } public boolean isTerminated() { return stopped; }
		public boolean awaitTermination(long time, TimeUnit unit) { return stopped; }
	}
}
