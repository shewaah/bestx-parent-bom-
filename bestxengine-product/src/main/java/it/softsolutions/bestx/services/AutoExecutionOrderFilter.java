package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.model.MarketOrder;

public class AutoExecutionOrderFilter implements MarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.isNotAutoExecute()) {
			operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.ORDERS_NO_AUTOEXECUTION, Messages.getString("LimitFile.doNotExecute")));
		}
	}
	
}
