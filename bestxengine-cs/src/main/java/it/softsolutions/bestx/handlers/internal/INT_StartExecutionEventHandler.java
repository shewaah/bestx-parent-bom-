/**
 * 
 */
package it.softsolutions.bestx.handlers.internal;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.internal.deprecated.INT_ExecutedState;
import it.softsolutions.bestx.states.internal.deprecated.INT_InternalInCurandoState;
import it.softsolutions.jsscommon.Money;

/**
 * @author Stefano
 *
 */
public class INT_StartExecutionEventHandler extends CSBaseOperationEventHandler {

    private static final long serialVersionUID = -7135660874704183219L;
    private MarketBuySideConnection cmfConnection;
    private static final Logger LOGGER = LoggerFactory.getLogger(INT_StartExecutionEventHandler.class);

    private final long waitingCMFDelay;

    /**
     * @param operation
     */
    public INT_StartExecutionEventHandler(Operation operation, MarketBuySideConnection cmfConnection, long waitingCMFDelay) {
        super(operation);
        this.cmfConnection = cmfConnection;
        this.waitingCMFDelay = waitingCMFDelay;
    }

    @Override
    public void onNewState(OperationState currentState) {
        Order order = this.operation.getOrder(); 
        Money limit = order.getLimit();
        Money best = null;
        if (operation.getLastAttempt().getSortedBook().getValidSideProposals(order.getSide()) != null &&
                        operation.getLastAttempt().getSortedBook().getValidSideProposals(order.getSide()).size() > 0) {
            best = operation.getLastAttempt().getSortedBook().getValidSideProposals(order.getSide()).get(0).getPrice();
        } else {    		
            LOGGER.info("No prices found for order: {}", order.getFixOrderId());
            operation.setStateResilient(new WarningState(currentState, null, Messages.getString("CMF_NOPRICE.0")), ErrorState.class);
        }

        Money price = null;
        if(limit == null){
            price = best; 
        } else {
            if(order.getSide().compareTo(OrderSide.SELL)== 0) {
                price = (best.getAmount().compareTo(BigDecimal.ZERO) != 0 && best.getAmount().compareTo(limit.getAmount())>= 0) ? best : limit;
            } else {
                price = (best.getAmount().compareTo(BigDecimal.ZERO) != 0 && best.getAmount().compareTo(limit.getAmount())<= 0) ? best : limit;
            }
        }
        operation.getLastAttempt().getMarketOrder().setLimit(price);
        //Invio dell'ordine a CMF
        try {
            // Attenzione, perche' questo metodo setta la Venue al MarketOrder
            cmfConnection.sendSubjectOrder(operation, operation.getLastAttempt().getMarketOrder());
        } catch (BestXException exc) {
            operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("INTMarketSendOrderError.0")), ErrorState.class);
        }
        operation.setInternalized(true);
        setupDefaultTimer(waitingCMFDelay, false);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleOrderPendingAccepted(it.softsolutions.bestx.connections.TradingConsoleConnection)
     */
    @Override
    public void onTradingConsoleOrderPendingAccepted(TradingConsoleConnection source) {
        //do nothing except a log
        LOGGER.info("Received acceptance for Pending Order.");
        operation.setStateResilient(new INT_ExecutedState(), ErrorState.class);

    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleOrderPendingExpired(it.softsolutions.bestx.connections.TradingConsoleConnection)
     */
    @Override
    public void onTradingConsoleOrderPendingExpired(TradingConsoleConnection source) {
        //20110530 - Ruggero
        //the customer wants orders rejected or not managed by the internal market to
        //go in the manual curando
        //operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
        stopDefaultTimer();
        operation.setStateResilient(new INT_InternalInCurandoState(Messages.getString("INTMarketOrderInCurandoOrderPendExp.0")), ErrorState.class);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleOrderPendingRejected(it.softsolutions.bestx.connections.TradingConsoleConnection, java.lang.String)
     */
    @Override
    public void onTradingConsoleOrderPendingRejected(TradingConsoleConnection source, String reason) {
        //20110530 - Ruggero
        //the customer wants orders rejected or not managed by the internal market to
        //go in the manual curando
        //operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
    	stopDefaultTimer();
    	operation.setStateResilient(new INT_InternalInCurandoState(Messages.getString("INTMarketOrderInCurandoOrderRej.0")), ErrorState.class);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleTradeReceived(it.softsolutions.bestx.connections.TradingConsoleConnection)
     */
    @Override
    public void onTradingConsoleTradeReceived(TradingConsoleConnection source) {
        // Do nothing		operation.setStateResilient(new INT_ExecutedState(), ErrorState.class);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleTradeRejected(it.softsolutions.bestx.connections.TradingConsoleConnection, java.lang.String)
     */
    @Override
    public void onTradingConsoleTradeRejected(TradingConsoleConnection source,
                    String reason) {
        //20110530 - Ruggero
        //the customer wants orders rejected or not managed by the internal market to
        //go in the manual curando
        //operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
    	stopDefaultTimer();
    	operation.setStateResilient(new INT_InternalInCurandoState(Messages.getString("INTMarketOrderInCurandoTradeRej.0")), ErrorState.class);
    }


    @Override
    public void onTradingConsoleOrderAutoExecution(TradingConsoleConnection source) {
        operation.setStateResilient(new INT_ExecutedState(), ErrorState.class);
    }

    @Override
    public void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
        //DO NOTHING
    }

    @Override
    public void onTradingConsoleTradeNotAcknowledged(TradingConsoleConnection source) {
        operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventCmfOrderSendError.0")), ErrorState.class);
    }

    @Override
    public void onTradingConsoleOrderPendingCounter(TradingConsoleConnection source, ClassifiedProposal counter) {
        operation.getLastAttempt().addExecutablePrice(new ExecutablePrice(counter), 0);
        // operation.setStateResilient(new INT_ManageCounterState(), ErrorState.class);
        // AMC 20100903 Sull'internalizzatore sistematico non sono permessi counter. Quindi, un counter viene gestito come rifiuto. Ticket #5735 di TG

        //20110530 - Ruggero
        //the customer wants orders rejected or not managed by the internal market to
        //go in the manual curando
        //operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
        stopDefaultTimer();
        operation.setStateResilient(new INT_InternalInCurandoState(Messages.getString("INTMarketOrderInCurandoPendingCounter.0")), ErrorState.class);
    }


    /*
     * CMF error replies management 
     */
    @Override
    public void onCmfErrorReply(TradingConsoleConnection source, String errorMessage, int errorCode)
    {
        LOGGER.debug("Error Reply received from CMF, error code {}, error message {}", errorCode, errorMessage);
        operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventCmfErrorReply.0")), ErrorState.class);
    }

    /**
     * @param cmfConnection the cmfConnection to set
     */
    public void setCmfConnection(MarketBuySideConnection cmfConnection) {
        this.cmfConnection = cmfConnection;
    }

    @Override
    public void onTimerExpired(String jobName, String groupName) {
    	String handlerJobName = super.getDefaultTimerJobName();
    	
        if (jobName.equals(handlerJobName)) {
            LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);
            //20110530 - Ruggero
            //the customer wants orders rejected or not managed by the internal market to
            //go in the manual curando
            //operation.setStateResilient(new INT_RejectedState(Messages.getString("EventCmfOrderExpired.1", this.waitingCMFDelay/1000)), ErrorState.class);	         
            operation.setStateResilient(new INT_InternalInCurandoState(Messages.getString("INTMarketOrderInCurandoTimer.0")), ErrorState.class);
        }
        else {
            super.onTimerExpired(jobName, groupName);
        }
    }

}
