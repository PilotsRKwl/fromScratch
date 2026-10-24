package com.fromscratch;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.api.gameval.ItemID;

/** Local presentation preferences; progression will be stored separately per character. */
@ConfigGroup(FromScratchConfig.GROUP)
public interface FromScratchConfig extends Config
{
	String GROUP = "fromscratch";

	@ConfigItem(
		keyName = "showFoundationHelp",
		name = "Legacy setup preference",
		description = "Retained for configuration compatibility; bank-only setup uses short instructions",
		hidden = true,
		position = 0
	)
	default boolean showFoundationHelp()
	{
		return true;
	}

	@ConfigItem(keyName = "bankVirtualQuantities", name = "Virtual bank quantities",
		description = "Show signed bank progress at bottom-right", position = 1)
	default boolean bankVirtualQuantities() { return true; }

	@ConfigItem(keyName = "lockedBankItems", name = "Dim locked bank items",
		description = "Dim items with zero or negative From Scratch bank balance; keep ordinary quantity labels visible", position = 2)
	default boolean lockedBankItems() { return true; }

	@ConfigItem(keyName = "virtualQuantityColor", name = "Virtual quantity color",
		description = "Color of virtual bank quantities and From Scratch Examine numbers", position = 3)
	default Color virtualQuantityColor() { return new Color(0x1DFF00); }

	@ConfigItem(keyName = "negativeVirtualQuantityColor", name = "Negative Virtual Quantity Color",
		description = "Color of negative virtual quantities and outstanding amounts", position = 4)
	default Color negativeVirtualQuantityColor() { return new Color(0xFF4040); }

	@ConfigItem(keyName = "lockedItemBackgroundColor", name = "Locked Item Background Color",
		description = "Neutral tint behind locked bank counters; independent of quantity text", position = 5)
	default Color lockedItemBackgroundColor() { return new Color(0x333333); }

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "lockedItemBackgroundOpacity", name = "Locked Item Background Opacity",
		description = "Locked bank tint opacity as a percentage", position = 6)
	default int lockedItemBackgroundOpacity() { return 50; }

	@ConfigItem(keyName = "virtualBankView", name = "Enable Virtual Bank View",
		description = "Show only virtually owned item types in your bank. Disable to view your full bank.", position = 0)
	default boolean virtualBankView() { return true; }

	@ConfigItem(keyName = "developmentItemTrace", name = "Development: bank diagnostics",
		description = "Retained developer DEBUG logging preference", hidden = true, position = 100)
	default boolean developmentItemTrace() { return false; }

	@ConfigItem(keyName = "developmentTraceItemId", name = "Development: traced item ID",
		description = "Retained historical preference; bank-only diagnostics do not log individual holdings", hidden = true, position = 101)
	default int developmentTraceItemId() { return ItemID.WOODEN_SHIELD; }
}
