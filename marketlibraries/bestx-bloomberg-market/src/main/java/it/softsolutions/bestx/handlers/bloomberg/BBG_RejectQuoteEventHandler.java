/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */
package it.softsolutions.bestx.handlers.bloomberg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.MultipleQuotesHandler;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.WarningState;

/**  
*
* Purpose: 
*
* Project Name : bestx-bloomberg-market
* First created by: paolo.midali
* Creation date: 05/ott/2012 
* 
**/
public class BBG_RejectQuoteEventHandler extends BaseOperationEventHandler {
    private static final long serialVersionUID = 2891618599873587829L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BBG_RejectQuoteEventHandler.class);

	private final MarketBuySideConnection bbgConnection;
    private MultipleQuotesHandler multipleQuoteHandler;
	
	protected int numAttempts;

	/**
	 * @param operation
	 */
	public BBG_RejectQuoteEventHandler(Operation operation, MarketBuySideConnection bbgConnection,
	                MultipleQuotesHandler multipleQuoteHandler) {
		super(operation);
        this.multipleQuoteHandler = multipleQuoteHandler;
		
		this.bbgConnection = bbgConnection;
	}

	@Override
	public void onNewState(OperationState currentState) {
		try {
		    numAttempts = 0;
		    
            String externalBroker = operation.getLastAttempt().getMarketOrder().getMarketMarketMaker().getMarketSpecificCode();
            
            Proposal lastQuote = multipleQuoteHandler.getUpdatedExternalQuote();
            
            String quoteId = null;
            String rejectReason = currentState.getComment();
            if (lastQuote != null) {
                quoteId = lastQuote.getSenderQuoteId();
            }
            LOGGER.info("[INT-TRACE] Order {} : rejecting quote from broker {}, quoteId={} : {}", operation.getOrder().getFixOrderId(), externalBroker, quoteId, rejectReason);

            numAttempts++;
			bbgConnection.rejectProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), lastQuote);
			
            setupDefaultTimer(30000, false);
		} catch(BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("BBGMarketSendQuoteRejectError.0")), ErrorState.class);
		}
	}
	
    // LOGGER.info("Accept refused by Dealer for orderID {} ({}), trying again", order.getFixOrderId(), reason);
    // operation.setStateResilient(new RejectedState(Messages.getString("GoingInBBGRejectedStateMessage", externalBroker)), ErrorState.class);
	
    @Override
    public void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text) {
        if (source.getMarketCode() != MarketCode.BLOOMBERG)
        {
           return;
        }
        operation.getLastAttempt().setAttemptState(AttemptState.PASSED_COUNTER);
        stopDefaultTimer();
        
        String externalBroker = operation.getLastAttempt().getMarketOrder().getMarketMarketMaker().getMarketSpecificCode();
        Proposal lastQuote = multipleQuoteHandler.getUpdatedExternalQuote();
        
        try {
            if ( (text == null) || (text.isEmpty()) ) {
                // msg has reason = null or "" --> reject accepted, proceed to 
                LOGGER.info("Reject confirmed for orderID {}", order.getFixOrderId());
                operation.setStateResilient(new RejectedState(Messages.getString("GoingInBBGRejectedStateMessage", externalBroker)), ErrorState.class);
            }
            else {
                // msg has reason != null, "" --> error, try again, numAttempts++, up to 3 times?
                // skips BBG_RejectedState, to avoid a new check on 2nd best spread
                if (numAttempts <= 3) {
                    LOGGER.info("Reject #{} refused for orderID {} ({}), trying again", numAttempts, order.getFixOrderId(), text);
                    numAttempts++;
                    lastQuote = multipleQuoteHandler.getUpdatedExternalQuote();
                    bbgConnection.rejectProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), lastQuote);
                    
                    setupDefaultTimer(30000, false);
                }
                else {
                    LOGGER.info("Reject refused for orderID {} ({}), max number of attempts reached", order.getFixOrderId(), text);
                    operation.setStateResilient(new RejectedState(Messages.getString("GoingInBBGRejectedStateMessage", externalBroker)), ErrorState.class);
                }
            }
        } catch(BestXException exc) {
            operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("BBGMarketSendQuoteRejectError.0")), ErrorState.class);
        }
        
    }

    @Override
    public void onTimerExpired(String jobName, String groupName) {
        String handlerJobName = super.getDefaultTimerJobName();
        
        if (jobName.equals(handlerJobName)) {
            LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);

            String externalBroker = operation.getLastAttempt().getMarketOrder().getMarketMarketMaker().getMarketSpecificCode();
            LOGGER.info("Reject not confirmed for orderID {}, switching anyway to Rejected state", operation.getOrder().getFixOrderId());
            
            operation.getLastAttempt().setAttemptState(AttemptState.REJECTED);
            operation.setStateResilient(new RejectedState(Messages.getString("GoingInBBGRejectedStateMessage", externalBroker)), ErrorState.class);
        }
        else {
            super.onTimerExpired(jobName, groupName);
        }
    }

    
    // can receive quote updates
    @Override
    public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal quote) {
        try {
            multipleQuoteHandler.manageQuote(quote, operation.getState().getClass().getSimpleName());
            operation.getLastAttempt().setAttemptState(AttemptState.COUNTER_RECEIVED);
        }
        catch (BestXException e) {
            LOGGER.error("Error while notifying Quote");
            operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("InvalidQuoteIDError.0")), ErrorState.class);
        }
    }
    
    // can receive quote status updates
    @Override
    public void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId, Proposal.ProposalType proposalStatus) {
        try {
            multipleQuoteHandler.manageQuoteStatusChange(source.getMarketCode(), quoteId, proposalStatus);
        }
        catch (BestXException e) {
            LOGGER.error("Error while notifying QuoteStatus");
            operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("InvalidQuoteIDError.0")), ErrorState.class);
            return;
        }
    }
    
}
