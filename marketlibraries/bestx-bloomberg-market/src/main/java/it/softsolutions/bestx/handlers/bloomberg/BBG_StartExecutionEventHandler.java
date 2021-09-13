/**
 * 
 */
package it.softsolutions.bestx.handlers.bloomberg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;

/**
 * @author Stefano
 *
 */
public class BBG_StartExecutionEventHandler extends BaseOperationEventHandler {
	
    private static final long serialVersionUID = 3476645384592497728L;
	private MarketConnection marketConnection;
	private static final Logger LOGGER = LoggerFactory.getLogger(BBG_StartExecutionEventHandler.class);

    /**
	 * @param operation
	 */
	public BBG_StartExecutionEventHandler(Operation operation, MarketConnection marketConnection) {
		super(operation);
		this.marketConnection = marketConnection;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		if(CheckIfBuySideMarketIsConnectedAndEnabled())
			operation.setStateResilient(new BBG_SendRfqState(), ErrorState.class);
		else
			operation.setStateResilient(new BBG_RejectedState(null, false), ErrorState.class);
	}
	
	/**
	 * Check if buy side market is connected and enabled.
	 *
	 * @return true, if successful
	 */
	public boolean CheckIfBuySideMarketIsConnectedAndEnabled (){
		if(marketConnection == null) return false;
		if (!marketConnection.isBuySideConnectionEnabled()) {             
			LOGGER.info("Market Bloomberg is not enabled");
			return false;			
		}
		
		if (!marketConnection.isBuySideConnectionAvailable()) {             
			LOGGER.info("MarketCode Bloomberg is not available");
			return false;			
		}		

		return true;		
	}

}
