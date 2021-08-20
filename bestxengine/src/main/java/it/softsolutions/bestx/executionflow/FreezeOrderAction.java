package it.softsolutions.bestx.executionflow;

public class FreezeOrderAction extends MarketOrderNextAction {

	public static enum NextPanel {ORDERS_NO_AUTOEXECUTION, PARKED, LIMIT_FILE, LIMIT_FILE_NO_PRICE}
	
	private NextPanel nextPanel;
	private String message;
	
	public FreezeOrderAction(NextPanel nextPanel, String message) {
		super(Action.FREEZE);
		this.nextPanel = nextPanel;
		this.message = message;
	}
	
	public NextPanel getNextPanel() {
		return this.nextPanel;
	}

	public String getMessage() {
		return message;
	}
	
	
	
}
