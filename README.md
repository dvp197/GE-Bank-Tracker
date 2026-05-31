# Bank Tracker

Displays Grand Exchange price changes directly on your bank items and in a sortable sidebar panel.

## Features

- **Bank overlay** — each item in your bank shows its price change (e.g. `+12.3%` or `-1.5M`) centered on the item icon
- **Sidebar panel** — lists all tracked items sorted from biggest increase to biggest decrease, updates live as prices load
- **Manual tracking** — right-click any bank item and select **Track** to pin it; pinned items are tracked across sessions even when the bank is closed
- **Stable price indicator** — pinned items with no price movement show `stable` in blue so you know they're being tracked

## Configuration

| Setting | Description |
|---|---|
| Time Frame | Compare prices over the last Day, Week, Month, or Year |
| Minimum item value | Hide overlays on items worth less than this amount (0 = show all) |
| Display mode | Show change as a percentage or gold value (k / M / B) |

## How it works

Price data is fetched from the [OSRS Wiki Prices API](https://prices.runescape.wiki/osrs) when you open your bank. Requests are staggered to avoid hammering the API. Prices are cached for the session — restarting the plugin fetches fresh data.

## Privacy

When you open your bank, the item IDs of your tradeable items are sent as HTTPS requests to `prices.runescape.wiki` to retrieve price data. This is Jagex-affiliated infrastructure. No personal information, account details, or item quantities are transmitted — only item IDs. Pinned items are fetched on plugin startup regardless of whether the bank is open.

## Data source

All price data comes from the [OSRS Wiki Prices API](https://prices.runescape.wiki/osrs). Prices reflect the average insta-buy (high) price aggregated from real in-game trades.
