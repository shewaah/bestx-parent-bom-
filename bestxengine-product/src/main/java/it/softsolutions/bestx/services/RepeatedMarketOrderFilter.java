package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.MarketOrder;

public class RepeatedMarketOrderFilter implements MarketOrderFilter {
	
   private static final Logger LOGGER = LoggerFactory.getLogger(RepeatedMarketOrderFilter.class);
   
	private String rejectMessage = "Market order already tried in a previous attempt";

	@Override
	public void filterMarketOrder(MarketOrder order, Operation operation) {
		Attempt currentAttempt = operation.getLastAttempt();
		if(currentAttempt.getMarketOrder() != null && currentAttempt.getMarketOrder().getBuilderType() != MarketOrderBuilder.BuilderType.CUSTOM)
			return;
		if (order != null) {
		   //SP-20210928: getAttemptsInCurrentCycle discard the current attemp, no needed to discard it from the previousAttempts array
			List<Attempt> previousAttempts = new ArrayList<>(operation.getAttemptsInCurrentCycle());
			
			for (Attempt attempt : previousAttempts) {
				if (attempt.getMarketOrder() != null) {
					MarketOrder marketOrderToCheck = attempt.getMarketOrder();
					
					LOGGER.info("Compare Market Orders. Limit: {}:{} - Market: {}:{} - Dealers: {}:{}",
					      marketOrderToCheck.getLimit().getAmount(), order.getLimit().getAmount(),
					      marketOrderToCheck.getMarket().getMarketCode(), order.getMarket().getMarketCode(),
					      marketOrderToCheck.getDealers(), order.getDealers());
					
					if (marketOrderToCheck.getLimit().equals(order.getLimit()) &&
							marketOrderToCheck.getMarket().equals(order.getMarket()) &&
							marketOrderToCheck.getDealers().equals(order.getDealers())) {
						operation.getLastAttempt().setNextAction(new RejectOrderAction(this.rejectMessage));
					}
					
				}
			}
		}
	}
}
