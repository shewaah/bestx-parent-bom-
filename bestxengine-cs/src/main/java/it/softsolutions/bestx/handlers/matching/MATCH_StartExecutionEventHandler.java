/**
 * 
 */
package it.softsolutions.bestx.handlers.matching;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.services.TitoliIncrociabiliService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.matching.MATCH_MatchFoundState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefano
 *
 */
public class MATCH_StartExecutionEventHandler extends CSBaseOperationEventHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MATCH_StartExecutionEventHandler.class);
	
	private final MarketBuySideConnection matchingConnection;
	private final TitoliIncrociabiliService titoliIncrociabiliService;
	
	/**
	 * @param operation
	 */
	public MATCH_StartExecutionEventHandler(Operation operation, 
			MarketBuySideConnection matchingConnection,
			TitoliIncrociabiliService titoliIncrociabiliService) {
		super(operation);
		this.matchingConnection = matchingConnection;
		this.titoliIncrociabiliService = titoliIncrociabiliService;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		try {
			matchingConnection.sendSubjectOrder(operation, operation.getLastAttempt().getMarketOrder());
		} catch (BestXException exc) {
			try {
				titoliIncrociabiliService.resetMatchingOperation(operation);
				operation.setMatchingOperation(null);
				operation.getOrder().setMatchingOrder(false);
			} catch (BestXException e) {
				LOGGER.error("Unable to reset multi matching operation", e);
			}
			operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketSendOrderError.0")), ErrorState.class);
		}
	}
	
	@Override
	public void onMarketMatchFound(MarketBuySideConnection source, Operation matching) {
		operation.setMatchingOperation(matching);
		operation.setStateResilient(new MATCH_MatchFoundState(), ErrorState.class);
	}
	
	@Override
	public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
		try {
			titoliIncrociabiliService.resetMatchingOperation(operation);
			matchingConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), comment);
			operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
		} catch (BestXException exc) {
			operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
		}
	}
	
	@Override
	public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
		//Passaggio allo stato manuale
		try {
			titoliIncrociabiliService.resetMatchingOperation(operation);
			matchingConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), comment);
			operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
		} catch (BestXException exc) {
			operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
		}
	}
	
	@Override
	public void onStateRestore(OperationState currentState) {
		this.onNewState(currentState);
	}
}
