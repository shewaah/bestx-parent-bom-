package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketOrder;

public class FixedMarketMarketOrderBuilder extends MarketOrderBuilder {

	private MarketFinder marketFinder;
	private MarketCode marketCode = MarketCode.TW;
	
	@Override
	public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) throws Exception {
		MarketOrder marketOrder = new MarketOrder();
		marketOrder.setValues(operation.getOrder());
		marketOrder.setTransactTime(DateService.newUTCDate());
		marketOrder.setMarket(marketFinder.getMarketByCode(this.marketCode, null));
		marketOrder.setLimit(operation.getOrder().getLimit());
		marketOrder.setBuilder(this);

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
	
}
