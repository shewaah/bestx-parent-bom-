package it.softsolutions.bestx.markets.historic;

public interface HistoricMarketMBean {
	int getNumPricePoints();
	void setNumPricePoints(int numPricePoints);
	int getNumDays();
	void setNumDays(int numDays);
}
