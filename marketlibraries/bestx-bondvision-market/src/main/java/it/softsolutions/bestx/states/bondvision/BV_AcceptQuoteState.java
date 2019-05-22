package it.softsolutions.bestx.states.bondvision;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

public class BV_AcceptQuoteState extends BaseState implements OperationState {

	public BV_AcceptQuoteState() {
		super(OperationState.Type.AcceptQuote, MarketCode.BV);
	}

	public BV_AcceptQuoteState(String comment) {
		super(OperationState.Type.AcceptQuote, MarketCode.BV);
		setComment(comment);
	}

	@Override
	public void validate() throws BestXException {
	}

	@Override
	public boolean isRevocable() {
		return true;
	}

	@Override
	public boolean mustSaveBook() {
		return false;
	}

	@Override
	public boolean isExpirable() {
		return true;
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new BV_AcceptQuoteState();
	}

}
