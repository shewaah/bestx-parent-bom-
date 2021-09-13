package it.softsolutions.bestx.executionflow;

public abstract class MarketOrderNextAction {

	public static enum Action {EXECUTE, REJECT, FREEZE};
	
	private Action action;
	private boolean finalDecision;
	
	protected MarketOrderNextAction(Action action) {
		this.action = action;
	}

	protected MarketOrderNextAction(Action action, boolean finalDecision) {
		this.action = action;
		this.finalDecision = finalDecision;
	}
	
	public Action getAction() {
		return this.action;
	}

	public boolean isFinalDecision() {
		return finalDecision;
	}

}