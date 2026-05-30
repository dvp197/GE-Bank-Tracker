package com.gepriceoverlay;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "GE Price Overlay",
		description = "Displays GE price changes for bank tab items",
		tags = {"ge", "price", "overlay", "bank"}
)
public class GEPriceOverlayPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "gepriceoverlay";
	private static final String PINNED_KEY = "pinnedItems";

	@Inject
	private Client client;

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

	@Inject
	private ConfigManager configManager;

	private GEPricePanel panel;
	private NavigationButton navButton;

	private final Map<Integer, PriceData> itemPriceCache = new ConcurrentHashMap<>();
	private final Map<Integer, String> itemNameCache = new ConcurrentHashMap<>();
	private final Set<Integer> pinnedItems = new HashSet<>();
	// tracks items already requested so we don't fire duplicate fetches
	private final Set<Integer> requestedIds = ConcurrentHashMap.newKeySet();
	private ScheduledExecutorService executor;

	@Provides
	GEPriceOverlayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GEPriceOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		executor = Executors.newScheduledThreadPool(2);
		overlayManager.add(overlay);
		panel = new GEPricePanel(config);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("GE Price Changes")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		loadPinnedItems();
		fetchPinnedItems();
	}

	@Override
	protected void shutDown()
	{
		executor.shutdownNow();
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		itemPriceCache.clear();
		itemNameCache.clear();
		pinnedItems.clear();
		requestedIds.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(CONFIG_GROUP))
		{
			return;
		}
		if (event.getKey().equals("timeFrame") || event.getKey().equals("minimumValue"))
		{
			itemPriceCache.clear();
			requestedIds.clear();
			fetchPinnedItems();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Examine is always the last entry — use it as a trigger so we add Track/Untrack exactly once
		if (!event.getOption().equals("Examine")
			|| event.getActionParam1() != WidgetInfo.BANK_ITEM_CONTAINER.getId())
		{
			return;
		}

		int slotIdx = event.getActionParam0();
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null || slotIdx < 0 || slotIdx >= bank.getItems().length)
		{
			return;
		}

		int itemId = itemManager.canonicalize(bank.getItems()[slotIdx].getId());
		if (itemId <= 0)
		{
			return;
		}

		boolean pinned = pinnedItems.contains(itemId);
		client.createMenuEntry(-1)
			.setOption(pinned ? "Untrack" : "Track")
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(e -> togglePin(itemId));
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

		int total = 0, fetching = 0, cached = 0, skipped = 0, delay = 0;

		for (var item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			total++;
			int itemId = itemManager.canonicalize(item.getId());
			String name = itemNameCache.computeIfAbsent(itemId,
				id -> itemManager.getItemComposition(id).getName());

			if (config.minimumValue() > 0
				&& !pinnedItems.contains(itemId)
				&& itemManager.getItemPrice(itemId) < config.minimumValue())
			{
				log.debug("Skipping {} (id={}) — below minimum value", name, itemId);
				skipped++;
				continue;
			}

			if (requestedIds.contains(itemId))
			{
				log.debug("Already cached {} (id={})", name, itemId);
				cached++;
				continue;
			}

			log.debug("Fetching price for {} (id={}) in {}ms", name, itemId, delay * 100);
			fetching++;
			fetchItem(itemId, name, delay++ * 100L);
		}

		log.info("Bank scan: {} items total, {} fetching, {} cached, {} skipped", total, fetching, cached, skipped);
		updatePanel();
	}

	public Map<Integer, PriceData> getItemPriceCache()
	{
		return itemPriceCache;
	}

	public Set<Integer> getPinnedItems()
	{
		return pinnedItems;
	}

	private void togglePin(int itemId)
	{
		if (!pinnedItems.remove(itemId))
		{
			pinnedItems.add(itemId);
			if (!requestedIds.contains(itemId))
			{
				String name = itemNameCache.computeIfAbsent(itemId,
					id -> itemManager.getItemComposition(id).getName());
				fetchItem(itemId, name, 0);
			}
		}
		savePinnedItems();
		updatePanel();
	}

	private void fetchItem(int itemId, String name, long delayMs)
	{
		requestedIds.add(itemId);
		executor.schedule(() ->
		{
			PriceData data = GEPriceFetcher.fetchSync(itemId, config.timeFrame());
			if (data != null)
			{
				log.debug("Got price for {} (id={}): current={} previous={} change={}%",
					name, itemId, data.getCurrent(), data.getPrevious(), data.getChangePercent());
				itemPriceCache.put(itemId, data);
			}
			else
			{
				log.debug("No price data for {} (id={}) — non-tradeable or API error", name, itemId);
			}
			updatePanel();
		}, delayMs, TimeUnit.MILLISECONDS);
	}

	private void updatePanel()
	{
		panel.update(
			new ConcurrentHashMap<>(itemPriceCache),
			new ConcurrentHashMap<>(itemNameCache),
			new HashSet<>(pinnedItems)
		);
	}

	private void fetchPinnedItems()
	{
		int delay = 0;
		for (int itemId : pinnedItems)
		{
			if (!requestedIds.contains(itemId))
			{
				// Name lookup requires client thread — startUp runs on EDT so skip it here.
				// itemNameCache will be populated properly when the bank is opened.
				fetchItem(itemId, itemNameCache.getOrDefault(itemId, "item " + itemId), delay++ * 100L);
			}
		}
	}

	private void loadPinnedItems()
	{
		pinnedItems.clear();
		String stored = configManager.getConfiguration(CONFIG_GROUP, PINNED_KEY);
		if (stored != null && !stored.isEmpty())
		{
			for (String part : stored.split(","))
			{
				try { pinnedItems.add(Integer.parseInt(part.trim())); }
				catch (NumberFormatException ignored) {}
			}
		}
		log.debug("Loaded {} pinned items", pinnedItems.size());
	}

	private void savePinnedItems()
	{
		configManager.setConfiguration(CONFIG_GROUP, PINNED_KEY,
			pinnedItems.stream().map(String::valueOf).collect(Collectors.joining(",")));
	}
}
