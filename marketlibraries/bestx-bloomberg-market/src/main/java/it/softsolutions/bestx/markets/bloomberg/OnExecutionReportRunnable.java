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

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
public class OnExecutionReportRunnable implements Runnable {
    private final Operation operation;
    private final Market counterMarket;
    private final BigDecimal lastPrice;
    private final ExecType execType;
    private final OrdStatus ordStatus;
    private final String clOrdID;
    private final String contractNo;
    private final BloombergMarket market;
    private final Date futSettDate;
    private final Date transactTime;
    private final String text;

    private static final Logger LOGGER = LoggerFactory.getLogger(OnExecutionReportRunnable.class);

    public OnExecutionReportRunnable(Operation operation, BloombergMarket market, Market counterMarket, String clOrdID, ExecType execType, OrdStatus ordStatus, BigDecimal accruedInterestAmount,
            BigDecimal accruedInterestRate, BigDecimal lastPrice, String contractNo, Date futSettDate, Date transactTime, String text) {
        this.operation = operation;
        this.counterMarket = counterMarket;
        this.lastPrice = lastPrice;
        this.execType = execType;
        this.ordStatus = ordStatus;
        this.clOrdID = clOrdID;
        this.contractNo = contractNo;
        this.market = market;
        this.futSettDate = futSettDate;
        this.transactTime = transactTime;
        this.text = text;
    }

    public void run() {
        Order order = this.operation.getOrder();
        Rfq rfq = this.operation.getRfq();
        Thread.currentThread().setName(
                "OnExecutionReportRunnable-" + this.operation.toString() + "-ISIN:"
                        + ((order != null && order.getInstrument() != null) ? order.getInstrument().getIsin() : (rfq != null && rfq.getInstrument() != null) ? rfq.getInstrument().getIsin() : "XXXX"));
        if(order == null) {
        	operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
        	return;
        }

        // ClassifiedProposal executionProposal =
        // operation.getLastAttempt().getExecutionProposal();
        MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
        marketExecutionReport.setActualQty(order.getQty());
        marketExecutionReport.setInstrument(order.getInstrument());
        marketExecutionReport.setMarket(counterMarket);
        marketExecutionReport.setOrderQty(order.getQty());
        marketExecutionReport.setPrice(new Money(order.getCurrency(), lastPrice));
        marketExecutionReport.setLastPx(lastPrice);
        marketExecutionReport.setSide(order.getSide());

        // TODO is it ok to convert exectype and ignore ordstatus?
        // the opposite conversion is done by FixExecutionReportOutputLazyBean:
        // execType = ( state == ExecutionReport.ExecutionReportState.FILLED ? ExecutionReport.ExecutionReportState.FILLED.getValue() :
        // ExecutionReport.ExecutionReportState.REJECTED.getValue() );
        ExecutionReportState executionReportState;
        if ((execType == ExecType.Trade) && (ordStatus == OrdStatus.Filled)) {
            executionReportState = ExecutionReportState.FILLED;
        } else {
            executionReportState = ExecutionReportState.REJECTED;
        }
        LOGGER.info("orderID={}, mapped execType {} - ordStatus {} to executionRepostState {}", order.getFixOrderId(), execType, ordStatus, executionReportState);
        marketExecutionReport.setState(executionReportState);
        marketExecutionReport.setTransactTime(this.transactTime);
        marketExecutionReport.setSequenceId(null); // ID only
        // needed for sending to customer
        marketExecutionReport.setMarketOrderID(clOrdID); // <-- market order id is buy side's clOrdID
        marketExecutionReport.setTicket(contractNo);
        marketExecutionReport.setFutSettDate(futSettDate);
        marketExecutionReport.setText(text);
        // Stefano 20080704 - for future purpose
        // Confrontare la settlment date dell'ordine con la
        // BBSettlementDate devono essere uguali
        // Verificare che la currency sia uguale
        // Dall'instrument recupero il rateo per il calcolo
        // dell'interest amount con la formula
        // interestAmount = rateo * amount
        // dove amount e' la qty dell'ordine
        // marketExecutionReport.setAccruedInterestAmount(new
        // Money(order.getInstrument().getCurrency(),
        // accruedInterestAmount));
        // marketExecutionReport.setAccruedInterestRate(accruedInterestRate);
        marketExecutionReport.setAccruedInterestAmount(null);
        marketExecutionReport.setAccruedInterestRate(null);
        if (order.getInstrument().getBBSettlementDate() != null && order.getInstrument().getCurrency() != null && order.getInstrument().getRateo() != null
                && order.getFutSettDate().equals(order.getInstrument().getBBSettlementDate()) && order.getCurrency().equals(order.getInstrument().getCurrency())) {

            BigDecimal interestAmount;
            BigDecimal rateo = order.getInstrument().getRateo();
            BigDecimal qty = order.getQty();

            rateo = rateo.setScale(10);
            qty = qty.setScale(5);
            interestAmount = rateo.multiply(qty).divide(BigDecimal.valueOf(100.00));
            interestAmount = interestAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
            marketExecutionReport.setAccruedInterestAmount(new Money(order.getInstrument().getCurrency(), interestAmount));
            marketExecutionReport.setAccruedInterestRate(rateo);
        }
        operation.onMarketExecutionReport(market, order, marketExecutionReport);
    }
}
