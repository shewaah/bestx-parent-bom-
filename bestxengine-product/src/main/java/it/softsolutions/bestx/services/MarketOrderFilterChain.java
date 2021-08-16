package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.model.MarketOrder;

public class MarketOrderFilterChain implements MarketOrderFilter {

	private List<MarketOrderFilter> filters = new ArrayList<>();

	public List<MarketOrderFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<MarketOrderFilter> filters) {
		this.filters = filters;
	}

	@Override
	public MarketOrderFilterResponse filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		MarketOrder lastMarketOrder = marketOrder;
		MarketOrderFilterResponse response = new MarketOrderFilterResponse(lastMarketOrder, NextAction.EXECUTE, null);
		for (MarketOrderFilter filter : filters) {
			if (lastMarketOrder != null) {
				response = filter.filterMarketOrder(lastMarketOrder, operation);
				lastMarketOrder = response.getMarketOrder();
			}
		}
		return response;
	}
	
	
	
}
