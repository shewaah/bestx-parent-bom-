package it.softsolutions.bestx.services;

import java.math.BigDecimal;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public class OrderLimitPriceMarketOrderFilter implements MarketOrderFilter {

	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		if (operation.getOrder().getLimit() != null && operation.getOrder().getLimit().getAmount() != null && marketOrder != null) { // If the original order has a limit
			if (marketOrder.getLimit() == null || marketOrder.getLimit().getAmount() == null) {
				marketOrder.setLimit(operation.getOrder().getLimit());
			} else {
				if (this.isFirstPriceWorseThanSecondPrice(operation.getOrder().getSide(), marketOrder.getLimit().getAmount(), operation.getOrder().getLimit().getAmount())) {
					marketOrder.setLimit(operation.getOrder().getLimit());
				}
			}
		}
	}
	
	private boolean isFirstPriceWorseThanSecondPrice(OrderSide side, BigDecimal firstPrice, BigDecimal secondPrice) {
		if (OrderSide.isBuy(side)) {
			return firstPrice.compareTo(secondPrice) > 0; // Conceptually equivalent to: firstPrice > secondPrice
		} else {
			return firstPrice.compareTo(secondPrice) < 0; // Conceptually equivalent to: firstPrice < secondPrice
		}
	}

	
}
