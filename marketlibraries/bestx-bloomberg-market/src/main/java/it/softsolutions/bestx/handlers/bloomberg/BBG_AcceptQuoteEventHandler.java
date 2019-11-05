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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.MultipleQuotesHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_ExecutedState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_AcceptBestIfStillValidState;
/**  
*
* Purpose: 
*
* Project Name : bestx-bloomberg-market
* First created by: paolo.midali
* Creation date: 05/ott/2012 
* 
**/
public class BBG_AcceptQuoteEventHandler extends BaseOperationEventHandler {
    private static final long serialVersionUID = 2891618599873587829L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BBG_AcceptQuoteEventHandler.class);

	private final MarketBuySideConnection bbgConnection;
	private final SerialNumberService serialNumberService;
	protected final long waitingExecutionDelay;
//	private final List<String> internalMMcodes;
    private MultipleQuotesHandler multipleQuoteHandler;

	/**
	 * @param operation
	 */
	public BBG_AcceptQuoteEventHandler(Operation operation, 
			MarketBuySideConnection bbgConnection, 
			SerialNumberService serialNumberService,
			long waitingExecutionDelay,/*,
			List<String> internalMMcodes*/
			MultipleQuotesHandler multipleQuoteHandler) {
		super(operation);
		this.bbgConnection = bbgConnection;
		this.serialNumberService = serialNumberService;
		this.waitingExecutionDelay = waitingExecutionDelay;
//		this.internalMMcodes = internalMMcodes;
        this.multipleQuoteHandler = multipleQuoteHandler;
	}

	@Override
	public void onNewState(OperationState currentState) {
		try {
            String externalBroker = operation.getLastAttempt().getMarketOrder().getMarketMarketMaker().getMarketSpecificCode();
            
            Proposal lastQuote = multipleQuoteHandler.getUpdatedExternalQuote();
            
            String quoteId = null;
            if (lastQuote != null) {
                quoteId = lastQuote.getSenderQuoteId();
                LOGGER.info("[INT-TRACE] Order {} : accepting quote from broker {}, quoteId={}", operation.getOrder().getFixOrderId(), externalBroker, quoteId);
    
                setupDefaultTimer(waitingExecutionDelay, false);
                
    			bbgConnection.acceptProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), lastQuote);
            }
		} catch(BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("BBGMarketAcceptQuoteError.0")), ErrorState.class);
		}
	}

	@Override
	public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
		stopDefaultTimer();
		
		// marketExecutionReport.getState() == ExecutionReportState.FILLED    : OK    , generate ExecutionReport
		//   or
		// anything else                                                      : fail  
		if (marketExecutionReport.getState() == ExecutionReportState.FILLED) {
    		Attempt currentAttempt = operation.getLastAttempt();
    		if (currentAttempt == null) {
    			LOGGER.error("No current Attempt found");
    			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("BBGMarketAttemptNotFoundError.0")), ErrorState.class);
    			return;
    		}
    		List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
    		if (marketExecutionReports == null) {
    			marketExecutionReports = new ArrayList<MarketExecutionReport>();
    			currentAttempt.setMarketExecutionReports(marketExecutionReports);
    		}
    		marketExecutionReports.add(marketExecutionReport);
    		// Create Customer Execution Report from Market Execution Report
    		ExecutionReport executionReport;
    		try {
    			executionReport = marketExecutionReport.clone();
    		}
    		catch (CloneNotSupportedException e1) {
    			LOGGER.error("Error while trying to create Execution Report from Market Execution Report");
    			operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("BBGMarketSendOrderError.0")), ErrorState.class);
    			return;
    		}
    		long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
    		
    		//      executionReport.setPrice(operation.getLastAttempt().getCounterOffer().getPrice());
    		executionReport.setLastPx(executionReport.getPrice().getAmount());
    
    		executionReport.setSequenceId(Long.toString(executionReportId));
    		marketExecutionReport.setExecBroker(operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());               
    		executionReport.setExecBroker(marketExecutionReport.getExecBroker());
    
    		executionReport.setCounterPart(marketExecutionReport.getExecBroker());
    		executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());
    
    		LOGGER.info("BBG market - new exec report added : " + executionReport.toString());
    		operation.getExecutionReports().add(executionReport);
    
    		operation.setStateResilient(new BBG_ExecutedState("ExecutionReport/Accepted received"), ErrorState.class);
		}
		else {
            LOGGER.info("Order {} : execution report/{} received", operation.getOrder().getFixOrderId(), marketExecutionReport.getState());
            
            Attempt attempt = operation.getLastAttempt();
            if (attempt != null && attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null && 
            		attempt.getExecutablePrice(0).getClassifiedProposal().getVenue() != null &&
            		operation.getLastAttempt().getExecutionProposal().getVenue() != null && 
            		attempt.getExecutablePrice(0).getClassifiedProposal().getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
                ClassifiedProposal counter = (ClassifiedProposal) attempt.getExecutablePrice(0).getClassifiedProposal();
                counter.setProposalState(Proposal.ProposalState.REJECTED);
                counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
                attempt.addExecutablePrice(new ExecutablePrice(counter), 0);
                attempt.setAttemptState(AttemptState.REJECTED);
            }

            String reason = "ExecutionReport/" + marketExecutionReport.getState() + " received";
            if ( (marketExecutionReport.getText() != null) && (!marketExecutionReport.getText().isEmpty()) ) {
                reason+= " [" + marketExecutionReport.getText() + "]";
            }
            //
            operation.setStateResilient(new RejectedState(reason), ErrorState.class);
		}
	}

    @Override
    public void onQuoteStatusTimeout(MarketBuySideConnection source, Order order, String sessionId) {
        if (source.getMarketCode() != MarketCode.BLOOMBERG)
        {
        	return;
        }
    
        operation.getLastAttempt().setAttemptState(AttemptState.REJECTED);
        LOGGER.warn("Accept sent to dealer, but no answer received for orderID {}", order.getFixOrderId());
        // no state change, wait for internal timer on ExecutionReport to expire...
    }
	
	@Override
	public void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text) {
		if (source.getMarketCode() != MarketCode.BLOOMBERG)
		{
		   return;
		}
		
		operation.getLastAttempt().setAttemptState(AttemptState.REJECTED);
        stopDefaultTimer();
		
		// if the order is limit file, go back to limit file state, otherwise behave as usual
		if (order.isLimitFile()) {
		    LOGGER.info("Accept refused by Dealer for orderID {} ({}), order is Limit, goint into Limit File state", order.getFixOrderId(), text);
            operation.setStateResilient(new LimitFileNoPriceState("Dealer reject, order is Limit, going back to Limit File"), ErrorState.class);
		}
		
		else {
		    // go back to BBG_INT_AcceptBestIfStillValidState, so a new attempt is made if the quote is still valid
            LOGGER.info("Accept refused by Dealer for orderID {} ({}), trying again", order.getFixOrderId(), text);
            operation.setStateResilient(new BBG_INT_AcceptBestIfStillValidState("Order rejected (" + text + "), trying again"), ErrorState.class);
           
//
// 		   PM se vado in BBG_INT_AcceptBestIfStillValidState (vedasi sopra) l'effetto collaterale, nel caso il dealer effettivamente non risponda (tsox test qty=17000)
//         e' che effettuo due tentativi di esecuzione.
//         Se pero' l'accept e' fallita perche' sono in ritardo sulla QuoteID, ha senso ritentare.
//         Se invece vado in Rejected, come qui sotto, evito il doppio tentativo, ma
//
//            Attempt attempt = operation.getLastAttempt();
//            if (attempt != null && attempt.getCounterOffer() != null && attempt.getCounterOffer().getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
//                ClassifiedProposal counter = (ClassifiedProposal) attempt.getCounterOffer();
//                counter.setProposalState(Proposal.ProposalState.REJECTED);
//                counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
//                attempt.setCounterOffer(counter);
//            }
//
//            String reason = "QuoteStatus/TradeEnded received";
//            if ( (text != null) && (!text.isEmpty()) ) {
//                reason+= " [" + text + "]";
//            }
//            operation.setStateResilient(new RejectedState("Order rejected (" + text + ")"), ErrorState.class);
		}
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		String handlerJobName = super.getDefaultTimerJobName();
		
		if (jobName.equals(handlerJobName)) {
		    LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);

			ClassifiedProposal counter = (ClassifiedProposal)operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal();
			counter.setProposalState(Proposal.ProposalState.REJECTED);
			counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
			operation.getLastAttempt().addExecutablePrice(new ExecutablePrice(counter), 0);
			operation.getLastAttempt().setAttemptState(AttemptState.EXPIRED);
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("BBGExecutionReportTImeout.0")), ErrorState.class);
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
