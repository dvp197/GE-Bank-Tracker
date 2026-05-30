package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import com.gepriceoverlay.GEPriceOverlayPlugin;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GEPriceOverlayPlugin.class);
		RuneLite.main(args);
	}
}
