package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.rest.CSMarketOrderBuilder;

public class CSLimitMonitorPriceMarketOrderFilter extends LimitMonitorPriceMarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (marketOrder != null && marketOrder.getBuilder() instanceof CSMarketOrderBuilder) {
			if (operation.getOrder().isLimitFile()) { // 59=1 40=2 (Limit File Order)
				if (marketOrder.getLimitMonitorPrice() == null) {
					operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE_NO_PRICE, Messages.getString("LimitFile.NoPrices")));
					return;
				} else if (marketOrder.getLimit() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid target price received from service", true));
					return;
				}
			} else if (operation.getOrder().getLimit() != null && operation.getOrder().getLimit().getAmount() != null) { // 59=0 40=2 (ALGO Limit Order)
				if (marketOrder.getLimit() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid target price received from service", true));
					return;
				} else if (marketOrder.getLimitMonitorPrice() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid limit monitor price received from service", true));
					return;
				}
			} else { // 59=0 40=1 (ALGO Market Order)
				if (marketOrder.getLimit() == null) {
					operation.getLastAttempt().setNextAction(new RejectOrderAction("Invalid target price received from service", true));
					return;
				}			
			}
		}
		super.filterMarketOrder(marketOrder, operation);
		if (marketOrder != null && marketOrder.getBuilder() instanceof CSMarketOrderBuilder && operation.getLastAttempt().getNextAction() instanceof FreezeOrderAction) {
		   operation.getLastAttempt().setNextAction(new FreezeOrderAction(NextPanel.LIMIT_FILE, Messages.getString("LimitFile.Rest", 
               (operation.getLastAttempt().getMarketOrder().getLimit() != null ? operation.getLastAttempt().getMarketOrder().getLimit().getAmount() : "NA"),
               (operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice() != null ? operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice().getAmount() : "NA"),
               operation.getOrder().getLimit().getAmount(),
               ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance())));
		}
	}

}
