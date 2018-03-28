/**
 * 
 */
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.services.OrderValidationService;
import it.softsolutions.bestx.services.ordervalidation.OrderResult;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderRejectableState;
/**
 * @author Stefano
 *
 */
public class ValidateByPunctualFilterEventHandler extends BaseOperationEventHandler {
	private final OrderValidationService orderValidationService;
	/**
	 * @param operation
	 */
	public ValidateByPunctualFilterEventHandler(Operation operation, OrderValidationService orderValidationService) {
		super(operation);
		this.orderValidationService = orderValidationService;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		OrderResult orderResult = orderValidationService.validateOrderOnPunctualFilters(operation, operation.getOrder());
		operation.setStateResilient(new OrderRejectableState(operation.getState().getComment() + " - " + orderResult.getReason()), ErrorState.class);
	}
}
