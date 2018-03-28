
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.markets.marketaxess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 19 feb 2017
 * 
 **/

public class OnOrderCancelRejectRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnOrderCancelRejectRunnable.class);

    final private MarketAxessMarket market;
	final private String reason;
	final private String clOrdId;
	final private String ordStatus;

	public OnOrderCancelRejectRunnable(String clOrdId, String reason, String ordStatus, MarketAxessMarket market) {
		this.clOrdId = clOrdId;
		this.reason = reason;
		this.market = market;
		this.ordStatus = ordStatus;
	}
	
	@Override
	public void run() {
        try {
            final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.MARKETAXESS_CLORD_ID, clOrdId);

            LOGGER.info("[MktMsg] OrderID {}, OrderCancelReject received from {} , OrigClOrdID {}, text: [{}]", operation.getOrder().getFixOrderId(), clOrdId, reason);

            market.getMarketStatistics().orderResponseReceived(clOrdId, 0.0);

            operation.onMarketOrderCancelRequestReject(market, operation.getOrder(), reason + " - OrdStatus = " + ordStatus);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), clOrdId, e);
        }
    }
}
