/**
 * 
 */
package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;

/**
 * @author anna.cochetti
 *
 */
public class CSRejectedEventHandler extends RejectedEventHandler {
	
	private PriceResult priceResult;
	private boolean rejectWhenBloombergIsBest;
	private SerialNumberService serialNumberService;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CSRejectedEventHandler.class);
	/**
	 * @param operation
	 */
	public CSRejectedEventHandler(Operation operation, boolean rejectWhenBloombergIsBest, SerialNumberService serialNumberService) {
		super(operation);
		this.rejectWhenBloombergIsBest = rejectWhenBloombergIsBest;
		this.serialNumberService = serialNumberService;
	}

    @Override
    public void onNewState(OperationState currentState) {
    	/* ask to the CSExecutionStrategy for the next steps */
    	ExecutionStrategyService executionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, priceResult, rejectWhenBloombergIsBest);
    	try {
            if (operation.isStopped()) return;
    		executionStrategyService.manageMarketReject(operation, operation.getLastAttempt(), serialNumberService);
    	} catch (BestXException e) {
    		LOGGER.info("Exception when trying to manage rejection from market. Going to default state WarningState");
    		operation.setStateResilient(new WarningState(currentState, e, "Exception when trying to manage rejection from market"), ErrorState.class);
    	}
    }
}
