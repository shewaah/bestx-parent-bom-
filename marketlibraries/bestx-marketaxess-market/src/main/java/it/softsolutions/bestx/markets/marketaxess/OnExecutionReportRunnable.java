
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

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ExecType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.OrdStatus;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 01 feb 2017
 * 
 **/

public class OnExecutionReportRunnable implements Runnable {
    private final Operation operation;
    private final MarketAxessMarket market;
    private final MarketAxessExecutionReport executionReport;

    private static final Logger LOGGER = LoggerFactory.getLogger(OnExecutionReportRunnable.class);

    /**
     * @param operation
     *            the state macchine of the Bestx engine.
     * @param market
     *            the Tradeweb Market.
     * @param executionReport
     *            the received market execution report
     */
    public OnExecutionReportRunnable(Operation operation, MarketAxessMarket market, MarketAxessExecutionReport executionReport) {
        this.operation = operation;
        this.market = market;
        this.executionReport = executionReport;
     }

    @Override
	public void run() {
        Order order = this.operation.getOrder();
        if(order == null) {
        	operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
        	return;
        }
        executionReport.setMarket(market.getMarket());
        executionReport.setSide(order.getSide());
        char ordStatus = executionReport.getOrdStatus();
        char execType = executionReport.getExecType().charAt(0);  //convert String to char
        ExecutionReportState executionReportState = null;
        switch(ordStatus) {
        case OrdStatus.FILLED:
            if (execType == ExecType.TRADE || execType == ExecType.TRADE_CORRECT) {
                executionReportState = ExecutionReportState.FILLED;
            }
            break;
        case OrdStatus.CANCELED:
        case OrdStatus.EXPIRED:
        	executionReportState = ExecutionReportState.CANCELLED;
        	break;
        case OrdStatus.NEW:
        	executionReportState = ExecutionReportState.NEW;
        	break;
        case OrdStatus.REJECTED:
        	executionReportState = ExecutionReportState.REJECTED;
			executionReport.setReason(MarketExecutionReport.RejectReason.TECHNICAL_FAILURE);
        	break;
		default:
			LOGGER.error("Order {}, exec report with OrdStatus {} unexpected, cannot map to an ExecutioReportState", order.getFixOrderId(), ordStatus);
			break;
        }
        
        LOGGER.info("orderID={}, mapped execType={} - ordStatus={} to executionReportState={}", order.getFixOrderId(), execType, ordStatus, executionReportState);
        executionReport.setState(executionReportState);
        executionReport.setSequenceId(null); // ID only
        if (order.getInstrument().getBBSettlementDate() != null && 
              order.getInstrument().getCurrency() != null && 
              order.getInstrument().getRateo() != null && 
              order.getFutSettDate() != null &&
              order.getFutSettDate().equals(order.getInstrument().getBBSettlementDate()) && 
              order.getCurrency().equals(order.getInstrument().getCurrency())) {

            BigDecimal interestAmount;
            BigDecimal rateo = order.getInstrument().getRateo();
            BigDecimal qty = order.getQty();

            rateo = rateo.setScale(10);
            qty = qty.setScale(5);
            interestAmount = rateo.multiply(qty).divide(BigDecimal.valueOf(100.00));
            interestAmount = interestAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
            executionReport.setAccruedInterestAmount(new Money(order.getInstrument().getCurrency(), interestAmount));
            executionReport.setAccruedInterestRate(rateo);
        }
        

        operation.onMarketExecutionReport(market, order, executionReport);
    }
    
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(this.getClass().getName());
		str.append(" for Order ");
		str.append(operation.getOrder().getFixOrderId());
		str.append(" and MarketOrder ");
		str.append(operation.getLastAttempt().getMarketOrder().getMarketSessionId());
		return super.toString();
	}
}