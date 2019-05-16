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

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.handlers.MultipleQuotesHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.InternalAttempt;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectQuoteAndSendAutoNotExecutionState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_AcceptQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_SendInternalRfqState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectQuoteState;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: stefano 
 * Creation date: 28-nov-2012 
 * 
 **/
public class BBG_ReceiveQuoteEventHandler extends BaseOperationEventHandler {

	private static final long serialVersionUID = 583484200565578885L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BBG_ReceiveQuoteEventHandler.class);
    private MultipleQuotesHandler multipleQuoteHandler;

    /**
     * Instantiates a new BBG receive quote event handler.
     *
     * @param operation the operation
     * 
     */
    public BBG_ReceiveQuoteEventHandler(Operation operation, MultipleQuotesHandler multipleQuoteHandler) {
        super(operation);

        this.multipleQuoteHandler = multipleQuoteHandler;
    }

    @Override
    public void onNewState(OperationState currentState) {
        Order order = operation.getOrder();
        LOGGER.info("Order {} : received Counter offer {}", order.getFixOrderId(), operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal());

        boolean allConditionsOk = true;

        String logMsg = ""; 
        String rejectReason = ""; 
        Attempt lastAttempt = operation.getLastAttempt();
        ClassifiedProposal lastQuote = (ClassifiedProposal)lastAttempt.getExecutablePrice(0).getClassifiedProposal();

        // check if second best spread from best is ok
        boolean mustSendAutoNotExecution = false;
        double[] resultValues = new double[2];
        try {
            List<ClassifiedProposal> bookProposals = lastAttempt.getSortedBook().getValidSideProposals(order.getSide());
            BigDecimal bestPrice = bookProposals.get(0).getPrice().getAmount();
            Money counterOfferPrice = lastQuote.getPrice();
            BigDecimal counterOfferAmount = counterOfferPrice.getAmount();

            boolean counterConfirmsTheQuote = (bestPrice.equals(counterOfferAmount));
            if (!counterConfirmsTheQuote) {
                LOGGER.info("Order {} : quote ({}) is different from best ({}), verifying spread with 2nd best", order.getFixOrderId(), counterOfferAmount, bestPrice);
                if (BookHelper.isSpreadOverTheMax(bookProposals, order, resultValues)) {
                    allConditionsOk = false;
                    lastQuote.setProposalState(Proposal.ProposalState.REJECTED);
                    logMsg = "spread between best and second best too wide";
                    rejectReason = Messages.getString("BestBook.21", resultValues[0], resultValues[1]);
                    
                    mustSendAutoNotExecution = true;
                }
            }
        } catch (BestXException e) {
            LOGGER.error("Order {}, error while verifying quote spread.", order.getFixOrderId(), e);
            String errorMessage = e.getMessage();
            operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);

            return;
        }

        // check if settlement date is ok
        if (allConditionsOk) {
            if (!DateUtils.isSameDay(order.getFutSettDate(), operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getFutSettDate())) {
                allConditionsOk = false;
                lastQuote.setProposalState(Proposal.ProposalState.REJECTED);
                logMsg = "invalid settlement date";
                rejectReason = Messages.getString("DiscardSettlementDateProposalClassifier.0");
            }
        }

        // check if counter is better than limit price
        if (allConditionsOk) {
            Money counterOfferPrice = lastQuote.getPrice();
            BigDecimal counterOfferAmount = counterOfferPrice.getAmount();
            boolean isBetterThanLimit = true;
            if (order.getSide() == OrderSide.BUY) {
                isBetterThanLimit = order.getLimit() == null ||  counterOfferAmount.compareTo(order.getLimit().getAmount()) <= 0;
            } else {
                isBetterThanLimit = order.getLimit() == null ||  counterOfferAmount.compareTo(order.getLimit().getAmount()) >= 0;
            }
            if (!isBetterThanLimit) {
                lastQuote.setProposalState(Proposal.ProposalState.REJECTED);
                allConditionsOk = false;
                logMsg = "worse than limit price";
                rejectReason = Messages.getString("DiscardWorseThanLimitPrice.0");
            }
        }

        // check if counter is better than second best
        if (allConditionsOk) {
            Money counterOfferPrice = lastQuote.getPrice();
            BigDecimal counterOfferAmount = counterOfferPrice.getAmount();
            Proposal secondBest = null;
            if(lastAttempt.getSortedBook().getValidSideProposals(order.getSide()).size() > 1) {
                secondBest = lastAttempt.getSortedBook().getValidSideProposals(order.getSide()).get(1);
            }

            boolean isBetterThanSecondBest = true;
            if(secondBest != null) {
                if(order.getSide() == OrderSide.SELL){
                    isBetterThanSecondBest = counterOfferAmount.compareTo(secondBest.getPrice().getAmount()) >= 0;
                } else {
                    isBetterThanSecondBest = counterOfferAmount.compareTo(secondBest.getPrice().getAmount()) <= 0;
                }
            }
            if (!isBetterThanSecondBest) {
                allConditionsOk = false;
                logMsg = "worse than second best price";
                rejectReason = Messages.getString("DiscardWorseThanSecondBestPrice.0");
                lastQuote.setProposalState(Proposal.ProposalState.VALID);
                LOGGER.info("[INT-TRACE] Counter with price worse than second best, reject it, but keep the counter as valid for further attempts.");
            }
        }

        // check if qtys are compatible
        if (allConditionsOk) {
            boolean qtysCompatible = (order.getQty().compareTo(lastQuote.getQty()) == 0);
            if (!qtysCompatible) {
                logMsg = "incompatible qty";
                rejectReason = Messages.getString("DiscardInsufficientQuantityProposalClassifier.0");
                lastQuote.setProposalState(Proposal.ProposalState.REJECTED);
                allConditionsOk = false;
            }
        }

        if (allConditionsOk) {
        	
            // normal operation or internalization
        	InternalAttempt internalAttempt = operation.getInternalAttempt();
            Money counterOfferPrice = lastQuote.getPrice();

            if (internalAttempt != null) {  // must internalize with internalAttempt's owner 
                internalAttempt.getMarketOrder().setLimit(counterOfferPrice);
                internalAttempt.getMarketOrder().setVenue(internalAttempt.getExecutionProposal().getVenue());

                operation.setStateResilient(new BBG_INT_SendInternalRfqState(Messages.getString("BBG_INT_BestPrice", counterOfferPrice.getAmount(), lastQuote.getVenue().getCode(), DateService.format("dd/MM/yyyy", lastQuote.getFutSettDate()))), ErrorState.class);
            }
            else {
                operation.setStateResilient(new BBG_AcceptQuoteState(), ErrorState.class);
            }
        } else {
            lastQuote.setReason(rejectReason);
            lastAttempt.addExecutablePrice(new ExecutablePrice(lastQuote), 0);

            if (mustSendAutoNotExecution) {
                LOGGER.info("[INT-TRACE] Order {} : quote is not valid ({}), 2nd best spread exceeded, rejecting it and sending not execution", order.getFixOrderId(), logMsg);
                operation.setStateResilient(new RejectQuoteAndSendAutoNotExecutionState(rejectReason, MarketCode.BLOOMBERG), ErrorState.class);
            }
            else {
                LOGGER.info("[INT-TRACE] Order {} : quote is not valid ({}), rejecting it", order.getFixOrderId(), logMsg);
                operation.setStateResilient(new BBG_RejectQuoteState(rejectReason), ErrorState.class);
            }
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
