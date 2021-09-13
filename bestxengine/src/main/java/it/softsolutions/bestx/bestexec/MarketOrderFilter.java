package it.softsolutions.bestx.bestexec;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.MarketOrder;

public interface MarketOrderFilter {

	void filterMarketOrder(MarketOrder marketOrder, Operation operation);
	
}
