package com.gepriceoverlay;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
		name = "GE Price Overlay",
		description = "Displays GE price changes for bank tab items",
		tags = {"ge", "price", "overlay", "bank"}
)
public class GEPriceOverlayPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GEPriceOverlay overlay;

	@Inject
	private GEPriceOverlayConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	private GEPricePanel panel;
	private NavigationButton navButton;

	// null value = non-tradeable sentinel (don't re-request)
	private final Map<Integer, PriceData> itemPriceCache = new HashMap<>();

	@Provides
	GEPriceOverlayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GEPriceOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		panel = new GEPricePanel(itemManager, config);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("GE Price Changes")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		itemPriceCache.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("gepriceoverlay") && event.getKey().equals("timeFrame"))
		{
			itemPriceCache.clear();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK.getId())
		{
			return;
		}

		ItemContainer container = event.getItemContainer();
		if (container == null)
		{
			return;
		}

		for (var item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			int itemId = itemManager.canonicalize(item.getId());

			if (config.minimumValue() > 0 && itemManager.getItemPrice(itemId) < config.minimumValue())
			{
				continue;
			}

			if (!itemPriceCache.containsKey(itemId))
			{
				// put null as sentinel so we don't re-request non-tradeable items
				itemPriceCache.put(itemId, null);
				GEPriceFetcher.fetchPriceData(itemId, config.timeFrame()).thenAccept(data ->
				{
					if (data != null)
					{
						itemPriceCache.put(itemId, data);
					}
					panel.update(itemPriceCache);
				});
			}
		}

		panel.update(itemPriceCache);
	}

	public Map<Integer, PriceData> getItemPriceCache()
	{
		return itemPriceCache;
	}
}
