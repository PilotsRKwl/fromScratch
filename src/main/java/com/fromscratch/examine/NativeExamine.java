package com.fromscratch.examine;

import com.fromscratch.bank.*;
import com.fromscratch.FromScratchConfig;
import com.fromscratch.buyback.*;
import com.fromscratch.wealth.*;
import java.util.*;
import java.util.function.Supplier;
import javax.inject.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.*;
import net.runelite.client.util.Text;
import net.runelite.api.widgets.WidgetUtil;

/** Correlates native item responses only. No menu entries, consumption, or game actions. Client thread only. */
@Singleton
public final class NativeExamine
{
	/** Core Examine queues its price supplement at priority zero. */
	public static final float RESPONSE_PRIORITY=-1;
	interface Items { int widget(int component,int child); int normalize(int id); int highAlchemy(int id); String name(int id); }
	private final Items items;
	private final PriceService prices;
	private final java.util.function.Consumer<String> output;
	private final Supplier<java.awt.Color> color;
	private final Supplier<java.awt.Color> negativeColor;
	private Supplier<BankRun> current = () -> null;
	private final java.util.Deque<Request> pending = new ArrayDeque<>();
	@Inject public NativeExamine(Client client, PriceService prices, ChatMessageManager chat,FromScratchConfig config)
	{
		this(new Items()
		{
			public int widget(int component,int child) { return widgetItem(client,component,child); }
			public int normalize(int id)
			{
				ItemComposition item=client.getItemDefinition(id);
				if(item.getPlaceholderTemplateId()!=-1) { id=item.getPlaceholderId();item=client.getItemDefinition(id); }
				return item.getNote()!=-1?item.getLinkedNoteId():id;
			}
			public int highAlchemy(int id) { return client.getItemDefinition(id).getHaPrice(); }
			public String name(int id) { return client.getItemDefinition(id).getMembersName(); }
		},prices,message->chat.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build()),config::virtualQuantityColor,config::negativeVirtualQuantityColor);
	}
	NativeExamine(Items items,PriceService prices,java.util.function.Consumer<String> output,Supplier<java.awt.Color> color)
	{ this(items,prices,output,color,()->com.fromscratch.overlay.BankOverlayState.DEFAULT_NEGATIVE_COLOR); }
	NativeExamine(Items items,PriceService prices,java.util.function.Consumer<String> output,Supplier<java.awt.Color> color,Supplier<java.awt.Color> negativeColor)
	{ this.items=items;this.prices=prices;this.output=output;this.color=color;this.negativeColor=negativeColor; }
	public void start(Supplier<BankRun> current) { clear(); this.current=current; }
	public void stop() { clear(); current=()->null; }
	public void clear() { pending.clear(); }
	public void clicked(MenuOptionClicked event)
	{
		requested(event.getMenuOption(),event.getMenuAction(),event.getId(),event.getParam1(),event.getParam0());
	}
	void requested(String option,MenuAction action,int itemId,int component,int child)
	{
		if (!"Examine".equals(option)) { return; }
		int id;
		switch(action)
		{
			case EXAMINE_ITEM_GROUND: id=itemId; break;
			case CC_OP_LOW_PRIORITY: id=items.widget(component,child); break;
			default: return;
		}
		BankRun run=current.get();
		// Unknown item contexts consume a response too, so a later known request cannot steal it.
		remember(id,run,System.nanoTime());
	}
	void remember(int id,BankRun run,long now)
	{
		if (pending.size()>=32) { clear(); id=-1; }
		pending.addLast(new Request(id,run==null?null:run.getRunId(),run==null?null:run.getCharacter(),now));
	}
	public void response(ChatMessage event)
	{
		if (event.getType()!=ChatMessageType.ITEM_EXAMINE) { return; }
		// RuneLite's own optional GE/HA line uses ITEM_EXAMINE as well. It is not a native response.
		if (Text.removeTags(event.getMessage()).startsWith("Price of ")) { return; }
		int id=take(current.get(),System.nanoTime());
		if(id<0) { return; }
		try
		{
			id=items.normalize(id);
			final int normalized=id;
			prices.register(Collections.singleton(id),i->normalized);
			BankRun run=current.get(); if(run==null) { return; }
			long quantity=BuybackEngine.balances(run).getOrDefault(id,0L);
			// GAMEMESSAGE prevents recursion. Queueing leaves the already received native line untouched.
			output.accept(ExamineFormatter.format(run,id,items.name(id),quantity,prices.snapshot(Collections.singleton(id)).get(id),items.highAlchemy(id),color.get(),negativeColor.get()));
		}
		catch(RuntimeException ex) { /* Missing definitions are an unsupported context, never a guessed item. */ }
	}
	int take(BankRun run,long now)
	{
		Request request=pending.pollFirst();
		if(request==null) { return -1; }
		if(now-request.at>10_000_000_000L) { clear(); return -1; }
		return run!=null&&Objects.equals(run.getRunId(),request.run)&&Objects.equals(run.getCharacter(),request.character)?request.id:-1;
	}
	private static int widgetItem(Client client,int component,int index)
	{
		Widget widget=client.getWidget(component); if(widget==null) { return -1; }
		int group=WidgetUtil.componentToInterface(component);
		Widget child=widget.getChild(group==InterfaceID.WORNITEMS?1:group==InterfaceID.SMITHING?2:index);
		return child==null?-1:child.getItemId();
	}
	private static final class Request
	{
		final int id; final UUID run; final String character; final long at;
		Request(int id,UUID run,String character,long at) { this.id=id;this.run=run;this.character=character;this.at=at; }
	}
}
