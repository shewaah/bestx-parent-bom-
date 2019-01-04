/*
* Copyright 1997-2018 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.CSOrdersEndOfDayService;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;
import it.softsolutions.jsscommon.Money;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : git-bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 12 lug 2018 
* 
**/
public class ParkedOrderEventHandler extends CSBaseOperationEventHandler {
   private static final long serialVersionUID = -5786572071857613045L;
   private static final Logger LOGGER = LoggerFactory.getLogger(ParkedOrderEventHandler.class);
   
   private final SerialNumberService serialNumberService;
   private final MarketFinder marketFinder;
   private final AutoCurandoStatus autoCurandoStatus;
   
   /**
    * @param operation
    */
   public ParkedOrderEventHandler(Operation operation, SerialNumberService serialNumberService, MarketFinder marketFinder, AutoCurandoStatus autoCurandoStatus){
      super(operation);
      this.serialNumberService = serialNumberService;
      this.marketFinder = marketFinder;
      this.autoCurandoStatus = autoCurandoStatus;
   }
   
   
   @Override
   public void onStateRestore(OperationState currentState) {
      Order order = operation.getOrder();
      // next datetime --> first time after this exact moment
      if (operation.getOrder().isLimitFile()) {
         long nextTimer = getTimerInterval(order);
         if (nextTimer > 0) {
            setupDefaultTimer(nextTimer, false);
            LOGGER.debug("set next timer in {} seconds", nextTimer / 1000);
         } else {
            operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
         }
      }
   }
   
   
   @Override
   public void onNewState(OperationState currentState) {
      Order order = operation.getOrder();
      // next datetime --> first time after this exact moment
      if (operation.getOrder().isLimitFile()) {
         long nextTimer = getTimerInterval(order);
         if (nextTimer > 0) {
            setupDefaultTimer(nextTimer, false);
            LOGGER.debug("set next timer in {} seconds", nextTimer / 1000);
         } else {
            operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
         }
      }
   }

   @Override
   public void onTimerExpired(String jobName, String groupName) {
      String handlerJobName = super.getDefaultTimerJobName();

      if (jobName.equalsIgnoreCase(handlerJobName)) {
         operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
      }
      else {
         super.onTimerExpired(jobName, groupName);
      }
  }
   
   protected long getTimerInterval(Order order) {
      long now = DateService.currentTimeMillis();
      
      if (order.getEffectiveTime() != null) {
         long effectiveTimeMillis = order.getEffectiveTime().getTime();
         
         if (effectiveTimeMillis > now) {
            return effectiveTimeMillis - now;
         }
      } else {
         if (order.getTryAfterMinutes() > 0) {
            return order.getTryAfterMinutes() * 60 * 1000; //need milliseconds but value is in minutes
         }
      }
      return 0;
   }
   
   @Override
   public void onOperatorSendDDECommand(OperatorConsoleConnection source,
         String comment) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onOperatorManualManage(OperatorConsoleConnection source,
         String comment) {
      stopDefaultTimer();
      operation.setStateResilient(new ManualExecutionWaitingPriceState(
            comment), ErrorState.class);
   }

   @Override
   public void onOperatorRetryState(OperatorConsoleConnection source,
         String comment) {
      // RIPROVA ORDINE
      operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
   }

