package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.rest.CSMarketOrderBuilder;

public class CSALGORESTValidationMarketOrderFilter implements MarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (marketOrder != null && marketOrder.getBuilder() instanceof CSMarketOrderBuilder) {
			if (operation.getOrder().isLimitFile()) {
				if (marketOrder.getLimitMonitorPrice() == null || marketOrder.getLimitMonitorPrice().getAmount() == null) {
					operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE_NO_PRICE, Messages.getString("LimitFile.NoPrices"), true));
				} else if (marketOrder.getLimit() == null || marketOrder.getLimit().getAmount() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid target price received from service", true));
				}
			} else if (marketOrder.getLimit() == null || marketOrder.getLimit().getAmount() == null) {
				operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid target price received from service", true));
			} else if (operation.getOrder().getLimit() != null && operation.getOrder().getLimit().getAmount() != null) {
				if (marketOrder.getLimitMonitorPrice() == null || marketOrder.getLimitMonitorPrice().getAmount() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid limit monitor price received from service", true));
				}
			}
				
		}
	}

}
