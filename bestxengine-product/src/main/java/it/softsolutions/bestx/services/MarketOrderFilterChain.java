package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.ExecutionInMarketAction;
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
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (marketOrder != null && marketOrder.getMarket() != null) {
			// for debugging pourposes use the GoToErrorStateAction instead of the default ExecutionInMarketAction
//			operation.getLastAttempt().setNextAction(new GoToErrorStateAction("No Execution Action has been chosen"));
			operation.getLastAttempt().setNextAction(new ExecutionInMarketAction());
		}
		for (MarketOrderFilter filter : filters) {
			if (operation.getLastAttempt().getNextAction() == null || !operation.getLastAttempt().getNextAction().isFinalDecision()) {
				filter.filterMarketOrder(marketOrder, operation);
			}
		}
	}
	
	
	
}
