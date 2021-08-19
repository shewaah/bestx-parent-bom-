package it.softsolutions.bestx.executionflow;

public class FreezeOrderAction extends MarketOrderNextAction {

	public static enum NextPanel {ORDERS_NO_AUTOEXECUTION, PARKED, LIMIT_FILE, LIMIT_FILE_NO_PRICE}
	
	private NextPanel nextPanel;
	
	public FreezeOrderAction(NextPanel nextPanel) {
		super(Action.FREEZE);
		this.nextPanel = nextPanel;
	}
	
	public NextPanel getNextPanel() {
		return this.nextPanel;
	}
	
}
