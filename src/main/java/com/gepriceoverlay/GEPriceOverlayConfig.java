package com.gepriceoverlay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("gepriceoverlay")
public interface GEPriceOverlayConfig extends Config
{
	@ConfigItem(
			keyName = "timeFrame",
			name = "Time Frame",
			description = "Time frame for price comparison"
	)
	default TimeFrame timeFrame()
	{
		return TimeFrame.DAY;
	}

	@ConfigItem(
			keyName = "minimumValue",
			name = "Minimum item value",
			description = "Hide price changes for items worth less than this amount (0 = show all)"
	)
	default int minimumValue()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "displayMode",
			name = "Display mode",
			description = "Show price change as a percentage or gold value"
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.PERCENTAGE;
	}

	enum TimeFrame
	{
		DAY,
		WEEK,
		MONTH,
		YEAR
	}

	enum DisplayMode
	{
		PERCENTAGE,
		MONEY
	}
}
