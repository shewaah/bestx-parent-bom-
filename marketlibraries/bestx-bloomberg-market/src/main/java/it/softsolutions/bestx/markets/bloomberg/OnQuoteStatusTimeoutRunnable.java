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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
public class OnQuoteStatusTimeoutRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnQuoteStatusTimeoutRunnable.class);
    private final BloombergMarket market;
    private final String dealer;
    private final String quoteReqId;
    private final String text;
    private final String quoteID;

    public OnQuoteStatusTimeoutRunnable(BloombergMarket market, String quoteReqId, String quoteID, String dealer, String text) {
        this.market = market;
        this.dealer = dealer;
        this.quoteReqId = quoteReqId;
        this.text = text;
        this.quoteID = quoteID;
    }

    @Override
    public void run() {
        try {
        	final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TSOX_CLORD_ID, quoteReqId);
            // operation.getOrder().getFixOrderId() value is equal to quoteReqId
            // registered by OnTradeEndedRunnable
            // LOGGER.debug("Rejecting order {}, registering statistics.", quoteReqId);
            
        	LOGGER.info("[MktMsg] OrderID {}, QuoteStatus/Timeout received from {} , QuoteReqID {}, QuoteID {}, text: [{}]", operation.getOrder().getFixOrderId(), quoteReqId, quoteID, text);
        	market.getMarketStatistics().orderResponseReceived(quoteReqId, 0.0);

            operation.onQuoteStatusTimeout(market, operation.getOrder(), quoteReqId);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), quoteReqId, e);
        }

    }
}
