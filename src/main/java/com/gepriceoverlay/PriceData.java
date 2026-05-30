package com.gepriceoverlay;

public class PriceData
{
	private final int current;
	private final int previous;

	public PriceData(int current, int previous)
	{
		this.current = current;
		this.previous = previous;
	}

	public int getCurrent() { return current; }

	public int getPrevious() { return previous; }

	public int getChange() { return current - previous; }

	public double getChangePercent()
	{
		if (previous == 0) return 0;
		return (current - previous) / (double) previous * 100;
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
