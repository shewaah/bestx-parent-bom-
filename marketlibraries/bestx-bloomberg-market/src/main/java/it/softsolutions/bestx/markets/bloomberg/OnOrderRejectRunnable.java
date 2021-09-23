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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
public class OnOrderRejectRunnable implements Runnable {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(OnOrderRejectRunnable.class);
    
    private final BloombergMarket market;
    private final String reason;
    private final String quoteReqId;

    public OnOrderRejectRunnable(String quoteReqId, String reason, BloombergMarket market) {
        this.market = market;
        this.reason = reason;
        this.quoteReqId = quoteReqId;

    }

    @Override
    public void run() {
        try {
            final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TSOX_CLORD_ID, quoteReqId);
            Attempt currentAttempt = operation.getLastAttempt();
            if (currentAttempt == null) {
                LOGGER.error("No current Attempt found");
                operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("TWMarketAttemptNotFoundError.0")), ErrorState.class);
                return;
            }
            List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
            if (marketExecutionReports == null) {
                marketExecutionReports = new ArrayList<MarketExecutionReport>();
                currentAttempt.setMarketExecutionReports(marketExecutionReports);
            }

            LOGGER.info("[MktMsg] OrderID {}, QuoteRequestReject received from {} , QuoteReqID {}, text: [{}]", 
                  operation.getOrder().getFixOrderId(), market.getMarketCode(), quoteReqId, reason);
            
            // operation.getOrder().getFixOrderId() value is equal to quoteReqId
            // registered by OnTradeEndedRunnable
            // LOGGER.debug("Rejecting order {}, registering statistics.", quoteReqId);
            market.getMarketStatistics().orderResponseReceived(quoteReqId, 0.0);
            
            MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
            marketExecutionReport.setState(ExecutionReportState.REJECTED);
            marketExecutionReport.setMarket(market.getMarket());
            marketExecutionReports.add(marketExecutionReport);

            operation.onMarketOrderReject(market, operation.getOrder(), reason, quoteReqId);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), quoteReqId, e);
        }

    }
}
