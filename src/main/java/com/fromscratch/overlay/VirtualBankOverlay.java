package com.fromscratch.overlay;

import com.fromscratch.bank.RuneLiteBankSource;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Normal-bank layer; item quantities/geometry stay native, only the counter display mode is scoped. */
@Singleton @Slf4j
public final class VirtualBankOverlay extends Overlay
{
	private final Context context;
	private final BankOverlayState state;
	private final Map<Integer, ItemIdentity> identities = new ConcurrentHashMap<>();
	private BankOverlayState.Projection frame;
	private BankOverlayState.Settings settings;
	private boolean physicalFailed;

	@Inject
	public VirtualBankOverlay(Client client, RuneLiteBankSource observer, BankOverlayState state, OverlayManager overlayManager,net.runelite.client.ui.overlay.tooltip.TooltipManager tooltips)
	{
		this(new Context()
		{
			private PhysicalCounter[] counters = new PhysicalCounter[0];
			private final Map<Integer,Boolean> stackable = new HashMap<>();
			private final Map<Integer,String> names=new HashMap<>();
			public void hint(Rectangle bounds,int id,int physical,long available)
			{
				net.runelite.api.Point mouse=client.getMouseCanvasPosition();if(mouse==null||!bounds.contains(mouse.getX(),mouse.getY())||available==0||physical<=available){return;}
				String name=names.computeIfAbsent(id,item->client.getItemDefinition(item).getMembersName().replace("<","").replace(">","")).replace('|','/');
				tooltips.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(String.format(java.util.Locale.ENGLISH,"%s: %,d in bank. %s available in From Scratch. %,d legacy.<br>Withdraw All withdraws all %,d.",name,physical,net.runelite.client.util.ColorUtil.wrapWithColorTag(String.format(java.util.Locale.ENGLISH,"%,d",available),state.getSettings().quantityColor(available)),physical-Math.max(0,available),physical)));
			}
			public void physicalQuantities(boolean enabled)
			{
				Widget layer = enabled ? client.getWidget(InterfaceID.Bankmain.ITEMS) : null;
				Widget[] children = layer == null ? null : layer.getChildren();
				if (children == null || counters.length != children.length)
				{
					for (PhysicalCounter counter : counters) { if (counter != null) { counter.restore(); } }
					if (children == null) { if (counters.length > 0) { counters = new PhysicalCounter[0]; } stackable.clear(); return; }
					counters = new PhysicalCounter[children.length];
				}
				Widget dragging = client.isDraggingWidget() ? client.getDraggedWidget() : null;
				for (int i = 0; i < children.length; i++)
				{
					Widget child = children[i]; PhysicalCounter counter = counters[i];
					if (counter != null && counter.widget != child) { counter.restore(); counters[i] = counter = null; }
					if (child == null || child == dragging || child.getId() != InterfaceID.Bankmain.ITEMS || child.getParent() != layer
						|| child.getType() != WidgetType.GRAPHIC || child.isHidden() || !BankItemPainter.eligible(child.getId(), child.getItemId(), child.getItemQuantity()))
					{ if (counter != null) { counter.restore(); counters[i] = null; } continue; }
					int mode = child.getItemQuantityMode();
					if (mode == ItemQuantityMode.NEVER && counter == null) { continue; } // Respect another plugin's hidden counter.
					int original = mode == ItemQuantityMode.NEVER ? counter.mode : mode;
					boolean show = original == ItemQuantityMode.ALWAYS || original == ItemQuantityMode.STACKABLE
						&& (child.getItemQuantity() != 1 || stackable.computeIfAbsent(child.getItemId(), id -> client.getItemDefinition(id).isStackable()));
					if (!show) { if (counter != null) { counter.restore(); counters[i] = null; } continue; }
					if (counter == null) { counter = counters[i] = new PhysicalCounter(child, original); }
					counter.mode = original; counter.quantity(child.getItemQuantity());
					if (mode != ItemQuantityMode.NEVER) { child.setItemQuantityMode(ItemQuantityMode.NEVER); }
				}
			}
			public String characterKey() { return observer.characterKey(); }
			public boolean bankVisible()
			{
				if (client.getGameState() != GameState.LOGGED_IN) { return false; }
				Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
				if (items == null || items.isHidden() || client.getComponentTable() == null) { return false; }
				boolean bank = false;
				for (WidgetNode node : client.getComponentTable())
				{
					if (node.getId() == InterfaceID.BANKPIN_KEYPAD || node.getId() == InterfaceID.BANKPIN_SETTINGS) { return false; }
					if (node.getId() == InterfaceID.BANKMAIN) { bank = true; }
				}
				return bank;
			}
			public boolean placeholder(int id) { return client.getItemDefinition(id).getPlaceholderTemplateId() != -1; }
			public int balanceItemId(int id)
			{
				ItemComposition item = client.getItemDefinition(id);
				// getPlaceholderId is bidirectional. Only a placeholder template links back
				// to the physical item; normal variants must keep their exact identity.
				return item.getPlaceholderTemplateId() != -1 ? item.getPlaceholderId() : id;
			}
			public Rectangle bankViewport()
			{
				Widget layer = client.getWidget(InterfaceID.Bankmain.ITEMS);
				Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
				if (layer == null || layer.isHidden() || container == null || container.isHidden()) { return null; }
				// ITEMS is the scrolling content viewport, not the surrounding tab/search area.
				return layer.getBounds().intersection(container.getBounds());
			}
			public void forEachItem(Rectangle viewport, ItemConsumer consumer)
			{
				Widget layer = client.getWidget(InterfaceID.Bankmain.ITEMS);
				if (layer == null || layer.isHidden()) { return; }
				Widget[] children = layer.getChildren();
				if (children == null) { return; }
				WidgetItem dragged = null;
				Widget draggingWidget = client.isDraggingWidget() ? client.getDraggedWidget() : null;
				for (WidgetItem item : overlayManager.getWidgetItems())
				{
					if (item.getWidget() != null && item.getWidget().getId() == InterfaceID.Bankmain.ITEMS
						&& item.getDraggingCanvasBounds() != null) { dragged = item; break; }
				}
				// The 1.13.3 callback list requires an item's origin inside the clip and omits
				// a partially visible top row. Read the real item widgets instead; their canvas
				// bounds are already updated by this draw hook, including the omitted row.
				// No bank-container indices, fixed grid positions or new visual slots are used.
				for (int i = 0; i < children.length; i++)
				{
					Widget child = children[i];
					if (child == null || child.getId() != InterfaceID.Bankmain.ITEMS || child.getParent() != layer || child.getType() != WidgetType.GRAPHIC
						|| child.isHidden() || !BankItemPainter.eligible(child.getId(), child.getItemId(), child.getItemQuantity())) { continue; }
					// A clipped-origin drag can also be absent from RuneLite's list. Its drag
					// offset is not exposed by Widget: omit that transient effect rather than
					// paint a ghost at its original slot or guess the mouse-to-item offset.
					if (child == draggingWidget && dragged == null) { continue; }
					Rectangle bounds = dragged != null && dragged.getWidget() == child ? dragged.getCanvasBounds() : child.getBounds();
					PhysicalCounter counter = i < counters.length ? counters[i] : null;
					String physical = counter != null && counter.widget == child && child.getItemQuantityMode() == ItemQuantityMode.NEVER ? counter.label : null;
					if (viewport.intersects(bounds)) { consumer.accept(child.getItemId(), child.getItemQuantity(), bounds, physical); }
				}
			}
		}, state);
	}
	/** Before the native item sprite is drawn: suppress its baked-in counter, not its item/quantity. */
	public void preparePhysicalQuantities()
	{
		if (physicalFailed) { return; }
		BankOverlayState.Projection projection = state.getProjection(); BankOverlayState.Settings options = state.getSettings();
		try { context.physicalQuantities(state.isEnabled() && projection != null && (options.isQuantities() || options.isDimLocked())
			&& context.bankVisible() && projection.getCharacterKey().equals(context.characterKey())); }
		catch (RuntimeException ex)
		{
			physicalFailed = true;
			try { context.physicalQuantities(false); } catch (RuntimeException cleanup) { ex.addSuppressed(cleanup); }
			log.warn("From Scratch physical counter styling failed; retaining native quantity rendering.", ex);
		}
	}

	VirtualBankOverlay(Context context, BankOverlayState state)
	{
		this.context = context;
		this.state = state;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.MANUAL);
		// showOnBank also includes shared group storage. Only the normal bank is in scope.
		drawAfterLayer(InterfaceID.Bankmain.ITEMS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		frame = state.getProjection();
		settings = state.getSettings();
		if (!state.isEnabled() || frame == null || (!settings.isQuantities() && !settings.isDimLocked())
			|| !context.bankVisible() || !frame.getCharacterKey().equals(context.characterKey()))
		{
			frame = null;
			return null;
		}
		Rectangle viewport = context.bankViewport();
		if (viewport == null || viewport.isEmpty()) { frame = null; return null; }
		Shape originalClip = graphics.getClip();
		try
		{
			graphics.clip(viewport);
			context.forEachItem(viewport, (itemId, physicalQuantity, bounds, physical) -> renderItem(graphics, viewport, itemId, physicalQuantity, bounds, physical));
			com.fromscratch.bank.BankTimings.mark(frame.getTrace(), com.fromscratch.bank.BankTimings.Phase.OVERLAY);
			return null;
		}
		finally { graphics.setClip(originalClip); frame = null; }
	}

	private void renderItem(Graphics2D graphics, Rectangle viewport, int itemId, int physicalQuantity, Rectangle bounds, String physical)
	{
		if (frame == null || !state.isEnabled() || bounds == null || !viewport.intersects(bounds)
			|| !graphics.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)
			|| !BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, itemId, physicalQuantity)) { return; }
		ItemIdentity identity = identities.computeIfAbsent(itemId, id -> new ItemIdentity(context.balanceItemId(id), context.placeholder(id)));
		boolean placeholder = physicalQuantity == 0 || identity.placeholder;
		long quantity = frame.balance(identity.balanceItemId, placeholder);
		// Keep the full item rectangle for label placement. Only its pixels are clipped;
		// moving the label into the visible fragment would make it jump while scrolling.
		BankItemPainter.paint(graphics, bounds, quantity, quantity == 0 ? "0" : frame.label(identity.balanceItemId), settings);
		if (physical != null) { BankItemPainter.paintPhysical(graphics, bounds, physical); }
		if(!placeholder){context.hint(bounds,identity.balanceItemId,physicalQuantity,quantity);}
	}

	public void clearCache() { context.physicalQuantities(false); identities.clear(); physicalFailed = false; }

	/** At most one entry per current child slot; original modes survive rebuilds and restore on exit. */
	private static final class PhysicalCounter
	{
		private final Widget widget;
		private int mode, amount = -1;
		private String label;
		private PhysicalCounter(Widget widget, int mode) { this.widget = widget; this.mode = mode; }
		private void quantity(int next) { if (amount != next) { amount = next; label = BankItemPainter.physicalLabel(next); } }
		private void restore() { if (widget.getItemQuantityMode() == ItemQuantityMode.NEVER) { widget.setItemQuantityMode(mode); } }
	}

	/** Cached definition metadata only. A placeholder never creates an item or changes bank accounting. */
	private static final class ItemIdentity
	{
		private final int balanceItemId;
		private final boolean placeholder;
		private ItemIdentity(int balanceItemId, boolean placeholder) { this.balanceItemId = balanceItemId; this.placeholder = placeholder; }
	}

	interface Context
	{
		default void physicalQuantities(boolean enabled) { }
		default void hint(Rectangle bounds,int id,int physical,long available) { }
		String characterKey();
		boolean bankVisible();
		boolean placeholder(int itemId);
		default int balanceItemId(int itemId) { return itemId; }
		Rectangle bankViewport();
		void forEachItem(Rectangle viewport, ItemConsumer consumer);
	}

	/** Synchronous client-thread visitor; no per-item DTO or per-frame ownership map is needed. */
	interface ItemConsumer
	{
		void accept(int itemId, int physicalQuantity, Rectangle bounds, String physicalLabel);
		default void accept(int itemId, int physicalQuantity, Rectangle bounds) { accept(itemId, physicalQuantity, bounds, null); }
	}
}
