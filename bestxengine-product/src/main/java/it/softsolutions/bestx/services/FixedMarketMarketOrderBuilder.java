package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.jsscommon.Money;

public class FixedMarketMarketOrderBuilder extends MarketOrderBuilder {
	
	public FixedMarketMarketOrderBuilder() {
      super();
//		super("Default");
	}

	private MarketFinder marketFinder;
	private MarketCode marketCode;

	private TargetPriceCalculator targetPriceCalculator;
	
	@Override
	public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) throws Exception {
		MarketOrder marketOrder = new MarketOrder();
		marketOrder.setValues(operation.getOrder());
		marketOrder.setTransactTime(DateService.newUTCDate());
		marketOrder.setMarket(this.marketFinder.getMarketByCode(this.marketCode, null));
		Money limitPrice = this.targetPriceCalculator.calculateTargetPrice(operation);
			
		marketOrder.setLimit(limitPrice != null ? limitPrice : operation.getOrder().getLimit());
		marketOrder.setBuilder(this);

		marketOrder.setLimitMonitorPrice(marketOrder.getLimit());
		
		listener.onMarketOrderBuilt(this, marketOrder);
	}

	public MarketFinder getMarketFinder() {
		return marketFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	public MarketCode getMarketCode() {
		return marketCode;
	}

	public void setMarketCode(MarketCode marketCode) {
		this.marketCode = marketCode;
	}

	public TargetPriceCalculator getTargetPriceCalculator() {
		return targetPriceCalculator;
	}

	public void setTargetPriceCalculator(TargetPriceCalculator targetPriceCalculator) {
		this.targetPriceCalculator = targetPriceCalculator;
	}
	
}
