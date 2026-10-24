package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.persistence.*;
import com.fromscratch.overlay.BankOverlayState;
import java.util.*;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Internal proof of the public BankTag predicate and native open/layout lifecycle, without any game actions. */
public class VirtualBankModeTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private final Access access=new Access();
	private final VirtualBankMode mode=new VirtualBankMode(access);
	private static final class Access implements VirtualBankMode.Access
	{
		Runnable onClose=()->{},onOpen=()->{},onRefresh=()->{};int definitions;boolean failRefresh,failRegister,refuseClose,ready=true;BankTag predicate;String active,remembered="",title="The Bank of Gielinor";int opened,closed,refresh,removed,nativeTab,closing;
		net.runelite.api.Item[] nativeSlots;int[] nativeSizes;
		final Set<String> savedTags=new HashSet<>();
		public void register(String name,BankTag tag) { predicate=tag;if(failRegister){throw new IllegalStateException("Synthetic tag registration failure");} }
		public void unregister(String name) { removed++;predicate=null; }
		public boolean ready(){return ready;}
		public void allItems(){nativeTab=0;}
		public int nativeTab(){return nativeTab;}public Set<Integer> nativeItems(){return nativeSlots==null?Collections.singleton(SHIELD):VirtualBankMode.tabItems(nativeTab,nativeSizes,nativeSlots);}
		public void open(String name) { active=name;remembered=name;title="Tag tab "+name;opened++;onOpen.run(); }
		public void close() { closed++;if(++closing>1){throw new AssertionError("Recursive native close");}try{onClose.run();if(!refuseClose){active=null;remembered="";title="The Bank of Gielinor";}}finally{closing--;} }
		public String active() { return active; }
		public String remembered(){return remembered;}public void remembered(String name){remembered=name;}
		public boolean savedTag(String name){return savedTags.contains(name);}
		public void refresh() { refresh++;onRefresh.run();if(failRefresh){throw new IllegalStateException("Synthetic layout failure");} }
		public int placeholder(int id){definitions++;return id==SHIELD?999999:-1;}
		public String title() { return title; } public void title(String text) { title=text; }
	}
	@Test public void dynamicPredicateFiltersPositiveAndRefreshesOnlyCommittedRevisions()
	{
		BankRun run=run(1);mode.setEnabled(true);mode.update(run,true);assertFalse(access.predicate.contains(SHIELD));
		run=BankEngine.observe(run,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME);mode.update(run,true);assertTrue(access.predicate.contains(SHIELD));assertEquals(1,access.refresh);
		mode.update(run,true);assertEquals(1,access.refresh);run=BankEngine.observe(run,bank(0),UUID.randomUUID(),SnapshotFixtures.TIME);mode.update(run,true);assertFalse(access.predicate.contains(SHIELD));
	}
	@Test public void normalViewClosingAndOtherUserTagsArePreserved()
	{
		List<Boolean> selections=new ArrayList<>();mode.fullBankSelected(()->selections.add(false));
		BankRun run=run(1);access.active="My saved tag";mode.setEnabled(false);mode.update(run,true);assertEquals("My saved tag",access.active);assertEquals(0,access.opened);assertTrue(selections.isEmpty());assertEquals(0,access.closed);assertEquals(0,access.removed);
		access.active=null;mode.setEnabled(true);mode.update(run,true);assertEquals(1,access.opened);mode.setEnabled(false);assertNull(access.active);assertEquals(1,access.closed);
		access.active="My saved tag";mode.setEnabled(true);mode.update(run,true);assertTrue(access.active.startsWith("from_scratch_virtual_"));assertEquals(2,access.opened);assertTrue(selections.isEmpty());
		access.active="Another user tag";mode.viewChanged();assertEquals("Another user tag",access.active);assertEquals(Collections.singletonList(false),selections);assertEquals(1,access.closed);
	}
	@Test public void settingDefaultsOnAndClosingReopeningAndMissingBankTagsAreSafe()
	{
		assertTrue(new com.fromscratch.FromScratchConfig(){}.virtualBankView());BankRun run=run(1);mode.update(null,true);assertNull(access.predicate);assertEquals(0,access.opened);
		mode.setEnabled(true);mode.update(run,true,new BankObservation(UUID.randomUUID(),0,SnapshotFixtures.TIME,null,"Waiting"));assertEquals(0,access.opened);assertNull(access.predicate);
		access.ready=false;mode.update(run,true);assertEquals(0,access.opened);access.ready=true;mode.update(run,true);assertNotNull(access.predicate);assertFalse(access.predicate.contains(SHIELD));mode.bankClosed();assertNull(access.active);assertNull(access.predicate);
		mode.update(run,true);assertNotNull(access.active);assertNotNull(access.predicate);mode.update(run,true);assertEquals(2,access.opened);
		mode.stop();mode.stop();assertEquals(2,access.removed);
		VirtualBankMode missing=new VirtualBankMode((VirtualBankMode.Access)null);List<Boolean> choices=new ArrayList<>();missing.fullBankSelected(()->choices.add(false));missing.setEnabled(true);missing.update(run,true);missing.availabilityChanged();missing.update(run,true);missing.stop();assertTrue(choices.isEmpty());
	}
	@Test public void observerControllerDurableCommitOverlayAndFilterFollowNormalDeposits() throws Exception
	{
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		Source source=new Source();Worker worker=new Worker();BankOverlayState overlay=new BankOverlayState();overlay.enable();
		BankController controller=new BankController(source,repo,Runnable::run,overlay::updateBank);
		mode.setEnabled(true);controller.informationActions(run->mode.update(run,source.open,source.observe()));controller.start(()->root,worker);worker.runAll();
		source.observer.tick();controller.tick();worker.runAll();controller.startRun(true,null,true);worker.runAll();
		assertFalse(access.predicate.contains(SHIELD));source.quantity(3);source.observer.tick();controller.tick();
		assertFalse(access.predicate.contains(SHIELD)); // Not published before persistence acknowledgment.
		worker.runAll();assertTrue(access.predicate.contains(SHIELD));assertEquals("2",overlay.getProjection().label(SHIELD));
		BankRun committed=repo.load(root,source.character).get();assertEquals(2,committed.balance(SHIELD));assertEquals(3,committed.physical(SHIELD));
		mode.setEnabled(false);controller.tick();worker.runAll();mode.setEnabled(true);mode.update(committed,true);source.observer.changed();source.observer.tick();controller.tick();worker.runAll();
		assertEquals(committed,repo.load(root,source.character).get());assertTrue(access.predicate.contains(SHIELD));controller.stop();
	}
	@Test public void titleKeepsPrivateIdentityRestoresFullBankAndNeverTouchesUserTags()
	{
		mode.setEnabled(true);mode.update(run(1),true);String privateTag=access.active;assertEquals("Temporary view must not be restored by core", "",access.remembered);
		assertTrue(privateTag.startsWith("from_scratch_virtual_"));mode.title(java.awt.Color.MAGENTA);
		assertEquals("From Scratch Virtual Bank",net.runelite.client.util.Text.removeTags(access.title));assertTrue(access.title.contains("ff00ff"));assertFalse(access.title.contains(privateTag));
		mode.setEnabled(false);assertEquals("The Bank of Gielinor",access.title);assertNull(access.active);
		mode.setEnabled(true);mode.update(run(1),true);mode.title(java.awt.Color.GREEN);mode.bankClosed();assertEquals("The Bank of Gielinor",access.title);
		mode.setEnabled(false);access.active="My tag";access.title="My tag title";mode.update(run(1),true);mode.title(java.awt.Color.GREEN);mode.stop();assertEquals("My tag title",access.title);
	}
	@Test public void closeCallbackCanReenterWithoutRecursion()
	{
		mode.setEnabled(true);mode.update(run(1),true);access.onClose=mode::bankClosed;mode.bankClosed();assertEquals(1,access.closed);assertEquals(1,access.removed);
		
	}	@Test public void membershipReadsNoDefinitionsAndQuantityChangesWithoutSignChangeDoNotRelayout()
	{
		BankRun a=BankEngine.observe(run(1),bank(3),UUID.randomUUID(),SnapshotFixtures.TIME);mode.setEnabled(true);mode.update(a,true);int reads=access.definitions;
		for(int i=0;i<12000;i++){assertTrue(access.predicate.contains(SHIELD));assertTrue(access.predicate.contains(999999));assertFalse(access.predicate.contains(OTHER));}assertEquals(reads,access.definitions);
		BankRun b=BankEngine.observe(a,bank(4),UUID.randomUUID(),SnapshotFixtures.TIME);mode.update(b,true);mode.update(b,true);assertEquals(0,access.refresh);assertEquals(reads,access.definitions);
	}
	@Test public void queuedTransitionsCoalesceAndStopBeforeExecutionCannotReopen()
	{
		Queue<Runnable> queue=new ArrayDeque<>();VirtualBankMode queued=new VirtualBankMode(access,queue::add);BankRun a=run(1);queued.setEnabled(true);queued.update(a,true);queued.update(a,true);assertEquals(1,queue.size());assertEquals(0,access.opened);queued.stop();queue.remove().run();assertEquals(0,access.opened);
		queued.setEnabled(true);queued.update(a,true);queue.remove().run();assertEquals(1,access.opened);queued.setEnabled(false);queue.remove().run();assertEquals(1,access.closed);
	}
	@Test public void deferredMenuObservationRunsAfterCoreClosesItsTag()
	{
		Queue<Runnable> queue=new ArrayDeque<>();VirtualBankMode queued=new VirtualBankMode(access,queue::add);List<Boolean> choices=new ArrayList<>();queued.fullBankSelected(()->choices.add(false));
		queued.setEnabled(true);queued.update(run(1),true);queue.remove().run();
		queued.viewChanged();assertTrue(choices.isEmpty()); // Our priority-0 handler queues, never reconciles inline.
		access.active=null;access.nativeTab=2; // Core's priority -1 handler runs next.
		while(!queue.isEmpty()){queue.remove().run();}
		assertEquals(Collections.singletonList(false),choices);assertEquals(1,access.opened);assertEquals(2,access.nativeTab);assertNull(access.predicate);
	}
	@Test public void optionalRegistrationFailurePreservesPreferenceAndCanRecoverAfterDisableEnable()
	{
		List<Boolean> choices=new ArrayList<>();mode.fullBankSelected(()->choices.add(false));mode.setEnabled(true);mode.update(null,false);assertNull(access.predicate);
		access.failRegister=true;mode.update(run(1),true);assertTrue(mode.isFaulted());assertTrue(choices.isEmpty());assertNull(access.predicate);assertEquals(0,access.opened);assertEquals(1,access.removed);
		for(int i=0;i<20;i++){mode.update(run(1),true);}assertEquals(1,access.removed);
		mode.stop();access.failRegister=false;mode.setEnabled(true);mode.update(run(1),true);assertFalse(mode.isFaulted());assertEquals(1,access.opened);assertTrue(choices.isEmpty());
	}
	@Test public void callbacksCannotRecursivelyOpenRefreshOrReattachDuringShutdown()
	{
		BankRun a=run(1);access.onOpen=()->mode.update(a,true);access.onRefresh=()->mode.update(a,true);access.onClose=()->{mode.update(a,true);mode.bankClosed();};mode.setEnabled(true);mode.update(a,true);assertEquals(1,access.opened);
		mode.update(BankEngine.observe(a,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME),true);assertEquals(1,access.refresh);mode.stop();mode.stop();assertEquals(1,access.closed);assertEquals(1,access.removed);
	}
	@Test public void nativeFullBankSelectionIsNotOverriddenAndFailureIsLatchedUntilExplicitRetry()
	{
		List<Boolean> selections=new ArrayList<>();mode.fullBankSelected(()->selections.add(false));
		mode.setEnabled(true);BankRun a=run(1);mode.update(a,true);access.active=null;access.nativeTab=2;mode.viewChanged();mode.update(a,true);assertEquals(1,access.opened);assertEquals(2,access.nativeTab);assertEquals(Collections.singletonList(false),selections);assertNull(access.predicate);
		mode.bankClosed();mode.update(a,true);assertEquals("Native selection is an explicit OFF preference",1,access.opened);
		mode.setEnabled(false);mode.setEnabled(true);mode.update(a,true);assertEquals(2,access.opened);access.failRefresh=true;BankRun b=BankEngine.observe(a,bank(2),UUID.randomUUID(),SnapshotFixtures.TIME);mode.update(b,true);assertNull(access.active);for(int i=0;i<20;i++){mode.update(b,true);}assertEquals(2,access.opened);assertEquals(1,access.refresh);assertEquals(2,access.removed);
		access.failRefresh=false;mode.setEnabled(false);mode.setEnabled(true);mode.update(b,true);assertEquals(3,access.opened);
	}
	@Test public void oldTemporaryTagFromForcedRestartIsReplacedButUserTagsAreNotDeleted()
	{
		access.active="from_scratch_virtual_00000000000000000000000000000000";mode.setEnabled(true);mode.update(run(1),true);mode.title(java.awt.Color.GREEN);assertEquals(1,access.closed);assertEquals(1,access.opened);assertEquals(VirtualBankMode.TITLE,net.runelite.client.util.Text.removeTags(access.title));assertEquals(1,access.removed);
	}
	@Test public void restoredRunWaitsForAuthoritativeBankAndNeverReusesAnotherCharactersMembership()
	{
		BankRun saved=run(1),earned=BankEngine.observe(saved,bank(3),UUID.randomUUID(),SnapshotFixtures.TIME);
		BankObservation bank=new BankObservation(UUID.randomUUID(),1,SnapshotFixtures.TIME,bank(3),"Ready");
		mode.setEnabled(true);mode.update(saved,true,bank);assertEquals(0,access.opened); // Offline deposit not committed yet.
		access.onOpen=()->assertTrue("Membership must be ready before native rebuild",access.predicate.contains(SHIELD));
		mode.update(earned,true,bank);assertEquals(1,access.opened);assertTrue(access.predicate.contains(SHIELD));
		mode.stop();access.onOpen=()->{};mode.setEnabled(true);mode.update(earned,true,bank);assertTrue(access.predicate.contains(SHIELD)); // Restart restores saved positives.
		BankRun other=BankEngine.start(SnapshotFixtures.CHARACTER_B,null,bank(1),true,UUID.randomUUID(),SnapshotFixtures.TIME);
		mode.update(other,true,new BankObservation(UUID.randomUUID(),2,SnapshotFixtures.TIME,null,"Waiting"));assertNull(access.active);assertFalse(mode.contains(SHIELD));
		mode.update(other,true,new BankObservation(UUID.randomUUID(),3,SnapshotFixtures.TIME,bank(1),"Ready"));assertNotNull(access.active);assertFalse(access.predicate.contains(SHIELD));
	}
	@Test public void noRunOrSavedOffClearsCoreRestoredSelectionsWithoutRegisteringOrChangingPreference()
	{
		for(boolean preference:new boolean[]{true,false})
		{
			Access core=new Access();String stale="from_scratch_virtual_0123456789abcdef0123456789abcdef";core.active=stale;core.remembered=stale;core.title="Tag tab "+stale;
			VirtualBankMode view=new VirtualBankMode(core);List<Boolean> choices=new ArrayList<>();view.fullBankSelected(()->choices.add(false));view.setEnabled(preference);view.update(null,true);
			assertTrue(view.fullBankForSetup());assertNull(core.active);assertNull(core.predicate);assertEquals(0,core.opened);assertEquals("",core.remembered);assertEquals("The Bank of Gielinor",core.title);assertTrue(choices.isEmpty());
			// Simulate core restoring another old selection AFTER From Scratch has already initialized.
			core.active=stale;core.remembered=stale;view.bankBuilt();assertNull(core.active);assertEquals("",core.remembered);assertEquals(0,core.opened);
			if(!preference){view.update(run(1),true);assertNull(core.active);assertNull(core.predicate);}
			view.stop();
		}
	}
	@Test public void savedUserTagWithLegacyLookingNameIsNeverClosedUnregisteredOrForgotten()
	{
		String user="from_scratch_virtual_0123456789abcdef0123456789abcdef";access.savedTags.add(user);access.active=user;access.remembered=user;access.title="My saved layout";
		mode.setEnabled(false);mode.update(null,true);mode.bankBuilt();assertTrue(mode.fullBankForSetup());mode.stop();assertEquals(user,access.active);assertEquals(user,access.remembered);assertEquals(0,access.closed);assertEquals(0,access.removed);assertEquals("My saved layout",access.title);
	}
	@Test public void setupBlocksUntilRestoredFilterClosesAndCommitsWholePhysicalBankBeforeActivation() throws Exception
	{
		Queue<Runnable> clientTasks=new ArrayDeque<>();VirtualBankMode view=new VirtualBankMode(access,clientTasks::add);
		String stale="from_scratch_virtual_0123456789abcdef0123456789abcdef";access.active=stale;access.remembered=stale;
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		Source source=new Source();source.items=new net.runelite.api.Item[]{new net.runelite.api.Item(SHIELD,40),new net.runelite.api.Item(OTHER,100)};source.observer.tick();Worker worker=new Worker();List<BankView> displays=new ArrayList<>();
		BankController controller=new BankController(source,repo,Runnable::run,displays::add);view.setEnabled(true);controller.initialBankView(view::fullBankForSetup);controller.informationActions(run->view.update(run,source.open,source.observe()));controller.start(()->root,worker);worker.runAll();
		assertFalse(displays.get(displays.size()-1).ready());controller.startRun(true,null,true);worker.runAll();assertFalse(repo.load(root,source.character).isPresent());assertEquals(0,access.opened);
		while(!clientTasks.isEmpty()){clientTasks.remove().run();}controller.tick();assertTrue(displays.get(displays.size()-1).ready());assertNull(access.active);
		controller.startRun(true,null,true);assertEquals(0,access.opened);assertFalse(repo.load(root,source.character).isPresent());worker.runAll();
		BankRun committed=repo.load(root,source.character).get();assertEquals(40,committed.getInitialBank().quantity(SHIELD));assertEquals(100,committed.getInitialBank().quantity(OTHER));assertEquals(0,access.opened);
		while(!clientTasks.isEmpty()){clientTasks.remove().run();}assertEquals(1,access.opened);assertFalse(access.predicate.contains(SHIELD));assertFalse(access.predicate.contains(OTHER));
		controller.tick();worker.runAll();committed=repo.load(root,source.character).get(); // Settle the normal first host/session check before testing view-only switching.
		view.setEnabled(false);while(!clientTasks.isEmpty()){clientTasks.remove().run();}assertNull(access.active);controller.tick();worker.runAll();assertEquals(committed,repo.load(root,source.character).get());controller.stop();view.stop();while(!clientTasks.isEmpty()){clientTasks.remove().run();}
	}
	@Test public void failedCloseBlocksSetupAndDoesNotLoop()
	{
		access.active="from_scratch_virtual_0123456789abcdef0123456789abcdef";access.refuseClose=true;mode.setEnabled(false);assertTrue(mode.isFaulted());assertFalse(mode.fullBankForSetup());int attempts=access.closed;
		for(int i=0;i<20;i++){mode.update(null,true);mode.bankBuilt();assertFalse(mode.fullBankForSetup());}assertEquals(attempts,access.closed);assertEquals(0,access.opened);
	}
	@Test public void startWithOffUsesFullBankAndResetKeepsOnPreferenceForNextCommittedRun() throws Exception
	{
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		Source source=new Source();Worker worker=new Worker();BankController controller=new BankController(source,repo,Runnable::run,v->{});
		mode.setEnabled(false);controller.initialBankView(mode::fullBankForSetup);controller.informationActions(run->mode.update(run,source.open,source.observe()));controller.start(()->root,worker);worker.runAll();source.observer.tick();controller.tick();
		source.open(false);source.observer.tick();controller.tick();source.open(true);source.observer.tick();controller.tick();assertNull(access.active);assertEquals(0,access.opened);
		controller.startRun(true,null,true);worker.runAll();BankRun first=repo.load(root,source.character).get();assertTrue(first.isActive());assertEquals(1,first.getInitialBank().quantity(SHIELD));assertNull(access.active);assertEquals(0,access.opened);
		mode.setEnabled(true);mode.update(first,true,source.observe());assertNotNull(access.active);assertEquals(1,access.opened);
		controller.reset(first.getRevision());worker.runAll();assertFalse(repo.load(root,source.character).get().isActive());assertNull(access.active);
		source.observer.tick();controller.tick();worker.runAll();controller.startRun(true,null,true);assertNull(access.active);worker.runAll();BankRun second=repo.load(root,source.character).get();assertTrue(second.isActive());assertNotEquals(first.getRunId(),second.getRunId());assertNotNull(access.active);assertEquals(2,access.opened);
		controller.stop();mode.stop();
	}
	@Test public void nativeTabsUseCachedIntersectionWithoutReopeningTagOrChangingPreference()
	{
		int ashes=net.runelite.api.gameval.ItemID.ASHES;net.runelite.api.Item[] initial={new net.runelite.api.Item(SHIELD,100),new net.runelite.api.Item(OTHER,100),new net.runelite.api.Item(ashes,1)};
		BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BankItems.aggregate(initial,id->false),true,UUID.randomUUID(),SnapshotFixtures.TIME);
		access.nativeSlots=new net.runelite.api.Item[]{new net.runelite.api.Item(SHIELD,105),new net.runelite.api.Item(OTHER,99),new net.runelite.api.Item(ashes,3)};access.nativeSizes=new int[]{2,1};
		run=BankEngine.observe(run,BankItems.aggregate(access.nativeSlots,id->false),UUID.randomUUID(),SnapshotFixtures.TIME);List<Boolean> choices=new ArrayList<>();mode.fullBankSelected(()->choices.add(false));mode.setEnabled(true);mode.update(run,true);int definitions=access.definitions;
		for(int tab:new int[]{1,2,0,1,2,0})
		{
			mode.nativeTabSelected();access.active=null;access.nativeTab=tab;int[] build={0};mode.script("bankBuildTab",build,1);assertEquals(1,build[0]);
			assertEquals(tab!=2,allowedByScript(SHIELD));assertEquals(tab!=1,allowedByScript(ashes));assertFalse(allowedByScript(OTHER));
			mode.bankBuilt();mode.title(java.awt.Color.GREEN);assertEquals(VirtualBankMode.TITLE,net.runelite.client.util.Text.removeTags(access.title));assertEquals(1,access.opened);assertEquals(0,access.refresh);
		}
		assertTrue(choices.isEmpty());assertEquals(definitions,access.definitions);mode.setEnabled(false);int[] normal={0};mode.script("bankBuildTab",normal,1);assertEquals(0,normal[0]);assertEquals(1,access.refresh);assertNull(access.active);
	}
	private boolean allowedByScript(int id){int[] result={-1,id};mode.script("bankSearchFilter",result,2);return result[0]!=0;}
	@Test public void nativeTabDepositsAndWithdrawalsRemainAuthoritativeAndSearchExitsSafely() throws Exception
	{
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());Source source=new Source();Worker worker=new Worker();BankController controller=new BankController(source,repo,Runnable::run,v->{});mode.setEnabled(true);controller.informationActions(run->mode.update(run,source.open,source.observe()));controller.start(()->root,worker);worker.runAll();source.observer.tick();controller.tick();controller.startRun(true,null,true);worker.runAll();source.quantity(3);source.observer.tick();controller.tick();worker.runAll();
		BankRun before=repo.load(root,source.character).get();mode.nativeTabSelected();access.active=null;access.nativeTab=1;int[] build={0};mode.script("bankBuildTab",build,1);mode.bankBuilt();assertEquals(before,repo.load(root,source.character).get());assertTrue(allowedByScript(SHIELD));
		for(int n:new int[]{1,0,2}){source.quantity(n);source.observer.tick();controller.tick();worker.runAll();assertEquals(n-1,repo.load(root,source.character).get().balance(SHIELD));assertEquals(n>1,allowedByScript(SHIELD));}
		int opened=access.opened;List<Boolean> preferences=new ArrayList<>();mode.fullBankSelected(()->preferences.add(false));mode.searchSelected();assertEquals(Collections.singletonList(false),preferences);assertEquals(opened,access.opened);int[] untouched={-1,SHIELD};mode.script("bankSearchFilter",untouched,2);assertEquals(-1,untouched[0]);controller.stop();mode.stop();
	}
	@Test public void incompleteNativeTabMetadataFailsSafeWithoutRefreshRecursion()
	{
		mode.setEnabled(true);mode.update(run(1),true);mode.nativeTabSelected();access.active=null;access.nativeTab=1;access.nativeSlots=new net.runelite.api.Item[]{null};access.nativeSizes=new int[]{1};int[] build={0};mode.script("bankBuildTab",build,1);assertTrue(mode.isFaulted());int refresh=access.refresh;for(int i=0;i<10;i++){mode.bankBuilt();mode.update(run(1),true);}assertEquals(refresh,access.refresh);assertEquals(1,access.opened);
	}
	@Test public void potionStoragePausesFilteringAndReturnsToNativeIntersectionWithoutReopen()
	{
		BankRun run=BankEngine.observe(run(1),bank(2),UUID.randomUUID(),SnapshotFixtures.TIME);List<Boolean> preferences=new ArrayList<>();mode.fullBankSelected(()->preferences.add(false));mode.setEnabled(true);mode.update(run,true);int opened=access.opened;
		mode.potionStorageSelected();access.active=null;access.nativeTab=15;mode.bankBuilt();mode.update(run,true);int[] untouched={0};mode.script("bankBuildTab",untouched,1);assertEquals(0,untouched[0]);assertFalse(mode.isFaulted());assertTrue(preferences.isEmpty());assertEquals(opened,access.opened);
		access.nativeTab=1;mode.nativeTabSelected();mode.bankBuilt();assertTrue(allowedByScript(SHIELD));assertNotNull(access.predicate);assertEquals(opened,access.opened);assertFalse(mode.isFaulted());assertTrue(preferences.isEmpty());int refresh=access.refresh;for(int i=0;i<10;i++){mode.bankBuilt();mode.update(run,true);}assertEquals(refresh,access.refresh);
	}
	@Test public void earnedBankPotionsSurviveStorageVisitMissingContainerReturnAndRelog() throws Exception
	{
		int potion=net.runelite.api.gameval.ItemID._4DOSEPRAYERRESTORE;com.fromscratch.potion.PotionType type=new com.fromscratch.potion.PotionType(1,"Prayer potion",false,Collections.singletonMap(potion,4));com.fromscratch.potion.PotionObservation storage=new com.fromscratch.potion.PotionObservation(true,Collections.singletonMap(1,type),Collections.emptyMap(),SnapshotFixtures.TIME);
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());repo.save(root,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,bank(0),true,UUID.randomUUID(),SnapshotFixtures.TIME,storage));Source source=new Source();source.items=new net.runelite.api.Item[0];source.storage=storage;Worker worker=new Worker();BankOverlayState overlay=new BankOverlayState();overlay.enable();List<Overdrafts.Warning> alerts=new ArrayList<>();BankController controller=new BankController(source,repo,Runnable::run,overlay::updateBank);controller.overdrafts(alerts::addAll);mode.setEnabled(true);controller.informationActions(run->mode.update(run,source.open,source.observe()));controller.start(()->root,worker);worker.runAll();source.observer.tick();controller.tick();worker.runAll();
		try
		{
			source.items=new net.runelite.api.Item[]{new net.runelite.api.Item(potion,3)};source.observer.changed();source.observer.tick();controller.tick();worker.runAll();BankRun earned=repo.load(root,source.character).get();assertEquals(3,earned.balance(potion));assertTrue(access.predicate.contains(potion));int opened=access.opened;
			mode.potionStorageSelected();access.active=null;access.nativeTab=15;mode.bankBuilt();source.storageOpen=true;source.fullBank=false;source.items=null;source.observer.changed();source.observer.tick();controller.tick();worker.runAll();assertEquals(earned,repo.load(root,source.character).get());assertNotNull(controller.currentRun());assertEquals(3,overlay.getProjection().balance(potion,false));
			source.items=new net.runelite.api.Item[]{new net.runelite.api.Item(potion,3)};source.observer.tick();controller.tick();worker.runAll();source.storageOpen=false;source.fullBank=true;access.nativeTab=0;mode.bankBuilt();controller.tick();worker.runAll();assertTrue(access.predicate.contains(potion));assertEquals(opened,access.opened);assertFalse(mode.isFaulted());
			source.character=null;source.observer.reset();controller.tick();worker.runAll();source.character=SnapshotFixtures.CHARACTER_A;source.observer.reset();controller.tick();worker.runAll();source.observer.tick();controller.tick();worker.runAll();assertEquals(3,repo.load(root,source.character).get().balance(potion));assertTrue(access.predicate.contains(potion));assertTrue(alerts.isEmpty());
		}finally{controller.stop();mode.stop();}
	}
	@Test public void disablingWhilePotionStorageOpenDoesNotResumeFilter()
	{
		mode.setEnabled(true);mode.update(run(1),true);mode.potionStorageSelected();access.active=null;access.nativeTab=15;mode.bankBuilt();mode.setEnabled(false);access.nativeTab=0;mode.bankBuilt();assertNull(access.predicate);int[] untouched={0};mode.script("bankBuildTab",untouched,1);assertEquals(0,untouched[0]);assertFalse(mode.isFaulted());
	}
	@Test public void closingPotionStorageThenReopeningBankRestoresSavedPreference()
	{
		BankRun run=BankEngine.observe(run(1),bank(2),UUID.randomUUID(),SnapshotFixtures.TIME);mode.setEnabled(true);mode.update(run,true);mode.potionStorageSelected();access.active=null;access.nativeTab=15;mode.bankBuilt();mode.bankClosed();access.nativeTab=0;mode.update(run,true);assertNotNull(access.predicate);assertTrue(access.predicate.contains(SHIELD));assertFalse(mode.isFaulted());assertEquals(2,access.opened);
	}
	
}
