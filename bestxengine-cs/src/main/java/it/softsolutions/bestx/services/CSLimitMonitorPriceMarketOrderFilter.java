package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.rest.CSMarketOrderBuilder;

public class CSLimitMonitorPriceMarketOrderFilter extends LimitMonitorPriceMarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		super.filterMarketOrder(marketOrder, operation);
		if (marketOrder.getBuilder() instanceof CSMarketOrderBuilder && operation.getLastAttempt().getNextAction() instanceof FreezeOrderAction) {
			operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile")));
		}
	}

}
