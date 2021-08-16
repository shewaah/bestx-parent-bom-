package it.softsolutions.bestx.bestexec;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.MarketOrder;

public interface MarketOrderFilter {

	static enum NextAction {
		EXECUTE, REJECT, WAIT
	}
	
	static class MarketOrderFilterResponse {
		private MarketOrder marketOrder;
		private NextAction nextAction;
		private String message;
		public MarketOrder getMarketOrder() {
			return marketOrder;
		}
		public NextAction getNextAction() {
			return nextAction;
		}
		public String getMessage() {
			return message;
		}
		public MarketOrderFilterResponse(MarketOrder marketOrder, NextAction nextAction, String message) {
			super();
			this.marketOrder = marketOrder;
			this.nextAction = nextAction;
			this.message = message;
		}
	}
	
	MarketOrderFilterResponse filterMarketOrder(MarketOrder marketOrder, Operation operation);
	
}
