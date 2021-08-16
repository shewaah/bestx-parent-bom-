package it.softsolutions.bestx.services;

import java.math.BigDecimal;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;

public class TargetPriceMarketOrderFilter implements MarketOrderFilter {

	private String rejectMessage = "Calculated target price is worse than requested limit price";
	
	@Override
	public MarketOrderFilterResponse filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		
		
		if (operation.getOrder().isLimitFile()) { // 59=1 40=2 (Limit File Order)
			// If target price is worse
			if (this.isTargetPriceWorseThanOriginalOrderPrice(operation.getOrder().getSide(), operation.getOrder().getLimit().getAmount(), marketOrder.getLimit().getAmount())) {
				int centsLFTolerance = ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance();
				
				BigDecimal targetPrice = marketOrder.getLimit().getAmount();
				BigDecimal limitPrice = operation.getOrder().getLimit().getAmount();
				BigDecimal differenceAbs = targetPrice.subtract(limitPrice).abs();
				BigDecimal differenceCents = differenceAbs.multiply(new BigDecimal(100));
				if (differenceCents.compareTo(new BigDecimal(centsLFTolerance)) <= 0) { // Price is worse but it is inside tolerance
					marketOrder.setLimit(operation.getOrder().getLimit());
					return new MarketOrderFilterResponse(marketOrder, NextAction.EXECUTE, null);
				} else { // Price is worse and outside tolerance
					return new MarketOrderFilterResponse(null, NextAction.WAIT, null);
				}
			} else { // If price is better
				return new MarketOrderFilterResponse(marketOrder, NextAction.EXECUTE, null);
			}
		} else if (operation.getOrder().getLimit() != null && operation.getOrder().getLimit().getAmount() != null) { // 59=0 40=2 (ALGO Limit Order)
			if (this.isTargetPriceWorseThanOriginalOrderPrice(operation.getOrder().getSide(), operation.getOrder().getLimit().getAmount(), marketOrder.getLimit().getAmount())) {
				return new MarketOrderFilterResponse(null, NextAction.REJECT, this.rejectMessage);
			} else {
				return new MarketOrderFilterResponse(marketOrder, NextAction.EXECUTE, null);
			}
		} else { // 59=0 40=1 (ALGO Market Order)
			return new MarketOrderFilterResponse(marketOrder, NextAction.EXECUTE, null);
		}
	}
	
	private boolean isTargetPriceWorseThanOriginalOrderPrice(OrderSide side, BigDecimal originalOrderPrice, BigDecimal targetPrice) {
		if (OrderSide.isBuy(side)) {
			return targetPrice.compareTo(originalOrderPrice) > 0; // Conceptually equivalent to: targetPrice > originalOrderPrice
		} else {
			return targetPrice.compareTo(originalOrderPrice) < 0; // Conceptually equivalent to: targetPrice < originalOrderPrice
		}
	}

	
}
