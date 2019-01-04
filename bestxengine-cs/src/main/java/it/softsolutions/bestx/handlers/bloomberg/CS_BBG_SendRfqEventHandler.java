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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.exceptions.ConnectionNotAvailableException;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.handlers.InternalizationHelper;
import it.softsolutions.bestx.handlers.LimitFileHelper;
import it.softsolutions.bestx.handlers.MultipleQuotesHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_ReceiveQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class CS_BBG_SendRfqEventHandler extends BaseOperationEventHandler {
    private static final long serialVersionUID = -3944107875602225118L;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CS_BBG_SendRfqEventHandler.class);
    private final SerialNumberService serialNumberService;

    private final MarketBuySideConnection buySideConnection;
    protected final long waitingAnswerDelay;
    private MultipleQuotesHandler multipleQuoteHandler;

    private Set<String> technicalRejectReasons;
    
    //String lastSentQuoteReqID = "";

    /**
     * Instantiates a new BBG event handler
     *
     * @param operation the operation
     * @param bbgConnection the bbg connection
     * @param serialNumberService the serial number service
     * @param waitingAnswerDelay the waiting answer delay
     * @param multipleQuoteHandler the multiple quote handler
     * 
     */
    public CS_BBG_SendRfqEventHandler(Operation operation, MarketBuySideConnection bbgConnection, SerialNumberService serialNumberService, long waitingAnswerDelay, MultipleQuotesHandler multipleQuoteHandler,
                    Set<String> technicalRejectReasons ) {
        super(operation);
        this.buySideConnection = bbgConnection;
        this.waitingAnswerDelay = waitingAnswerDelay;
        this.serialNumberService = serialNumberService;
        this.multipleQuoteHandler = multipleQuoteHandler;
        this.technicalRejectReasons = technicalRejectReasons;
    }

    @Override
    public void onNewState(OperationState currentState) {
        try {
            setupDefaultTimer(waitingAnswerDelay, false);

            // sendRfq binds sets clOrdID as marketSessionID to marketOrder, 
            // and binds it to Operation : (OperationIdType.TSOX_CLORD_ID, clOrdID) to  
            buySideConnection.sendRfq(operation, operation.getLastAttempt().getMarketOrder());
            //lastSentQuoteReqID = operation.getLastAttempt().getMarketOrder().getMarketSessionId();
        } catch (ConnectionNotAvailableException e) { 
            LOGGER.error("TSOX Connection not available, can not send rfq to BBG: {}", e.getMessage(), e);
            try {
                ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
                String message = e.getMessage();
                
                if (operation.getOrder().isLimitFile()) {
                    //send back to OTEX their original message, they can understand what happend in other ways
                    //coming here the order must be a Limit File, not a Limit File No Price
                    String orderComment = operation.getOrder().getText();
                    orderComment = LimitFileHelper.getInstance().getCommentWithPrefix(orderComment, false);
                    operation.getOrder().setText(orderComment);
                }
                
                operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
            } catch (BestXException e1) {
                LOGGER.error("An error occurred while sending execution failure to BBG", e1);
                operation.setStateResilient(new WarningState(currentState, e1, Messages.getString("BBGMarketSendRFQRejectError.0")), ErrorState.class);
            }
        } catch (BestXException e2) {
            LOGGER.error("An error occurred while sending rfq to BBG", e2);
            operation.setStateResilient(new WarningState(currentState, e2, Messages.getString("BBGMarketSendRFQRejectError.0")), ErrorState.class);
        }
    }

    // can receive quote updates
    @Override
    public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal quote) {
        // see comment above (before .sendRfq): clOrdID is the marketOrder's marketSessioID
        String orderQuoteReqId = operation.getLastAttempt().getMarketOrder().getMarketSessionId();
        String quoteQuoteReqId = quote.getQuoteReqId();
        //String orderQuoteReqId = lastSentQuoteReqID;
        
        if (orderQuoteReqId != null && orderQuoteReqId.equals(quoteQuoteReqId)) {
            try {
                multipleQuoteHandler.manageQuote(quote, operation.getState().getClass().getSimpleName());
            }
            catch (BestXException e) {
                LOGGER.error("Error while notifying Quote");
                operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("InvalidQuoteIDError.0")), ErrorState.class);
                return;
            }


            //[RR20130307] Here we should receive only quotes from the external broker
            if (!InternalizationHelper.isInternalQuote(quote.getMarket().getMarketCode(), quote.getQuoteReqId())) {
                stopDefaultTimer();
                operation.getLastAttempt().addExecutablePrice(new ExecutablePrice(quote), 0);
//                try {
//                    buySideConnection.ackProposal(operation, quote);
//                } catch (BestXException e) {
//                    LOGGER.error("Could not send QuoteAck message for quoteReqId {}.", quoteQuoteReqId);
//                    operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventStateTimeout.0")), ErrorState.class);
//                }
                operation.setStateResilient(new BBG_ReceiveQuoteState(), ErrorState.class);
            } else {
                LOGGER.warn("Order {}, Internal quote received, id {}, but external broker quote expected, ignore this quote.", operation.getOrder().getFixOrderId(), quote.getQuoteReqId());
            }
        } else {
            LOGGER.warn("Received quote for quoteReqId {}, expected quoteReqId {}. Ignore the message.", quoteQuoteReqId, orderQuoteReqId);
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
    
    @Override
    public void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text) {
        LOGGER.info("OrderID [{}], received and ignored QuoteStatus/TradeEnded, waiting for ExecutionReport/Canceled as order reject from market", order.getFixOrderId());
    }
    
    
    @Override
    public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
        if (source.getMarketCode() != MarketCode.BLOOMBERG) {
            return;
        }
        
        String reason = marketExecutionReport.getText();
        //order.getCustomerOrderId() // clOrdID
        //marketExecutionReport.getClOrdID (non esiste...)
        if (marketExecutionReport.getState() == ExecutionReportState.REJECTED) {
            //rfq for the best broker, we could receive reject related to the last attempts
            String rejectQuoteReqId = operation.getLastAttempt().getMarketOrder().getMarketSessionId();
            // sessionId is the quoteReqId returned by the market
            stopDefaultTimer();
            // originating from a market reject there is no need to send a revoke to the market and we have no fills to manage
            LOGGER.info("Managing a market reject for orderID {}", order.getFixOrderId());
            if (!checkCustomerRevoke(order)) {
                for (Attempt attempt : operation.getAttempts()) {
                    if (attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null && attempt.getExecutablePrice(0).getClassifiedProposal().getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
                        ClassifiedProposal counter = (ClassifiedProposal) attempt.getExecutablePrice(0).getClassifiedProposal();
                        counter.setProposalState(Proposal.ProposalState.REJECTED);
                        counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
                        attempt.addExecutablePrice(new ExecutablePrice(counter), 0);
                    }
                }
                if (reason != null && reason.length() > 0) {
                    boolean isTechnicalReject = false;
                    for (String r : technicalRejectReasons) {
                        if (reason.toLowerCase().contains(r)) {
                            isTechnicalReject = true;
                            LOGGER.info("Order {}, must send technical Reject as reject reason ({}) matches one of the configured ones ({})", order.getFixOrderId(), reason, r);
                            break;
                        }
                    }
                    operation.setStateResilient(new BBG_RejectedState(reason, isTechnicalReject), ErrorState.class);
                } else {
                    operation.setStateResilient(new BBG_RejectedState("", false), ErrorState.class);
                }
            }
        }
        else {
            LOGGER.info("Unexpected ExecutionReport state for orderID {}, notifying to default handler", order.getFixOrderId());
            super.onMarketExecutionReport(source, order, marketExecutionReport);
        }

    }
    
