package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.persistence.BankRepository;
import com.fromscratch.persistence.BankRunCodec;
import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/**
 * Synthetic input and deferred-callback integration, never game input. The close condition is
 * taken from RuneLite 1.13.4 BankMainBuild.rs2asm: bankBuildTab=1 jumps to
 * singletabbuildmode -> LABEL879 -> LABEL883 -> invoke 299 (meslayer_close).
 * Native whole-bank search bypasses that branch. This exercises that consequence, rather than
 * treating a correct bankSearchFilter predicate as proof that the input survives a bank build.
 */
public class NativeBankSearchLifecycleTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private static final int SHIELD=ItemID.WOODEN_SHIELD,COINS=ItemID.COINS,ASHES=ItemID.ASHES;
	private static final Map<Integer,String> NAMES=new LinkedHashMap<>();
	static {NAMES.put(SHIELD,"Wooden shield");NAMES.put(COINS,"Coins");NAMES.put(ASHES,"Ashes");}

	private static final class Lifecycle implements VirtualBankMode.Access
	{
		final Queue<Runnable> deferred=new ArrayDeque<>();
		final VirtualBankMode mode=new VirtualBankMode(this,deferred::add);
		final List<Boolean> settings=new ArrayList<>();
		String active,query="";
		boolean inputOpen;
		BankTag tag;
		int tab,opens,refreshes,builds,forcedCloses;
		BankRun run,initial;
		Lifecycle(boolean earned)
		{
			Item[] original={new Item(SHIELD,1),new Item(COINS,100),new Item(ASHES,1)};
			run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BankItems.aggregate(original,id->false),true,UUID.randomUUID(),SnapshotFixtures.TIME);
			initial=run;
			if(earned){run=BankEngine.observe(run,BankItems.aggregate(new Item[]{new Item(SHIELD,3),new Item(COINS,100),new Item(ASHES,2)},id->false),UUID.randomUUID(),SnapshotFixtures.TIME);}
			mode.fullBankSelected(()->settings.add(false));mode.setEnabled(true);mode.update(run,true);drain();
		}
		public void register(String name,BankTag value){tag=value;}
		public void unregister(String name){tag=null;}
		public void open(String name){active=name;opens++;inputOpen=false;query="";build();}
		public void close(){active=null;build();}
		public String active(){return active;}
		public void refresh(){refreshes++;build();}
		public int nativeTab(){return tab;}
		public Set<Integer> nativeItems(){return tab==1?new HashSet<>(Arrays.asList(SHIELD,COINS)):Collections.singleton(ASHES);}
		// Mirrors [proc,bankmain_searching], including a submitted retained query.
		public boolean searching(){return inputOpen||!query.isEmpty();}
		void drain()
		{
			int remaining=100;
			while(!deferred.isEmpty()){assertTrue("No recursive refresh/rebuild loop",remaining-->0);deferred.remove().run();}
		}
		void build()
		{
			assertTrue("Bounded native builds",++builds<1000);
			boolean nativeSearching=searching(); // BankMainBuild local 37, before the callback.
			int[] singleTab={0};mode.script("bankBuildTab",singleTab,1);
			if(singleTab[0]==1&&nativeSearching)
			{
				// The actual injected native branch closes the message layer and clears its text.
				inputOpen=false;query="";forcedCloses++;
			}
			mode.bankBuilt();
		}
		void clickSearch()
		{
			mode.viewChanged(); // Plugin menu priority 0: queues reconciliation.
			mode.searchSelected(); // Plugin script-pre priority 1, before Bank Tags priority 0.
			active=null; // TabInterface.closeTag(false), with no reset or relayout.
			tab=0; // Native search_toggle selects the all-items tab.
			if(searching()){inputOpen=false;query="";}else{inputOpen=true;query="";}
			build(); // Native timer/queued layout, after the input has opened.
			drain();
		}
		void type(String text)
		{
			assertTrue("Player must still be able to type into native search",inputOpen);
			for(char character:text.toCharArray()){query+=character;build();drain();assertTrue(inputOpen);}
		}
		void clear(){assertTrue(inputOpen);query="";build();drain();}
		Set<Integer> results()
		{
			Set<Integer> result=new HashSet<>();
			NAMES.forEach((id,name)->{
				int nativeResult=name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))?1:0;
				if(active!=null&&tag!=null){nativeResult=tag.contains(id)?1:0;}
				int[] stack={nativeResult,id};mode.script("bankSearchFilter",stack,2);if(stack[0]!=0){result.add(id);}
			});return result;
		}
		void clickTab(int number)
		{
			mode.nativeTabSelected();active=null;tab=number;
			// Native tab navigation itself ends a search. From Scratch must keep its preference.
			inputOpen=false;query="";build();drain();
		}
	}

	@Test public void searchInputSurvivesFirstNativeBuildDeferredCallbacksAndTypedQueries()
	{
		Lifecycle bank=new Lifecycle(true);BankRun unchanged=bank.run;
		for(int cycle=0;cycle<8;cycle++)
		{
			bank.clickSearch();assertTrue("Opening search must survive the native bank build",bank.inputOpen);
			bank.type("shield");assertEquals(Collections.singleton(SHIELD),bank.results());
			bank.clear();assertEquals(new HashSet<>(Arrays.asList(SHIELD,ASHES)),bank.results());
			bank.type("coins");assertTrue("Native legacy-only matches remain hidden",bank.results().isEmpty());
			bank.clear();bank.type("no native match");assertTrue(bank.results().isEmpty());
			bank.clear();bank.clickSearch();assertFalse(bank.inputOpen);assertEquals("",bank.query);
			bank.drain();assertTrue(bank.settings.isEmpty());assertEquals(unchanged,bank.run);
		}
		assertEquals(0,bank.forcedCloses);assertEquals(1,bank.opens);assertEquals(0,bank.refreshes);
	}

	@Test public void emptyVirtualBankAndSubmittedQueryKeepNativeSearchUsable()
	{
		Lifecycle bank=new Lifecycle(false);bank.clickSearch();assertTrue(bank.inputOpen);
		bank.type("shield");assertTrue(bank.results().isEmpty());bank.clear();assertTrue(bank.results().isEmpty());
		bank.type("coins");bank.inputOpen=false; // Enter: native search keeps the nonempty query.
		bank.build();bank.drain();assertEquals("coins",bank.query);assertTrue(bank.results().isEmpty());assertEquals(0,bank.forcedCloses);
		bank.clickSearch();bank.clickSearch();assertTrue(bank.inputOpen);bank.type("ashes");assertTrue(bank.results().isEmpty());
		assertTrue(bank.settings.isEmpty());assertEquals(1,bank.opens);
	}

	@Test public void nativeTabsPotionStorageAndFullBankRetainTheirOwnLifecycles()
	{
		Lifecycle bank=new Lifecycle(true);bank.clickSearch();bank.type("shield");
		bank.clickTab(2);assertEquals(Collections.singleton(ASHES),bank.results());assertFalse(bank.inputOpen);
		bank.clickSearch();bank.type("shield");assertEquals(Collections.singleton(SHIELD),bank.results());
		bank.mode.potionStorageSelected();bank.active=null;bank.tab=15;bank.inputOpen=false;bank.query="";bank.build();bank.drain();
		int[] potionResult={1,COINS};bank.mode.script("bankSearchFilter",potionResult,2);assertEquals(1,potionResult[0]);
		bank.tab=0;bank.build();bank.drain();bank.mode.update(bank.run,true);bank.drain();bank.clickSearch();bank.type("ashes");assertEquals(Collections.singleton(ASHES),bank.results());
		assertTrue(bank.settings.isEmpty());assertEquals(1,bank.opens);
		bank.mode.setEnabled(false);bank.drain();assertEquals("ashes",bank.query);assertTrue(bank.inputOpen);
		bank.clear();bank.type("coins");assertEquals(Collections.singleton(COINS),bank.results());
		bank.clickSearch();bank.clickSearch();bank.type("shield");assertEquals(Collections.singleton(SHIELD),bank.results());
		assertEquals(0,bank.forcedCloses);assertTrue(bank.settings.isEmpty());
	}

	@Test public void nativeSearchStateIncludesEmptyOpenInputAndRetainedSubmittedText()
	{
		assertTrue(VirtualBankMode.searching(net.runelite.api.vars.InputType.SEARCH.getType(),""));
		assertTrue(VirtualBankMode.searching(net.runelite.api.vars.InputType.NONE.getType(),"shield"));
		assertTrue(VirtualBankMode.searching(net.runelite.api.vars.InputType.RUNELITE.getType(),"shield"));
		assertFalse(VirtualBankMode.searching(net.runelite.api.vars.InputType.NONE.getType(),""));
		assertFalse(VirtualBankMode.searching(net.runelite.api.vars.InputType.NONE.getType(),null));
		assertFalse(VirtualBankMode.searching(net.runelite.api.vars.InputType.PRIVATE_MESSAGE.getType(),"shield"));
	}

	@Test public void queuedMembershipRefreshDuringTextEntryPreservesInputWithoutReopening()
	{
		Lifecycle bank=new Lifecycle(true);bank.clickSearch();bank.type("shield");
		for(int shields:new int[]{0,3,0,3})
		{
			bank.run=BankEngine.observe(bank.run,BankItems.aggregate(new Item[]{new Item(SHIELD,shields),new Item(COINS,100),new Item(ASHES,2)},id->false),UUID.randomUUID(),SnapshotFixtures.TIME);
			bank.mode.update(bank.run,true);bank.mode.viewChanged();bank.drain();
			assertTrue(bank.inputOpen);assertEquals("shield",bank.query);
			assertEquals(shields==0?Collections.emptySet():Collections.singleton(SHIELD),bank.results());
		}
		assertEquals(4,bank.refreshes);assertEquals(1,bank.opens);assertEquals(0,bank.forcedCloses);assertTrue(bank.settings.isEmpty());
	}

	@Test public void searchCallbacksLeaveAcknowledgedProgressionUnchanged() throws Exception
	{
		Lifecycle bank=new Lifecycle(true);Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		BankRepository repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());repository.save(root,null,bank.initial);repository.save(root,bank.initial,bank.run);
		BankFixtures.Source source=new BankFixtures.Source();source.items=new Item[]{new Item(SHIELD,3),new Item(COINS,100),new Item(ASHES,2)};
		BankFixtures.Worker worker=new BankFixtures.Worker();BankController controller=new BankController(source,repository,Runnable::run,view->{});
		controller.informationActions(run->bank.mode.update(run,true,source.observe()));controller.start(()->root,worker);worker.runAll();source.observer.tick();controller.tick();worker.runAll();
		try
		{
			BankRun committed=repository.load(root,source.character).get();
			for(int cycle=0;cycle<3;cycle++)
			{
				bank.clickSearch();bank.type("shield");bank.clear();bank.type("coins");bank.clear();bank.clickSearch();controller.tick();worker.runAll();bank.drain();
				assertEquals(committed,repository.load(root,source.character).get());
			}
			assertEquals(0,bank.forcedCloses);assertTrue(bank.settings.isEmpty());
		}
		finally{controller.stop();bank.mode.stop();bank.drain();}
	}
}
