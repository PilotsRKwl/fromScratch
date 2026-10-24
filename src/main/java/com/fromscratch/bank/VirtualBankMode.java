package com.fromscratch.bank;

import com.fromscratch.buyback.BuybackEngine;
import java.util.*;
import java.util.function.Consumer;
import javax.inject.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.banktags.*;

/** Native Bank Tags owns input/layout. All lifecycle operations are deferred off bank script callbacks. */
@Singleton @Slf4j
public final class VirtualBankMode
{
	interface Access
	{
		void register(String name,BankTag tag); void unregister(String name);
		void open(String name); void close(); String active(); void refresh();
		default boolean ready(){return true;} default void allItems(){}
		default boolean filtering(){return active()!=null;}
		default String remembered(){return "";} default void remembered(String name){}
		default boolean savedTag(String name){return false;}
		default String title(){return null;} default void title(String text){}
		default int placeholder(int id){return -1;}
		default int nativeTab(){return 0;} default Set<Integer> nativeItems(){return null;}
	}
	private final PluginManager plugins;
	private final Client client;
	private final Access fixture;
	private final Consumer<Runnable> later;
	private Access access;
	private Plugin owner;
	private boolean enabled,wanted,attempted,queued,operating,faulted,selected,suspended,recursionLogged,registered,cleanupFailed;
	private final String name="from_scratch_virtual_"+UUID.randomUUID().toString().replace("-","");
	private volatile Set<Integer> membership=Collections.emptySet();
	private Set<Integer> positives=Collections.emptySet(),applied=Collections.emptySet();
	private Set<Integer> eligible=Collections.emptySet();
	private boolean nativeMode,tabDirty=true;
	private boolean potionView;
	// Core Bank Tags identifies the native Potion Storage view by BANK_CURRENTTAB=15.
	private static final int POTION_TAB=15;
	private int tabId=-1;
	private final Map<Integer,Integer> placeholders=new HashMap<>();
	private BankRun previous;
	private boolean membershipReady;
	private Runnable fullBankSelected=()->{};
	private String previousTitle;
	public static final String TITLE="From Scratch Virtual Bank";
	@Inject public VirtualBankMode(PluginManager plugins,Client client,ClientThread thread){this(plugins,client,null,thread::invokeLater);}
	private VirtualBankMode(PluginManager plugins,Client client,Access fixture,Consumer<Runnable> later){this.plugins=plugins;this.client=client;this.fixture=fixture;this.later=later;}
	VirtualBankMode(Access access){this(access,Runnable::run);}
	VirtualBankMode(Access access,Consumer<Runnable> later){this(null,null,access,later);}
	public void setEnabled(boolean value)
	{
		if(enabled!=value){enabled=value;faulted=false;cleanupFailed=false;attempted=false;suspended=false;if(enabled){selected=false;}}
		if(!enabled){wanted=false;}request();
	}
	public void availabilityChanged(){attempted=false;faulted=false;cleanupFailed=false;if(!operating){request();}}
	public boolean isFaulted(){return faulted;}
	public void fullBankSelected(Runnable listener){fullBankSelected=listener;}
	/** Core closes tags for native tabs/search. Observe that choice; never reopen from a script callback. */
	public void viewChanged(){if(!operating&&selected){request();}}
	/** A real native-tab click authorizes native filtering, never a tag reopen. */
	public void nativeTabSelected(){if(potionView){return;}if(wanted&&selected&&!faulted){nativeMode=true;tabDirty=true;request();}}
	/** Observe the native button, letting core close its tag before any deferred cleanup. */
	public void potionStorageSelected(){if(nativeTab()!=POTION_TAB){pausePotions();}}
	private void pausePotions()
	{potionView=true;wanted=false;selected=false;nativeMode=false;previousTitle=null;tabDirty=true;applied=Collections.emptySet();request();}
	/** Native search owns its input; leave our single-tab build mode rather than close that input. */
	public void searchSelected(){if(wanted&&selected){selectFullBank();request();}}
	public void itemsChanged(){tabDirty=true;}
	/** Core may restore a remembered tag without any From Scratch activation. */
	public void bankBuilt()
	{
		if(nativeTab()==POTION_TAB){if(!potionView){pausePotions();}return;}
		if(potionView){potionView=false;if(enabled&&previous!=null&&membershipReady){wanted=true;selected=true;nativeMode=true;tabDirty=true;}}
		if(!operating){attempted=false;request();}
	}
	/** Absolutely no client calls, mutation, registration, IO or refresh in the script's predicate. */
	boolean contains(int id){return membership.contains(id);}
	public void update(BankRun run,boolean bankOpen)
	{update(run,bankOpen,true);}
	public void update(BankRun run,boolean bankOpen,BankObservation bank)
	{update(run,bankOpen,run!=null&&bank.isReady()&&run.getPhysical().equals(bank.getBank().getQuantities()));}
	private void update(BankRun run,boolean bankOpen,boolean complete)
	{
		if(operating){reentry();return;}
		if(previous!=null&&run!=null&&(!run.getCharacter().equals(previous.getCharacter())||!Objects.equals(run.getRunId(),previous.getRunId())))
		{previous=null;membershipReady=false;positives=Collections.emptySet();membership=Collections.emptySet();if(!complete){selected=false;}}
		wanted=enabled&&!potionView&&bankOpen&&run!=null&&run.isActive()&&run.isAnchored()&&!faulted;
		// A mounted interface alone is not a complete bank observation. Keep an already selected
		// committed view during a pending bank write, but never open an uninitialized/stale view.
		if(!complete&&!selected){wanted=false;}
		if(!wanted){if(run==null){previous=null;membershipReady=false;positives=Collections.emptySet();membership=Collections.emptySet();}if(access!=null||queued||bankOpen){request();}return;}
		if(!complete){return;}
		if(previous==null||!run.getRevision().equals(previous.getRevision()))
		{
			boolean changed=previous==null||!run.getBalances().equals(previous.getBalances())||!run.getPhysical().equals(previous.getPhysical())||!run.getBuybacks().equals(previous.getBuybacks());
			previous=run;
			if(changed)
			{
				tabDirty=true;
				Set<Integer> next=new HashSet<>();BuybackEngine.balances(run).forEach((id,q)->{if(q>0){next.add(id);}});
				if(!membershipReady||!next.equals(positives))
				{
					positives=Collections.unmodifiableSet(next);Set<Integer> ids=new HashSet<>(next);
					// Resolve definitions only on a committed membership change, never inside contains().
					int missing=0;for(int id:next){try{int placeholder=placeholders.computeIfAbsent(id,this::placeholder);if(placeholder>=0){ids.add(placeholder);}}catch(RuntimeException ex){missing++;}}if(missing>0){log.debug("From Scratch Virtual Bank: {} placeholder definitions unavailable",missing);}
					eligible=Collections.unmodifiableSet(ids);membership=eligible;membershipReady=true;log.debug("From Scratch Virtual Bank: membership changed, types={}",next.size());
				}
			}
		}
		request();
	}
	private int placeholder(int id)
	{
		if(fixture!=null){return fixture.placeholder(id);}ItemComposition item=client.getItemDefinition(id);
		int linked=item.getPlaceholderId();if(linked<0||item.getPlaceholderTemplateId()!=-1){return -1;}
		ItemComposition placeholder=client.getItemDefinition(linked);return placeholder.getPlaceholderTemplateId()!=-1&&placeholder.getPlaceholderId()==id?linked:-1;
	}
	private int nativeTab(){return fixture!=null?fixture.nativeTab():client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB);}
	private void tabMembership()
	{
		int tab=nativeTab();if(!tabDirty&&tab==tabId){return;}
		Set<Integer> items=null;
		if(tab!=0)
		{
			if(tab<1||tab>9){throw new IllegalStateException("Virtual Bank supports native item tabs, not special storage");}
			if(fixture!=null){items=fixture.nativeItems();if(items==null){throw new IllegalStateException("Native tab contents unavailable");}}
			else
			{
				int[] counts={net.runelite.api.gameval.VarbitID.BANK_TAB_1,net.runelite.api.gameval.VarbitID.BANK_TAB_2,net.runelite.api.gameval.VarbitID.BANK_TAB_3,net.runelite.api.gameval.VarbitID.BANK_TAB_4,net.runelite.api.gameval.VarbitID.BANK_TAB_5,net.runelite.api.gameval.VarbitID.BANK_TAB_6,net.runelite.api.gameval.VarbitID.BANK_TAB_7,net.runelite.api.gameval.VarbitID.BANK_TAB_8,net.runelite.api.gameval.VarbitID.BANK_TAB_9};
				net.runelite.api.ItemContainer bank=client.getItemContainer(net.runelite.api.gameval.InventoryID.BANK);net.runelite.api.Item[] slots=bank==null?null:bank.getItems();
				int[] sizes=new int[counts.length];for(int i=0;i<counts.length;i++){sizes[i]=client.getVarbitValue(counts[i]);}items=tabItems(tab,sizes,slots);
			}
		}
		Set<Integer> next=new HashSet<>(eligible);if(items!=null){next.retainAll(items);}membership=Collections.unmodifiableSet(next);tabId=tab;tabDirty=false;
	}
	static Set<Integer> tabItems(int tab,int[] sizes,net.runelite.api.Item[] slots)
	{
		if(slots==null||sizes==null||tab<1||tab>sizes.length){throw new IllegalStateException("Native bank tab data unavailable");}
		int start=0;for(int i=0;i<tab-1;i++){if(sizes[i]<0){throw new IllegalStateException("Invalid native tab size");}start=Math.addExact(start,sizes[i]);}int end=Math.addExact(start,sizes[tab-1]);
		if(end<start||end>slots.length){throw new IllegalStateException("Native tab range is incomplete");}Set<Integer> items=new HashSet<>();
		for(int i=start;i<end;i++){if(slots[i]==null){throw new IllegalStateException("Native tab slot is incomplete");}if(slots[i].getId()>=0){items.add(slots[i].getId());}}return items;
	}
	/** Official bank script callbacks: no registration, refresh, definitions, or IO in these callbacks. */
	public void script(String event,int[] stack,int size)
	{
		if(!wanted||faulted||!registered||!selected||access==null){return;}
		String active=access.active();if(!name.equals(active)&&!(nativeMode&&active==null)){return;}
		try
		{
			if("bankBuildTab".equals(event)&&size>=1){tabMembership();stack[size-1]=1;applied=membership;}
			else if("bankSearchFilter".equals(event)&&size>=2&&!contains(stack[size-1])){stack[size-2]=0;}
		}
		catch(RuntimeException ex){log.warn("From Scratch Virtual Bank tab filtering failed; returning to Full Bank.",ex);faulted=true;wanted=false;request();}
	}
	private void request()
	{
		if(queued||operating){return;}queued=true;later.accept(()->
		{
			queued=false;if(operating){reentry();return;}operating=true;
			try{reconcile();}
			catch(RuntimeException ex){failed(ex);}
			finally{operating=false;}
			// An explicit setting/stop received during a synchronous script must still take effect.
			if(!wanted&&registered&&!cleanupFailed){request();}
		});
	}
	private void reconcile()
	{
		if(cleanupFailed){return;}
		if(owner!=null&&!plugins.isPluginActive(owner)){faulted=true;wanted=false;detach();return;}
		if(access==null&&!attempted){attempted=true;connect();}
		if(access==null){if(wanted){faulted=true;wanted=false;log.debug("From Scratch Virtual Bank: Bank Tags unavailable; retaining the saved preference and leaving Full Bank usable");}return;}
		String active=access.active();
		// Cleanup is independent of preference/run readiness: core can restore a previous process's tag.
		if(owned(access,access.remembered())){access.remembered("");}
		if(!wanted||faulted){deactivate(access);return;}
		if(owned(access,active)&&(!name.equals(active)||!registered)){deactivate(access);if(!wanted){return;}active=access.active();}
		if(nativeMode&&active==null){if(!registered){registered=true;access.register(name,this::contains);}tabMembership();if(!membership.equals(applied)){applied=membership;access.refresh();}return;}
		if(!name.equals(active))
		{
			if(selected){selectFullBank();return;}if(suspended||!membershipReady||!access.ready()){return;}
			if(!registered){registered=true;log.debug("From Scratch Virtual Bank: registering");access.register(name,this::contains);}
			// A saved ON preference selects our view after run loading. A user changing tags
			// while our view was selected takes the exit path above, without a reopen loop.
			previousTitle=active==null?access.title():null;selected=true;applied=membership;
			tabMembership();applied=membership;
			log.debug("From Scratch Virtual Bank: opening");access.open(name);
			if(!name.equals(access.active())||!access.filtering()){throw new IllegalStateException("Bank Tags did not activate the requested virtual view");}
			// This runtime-only view must never be resurrected by core Remember last tag tab.
			if(name.equals(access.remembered())){access.remembered("");}log.debug("From Scratch Virtual Bank: open completed");
		}
		else if(!access.filtering()){throw new IllegalStateException("Bank Tags selected name has no active filter");}
		else{tabMembership();if(!membership.equals(applied)){applied=membership;log.debug("From Scratch Virtual Bank: refreshing membership");access.refresh();log.debug("From Scratch Virtual Bank: refresh completed");}}
	}
	private void selectFullBank()
	{
		boolean notify=enabled;enabled=false;wanted=false;suspended=true;selected=false;
		if(notify){log.debug("From Scratch Virtual Bank: native view selected; clearing the view preference");fullBankSelected.run();}
	}
	private void connect()
	{
		if(fixture!=null){attach(fixture);return;}
		if(plugins==null){return;}
		for(Plugin plugin:plugins.getPlugins())
		{
			if(plugin instanceof BankTagsPlugin&&plugins.isPluginActive(plugin))
			{
				BankTagsService service=(BankTagsService)plugin;TagManager tags=plugin.getInjector().getInstance(TagManager.class);BankSearch search=plugin.getInjector().getInstance(BankSearch.class);BankTagsConfig config=plugin.getInjector().getInstance(BankTagsConfig.class);net.runelite.client.config.ConfigManager configs=plugin.getInjector().getInstance(net.runelite.client.config.ConfigManager.class);owner=plugin;
				attach(new Access(){public void register(String n,BankTag tag){tags.registerTag(n,tag);}public void unregister(String n){tags.unregisterTag(n);}public boolean ready(){net.runelite.api.widgets.Widget w=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS);return w!=null&&!w.isHidden()&&w.getOnInvTransmitListener()!=null;}public void allItems(){client.setVarbit(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB,0);}public void open(String n){service.openBankTag(n,BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME);}public void close(){service.closeBankTag();}public String active(){return service.getActiveTag();}public boolean filtering(){return service.getActiveBankTag()!=null;}public String remembered(){return config.tab();}public void remembered(String n){config.tab(n);}public boolean savedTag(String n){return net.runelite.client.util.Text.fromCSV(java.util.Objects.toString(configs.getConfiguration(BankTagsPlugin.CONFIG_GROUP,BankTagsPlugin.TAG_TABS_CONFIG),"")).contains(n)||configs.getConfiguration(BankTagsPlugin.CONFIG_GROUP,BankTagsPlugin.TAG_LAYOUT_PREFIX+n)!=null||!tags.getItemsForTag(n).isEmpty();}public void refresh(){search.layoutBank();}public String title(){net.runelite.api.widgets.Widget w=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE);return w==null?null:w.getText();}public void title(String text){net.runelite.api.widgets.Widget w=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE);if(w!=null&&!text.equals(w.getText())){w.setText(text);}}});break;
			}
		}
	}
	private void attach(Access next){access=next;}
	/** Only exact legacy UUID keys with no user tag/tab/layout data qualify for migration cleanup. */
	private boolean owned(Access a,String tag){return tag!=null&&(name.equals(tag)||(tag.matches("from_scratch_virtual_[a-f0-9]{32}")&&!a.savedTag(tag)));}
	private void deactivate(Access old)
	{
		boolean unregister=registered,redraw=nativeMode;registered=false;selected=false;nativeMode=false;tabDirty=true;tabId=-1;applied=Collections.emptySet();String title=previousTitle;previousTitle=null;
		String active=old.active();
		try
		{
			if(owned(old,active))
			{
				old.close();
				if(owned(old,old.active())||(old.active()==null&&old.filtering())){throw new IllegalStateException("Bank Tags did not close the virtual view");}
				if(title!=null&&!title.contains("from_scratch_virtual_")&&old.active()==null){old.title(title);}
				if(!name.equals(active)){old.unregister(active);}
				log.debug("From Scratch Virtual Bank: close verified");
			}
			else if(redraw&&active==null){old.refresh();}
			if(owned(old,old.remembered())){old.remembered("");}
		}
		finally{if(unregister){old.unregister(name);}}
	}
	private void detach()
	{
		Access old=access;access=null;owner=null;attempted=false;
		if(old==null){return;}log.debug("From Scratch Virtual Bank: detaching");
		deactivate(old);
	}
	/** Client-thread setup gate; cannot approve a baseline while our restored filter remains selected. */
	public boolean fullBankForSetup()
	{
		if(operating){return false;}
		try
		{
			if(access==null&&!attempted){attempted=true;connect();}
			if(access==null){return !faulted;}
			boolean clear=!nativeMode&&!owned(access,access.active())&&!owned(access,access.remembered())&&!(access.active()==null&&access.filtering());
			if(!clear){wanted=false;request();}return clear;
		}
		catch(RuntimeException ex){failed(ex);return false;}
	}
	/** Optional filtering errors never escape into core publication or rewrite the user's preference. */
	public void failed(RuntimeException ex)
	{
		if(!faulted){log.warn("From Scratch Virtual Bank failed; leaving Full Bank usable and retaining the saved preference. Toggle the setting to retry.",ex);}
		faulted=true;wanted=false;previous=null;membershipReady=false;positives=Collections.emptySet();membership=Collections.emptySet();
		try{detach();}catch(RuntimeException cleanup){cleanupFailed=true;log.warn("From Scratch tag cleanup failed; select the native View all items control.",cleanup);}
	}
	public void title(java.awt.Color accent)
	{
		try{if(wanted&&!faulted&&registered&&selected&&access!=null&&((name.equals(access.active())&&access.filtering())||(nativeMode&&access.active()==null))){access.title(net.runelite.client.util.ColorUtil.wrapWithColorTag(TITLE,accent==null?new java.awt.Color(0x1DFF00):accent));}}
		catch(RuntimeException ex){failed(ex);}
	}
	private void reentry(){if(!recursionLogged){recursionLogged=true;log.debug("From Scratch Virtual Bank: ignored re-entrant bank lifecycle callback");}}
	public void bankClosed()
	{
		potionView=false;wanted=false;attempted=false;suspended=false;recursionLogged=false;if(operating){reentry();return;}request();
	}
	public void stop(){potionView=false;enabled=false;wanted=false;previous=null;membershipReady=false;positives=Collections.emptySet();membership=Collections.emptySet();placeholders.clear();request();}
}
