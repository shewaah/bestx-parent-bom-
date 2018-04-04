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
package it.softsolutions.bestx.markets.bloomberg;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Rfq;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
public class OnCounterRunnable implements Runnable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OnCounterRunnable.class);
    private final BloombergMarket market;
    private final String quoteReqId;
    private final String quoteId;
    private final ClassifiedProposal proposal;

    public OnCounterRunnable(BloombergMarket market, String quoteReqId, String quoteId, ClassifiedProposal proposal) {

        this.market = market;
        this.quoteReqId = quoteReqId;
        this.quoteId = quoteId;
        this.proposal = proposal;
    }

    @Override
    public void run() {
        try {
        	final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TSOX_CLORD_ID, quoteReqId);
        	LOGGER.info("[temp] OnCounterRunnable operationID={}, QuoteID {}, start run", operation.getId(), quoteId);
            // operation.getOrder().getFixOrderId() value is equal to quoteReqId
            Order order = operation.getOrder();
            Rfq rfq = operation.getRfq();

            Thread.currentThread().setName(
                    "OnCounterRunnable-"
                            + operation.toString()
                            + "-ISIN:"
                            + ((order != null && order.getInstrument() != null) ? order.getInstrument().getIsin() : (rfq != null && rfq.getInstrument() != null) ? rfq.getInstrument().getIsin()
                                    : "XXXX"));

            // allow multiple quote updates only for internalized states
            boolean allowMultipleQuotes = ((BaseState) operation.getState()).areMultipleQuotesAllowed();
            String stateName = ((BaseState) operation.getState()).getClass().getSimpleName();
            
            boolean mustReject = false;
            if (operation.isProcessingCounter() && !(allowMultipleQuotes)) {
                mustReject = true;
            }
            
            final String orderId = operation.getOrder().getFixOrderId();

            if (mustReject) {
                LOGGER.info("[INT-TRACE] [{}CNT] {} discarding counter, order state does not consider this quote: quoteReqID={}, quoteID={}, state={} - rejecting it", market.getMarketCode(), 
                		orderId, quoteReqId, quoteId, stateName);

                BigDecimal price = (proposal.getPrice() != null ? proposal.getPrice().getAmount() : null);
                BigDecimal qty = proposal.getQty();
                ProposalSide side = proposal.getSide();
                String mm = (proposal.getMarketMarketMaker() != null ? proposal.getMarketMarketMaker().getMarketSpecificCode() : "");
                LOGGER.info("[{}CNT],CounterData,quoteReqId: {} ,marketMaker: {} ,quoteId: {} ,price: {}, quantity: {},side: {}", market.getMarketCode(),
                                quoteReqId, mm, quoteId, 
                                (price != null ? price.toPlainString() : price), (qty != null ? qty.toPlainString() : qty), (side != null ? side.name() : side));
            }
            else {
                LOGGER.debug("[{}CNT] operationID={}, setting flag to true", operation.getId(), market.getMarketCode());
                operation.setProcessingCounter(true);

                Instrument instrument = operation.getOrder().getInstrument();

                //BXMNT-356: counterproposals must be of type COUNTER
                proposal.setType(Proposal.ProposalType.COUNTER);
                
                operation.onMarketProposal(market, instrument, proposal);
            }
            
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with {} QuoteReqId {}", market.getMarketCode(), quoteReqId, e);
        }
        LOGGER.info("[temp] OnCounterRunnable QuoteID {}, end run", quoteId);
    }
}
