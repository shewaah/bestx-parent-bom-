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

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;

/**
 * All the instances of this class must be executed using a Thread. Purpose:
 * this class is mainly for notify the Bestx engine about new execution report.
 * This happens if the TradeXpress platform's answer to an order is
 * ExecutionReport.
 * @author Davide Rossoni
 * 
 */

/*
 * Purpose: this class is mainly for ... Project Name : bestx-tradeweb-market
 * First created by: davide.rossoni Creation date: 19/dec/2014
 */
public class OnExecutionReportRunnable implements Runnable {

   private final Operation operation;
   private final Market counterMarket;
   private final BigDecimal lastPrice;
   private final ExecType execType;
   private final OrdStatus ordStatus;
   private final String clOrdID;
   private final String contractNo;
   private final TradewebMarket market;
   private final Date futSettDate;
   private final Date transactTime;
   private final String text;
   private final MarketMarketMaker executionBroker;
   private final String executionDealerCode;
   private String micCode;
   private BigDecimal accruedInterestAmount;
   private BigDecimal accruedInterestRate;
   private Integer numDaysInterest;
   private final String settlType;

   //BESTX-385: SP-20190116 manage factor (228) field
   private BigDecimal factor;

   private static final Logger LOGGER = LoggerFactory.getLogger(OnExecutionReportRunnable.class);

   /**
    * @param marketMakerFinder
    * 			needed to find the market maker from the dealer
    * @param operation
    *            the state macchine of the Bestx engine.
    * @param market
    *            the Tradeweb Market.
    * @param counterMarket
    *            the counter market
    * @param clOrdID
    *            the identifier of the corresponding MDSClient order.
    * @param execType
    *            the purpose of the execution report (New, Trade, etc.).
    * @param ordStatus
    *            the current state of the order(New,Filled, Cancel, etc.).
    * @param accruedInterestAmount
    *            the amount of accrued interest.
    * @param accruedInterestRate
    *            the annualized accrued interest amount divided by the purchase
    *            price.
    * @param lastPrice
    *            the price of this order if it has been filled.
    * @param contractNo
    *            the unique identifier of the corresponding execution report as
    *            assigned by sell-side.
    * @param futSettDate
    *            the date of trade settlement.
    * @param transactTime
    *            the time of execution/order creation.
    * @param text
    *            the text. Can contain additional informations.
    * @param executionBroker
    * 				the execution firm code. May be null if executionDealerCode is not among the recognised market codes
    * @param executionDealerCode
    * 				the execution firm code as sent by the market
    * @param micCode
    * 				the MIC code of the execution MTF
    * @param numDaysInterest
    * 				the days of interest for the accrual calculation
    * @param factor
    * 				the factor (may be null since it is sent for inflation linked bonds only)
    */
   public OnExecutionReportRunnable(Operation operation, TradewebMarket market, Market counterMarket, String clOrdID, ExecType execType, OrdStatus ordStatus, BigDecimal accruedInterestAmount,
         BigDecimal accruedInterestRate, BigDecimal lastPrice, String contractNo, Date futSettDate, Date transactTime, String text, MarketMarketMaker executionBroker, String executionDealerCode,
         String micCode, Integer numDaysInterest, BigDecimal factor, String settlType){
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
      this.executionDealerCode = executionDealerCode;
      this.executionBroker = executionBroker;
      this.micCode = micCode;
      this.accruedInterestAmount = accruedInterestAmount;
      this.accruedInterestRate = accruedInterestRate;
      this.numDaysInterest = numDaysInterest;
      this.factor = factor;
      this.settlType = settlType;
   }

   @Override
   public void run() {
      LOGGER.debug("Running into executor message with status {} for order {} for management", ordStatus, clOrdID);
      Order order = this.operation.getOrder();
      Rfq rfq = this.operation.getRfq();
      MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
      if (order == null) {
         operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
         return;
      }

      // ClassifiedProposal executionProposal =
      // operation.getLastAttempt().getExecutionProposal();
      marketExecutionReport.setActualQty(order.getQty());
      marketExecutionReport.setInstrument(order.getInstrument());
      marketExecutionReport.setMarket(counterMarket);
      marketExecutionReport.setOrderQty(order.getQty());
      marketExecutionReport.setPrice(order.getLimit());
      marketExecutionReport.setLastPx(lastPrice);
      marketExecutionReport.setSide(order.getSide());
      marketExecutionReport.setLastMkt(micCode);
      // TODO is it ok to convert exectype and ignore ordstatus?
      // the opposite conversion is done by FixExecutionReportOutputLazyBean:
      // execType = ( state == ExecutionReport.ExecutionReportState.FILLED ?
      // ExecutionReport.ExecutionReportState.FILLED.getValue() :
      // ExecutionReport.ExecutionReportState.REJECTED.getValue() );
      ExecutionReportState executionReportState = null;
      switch (ordStatus) {
         case Filled:
            if (execType == ExecType.Trade) {
               executionReportState = ExecutionReportState.FILLED;
               if (this.lastPrice == null || this.lastPrice.equals(new BigDecimal("0"))) {
                  operation.onApplicationError(operation.getState(), null, Messages.getString("NoExecutionPriceError.0"));
                  return;
               }
            }
         break;
         case Canceled:
            executionReportState = ExecutionReportState.CANCELLED;
         break;
         case New:
            executionReportState = ExecutionReportState.NEW;
         break;
         case Rejected:
            executionReportState = ExecutionReportState.REJECTED;
         break;
         default:
            LOGGER.error("Order {}, exec report with OrdStatus {} unexpected, cannot map to an ExecutioReportState", order.getFixOrderId(), ordStatus);
         break;
      }

      LOGGER.info("orderID={}, mapped execType={} - ordStatus={} to executionReportState={}", order.getFixOrderId(), execType, ordStatus, executionReportState);
      marketExecutionReport.setState(executionReportState);
      marketExecutionReport.setTransactTime(this.transactTime);
      marketExecutionReport.setSequenceId(null); // ID only
      // needed for sending to customer
      marketExecutionReport.setMarketOrderID(clOrdID); // <-- market order id
                                                       // is buy side's
                                                       // clOrdID
      marketExecutionReport.setTicket(contractNo);
      marketExecutionReport.setFutSettDate(futSettDate);
      marketExecutionReport.setSettlType(settlType);
      marketExecutionReport.setText(text);
      marketExecutionReport.setAccruedInterestAmount(new Money(order.getInstrument().getCurrency(), accruedInterestAmount));
      marketExecutionReport.setAccruedInterestRate(accruedInterestRate);
      if (order.getInstrument().getBBSettlementDate() != null && order.getInstrument().getCurrency() != null && order.getInstrument().getRateo() != null && order.getFutSettDate() != null
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
      if (executionBroker != null) {
         marketExecutionReport.setMarketMaker(executionBroker.getMarketMaker());
         marketExecutionReport.setExecBroker(executionBroker.getMarketSpecificCode());
      }
      else {
         // use executionDealerCode
         LOGGER.info("IMPORTANT! Tradeweb returned dealer {} not configured in BestX!. Please configure it", executionDealerCode);
         marketExecutionReport.setExecBroker(executionDealerCode);
      }
      //BESTX-348: SP-20180905 added numDaysInterest field
      if (numDaysInterest != null) {
         marketExecutionReport.setAccruedInterestDays(numDaysInterest);
      }
      if (factor != null) {
         marketExecutionReport.setFactor(factor);
      }

      operation.onMarketExecutionReport(market, order, marketExecutionReport);
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
