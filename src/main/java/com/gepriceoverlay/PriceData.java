package com.gepriceoverlay;

public class PriceData
{
	private final int current;
	private final int previous;
	private final long fetchedAt;

	public PriceData(int current, int previous)
	{
		this.current = current;
		this.previous = previous;
		this.fetchedAt = System.currentTimeMillis();
	}

	public boolean isStale(GEPriceOverlayConfig.TimeFrame tf)
	{
		long age = System.currentTimeMillis() - fetchedAt;
		return age > ttlMs(tf);
	}

	private static long ttlMs(GEPriceOverlayConfig.TimeFrame tf)
	{
		switch (tf)
		{
			case DAY:   return 60 * 60 * 1000L;         // 1 hour
			case WEEK:  return 6 * 60 * 60 * 1000L;     // 6 hours
			case MONTH:
			case YEAR:  return 24 * 60 * 60 * 1000L;    // 24 hours
			default:    return 60 * 60 * 1000L;
		}
	}

	public int getCurrent() { return current; }

	public int getPrevious() { return previous; }

	public int getChange() { return current - previous; }

	public double getChangePercent()
	{
		if (previous == 0) return 0;
		return (current - previous) / (double) previous * 100;
	}

	public static String formatPercent(double changePercent)
	{
		String sign = changePercent > 0 ? "+" : "-";
		double abs = Math.abs(changePercent);
		if (abs > 0 && abs < 0.05)
		{
			return sign + "<0.1%";
		}
		return String.format("%s%.1f%%", sign, abs);
	}

	public static String formatGold(int change)
	{
		String sign = change > 0 ? "+" : "-";
		long abs = Math.abs(change);
		if (abs >= 1_000_000_000)
		{
			return sign + String.format("%.1fB", abs / 1_000_000_000.0);
		}
		if (abs >= 1_000_000)
		{
			return sign + String.format("%.1fM", abs / 1_000_000.0);
		}
		if (abs >= 1_000)
		{
			return sign + String.format("%.1fk", abs / 1_000.0);
		}
		return sign + abs;
	}
}
