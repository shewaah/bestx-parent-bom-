package it.softsolutions.bestx.executionflow;

public class GoToErrorStateAction extends MarketOrderNextAction {

	private String message;
	
	public GoToErrorStateAction(String message) {
		super(Action.GOTOERROR);
		this.message = message;
	}

	public GoToErrorStateAction(String message, boolean finalDecision) {
		super(Action.REJECT, finalDecision);
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}
	
}