   @Override
   public void onOperatorManualExecution(OperatorConsoleConnection source,
         String comment, Money price, BigDecimal qty, String mercato,
         String marketMaker, Money prezzoRif) {
      try {
         stopDefaultTimer();

         ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(
               operation.getOrder().getInstrument(),
                  operation.getOrder().getSide(),
                     this.serialNumberService);

         // Creation of the market execution reports for AMOS (sending of
         // fills)
         MarketExecutionReport mktExecRep = new MarketExecutionReport();
         mktExecRep.setInstrument(operation.getOrder().getInstrument());
         mktExecRep.setSide(operation.getOrder().getSide());
         mktExecRep.setTransactTime(DateService.newLocalDate());
         long mktExecutionReportId = serialNumberService
               .getSerialNumber("EXEC_REP");
         mktExecRep.setSequenceId(Long.toString(mktExecutionReportId));

         newExecution.setPrice(prezzoRif);
         newExecution.setAveragePrice(prezzoRif.getAmount());
         newExecution.setActualQty(qty);
         newExecution.setState(ExecutionReportState.FILLED);

         mktExecRep.setPrice(prezzoRif);
         mktExecRep.setAveragePrice(prezzoRif.getAmount());
         mktExecRep.setActualQty(qty);
         mktExecRep.setState(ExecutionReportState.FILLED);

         String subMarkets = "";
         for (int i = 0; i < Market.SubMarketCode.values().length; i++) {
            subMarkets += Market.SubMarketCode.values()[i] + "_";
         }
         Market tmpMkt = null;
         if (subMarkets.indexOf(marketMaker) >= 0) {
            tmpMkt = marketFinder.getMarketByCode(
                  Market.MarketCode.valueOf(mercato),
                  Market.SubMarketCode.valueOf(marketMaker));
            newExecution.setMarket(tmpMkt);
            mktExecRep.setMarket(tmpMkt);
         } else {
            tmpMkt = marketFinder.getMarketByCode(
                  Market.MarketCode.valueOf(mercato), null);
            newExecution.setMarket(tmpMkt);
            mktExecRep.setMarket(tmpMkt);
         }

         newExecution.setExecBroker(marketMaker);
         // set counterpart for AMOS
         newExecution.setCounterPart(marketMaker);

         mktExecRep.setExecBroker(marketMaker);
         mktExecRep.setCounterPart(marketMaker);

         if (price == null
               || price.getAmount().compareTo(prezzoRif.getAmount()) == 0) {
            newExecution.setLastPx(prezzoRif.getAmount());
            mktExecRep.setLastPx(prezzoRif.getAmount());
         } else {
            newExecution.setLastPx(price.getAmount());
            mktExecRep.setLastPx(price.getAmount());
         }

         if (operation.getLastAttempt().getMarketExecutionReports() == null) {
            operation.getLastAttempt().setMarketExecutionReports(
                  new ArrayList<MarketExecutionReport>());
         }
         operation.getLastAttempt().getMarketExecutionReports()
               .add(mktExecRep);
         LOGGER.info("Manual execution, execution report {}",
               newExecution.toString());
         operation.getExecutionReports().add(newExecution);
         if (newExecution.getMarket() != null) {
            comment = getCommentWithPrefix(comment);
            operation.getOrder().setText(comment);
            operation.setStateResilient(new SendExecutionReportState(
                  getCommentWithPrefix(comment)), ErrorState.class);
         } else {
            operation.setStateResilient(
                  new WarningState(operation.getState(), null, Messages
                        .getString("MANUALMarketError.0")),
                  ErrorState.class);
         }
      } catch (BestXException e) {
         LOGGER.error(
               "Unable to create and store ExecutionReport from manual execution state",
               e);
         operation.setStateResilient(new WarningState(operation.getState(),
               e, Messages.getString("MANUALExecutionReportError.0")),
               ErrorState.class);
      }
   }

   @Override
   public void onOperatorForceNotExecution(OperatorConsoleConnection source,
         String comment) {
      stopDefaultTimer();
      ExecutionReport newExecution = ExecutionReportHelper
            .createExecutionReport(
                  operation.getOrder().getInstrument(), operation
                  .getOrder().getSide(),
                  this.serialNumberService);
      newExecution.setState(ExecutionReportState.REJECTED);
      List<ExecutionReport> executionReports = operation
            .getExecutionReports();
      executionReports.add(newExecution);
      operation.setExecutionReports(executionReports);
      comment = getCommentWithPrefix(comment);
      operation.getOrder().setText(comment);
      operation.setStateResilient(new SendNotExecutionReportState(
            getCommentWithPrefix(comment)), ErrorState.class);
   }

   private String getCommentWithPrefix(String comment) {
      return LimitFileHelper.getInstance()
            .getCommentWithPrefix(comment, true);
   }
}
