package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

/** On-request native contents proof. Missing containers/widgets never imply an empty bag. */
public final class LootingBagReader
{
	public interface Access { boolean live(); boolean visible(); Item[] contents(); Item[] displayed(); String name(int id); default boolean emptyMessage(){return false;} }
	private final Access access;
	public LootingBagReader(Client client)
	{
		this(new Access(){public boolean live(){return client.isClientThread()&&client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null;}
			private boolean shown(int id){Widget w=client.getWidget(id);return w!=null&&!w.isHidden();}
			private boolean mounted(int group){HashTable<WidgetNode> table=client.getComponentTable();if(table!=null){for(WidgetNode node:table){if(node.getId()==group){return true;}}}return false;}
			private boolean bankSide(){return mounted(InterfaceID.BANKSIDE)&&shown(InterfaceID.Bankside.LOOTINGBAG_CONTAINER);}
			private Widget items(){return client.getWidget(bankSide()?InterfaceID.Bankside.LOOTINGBAG_ITEMS:InterfaceID.WildernessLootingbag.ITEMS);}
			public boolean visible(){return bankSide()||mounted(InterfaceID.WILDERNESS_LOOTINGBAG)&&shown(InterfaceID.WildernessLootingbag.UNIVERSE);}
			public Item[] contents(){ItemContainer c=client.getItemContainer(InventoryID.LOOTING_BAG);return c==null?null:c.getItems();}
			public Item[] displayed(){Widget w=items();if(w==null||w.isHidden()){return null;}Widget[] children=w.getDynamicChildren();if(children==null||children.length==0){children=w.getStaticChildren();}if(children==null){return null;}Item[] items=new Item[children.length];for(int i=0;i<items.length;i++){items[i]=children[i]==null?new Item(-1,0):new Item(children[i].getItemId(),children[i].getItemQuantity());}return items;}
			public boolean emptyMessage(){return bankSide()&&emptyText(client.getWidget(InterfaceID.Bankside.LOOTINGBAG_CONTAINER_TEXT1))||emptyText(items());}
			private boolean emptyText(Widget w){if(w==null||w.isHidden()){return false;}if(isEmptyMessage(w.getText())){return true;}for(Widget[] children:new Widget[][]{w.getDynamicChildren(),w.getStaticChildren()}){if(children!=null){for(Widget child:children){if(child!=null&&!child.isHidden()&&isEmptyMessage(child.getText())){return true;}}}}return false;}
			public String name(int id){return client.getItemDefinition(id).getName();}});
	}
	public LootingBagReader(Access access){this.access=access;}
	public BagObservation read(BankObservation bank,UUID serverSession)
	{
		try
		{
			if(bank==null||!bank.isReady()||!bank.getSession().equals(serverSession)||!access.live()){return BagObservation.unknown();}
			long count=0;int bag=-1;for(int id:BuybackCatalog.variants("looting_bag_family").keySet()){long n=bank.getBank().quantity(id);count=Math.addExact(count,n);if(n>0){bag=id;}}
			if(count!=1){return BagObservation.unknown();}
			// A packet received for a banked bag is fresh without an inventory-side panel.
			Item[] contents=access.contents();if(contents==null||contents.length>28){return BagObservation.unknown();}
			return observation(bag,aggregate(contents));
		}
		catch(RuntimeException ex){return BagObservation.unknown();}
	}
	/** A native View request identifies the bag. Its inspection may precede returning the bag to the bank. */
	public BagObservation inspect(BankObservation bank,int requestedId,boolean currentPacket)
	{
		try
		{
			if(bank==null||!access.live()||!access.visible()||!BuybackCatalog.variants("looting_bag_family").containsKey(requestedId)){return BagObservation.unknown();}
			Item[] contents=access.contents(),display=access.displayed();
			Map<Integer,Long> shown=display==null?null:aggregate(display);
			// Native empty text is explicit evidence, never an inference from a missing container.
			if(contents==null||!currentPacket){return (shown==null||shown.isEmpty())&&access.emptyMessage()?observation(requestedId,Collections.emptyMap()):BagObservation.unknown();}
			if(contents.length>28){return BagObservation.unknown();}
			Map<Integer,Long> received=aggregate(contents);
			if(shown==null?!received.isEmpty():!shown.equals(received)){return BagObservation.unknown();}
			return observation(requestedId,received);
		}
		catch(RuntimeException ex){return BagObservation.unknown();}
	}
	public static boolean isEmptyMessage(String text)
	{
		if(text==null){return false;}
		String plain=net.runelite.client.util.Text.removeTags(text).replace('\u00a0',' ').trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
		return plain.equals("the bag is empty.")||plain.equals("your looting bag is empty.")||plain.equals("the looting bag is empty.")||plain.equals("your looting bag is currently empty.");
	}
	/** Short-lived proof scoped to this character/login-bank session, not a saved ownership ledger. */
	public static final class Session
	{
		private static final long REQUEST_TICKS=5;
		private final LootingBagReader reader;
		private final java.util.function.Supplier<Instant> clock;
		private String character;
		private UUID session;
		private long requestTick,packetTick;
		private int requested=-1;
		private Instant packetAt;
		private BagObservation captured;
		public Session(LootingBagReader reader){this(reader,Instant::now);}
		public Session(LootingBagReader reader,java.util.function.Supplier<Instant> clock){this.reader=reader;this.clock=clock;}
		public boolean active(){return requested>=0||packetAt!=null||captured!=null;}
		public boolean awaitingView(){return requested>=0;}
		public void clear(){character=null;session=null;invalidate();}
		public void invalidate(){requested=-1;packetAt=null;captured=null;}
		/** Closing the bank discards passive packet freshness, but not an explicit recent View. */
		public void bankClosed(){packetAt=null;}
		private boolean scope(String key,BankObservation bank)
		{
			if(key==null||bank==null){clear();return false;}
			if(!key.equals(character)||!bank.getSession().equals(session)){clear();character=key;session=bank.getSession();}
			return true;
		}
		public void request(String key,BankObservation bank,int id,long tick)
		{
			if(!scope(key,bank)||!BuybackCatalog.variants("looting_bag_family").containsKey(id)){return;}
			captured=null;packetAt=null;requested=id;requestTick=tick;
		}
		public void changed(String key,BankObservation bank,long tick,boolean bankOpen)
		{
			if(!scope(key,bank)){return;}
			// An identical repeat/late packet must not discard a valid View. A genuinely
			// different complete packet invalidates it before preview/confirmation.
			if(captured!=null)
			{
				try{Item[] items=reader.access.contents();if(items!=null&&(items.length>28||!aggregate(items).equals(captured.getQuantities()))){captured=null;}}
				catch(RuntimeException ex){captured=null;}
			}
			packetAt=bankOpen||requested>=0?clock.get():null;packetTick=tick;
		}
		public void message(String key,BankObservation bank,long tick,ChatMessageType type,String text)
		{
			if(scope(key,bank)&&(type==ChatMessageType.GAMEMESSAGE||type==ChatMessageType.SPAM)&&requested>=0&&tick>=requestTick&&tick<=requestTick+REQUEST_TICKS&&isEmptyMessage(text))
			{
				// Even a native empty response must not override a conflicting current packet.
				if(packetAt!=null&&packetTick>=requestTick)
				{
					try{Item[] items=reader.access.contents();if(items!=null&&(items.length>28||!aggregate(items).isEmpty())){return;}}
					catch(RuntimeException ex){return;}
				}
				captured=new BagObservation(requested,Collections.emptyMap(),Collections.emptyMap(),clock.get());requested=-1;packetAt=null;
			}
		}
		public void tick(String key,BankObservation bank,long tick,boolean bankOpen)
		{
			if(!scope(key,bank)){return;}
			if(captured!=null&&!captured.fresh(clock.get())){captured=null;}
			if(requested>=0&&tick>requestTick)
			{
				if(tick>requestTick+REQUEST_TICKS){requested=-1;}
				else
				{
					BagObservation proof=reader.inspect(bank,requested,packetAt!=null&&packetTick>=requestTick);
					if(proof.fresh(clock.get())){captured=proof;requested=-1;}
				}
			}
			if(!bankOpen&&requested<0){bankClosed();}
		}
		public BagObservation read(String key,BankObservation bank,long tick)
		{
			if(!scope(key,bank)){return BagObservation.unknown();}
			tick(key,bank,tick,true);
			if(captured!=null&&captured.fresh(clock.get())&&bank.isReady())
			{
				long count=0;int form=-1;
				for(int id:BuybackCatalog.variants("looting_bag_family").keySet()){long n=bank.getBank().quantity(id);count=Math.addExact(count,n);if(n>0){form=id;}}
				if(count==1){return new BagObservation(form,captured.getQuantities(),captured.getNames(),captured.getAt());}
			}
			Instant now=clock.get();return packetAt!=null&&!packetAt.isAfter(now)&&now.isBefore(packetAt.plusSeconds(120))?reader.read(bank,session):BagObservation.unknown();
		}
	}
	private BagObservation observation(int id,Map<Integer,Long> quantities)
	{Map<Integer,String> names=new TreeMap<>();quantities.keySet().forEach(item->names.put(item,access.name(item)));return new BagObservation(id,quantities,names,Instant.now());}
	private static Map<Integer,Long> aggregate(Item[] items)
	{
		Map<Integer,Long> q=new TreeMap<>();for(Item item:items){if(item==null){throw new IllegalArgumentException("Incomplete contents");}if(item.getId()<0){continue;}if(item.getQuantity()<=0){throw new IllegalArgumentException("Incomplete quantity");}q.merge(item.getId(),(long)item.getQuantity(),Math::addExact);}return BagObservation.quantities(q);
	}
}