// should be used if QuoteRequest is sent and rejected, instead of NewOrderSingle    
//    @Override
//    public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String orderQuoteReqId) {
//        if (source.getMarketCode() != MarketCode.BLOOMBERG) {
//            return;
//        }
//        
//        //rfq for the best broker, we could receive reject related to the last attempts
//        String rejectQuoteReqId = operation.getLastAttempt().getMarketOrder().getMarketSessionId();
//        // sessionId is the quoteReqId returned by the market
//        if (orderQuoteReqId != null && orderQuoteReqId.equals(rejectQuoteReqId)) {
//            stopDefaultTimer();
//            // originating from a market reject there is no need to send a revoke to the market and we have no fills to manage
//            LOGGER.info("Managing a market reject, we have already received a revoke, start processing the last one.");
//            if (!checkCustomerRevoke(order)) {
//                for (Attempt attempt : operation.getAttempts()) {
//                    if (attempt.getCounterOffer() != null && attempt.getCounterOffer().getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
//                        ClassifiedProposal counter = (ClassifiedProposal) attempt.getCounterOffer();
//                        counter.setProposalState(Proposal.ProposalState.REJECTED);
//                        counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
//                        attempt.setCounterOffer(counter);
//                    }
//                }
//                if (reason != null && reason.length() > 0) {
//                    operation.setStateResilient(new BBG_RejectedState(reason), ErrorState.class);
//                } else {
//                    operation.setStateResilient(new BBG_RejectedState(""), ErrorState.class);
//                }
//            }
//        } else {
//            LOGGER.warn("Received reject for quoteReqId {}, expected quoteReqId {}. Ignore the message.", rejectQuoteReqId, orderQuoteReqId);
//        }
//    }

    @Override
    public void onTimerExpired(String jobName, String groupName) {
		String handlerJobName = super.getDefaultTimerJobName();
    	
        if (jobName.equals(handlerJobName)) {
            LOGGER.info("[Timeout] No answer from MM for orderID {}, quoteReqID {}", operation.getOrder().getFixOrderId(), operation.getLastAttempt().getMarketOrder().getMarketSessionId());

            for (Attempt attempt : operation.getAttempts()) {
                if (attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null && attempt.getExecutablePrice(0).getClassifiedProposal().getVenue().getCode().equalsIgnoreCase(operation.getLastAttempt().getExecutionProposal().getVenue().getCode())) {
                    ClassifiedProposal counter = (ClassifiedProposal) attempt.getExecutablePrice(0).getClassifiedProposal();
                    counter.setProposalState(Proposal.ProposalState.REJECTED);
                    counter.setReason(Messages.getString("DiscardTriedInEarlierAttemptProposalClassifier.0"));
                    attempt.addExecutablePrice(new ExecutablePrice(counter), 0);
                }
            }
            operation.setStateResilient(new BBG_RejectedState(Messages.getString("EventRfqNoAnswer.0"), false), ErrorState.class);
        }
        else {
            super.onTimerExpired(jobName, groupName);
        }
    }
}
