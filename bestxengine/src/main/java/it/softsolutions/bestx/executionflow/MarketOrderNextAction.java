package it.softsolutions.bestx.executionflow;

public abstract class MarketOrderNextAction {

	public static enum Action {EXECUTE, REJECT, FREEZE};
	
	private Action action;
	
	protected MarketOrderNextAction(Action action) {
		this.action = action;
	}
	
	public Action getAction() {
		return this.action;
	}
	
}
