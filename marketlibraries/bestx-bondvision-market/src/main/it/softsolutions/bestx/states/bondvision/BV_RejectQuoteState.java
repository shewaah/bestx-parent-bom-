package it.softsolutions.bestx.states.bondvision;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

public class BV_RejectQuoteState extends BaseState implements OperationState {
	
	public BV_RejectQuoteState(Type type, MarketCode marketCode) {
		super(type, marketCode);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void validate() throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

}
