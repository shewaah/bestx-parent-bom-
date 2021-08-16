package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.MarketOrder;

public class RepeatedMarketOrderFilter implements MarketOrderFilter {
	
	private String rejectMessage = "Market order already tried in a previous attempt";

	@Override
	public MarketOrderFilterResponse filterMarketOrder(MarketOrder order, Operation operation) {
		List<Attempt> previousAttempts = new ArrayList<>(operation.getAttempts());
		previousAttempts.remove(previousAttempts.size() - 1); // We do not need to take into account the last attempt
		
		for (Attempt attempt : previousAttempts) {
			if (attempt.getMarketOrder() != null) {
				MarketOrder marketOrderToCheck = attempt.getMarketOrder();
				
				if (marketOrderToCheck.getLimit().equals(order.getLimit()) &&
						marketOrderToCheck.getMarket().equals(order.getMarket()) &&
						marketOrderToCheck.getDealers().equals(order.getDealers())) {
					return new MarketOrderFilterResponse(null, NextAction.REJECT, this.rejectMessage);
				}
				
			}
		}
		return new MarketOrderFilterResponse(order, NextAction.EXECUTE, null);
	}

}
