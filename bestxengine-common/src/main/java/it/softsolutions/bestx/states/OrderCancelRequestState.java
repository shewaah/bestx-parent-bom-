package it.softsolutions.bestx.states;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;

//BESTX-483 TDR 20190828
public class OrderCancelRequestState extends BaseState implements Cloneable {
	public OrderCancelRequestState() {
		super(OperationState.Type.OrderCancelRequest, null);
	}

	public OrderCancelRequestState(String comment) {
		super(OperationState.Type.OrderCancelRequest, null);
		setComment(comment);
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new OrderCancelRequestState(getComment());
	}

	@Override
	public void validate() throws BestXException {
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public boolean mustSaveBook() {
		return true;
	}

	@Override
	public boolean isExpirable() {
		return false;
	}
}
