/*
 * Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.markets.tradeweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;

/**
 * All the instances of this class must be executed using a Thread. Purpose:
 * this class is mainly for notify the Bestx engine about new rejected market
 * orders. This happens if the TradeXpress platform's answer to an order is
 * Business Message Reject.
 * 
 * @author Davide Rossoni
 */

/*
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 */
public class OnOrderRejectRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnOrderRejectRunnable.class);

    private final TradewebMarket market;
    private final String reason;
    private final String quoteReqId;

    /**
     * @param quoteReqId
     *            the identifier of the rejected market order.
     * @param reason
     *            explains why the market order has been rejected.
     * @param market
     *            the TradeWeb market.
     */
    public OnOrderRejectRunnable(String quoteReqId, String reason, TradewebMarket market) {
        this.market = market;
        this.reason = reason;
        this.quoteReqId = quoteReqId;

    }

    @Override
    public void run() {
        try {
            final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TRADEWEB_CLORD_ID, quoteReqId);

            LOGGER.info("[MktMsg] OrderID {}, QuoteRequestReject received from {} , QuoteReqID {}, text: [{}]", operation.getOrder().getFixOrderId(), quoteReqId);

            // operation.getOrder().getFixOrderId() value is equal to quoteReqId
            // registered by OnTradeEndedRunnable
            // LOGGER.debug("Rejecting order {}, registering statistics.",
            // quoteReqId);
            market.getMarketStatistics().orderResponseReceived(quoteReqId, 0.0);

            operation.onMarketOrderReject(market, operation.getOrder(), reason, quoteReqId);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), quoteReqId, e);
        }

    }
}
