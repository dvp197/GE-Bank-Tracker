package com.gepriceoverlay;

import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import java.awt.*;

public class GEPriceOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;
	private final GEPriceOverlayPlugin plugin;
	private final GEPriceOverlayConfig config;

	@Inject
	public GEPriceOverlay(ItemManager itemManager, GEPriceOverlayPlugin plugin, GEPriceOverlayConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
	{
		int canonicalId = itemManager.canonicalize(itemId);

		if (config.minimumValue() > 0
			&& !plugin.getPinnedItems().contains(canonicalId)
			&& itemManager.getItemPrice(canonicalId) < config.minimumValue())
		{
			return;
		}

		PriceData priceData = plugin.getItemPriceCache().get(canonicalId);
		boolean pinned = plugin.getPinnedItems().contains(canonicalId);

		if (priceData == null)
		{
			return;
		}

		int change = priceData.getChange();
		if (change == 0 && !pinned)
		{
			return;
		}

		Color color = change > 0 ? Color.GREEN : change < 0 ? Color.RED : Color.CYAN;
		String text = change == 0
			? "stable"
			: config.displayMode() == GEPriceOverlayConfig.DisplayMode.MONEY
				? PriceData.formatGold(change)
				: PriceData.formatPercent(priceData.getChangePercent());

		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = graphics.getFontMetrics();

		Rectangle bounds = item.getCanvasBounds();
		int x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
		int y = bounds.y + (bounds.height + fm.getAscent()) / 2;

		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text, color);
	}
}
