/**
 * 
 */
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.WarningState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 *
 */
public class FormalValidationKOEventHandler extends BaseOperationEventHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FormalValidationKOEventHandler.class);

	/**
	 * @param operation
	 */
	public FormalValidationKOEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onNewState(OperationState currentState) {
		//ordine formalmente non valido, inviare ORDER_RESP negativa a TAS e mettere in stato finale
		try {
			customerConnection.sendOrderResponseNack(operation, 
					operation.getOrder(), 
					operation.getIdentifier(OperationIdType.ORDER_ID), 
					"Order not accepted - " + currentState.getComment());
			operation.setStateResilient(new OrderNotExecutedState(), ErrorState.class);
		} catch (BestXException e) {
			LOGGER.warn("{}", e.getMessage(), e);
			operation.setStateResilient(new WarningState(currentState, null, Messages.getString("EventFixNoConfirm.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
		}
	}
}
