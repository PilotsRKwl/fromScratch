package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.observation.CharacterIdentity;
import java.util.*;
import javax.inject.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.gameval.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.*;

/** Complete BANK and verified Potion Storage, with on-request pouch/bag proofs; no ownership inventory polling. */
@Singleton @Slf4j
public final class RuneLiteBankSource implements BankSource
{
	private final Client client;
	private final ConfigManager config;
	private final BankObserver observer;
	private final BankTimings timings;
	private final com.fromscratch.potion.RuneLitePotionSource potions;
	private String character;
	private long verifiedHash;
	private RuneScapeProfileType verifiedType;
	private String verifiedProfile;
	private volatile boolean diagnostics;
	private long lastDiagnostic;
	private final com.fromscratch.buyback.LootingBagReader.Session bagProof;
	@Inject public RuneLiteBankSource(Client client, ConfigManager config, BankTimings timings)
	{
		this.client = client; this.config = config; this.timings = timings;this.potions=new com.fromscratch.potion.RuneLitePotionSource(client);
		bagProof=new com.fromscratch.buyback.LootingBagReader.Session(new com.fromscratch.buyback.LootingBagReader(client));
		observer = new BankObserver(new BankObserver.Access()
		{
			// Both native views share BANKMAIN, but quantities come ONLY from InventoryID.BANK.
			public boolean normalBankOpen() { return bankOpen(); }
			public ContainerSnapshot readBank() { return read(); }
			public BankTimings.Trace trace() { return timings.current(); }
		});
	}
	public void resetSession() { observer.reset(); character = null; bagUnavailable();potions.reset(); }
	public void potionScript(int id){potions.script(id);}
	public void potionVarp(int id){potions.varp(id);}
	public void potionClosed(){potions.closed();}
	public void newPotionSetup(){potions.closed();}
	public boolean validPotionCapture(com.fromscratch.potion.PotionObservation observation){return potions.validCapture(observation);}
	public boolean potionReadCurrent(){return potions.current();}
	public com.fromscratch.potion.PotionObservation potions(){return potions.observe(characterKey(),bankOpen());}
	public boolean fullBankView(){return client.getGameState()==GameState.LOGGED_IN&&visible(InterfaceID.Bankmain.ITEMS_CONTAINER)&&!potionStorageOpen();}
	public boolean potionStorageOpen(){return client.getGameState()==GameState.LOGGED_IN&&visible(InterfaceID.Bankmain.POTIONSTORE_ITEMS)&&client.getVarbitValue(VarbitID.BANK_CURRENTTAB)==com.fromscratch.potion.RuneLitePotionSource.NATIVE_TAB;}
	public boolean potionStorageUnlocked(){return client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null&&client.getVarbitValue(VarbitID.MM_POTION_STORAGE_UNLOCKED)!=0;}
	public boolean potionStorageUnlockKnown(){return client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null&&client.getVarps()!=null&&bankOpen();}
	/** A server contents packet must belong to this bank session, never a retained old container. */
	public void bagChanged(){bagProof.changed(characterKey(),observer.observe(),client.getTickCount(),bankOpen());}
	public void bagUnavailable(){bagProof.clear();}
	public void bagBankClosed(){bagProof.bankClosed();}
	/** Explicit native View/Check only. No inventory container is read or tracked. */
	public void bagClicked(net.runelite.api.events.MenuOptionClicked event)
	{
		String option=net.runelite.client.util.Text.removeTags(event.getMenuOption()).trim();
		// Observe content-changing native choices only to discard old proof; never intercept them.
		if(event.getParam1()==InterfaceID.Bankside.LOOTINGBAG_BANKALL||event.getParam1()==InterfaceID.Bankside.LOOTINGBAG_ITEMS&&option.startsWith("Deposit")){bagProof.invalidate();return;}
		boolean view="View".equals(option)||"Check".equals(option);
		boolean contentsMayChange=option.startsWith("Empty")||option.startsWith("Destroy")||"Drop".equals(option)||"Use".equals(option)||("Open".equals(option)||"Close".equals(option))&&!bankOpen();
		if(!view&&!contentsMayChange){return;}
		MenuEntry entry=event.getMenuEntry();int id=entry==null?-1:entry.getItemId();
		if(!com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").containsKey(id))
		{Widget parent=client.getWidget(event.getParam1());Widget item=parent==null||event.getParam0()<0?null:parent.getChild(event.getParam0());if(item!=null){id=item.getItemId();}}
		if(com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").containsKey(id))
		{
			if(view){bagProof.request(characterKey(),observer.observe(),id,client.getTickCount());}
			else{bagProof.invalidate();}
		}
	}
	/** Some empty checks return a native message instead of opening a contents panel. */
	public void bagMessage(net.runelite.api.events.ChatMessage event)
	{
		if(bagProof.awaitingView()){bagProof.message(characterKey(),observer.observe(),client.getTickCount(),event.getType(),event.getMessage());}
	}
	public com.fromscratch.buyback.BagObservation bag()
	{
		String key=characterKey();return bagProof.read(key,observer.observe(),client.getTickCount());
	}
	/** Interface lifetime is distinct from a container awaiting its next complete tick observation. */
	public boolean bankOpen() { return mounted(InterfaceID.BANKMAIN) && (visible(InterfaceID.Bankmain.ITEMS_CONTAINER)||visible(InterfaceID.Bankmain.POTIONSTORE_ITEMS)) && !mounted(InterfaceID.BANKPIN_KEYPAD) && !mounted(InterfaceID.BANKPIN_SETTINGS); }
	public void gameTick() { observer.tick();if(bagProof.active()){String key=characterKey();bagProof.tick(key,observer.observe(),client.getTickCount(),bankOpen());} }
	public void changed() { timings.event(); observer.changed(); }
	public void configure(boolean enabled) { diagnostics = enabled; timings.configure(enabled); }
	public String characterKey()
	{
		if (!client.isClientThread()) { throw new IllegalStateException("Bank observations require the client thread"); }
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		long hash = loggedIn ? client.getAccountHash() : 0;
		RuneScapeProfileType type = loggedIn ? RuneScapeProfileType.getCurrent(client) : null;
		String profile = loggedIn ? config.getRSProfileKey() : null;
		// Rendering revalidates cheap identity fields, not profile lists or SHA hashes on each frame.
		if (character != null && loggedIn && hash == verifiedHash && type == verifiedType && Objects.equals(profile, verifiedProfile)) { return character; }
		String key = loggedIn ? CharacterIdentity.verify(true, hash, type, profile, config.getRSProfiles()).orElse(null) : null;
		if (!Objects.equals(key, character)) { observer.reset();potions.reset();bagProof.clear(); character = key; }
		verifiedHash = hash; verifiedType = type; verifiedProfile = profile;
		return key;
	}
	public BankObservation observe()
	{
		BankObservation result = observer.observe();
		long now = System.nanoTime();
		if (diagnostics && log.isDebugEnabled() && (lastDiagnostic == 0 || now - lastDiagnostic >= 5_000_000_000L))
		{ lastDiagnostic = now; log.debug("From Scratch bank: mounted={} ready={} types={} tick={} predicate={}", mounted(InterfaceID.BANKMAIN), result.isReady(), result.isReady() ? result.getBank().getQuantities().size() : -1, result.getTick(), result.getInstruction()); }
		return result;
	}
	public String itemName(int id) { try { return client.getItemDefinition(id).getName(); } catch (RuntimeException ex) { return "Item #" + id; } }
	public int priceId(int id)
	{
		ItemComposition item=client.getItemDefinition(id);
		return item.getNote()!=-1?item.getLinkedNoteId():id;
	}
	public boolean buybackDefinition(int id)
	{
		try
		{
			ItemComposition item=client.getItemDefinition(id);
			return item!=null&&((!item.isTradeable()&&!item.isGeTradeable())||com.fromscratch.buyback.SelectedBuybacks.tradeableException(id))&&item.getNote()==-1&&item.getPlaceholderTemplateId()==-1;
		}
		catch(RuntimeException ex){return false;}
	}
	public com.fromscratch.pouch.PouchObservation pouch(){return new com.fromscratch.pouch.RuneLitePouchReader(client).banked(observer.observe());}
	private boolean mounted(int group)
	{
		HashTable<WidgetNode> components = client.getComponentTable(); if (components == null) { return false; }
		for (WidgetNode node : components) { if (node.getId() == group) { return true; } } return false;
	}
	private boolean visible(int id) { Widget w = client.getWidget(id); return w != null && !w.isHidden(); }
	private ContainerSnapshot read()
	{
		ItemContainer container = client.getItemContainer(InventoryID.BANK);
		Item[] items = container == null ? null : container.getItems();
		if (items != null) { BankTimings.mark(timings.current(), BankTimings.Phase.CAPTURE); }
		ContainerSnapshot result = BankItems.aggregate(items, id -> client.getItemDefinition(id).getPlaceholderTemplateId() != -1);
		if (result != null) { BankTimings.mark(timings.current(), BankTimings.Phase.AGGREGATE); } return result;
	}
}
