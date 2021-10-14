package it.softsolutions.bestx.services;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder.BuilderType;
import it.softsolutions.bestx.bestexec.MarketOrderFilter;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;

public class LimitMonitorPriceMarketOrderFilter implements MarketOrderFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(LimitMonitorPriceMarketOrderFilter.class);
	
	@Override
	public void filterMarketOrder(MarketOrder marketOrder, Operation operation) {
		
		if (operation.getOrder().isLimitFile()) { // 59=1 40=2 (Limit File Order)
			// If target price is worse
			if (marketOrder == null) {
				operation.getLastAttempt().setNextAction(new FreezeOrderAction(null, null)); // TODO Check if LF or LFNP
			} else if (this.isTargetPriceWorseThanOriginalOrderPrice(operation.getOrder().getSide(), operation.getOrder().getLimit().getAmount(), marketOrder.getLimitMonitorPrice().getAmount())) {
				int centsLFTolerance = ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance();
				
				BigDecimal targetPrice = marketOrder.getLimitMonitorPrice().getAmount();
				BigDecimal limitPrice = operation.getOrder().getLimit().getAmount();
				BigDecimal differenceAbs = targetPrice.subtract(limitPrice).abs();
				BigDecimal differenceCents = differenceAbs.multiply(new BigDecimal(100));
				if (differenceCents.compareTo(new BigDecimal(centsLFTolerance)) > 0) { // Price is worse but it is inside tolerance
					operation.getLastAttempt().setNextAction(new FreezeOrderAction(null, null)); // TODO Check if LF or LFNP
				}
			}
		} else if (operation.getOrder().getLimit() != null && operation.getOrder().getLimit().getAmount() != null) { // 59=0 40=2 (ALGO Limit Order)
			if (marketOrder != null && this.isTargetPriceWorseThanOriginalOrderPrice(operation.getOrder().getSide(), operation.getOrder().getLimit().getAmount(), marketOrder.getLimitMonitorPrice().getAmount())) {
			   if (marketOrder.getBuilder().getType() == BuilderType.CUSTOM) {
	            operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("LimitMonitorPriceMarketOrderFilter.rejectMessage.rest", 
                     (marketOrder.getLimit() != null ? marketOrder.getLimit().getAmount() : "NA"),
	                  (marketOrder.getLimitMonitorPrice() != null ? marketOrder.getLimitMonitorPrice().getAmount() : "NA"),
	                  operation.getOrder().getLimit().getAmount())));
			   } else {
   			   operation.getLastAttempt().setNextAction(new RejectOrderAction(Messages.getString("LimitMonitorPriceMarketOrderFilter.rejectMessage.fallback")));
			   }
			}
		}
		// 59=0 40=1 (ALGO Market Order, it is OK as it is)
	}
	
	private boolean isTargetPriceWorseThanOriginalOrderPrice(OrderSide side, BigDecimal originalOrderPrice, BigDecimal targetPrice) {
		if (OrderSide.isBuy(side)) {
			return targetPrice.compareTo(originalOrderPrice) > 0; // Conceptually equivalent to: targetPrice > originalOrderPrice
		} else {
			return targetPrice.compareTo(originalOrderPrice) < 0; // Conceptually equivalent to: targetPrice < originalOrderPrice
		}
	}

	
}
