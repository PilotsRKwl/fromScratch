package com.fromscratch;

import com.fromscratch.bank.BankController;
import com.fromscratch.bank.RuneLiteBankSource;
import com.fromscratch.overlay.BankOverlayRegistration;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.bank.BankSidebar;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Wires lifecycle and supported observations to local snapshot setup. It never
 * sends game actions; storage and UI remain outside the client observation layer.
 */
@Slf4j
@PluginDescriptor(
	name = "From Scratch",
	internalName = "from_scratch",
	configName = FromScratchConfig.GROUP,
	description = "Restart item progression on your existing character with a Virtual Bank and legacy gear buybacks.",
	tags = {"bank", "virtual", "progression", "challenge", "fresh start", "legacy", "solo", "friends", "wealth"}
)
public class FromScratchPlugin extends Plugin
{
	@Inject
	private BankSidebar sidebar;

	@Inject
	private FromScratchConfig config;

	@Inject
	private BankController setupController;

	@Inject
	private RuneLiteBankSource observer;

	@Inject
	private ClientThread clientThread;
	@Inject private BankOverlayRegistration bankOverlay;
	@Inject private BankOverlayState bankOverlayState;
	@Inject private com.fromscratch.wealth.PriceService prices;
	@Inject private com.fromscratch.ge.RuneLiteGeSource exchangeObserver;
	@Inject private com.fromscratch.examine.NativeExamine examine;
	@Inject private com.fromscratch.bank.VirtualBankMode virtualBank;
	@Inject private com.fromscratch.playtime.PlaytimeTracker playtime;
	@Inject private net.runelite.api.Client client;
	@Inject private ConfigManager configManager;
	@Inject private net.runelite.client.Notifier notifier;
	@Inject private net.runelite.client.chat.ChatMessageManager chat;

