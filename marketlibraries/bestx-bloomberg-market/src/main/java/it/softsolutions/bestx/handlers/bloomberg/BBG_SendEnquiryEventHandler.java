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
package it.softsolutions.bestx.handlers.bloomberg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.exceptions.ConnectionNotAvailableException;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_ExecutedState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class BBG_SendEnquiryEventHandler extends BaseOperationEventHandler {

   private static final long serialVersionUID = -3944107875602225118L;

   private static final Logger LOGGER = LoggerFactory.getLogger(BBG_SendEnquiryEventHandler.class);
   private final SerialNumberService serialNumberService;

   private final MarketBuySideConnection buySideConnection;
   protected final long waitingAnswerDelay;
   private long orderCancelDelay;

   private Set<String> technicalRejectReasons;

   private boolean isCancelBestXInitiative = false;
   private boolean isCancelRequestedByUser = false;

   public static final String REVOKE_TIMER_SUFFIX = "#REVOKE";

   //String lastSentQuoteReqID = "";

   /**
    * Instantiates a new BBG event handler
    *
    * @param operation the operation
    * @param bbgConnection the bbg connection
    * @param serialNumberService the serial number service
    * @param waitingAnswerDelay the waiting answer delay
    * @param multipleQuoteHandler the multiple quote handler
    * 
    */
   public BBG_SendEnquiryEventHandler(Operation operation, MarketBuySideConnection bbgConnection, SerialNumberService serialNumberService, long waitingAnswerDelay, long orderCancelDelay,
         Set<String> technicalRejectReasons){
      super(operation);
      this.buySideConnection = bbgConnection;
      this.waitingAnswerDelay = waitingAnswerDelay;
      this.orderCancelDelay = orderCancelDelay;
      this.serialNumberService = serialNumberService;
      this.technicalRejectReasons = technicalRejectReasons;
   }

   @Override
   public void onNewState(OperationState currentState) {
      try {
         setupDefaultTimer(waitingAnswerDelay, false);

         // sendRfq binds sets clOrdID as marketSessionID to marketOrder, 
         // and binds it to Operation : (OperationIdType.TSOX_CLORD_ID, clOrdID) to  
         buySideConnection.sendRfq(operation, operation.getLastAttempt().getMarketOrder());
      }
      catch (ConnectionNotAvailableException e) {
         stopDefaultTimer();
         LOGGER.error("TSOX Connection not available, can not send rfq to BBG: {}", e.getMessage(), e);
         operation.setStateResilient(new WarningState(currentState, e, Messages.getString("BBGMarketSendRFQRejectError.0")), ErrorState.class);
      }
      catch (BestXException e2) {
         stopDefaultTimer();
         LOGGER.error("An error occurred while sending rfq to BBG", e2);
         operation.setStateResilient(new WarningState(currentState, e2, Messages.getString("BBGMarketSendRFQRejectError.0")), ErrorState.class);
      }
   }

   @Override
   public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
      String ordCancelRejTimer = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
      if (source.getMarketCode() != MarketCode.BLOOMBERG) {
         return;
      }
      if (marketExecutionReport.getState() == ExecutionReportState.NEW) {
         LOGGER.info("Order {}, received exec report in NEW state", operation.getOrder().getFixOrderId());
         return;
      }
      stopDefaultTimer();
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
      marketExecutionReports.add(marketExecutionReport);

      String reason = marketExecutionReport.getText();

      // Create Customer Execution Report from Market Execution Report
      ExecutionReport executionReport;
      try {
         executionReport = marketExecutionReport.clone();
      }
      catch (CloneNotSupportedException e1) {
         LOGGER.error("Error while trying to create Execution Report from Market Execution Report");
         operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("TWMarketExecutionReportError.0")), ErrorState.class);
         return;
      }

      long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");

      executionReport.setLastPx(marketExecutionReport.getLastPx());
      executionReport.setAveragePrice(marketExecutionReport.getAveragePrice());
      executionReport.setPrice(operation.getOrder().getLimit());
      executionReport.setPriceType(operation.getOrder().getPriceType());
      executionReport.setSequenceId(Long.toString(executionReportId));

      //        marketExecutionReport.setExecBroker(operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());
      // AMC TW market execution report has the MarketMaker code inside
      if (marketExecutionReport.getMarketMaker() != null) {
         executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
         executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
      }
      else if (marketExecutionReport.getExecBroker() != null) {
         executionReport.setExecBroker(marketExecutionReport.getExecBroker());
         executionReport.setCounterPart(marketExecutionReport.getExecBroker());
      }
      executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());

      //TDRCHECK Is it right?
      operation.getExecutionReports().add(executionReport);

      // FIXME 20190506 verify that the Execution Report carries the order ID
      //order.getCustomerOrderId() // clOrdID
      //marketExecutionReport.getClOrdID (non esiste...)
      if (marketExecutionReport.getState() == ExecutionReportState.FILLED) {
         operation.setStateResilient(new BBG_ExecutedState(reason), ErrorState.class);
         try {
            //this timer could not exist, in this case nothing will happen with this call
            stopTimer(ordCancelRejTimer);
         }
         catch (SchedulerException e) {
            LOGGER.warn("Error while stopping timer {}", ordCancelRejTimer, e);
         }
      }
      else if (marketExecutionReport.getState() == ExecutionReportState.CANCELLED || marketExecutionReport.getState() == ExecutionReportState.REJECTED
            || marketExecutionReport.getState() == ExecutionReportState.DONE_FOR_DAY) {
         // go to next execution attempt
         boolean isTechnicalReject = false;
         if (reason != null && reason.length() > 0) {
            for (String r : technicalRejectReasons) {
               if (reason.toLowerCase().contains(r)) {
                  isTechnicalReject = true;
                  LOGGER.info("Order {}, must send technical Reject as reject reason ({}) matches one of the configured ones ({})", order.getFixOrderId(), reason, r);
                  break;
               }
            }
         }

         String rejectReason = reason;
         if (marketExecutionReport.getState() == ExecutionReportState.CANCELLED) {
            try {
               //this timer could not exist, in this case nothing will happen with this call
               stopTimer(ordCancelRejTimer);
            }
            catch (SchedulerException e) {
               LOGGER.warn("Error while stopping timer {}", ordCancelRejTimer, e);
            }

            if (isCancelRequestedByUser) {
               rejectReason = "Revoke requested by the customer";
            }
            else if (isCancelBestXInitiative) {
               rejectReason = "No answer received after the configurated number of seconds. Order has been automatically cancelled by BestX:FI-A";
            }

            operation.setStateResilient(new BBG_RejectedState(rejectReason, isTechnicalReject), ErrorState.class);
         }
         else {
            operation.setStateResilient(new BBG_RejectedState("", false), ErrorState.class);
         }
      }
   }

   //	 should be used if Enquiry is sent and rejected    
   @Override
   public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String orderQuoteReqId) {
      if (source.getMarketCode() != MarketCode.BLOOMBERG) {
         return;
      }

      stopDefaultTimer();
      // originating from a market reject there is no need to send a revoke to the market and we have no fills to manage
      LOGGER.info("Managing a market reject, we have already received a revoke????, start processing the last one.");
      operation.setStateResilient(new BBG_RejectedState(reason, false), ErrorState.class);
   }

   @Override
   public void onTimerExpired(String jobName, String groupName) {
	   String handlerJobName = super.getDefaultTimerJobName();

	   if (jobName.equals(handlerJobName)) {
		   LOGGER.warn("Order {}: Timer BloombergExecTimeout expired.", operation.getOrder().getFixOrderId());
		   try {
			   //create the timer for the order cancel
			   handlerJobName += REVOKE_TIMER_SUFFIX;
			   setupTimer(handlerJobName, orderCancelDelay, false);
			   // send order cancel message to the market
			   isCancelBestXInitiative = true;
			   this.buySideConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("MARKETAXESS_RevokeOrderForTimeout"));
		   }
		   catch (BestXException e) {
			   try {
				   stopTimer(handlerJobName);
			   } catch (SchedulerException e1) {
				   LOGGER.error("Error in stop timer " + handlerJobName, e1);
			   }
			   LOGGER.error("Error {} while revoking the order {}", e.getMessage(), operation.getOrder().getFixOrderId(), e);
			   operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("BBG_MarketRevokeOrderError", operation.getOrder().getFixOrderId())), ErrorState.class);
		   }
	   }
	   else if (jobName.equals(handlerJobName + REVOKE_TIMER_SUFFIX)) {
		   //The timer created after receiving an Order Cancel Reject is expired without receiving an execution or a cancellation
	      LOGGER.warn("Order {} : Timer {} expired.Order cancel response not received or no further messages received.", operation.getOrder().getFixOrderId(), jobName);
		   operation.setStateResilient(
				   new WarningState(operation.getState(), null, Messages.getString("CancelRejectWithNoExecutionReportTimeout.0", operation.getLastAttempt().getMarketOrder().getMarket().getName())),
				   ErrorState.class);
	   }
	   else {
		   super.onTimerExpired(jobName, groupName);
	   }
   }

   @Override
   public void onRevoke() {
	   // onRevoke is invoked by webapp and must only reject this market so no need to
	   // set customerRevokeReceived so that operations may continue on other markets

	   String handlerJobName = super.getDefaultTimerJobName();
	   try {
		   stopTimer(handlerJobName);
	   } catch (SchedulerException e2) {
		   LOGGER.error("Error in stop timer " + handlerJobName, e2);
	   }

	   handlerJobName += REVOKE_TIMER_SUFFIX;

	   try {
		   // create the timer for the order cancel
		   setupTimer(handlerJobName, orderCancelDelay, false);
		   // send order cancel message to the market
		   this.isCancelBestXInitiative = true;
		   this.isCancelRequestedByUser = true;
		   this.buySideConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), Messages.getString("BBG_RevokeOrder"));
	   }
	   catch (BestXException e) {
		   try {
			   stopTimer(handlerJobName);
		   } catch (SchedulerException e1) {
			   LOGGER.error("Error in stop timer " + handlerJobName, e1);
		   }
		   LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
		   operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("BBG_MarketRevokeOrderError", operation.getOrder().getFixOrderId())), ErrorState.class);
	   }

   }

   @Override
   public void onFixRevoke(CustomerConnection source) {
      MarketOrder marketOrder = operation.getLastAttempt().getMarketOrder();

      String reason = Messages.getString("EventRevocationRequest.0");
      updateOperationToRevocated(reason);

      // TW
      try {
         this.isCancelBestXInitiative = true;
         this.isCancelRequestedByUser = true;
         this.buySideConnection.revokeOrder(operation, marketOrder, reason);
      }
      catch (BestXException e) {
         LOGGER.error("An error occurred while revoking the order {}", operation.getOrder().getFixOrderId(), e);
         operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("BBG_MarketRevokeOrderError", operation.getOrder().getFixOrderId())), ErrorState.class);
      }

   }

   @Override
   public void onMarketOrderCancelRequestReject(MarketBuySideConnection source, Order order, String reason) {
      String handlerJobName = super.getDefaultTimerJobName() + REVOKE_TIMER_SUFFIX;
      try {
         isCancelBestXInitiative = false;
         stopTimer(handlerJobName);
         LOGGER.info("Order {} cancel rejected,stopping order cancel response timer and waiting for the order execution or cancellation", order.getFixOrderId());
         //recreate the timer with a longer timeout (the same used for the execution)
         setupTimer(handlerJobName, waitingAnswerDelay, false);
      }
      catch (SchedulerException e) {
         LOGGER.error("Cannot stop timer {}", handlerJobName, e);
      }
   }

}
