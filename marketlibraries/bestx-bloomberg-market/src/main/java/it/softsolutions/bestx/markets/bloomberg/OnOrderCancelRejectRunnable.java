/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
 * All the instances of this class must be executed using a Thread. Purpose:
 * this class is mainly for notify the Bestx engine of an Order Cancel Reject.
 */

/*
 * Project Name : bestx-tradeweb-market First created by: ruggero.rizzo
 * Creation date: 09/feb/2015
 */
public class OnOrderCancelRejectRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnOrderCancelRejectRunnable.class);

    private final BloombergMarket market;
    private final String reason;
    private final String origClOrdId;

    /**
     * @param origClOrdId
     *            the identifier of the rejected order cancel.
     * @param reason
     *            explains why the order cancel has been rejected.
     * @param market
     *            the TradeWeb market.
     */
    public OnOrderCancelRejectRunnable(String origClOrdId, String reason, BloombergMarket market) {
        this.market = market;
        this.reason = reason;
        this.origClOrdId = origClOrdId;

    }

    @Override
    public void run() {
        try {
            final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TSOX_CLORD_ID, origClOrdId);

            LOGGER.info("[MktMsg] OrderID {}, OrderCancelReject received from {} , OrigClOrdID {}, text: [{}]", operation.getOrder().getFixOrderId(), origClOrdId, reason);

            market.getMarketStatistics().orderResponseReceived(origClOrdId, 0.0);

            operation.onMarketOrderCancelRequestReject(market, operation.getOrder(), reason);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), origClOrdId, e);
        }
    }
}