	@Override
	protected void startUp()
	{
		bankOverlayState.configure(config);
		sidebar.negativeColor(config.negativeVirtualQuantityColor());
		observer.configure(config.developmentItemTrace());
		bankOverlay.start();
		prices.start();
		// Queue UI work without blocking the client thread. Lifecycle operations
		// share the EDT queue, so rapid enable/disable sequences retain their order.
		clientThread.invokeLater(() ->
		{
			observer.resetSession();exchangeObserver.resetSession();
			virtualBank.setEnabled(config.virtualBankView());
			virtualBank.fullBankSelected(()->SwingUtilities.invokeLater(()->configManager.setConfiguration(FromScratchConfig.GROUP,"virtualBankView",false)));
			setupController.informationActions(this::updateVirtualBank);
			setupController.initialBankView(virtualBank::fullBankForSetup);
			setupController.overdrafts(warnings->{for(com.fromscratch.bank.Overdrafts.Warning warning:warnings){String text=warning.message();chat.queue(net.runelite.client.chat.QueuedMessage.builder().type(net.runelite.api.ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(net.runelite.client.util.ColorUtil.wrapWithColorTag("From Scratch: LEGACY ITEMS OVERDRAWN",java.awt.Color.RED)+" "+text).build());try{notifier.notify("From Scratch: LEGACY ITEMS OVERDRAWN. "+text);}catch(RuntimeException ex){log.debug("From Scratch notification delivery failed",ex);}}});
		});
		setupController.start(this::getPluginDirectory);
		clientThread.invokeLater(()->
		{
			examine.start(setupController::currentRun);
			playtime.start(this::getPluginDirectory);
			setupController.restoreLifecycle(playtime::beforeRestore,playtime::afterRestore);
			setupController.exportLifecycle(playtime::checkpoint);
		});
		SwingUtilities.invokeLater(() ->
		{
			sidebar.bind(setupController);
			sidebar.show();
		});
		log.debug("From Scratch started");
	}

	@Override
	protected void shutDown()
	{
		bankOverlay.stop();
		setupController.stop();
		prices.stop();
		clientThread.invokeLater(()->{examine.stop();virtualBank.stop();playtime.stop();});
		clientThread.invokeLater(() -> {observer.resetSession();exchangeObserver.resetSession();});
		SwingUtilities.invokeLater(sidebar::hide);
		log.debug("From Scratch stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		flushExchangeObservations();
		observer.gameTick();
		setupController.tick();
		playtime.tick(setupController.currentRun(),client.getGameState()==GameState.LOGGED_IN,client.getTickCount());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if(state!=GameState.LOGGED_IN&&state!=GameState.LOADING){virtualBank.update(null,false);}
		if(state!=GameState.LOGGED_IN){observer.bagUnavailable();observer.storageUnavailable();}
		examine.clear();
		if(state!=GameState.LOGGED_IN) { playtime.interrupted(); }
		if(state==GameState.LOGGING_IN||state==GameState.HOPPING||state==GameState.CONNECTION_LOST||state==GameState.LOGIN_SCREEN){exchangeObserver.resetSession();}
		// Region loading and a normal world hop do not establish a new host login.
		// The renderer already hides itself outside LOGGED_IN. Clearing a valid
		// projection here could strand it when unchanged state is restored.
		if (state != GameState.LOGGED_IN && state != GameState.LOADING && state != GameState.HOPPING)
		{
			bankOverlayState.clear();
			observer.resetSession();
			setupController.tick();
		}
		else if (state == GameState.LOGGED_IN) { setupController.tick(); }
	}
	@Subscribe public void onMenuOptionClicked(MenuOptionClicked event)
	{
		examine.clicked(event);observer.bagClicked(event);
		if(event.getParam1()==InterfaceID.Bankmain.POTIONSTORE_BUTTON){virtualBank.potionStorageSelected();return;}
		// invokeLater defers reconciliation until core Bank Tags has handled this same click.
		if(event.getMenuOption().startsWith("View tab")||event.getMenuOption().equals("View all items")){virtualBank.nativeTabSelected();}else{virtualBank.viewChanged();}
	}
	@Subscribe public void onBeforeRender(net.runelite.api.events.BeforeRender event) { bankOverlay.beforeRender(); }
	@Subscribe(priority=-1) public void onScriptCallbackEvent(net.runelite.api.events.ScriptCallbackEvent event)
	{String name=event.getEventName();if("bankBuildTab".equals(name)||"bankSearchFilter".equals(name)){virtualBank.script(name,client.getIntStack(),client.getIntStackSize());}}
	@Subscribe(priority=1) public void onScriptPreFired(net.runelite.api.events.ScriptPreFired event)
	{if(event.getScriptId()==net.runelite.api.ScriptID.BANKMAIN_SEARCH_TOGGLE){virtualBank.searchSelected();}}
	@Subscribe(priority=-1) public void onScriptPostFired(net.runelite.api.events.ScriptPostFired event)
	{
		observer.potionScript(event.getScriptId());
		if(event.getScriptId()==net.runelite.api.ScriptID.BANKMAIN_FINISHBUILDING){virtualBank.bankBuilt();virtualBank.title(config.virtualQuantityColor());}
	}
	@Subscribe public void onVarbitChanged(net.runelite.api.events.VarbitChanged event){observer.potionVarp(event.getVarpId());}
	private void updateVirtualBank(com.fromscratch.bank.BankRun run)
	{
		try
		{
			boolean open=client.getGameState()==GameState.LOGGED_IN&&observer.bankOpen();
			if(run==null||!open){virtualBank.update(null,open);return;}
			virtualBank.update(run,true,observer.observe());
		}
		catch(RuntimeException ex){virtualBank.failed(ex);}
	}
	// Core Examine runs at priority 0 and queues its price line first; queue our line afterward.
	@Subscribe(priority=com.fromscratch.examine.NativeExamine.RESPONSE_PRIORITY) public void onChatMessage(ChatMessage event) { examine.response(event);observer.bagMessage(event); }
	@Subscribe public void onPluginChanged(net.runelite.client.events.PluginChanged event)
	{
		if(event.getPlugin() instanceof net.runelite.client.plugins.banktags.BankTagsPlugin)
		{ clientThread.invokeLater(()->{virtualBank.availabilityChanged();updateVirtualBank(setupController.currentRun());}); }
	}

	@Subscribe public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		exchangeObserver.changed(event.getSlot(),event.getOffer());
		// The API event already represents a completed offer update. Queue its value copy
		// immediately; no filesystem work runs here and logout need not wait for another tick.
		flushExchangeObservations();
	}
	private void flushExchangeObservations()
	{
		for(com.fromscratch.ge.GeObservation offer:exchangeObserver.drain()){setupController.exchange(offer);}
		if(exchangeObserver.takeObservationGap()){setupController.exchangeGap();}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Rendering revalidates character identity each frame. A same-character
		// configuration-profile change must not strand its valid projection.
		clientThread.invokeLater(setupController::tick);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		observer.storageChanged(id);
		if(id==InventoryID.LOOTING_BAG){observer.bagChanged();}
		if (id == InventoryID.BANK)
		{
			virtualBank.itemsChanged();
			observer.changed();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN || event.getGroupId() == InterfaceID.BANKPIN_KEYPAD
			|| event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			observer.changed();
		}
	}

	@Subscribe(priority=1)
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			if(event.isUnload()) { virtualBank.bankClosed(); observer.bagBankClosed();observer.potionClosed(); }
			observer.changed();
		}
		else if (event.getGroupId() == InterfaceID.BANKPIN_KEYPAD || event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			observer.changed();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (FromScratchConfig.GROUP.equals(event.getGroup()))
		{
			bankOverlayState.configure(config);
			sidebar.negativeColor(config.negativeVirtualQuantityColor());
			observer.configure(config.developmentItemTrace());
			clientThread.invokeLater(()->{virtualBank.setEnabled(config.virtualBankView());updateVirtualBank(setupController.currentRun());virtualBank.title(config.virtualQuantityColor());});
		}
	}

	@Provides
	FromScratchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FromScratchConfig.class);
	}
}
