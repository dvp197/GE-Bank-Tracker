package com.gepriceoverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GEPriceFetcher
{
	private static final String API_BASE = "https://prices.runescape.wiki/api/v1/osrs";

	static PriceData fetchSync(int itemId, GEPriceOverlayConfig.TimeFrame tf)
	{
		try
		{
			String timestep = toTimestep(tf);
			URL url = new URL(API_BASE + "/timeseries?timestep=" + timestep + "&id=" + itemId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "ge-price-overlay RuneLite plugin");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.connect();

			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					sb.append(line);
				}
			}

			return parseTimeseries(sb.toString(), toLookback(tf));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private static String toTimestep(GEPriceOverlayConfig.TimeFrame tf)
	{
		switch (tf)
		{
			case DAY:   return "5m";
			case WEEK:  return "1h";
			case MONTH: return "6h";
			case YEAR:  return "24h";
			default:    return "5m";
		}
	}

	private static int toLookback(GEPriceOverlayConfig.TimeFrame tf)
	{
		switch (tf)
		{
			case DAY:   return 288;  // 24h ÷ 5min
			case WEEK:  return 168;  // 7 × 24h
			case MONTH: return 120;  // 30 × 4 (6h intervals)
			case YEAR:  return 365;
			default:    return 288;
		}
	}

	private static PriceData parseTimeseries(String json, int lookback)
	{
		int dataIdx = json.indexOf("\"data\":[");
		if (dataIdx < 0)
		{
			return null;
		}

		List<Integer> prices = new ArrayList<>();
		int searchIdx = dataIdx;

		while (true)
		{
			int priceIdx = json.indexOf("\"avgHighPrice\":", searchIdx);
			if (priceIdx < 0)
			{
				break;
			}

			int valueStart = priceIdx + "\"avgHighPrice\":".length();
			while (valueStart < json.length() && json.charAt(valueStart) == ' ')
			{
				valueStart++;
			}

			if (json.startsWith("null", valueStart))
			{
				searchIdx = valueStart + 4;
				continue;
			}

			int valueEnd = valueStart;
			while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-'))
			{
				valueEnd++;
			}

			if (valueEnd > valueStart)
			{
				try
				{
					prices.add(Integer.parseInt(json.substring(valueStart, valueEnd)));
				}
				catch (NumberFormatException e)
				{
					// ignore malformed value
				}
			}

			searchIdx = valueEnd;
		}

		if (prices.size() < 2)
		{
			return null;
		}

		int current = prices.get(prices.size() - 1);
		int prevIndex = Math.max(0, prices.size() - 1 - lookback);
		int previous = prices.get(prevIndex);

		return new PriceData(current, previous);
	}
}
