package it.softsolutions.bestx.executionflow;

public class RejectOrderAction extends MarketOrderNextAction {

	private String rejectReason;
	
	public RejectOrderAction(String rejectReason) {
		super(Action.REJECT);
		this.rejectReason = rejectReason;
	}
	
	public String getRejectReason() {
		return this.rejectReason;
	}
	
}
