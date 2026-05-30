package com.gepriceoverlay;

public class PriceData
{
    private final int avgHighPrice;
    private final int highPriceVolume;
    private final int avgLowPrice;
    private final int lowPriceVolume;

    private final int current;
    private final int previous;

    public PriceData(int avgHighPrice, int highPriceVolume, int avgLowPrice, int lowPriceVolume)
    {
        this.avgHighPrice = avgHighPrice;
        this.highPriceVolume = highPriceVolume;
        this.avgLowPrice = avgLowPrice;
        this.lowPriceVolume = lowPriceVolume;

        this.current = 0;
        this.previous = 0;
    }

    public PriceData(int current, int previous)
    {
        this.current = current;
        this.previous = previous;

        this.avgHighPrice = 0;
        this.highPriceVolume = 0;
        this.avgLowPrice = 0;
        this.lowPriceVolume = 0;
    }

    public int getAvgHighPrice() { return avgHighPrice; }

    public int getHighPriceVolume() { return highPriceVolume; }

    public int getAvgLowPrice() { return avgLowPrice; }

    public int getLowPriceVolume() { return lowPriceVolume; }

    public int getCurrent() { return current; }

    public int getPrevious() { return previous; }

    public int getChange() { return current - previous; }

    public int getChangePercent()
    {
        if (previous == 0) return 0;
        return (int) ((current - previous) / (double) previous * 100);
    }
}
