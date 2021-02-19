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
package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.amosgateway.CSFixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.amosgateway.OMS1FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.csfix.CSOrderCancelRejectLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqInputLazyBean;
import it.softsolutions.bestx.dao.sql.SqlInstrumentDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.handlers.LimitFileHelper;
import it.softsolutions.bestx.management.FixCustomerAdapterMBean;
import it.softsolutions.bestx.management.statistics.CustomerAdapterStatistics;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.CSPOBexExecutionReport;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.LazyExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.MICCodeService;
import it.softsolutions.bestx.services.financecalc.SettlementDateCalculator;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;

/**
 * 
 * Purpose: this class is mainly for manage the messages exchange between BestX and the customer connection
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 18/giu/2012
 * 
 **/
public class OMS1CustomerAdapter extends CustomerAdapterStatistics implements CustomerConnection, FixGatewayConnectionListener, ConnectionListener, FixCustomerAdapterMBean {

   private static final Logger LOGGER = LoggerFactory.getLogger(OMS1CustomerAdapter.class);

   private OperationRegistry operationRegistry;
   private Executor ordersExecutor;
   private Executor acksExecutor;
   private CustomerFinder customerFinder;
   private InstrumentFinder instrumentFinder;
   private FixGatewayConnection fixGatewayConnection;
   private ConnectionListener connectionListener;
   private ConnectionHelper connectionHelper;
   private Customer defaultCustomer;
   private SettlementDateCalculator settlementDateCalculator;
   // Monitorable statistics variables this.executionTime = new NumericValueMonitor("ExecutionTime", "Market_"+ marketCode, true, "info",
   // "[MARKET_STATISTICS]");

   /*
    * @Monitorable(name="OrderNotifications", jmxSection = "TASConnection", type = "default", samplingRate=60000)
    */private AtomicLong statTotalFixOrderNotifications;
   /*
    * @Monitorable(name="FailedOrderNotifications", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
    */private AtomicLong statFailedFixOrderNotifications;
   private AtomicLong statTotalFixRfqNotifications;
   private AtomicLong statFailedFixRfqNotifications;
   private AtomicLong statTotalFixQuotesAcknowledged;
   private AtomicLong statFailedFixQuotesAcknowledged;
   private AtomicLong statTotalFixQuotesNotAcknowledged;
   private AtomicLong statFailedFixQuotesNotAcknowledged;

   /*
    * @Monitorable(name="TotalFixTradesAcknowledged", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
    */private AtomicLong statTotalFixTradesAcknowledged;
   // @Monitorable(name="FailedFixTradesAcknowledged", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statFailedFixTradesAcknowledged;
   // @Monitorable(name="TotalFixTradesNotAcknowledged", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statTotalFixTradesNotAcknowledged;
   // @Monitorable(name="FailedFixTradesNotAcknowledged", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statFailedFixTradesNotAcknowledged;
   private AtomicLong statRfqResponses;
   // @Monitorable(name="OrderRejects", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statOrderRejects;
   private AtomicLong statRfqReject;
   // @Monitorable(name="RejectExecutionReport", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statRejectExecutionReport;
   // @Monitorable(name="FillExecutionReport", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statFillExecutionReport;
   // @Monitorable(name="Exceptions", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statExceptions;
   private volatile boolean statConnected;
   // @Monitorable(name="OrderResponse", jmxSection = "TASConnection", type = "tick", samplingRate=60000)
   private AtomicLong statOrderResponse;

   private Date statStart;
   private Date statLastStartup;
   private MICCodeService micCodeService;
   private SqlInstrumentDao sqlInstrumentDao;

   private void checkPreRequisites() throws ObjectNotInitializedException {
      if (operationRegistry == null) {
         throw new ObjectNotInitializedException("Operation registry not set");
      }
      if (ordersExecutor == null) {
         throw new ObjectNotInitializedException("Order Executor not set");
      }
      if (acksExecutor == null) {
         throw new ObjectNotInitializedException("Acks Executor not set");
      }
      if (customerFinder == null) {
         throw new ObjectNotInitializedException("Operation finder not set");
      }
      if (instrumentFinder == null) {
         throw new ObjectNotInitializedException("Instrument finder not set");
      }
      if (defaultCustomer == null) {
         throw new ObjectNotInitializedException("Default customer not set");
      }
      if (connectionHelper == null) {
         throw new ObjectNotInitializedException("Connection helper not set");
      }
      if (fixGatewayConnection == null) {
         throw new ObjectNotInitializedException("Fix gateway connection not set");
      }
      if (micCodeService == null) {
         throw new ObjectNotInitializedException("micCodeService not set");
      }
   }

   public void setOperationRegistry(OperationRegistry operationRegistry) {
      this.operationRegistry = operationRegistry;
   }

   public void setCustomerFinder(CustomerFinder customerFinder) {
      this.customerFinder = customerFinder;
   }

   public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
      this.instrumentFinder = instrumentFinder;
   }

   public void setDefaultCustomer(Customer defaultCustomer) {
      this.defaultCustomer = defaultCustomer;
   }

   public void setConnectionHelper(ConnectionHelper connectionHelper) {
      this.connectionHelper = connectionHelper;
   }

   public void setFixGatewayConnection(FixGatewayConnection fixGatewayConnection) {
      this.fixGatewayConnection = fixGatewayConnection;
      this.fixGatewayConnection.setFixGatewayListener(this);
   }

   @Override
   public void setConnectionListener(ConnectionListener connectionListener) {
      this.connectionListener = connectionListener;
   }

   public void init() throws BestXException {
      checkPreRequisites();
      resetStats();
      connectionHelper.setConnection(fixGatewayConnection);
      connectionHelper.setConnectionListener(this);
      setChannelName("OMS1");
      orderReceived = 0;
      String timerName = RECEIVED_ORDERS_STATISTICS_LABEL + "_" + getChannelName();
      try {
         createTimer(timerName, OMS1CustomerAdapter.class.getSimpleName());
      }
      catch (SchedulerException e) {
         LOGGER.error("Error while scheduling customer connection statistics timer {}!", timerName, e);
      }

      CommonMetricRegistry.INSTANCE.registerHealtCheck(this);
   }

   @Override
   public void onFixOrderNotification(final FixGatewayConnection source, final FixOrderInputLazyBean fixOrder) {
      LOGGER.info("New order from FIX interface - OrderId: {} - {}", fixOrder.getFixOrderId(), fixOrder);

      this.ordersExecutor.execute(new Runnable() {

         @Override
         public void run() {

            statTotalFixOrderNotifications.incrementAndGet();
            orderReceived++;

            ApplicationMonitor.onNewOrder(source.getConnectionName()); // for statistics logging
            //		        try {
            //					Restrictions.onNewOrder();
            //				} catch (BestXException e1) {
            //					LOGGER.info(e1.getMessage());
            //				}
            String orderId = fixOrder.getFixOrderId();
            if (orderId == null) {
               LOGGER.error("ORDER without Order ID arrived! Discard it.");
               statFailedFixRfqNotifications.incrementAndGet();
               return;
            }
            try {
               long t0 = DateService.currentTimeMillis();
               if (operationRegistry.operationExistsById(OperationIdType.ORDER_ID, orderId, false)) {
                  LOGGER.error("An order notification with the same orderId is already existing: order id ({})", orderId);

                  return;
               }
               LOGGER.info("1. operationExistsById = " + (DateService.currentTimeMillis() - t0) + " ms");

               t0 = DateService.currentTimeMillis();
               final Operation operation = operationRegistry.getNewOperationById(OperationIdType.ORDER_ID, orderId, false);
               LOGGER.info("2. getNewOperationById = " + (DateService.currentTimeMillis() - t0) + " ms");

               String sessionId = fixOrder.getSessionId();
               LOGGER.debug("SESSION ID: {}", sessionId);
               if (sessionId != null) {
                  operationRegistry.bindOperation(operation, OperationIdType.FIX_SESSION, sessionId);
                  operation.addIdentifier(OperationIdType.CUSTOMER_CHANNEL, "OMS1");
               }

               fixOrder.setCustomerFinder(customerFinder);
               fixOrder.setInstrumentFinder(instrumentFinder);
               fixOrder.setSqlInstrumentDao(sqlInstrumentDao);
               fixOrder.setCustomer(customerFinder.getCustomerByFixId(fixOrder.getClientId()));
               // Save order ID to operation data
               operation.addIdentifier(OperationIdType.ORDER_ID, orderId);
               LOGGER.debug("OrderID = {}: forward event to operation in new thread", orderId);
               operation.onCustomerOrder(OMS1CustomerAdapter.this, fixOrder);
            }
            catch (BestXException e) {
               statFailedFixOrderNotifications.incrementAndGet();
               statExceptions.incrementAndGet();
               LOGGER.error("Error occurred while retrieving operation by order id ({})", orderId, e);
            }
            catch (Exception e) {
               // [RR20121126] The CustomerFilterTable was not accessible due to missing privileges, the exception
               // was not catched and the gateway connection went down. Catching here the exception keeps the
               // customer connection active allowing us to log the error
               statFailedFixOrderNotifications.incrementAndGet();
               statExceptions.incrementAndGet();
               LOGGER.error("Error while processing new order {} : {}", orderId, e.getMessage(), e);
            }
         }
      });
   }

   @Override
   public void onFixRfqNotification(FixGatewayConnection source, final FixRfqInputLazyBean fixRfq) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onFixQuoteAcknowledged(FixGatewayConnection source, final String quoteRequestId) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onFixQuoteNotAcknowledged(FixGatewayConnection fixCustomerConnector, final String quoteRequestId, final Integer errorCode, final String errorMessage) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onFixTradeAcknowledged(FixGatewayConnection fixCustomerConnector, final String orderId, String executionReportId) {
      LOGGER.info("New Ack to Execution Report from FIX interface - OrderId: {}", orderId);
      statTotalFixTradesAcknowledged.incrementAndGet();
      if (orderId == null) {
         LOGGER.error("Ack to Execution Report without Order ID arrived! Discard it.");
         statFailedFixTradesAcknowledged.incrementAndGet();
         return;
      }
      try {
         final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
         // move all work to child thread, while preserving object-orientation, sending around only full featured objects, not IDs
         final LazyExecutionReport executionReport = new LazyExecutionReport(operation);
         LOGGER.debug("Forward event to operation in new thread");
         this.acksExecutor.execute(new Runnable() {

            public void run() {
               operation.onCustomerExecutionReportAcknowledged(OMS1CustomerAdapter.this, executionReport);
            }
         });
      }
      catch (OperationNotExistingException e) {
         statFailedFixTradesAcknowledged.incrementAndGet();
         LOGGER.error("Execution Report Ack received for order: {}. But order is not bound to any operation.", orderId, e);
      }
      catch (BestXException e) {
         statFailedFixTradesAcknowledged.incrementAndGet();
         statExceptions.incrementAndGet();
         LOGGER.error("Error occurred while retrieving operation by order id ({}): ", orderId, e);
      }
   }

   @Override
   public void onFixTradeNotAcknowledged(FixGatewayConnection fixCustomerConnector, final String orderId, String executionReportId, final Integer errorCode, final String errorMessage) {
      LOGGER.info("New Nack to Execution Report from FIX interface - OrderId: {}", orderId);
      statTotalFixTradesNotAcknowledged.incrementAndGet();
      if (orderId == null) {
         LOGGER.error("Nack to Execution Report without Order ID arrived! Discard it.");
         statFailedFixTradesNotAcknowledged.incrementAndGet();
         return;
      }
      try {
         final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
         // move all work to child thread, while preserving object-orientation, sending around only full featured objects, not IDs
         final LazyExecutionReport executionReport = new LazyExecutionReport(operation);
         LOGGER.debug("Forward event to operation in new thread");
         this.acksExecutor.execute(new Runnable() {

            public void run() {
               operation.onCustomerExecutionReportNotAcknowledged(OMS1CustomerAdapter.this, executionReport, errorCode, errorMessage);
            }
         });
      }
      catch (OperationNotExistingException e) {
         statFailedFixTradesNotAcknowledged.incrementAndGet();
         LOGGER.error("Execution Report Nack received for order: {}. But order is not bound to any operation.", orderId, e);
      }
      catch (BestXException e) {
         statFailedFixTradesNotAcknowledged.incrementAndGet();
         statExceptions.incrementAndGet();
         LOGGER.error("Error occurred while retrieving operation by order id ({}): ", orderId, e);
      }
   }

   @Override
   public void sendRfqResponse(Operation source, Rfq rfq, String rfqId, Quote quote) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRfqReject(Operation source, Rfq rfq, String rfqId, CustomerConnection.ErrorCode errorCode, String reason) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendOrderReject(Operation source, Order order, String orderId, ExecutionReport executionReport, CustomerConnection.ErrorCode errorCode, String rejectReason) throws BestXException {
      // Send Fix SessionID
      if (executionReport != null) {
         LOGGER.info("Sending Order Reject [{}] [{}] for OrderId: {}", executionReport.getState(), executionReport.getText(), orderId);
      }
      else {
         LOGGER.info("Sending Order Reject [{}] [{}] for OrderId: {}", orderId);
      }

      statRejectExecutionReport.incrementAndGet();
      if (source == null) {
         throw new BestXException("Operation is null");
      }
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      LimitFileHelper.getInstance().getComment(order, rejectReason);
      if (errorCode == null) {
         errorCode = ErrorCode.OK;
      }
      
      //SP-20200730 - BESTX-694 Manage null order settlement date
      if (settlementDateCalculator != null && order.getFutSettDate() == null) {
         Integer instrumentStdSettlDays = 2;
         if (order.getInstrument() != null) {
            instrumentStdSettlDays = order.getInstrument().getStdSettlementDays();
         }
         Date startDate = order.getTransactTime();
         if (startDate == null) {
             startDate = DateService.newLocalDate();
         }
         order.setFutSettDate(settlementDateCalculator.getCalculatedSettlementDate(instrumentStdSettlDays, null, startDate));
      }

      OMS1FixExecutionReportOutputLazyBean bean = new OMS1FixExecutionReportOutputLazyBean(fixSessionId, order, orderId, executionReport, errorCode.getCode(), rejectReason, micCodeService);
      fixGatewayConnection.sendExecutionReport(this, bean);
   }

   @Override
   public void sendExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport) throws BestXException {
      // Send Fix SessionID
      LOGGER.debug("Sending Execution Report for OrderId: {}", orderId);
      LOGGER.info("Sending fill={}", executionReport);
      ApplicationMonitor.onOrderClose(fixGatewayConnection.getConnectionName()); // for statistics logging
      statFillExecutionReport.incrementAndGet();
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      
      OMS1FixExecutionReportOutputLazyBean bean = null;
      if (executionReport instanceof CSPOBexExecutionReport && ExecutionReportState.NEW.equals(executionReport.getState())) {
         bean = new OMS1FixExecutionReportOutputLazyBean(fixSessionId, quote, order, orderId, attempt, (CSPOBexExecutionReport) executionReport, micCodeService, settlementDateCalculator);
      }
      else {
         bean = new OMS1FixExecutionReportOutputLazyBean(fixSessionId, quote, order, orderId, attempt, executionReport, micCodeService, settlementDateCalculator);
      }
      fixGatewayConnection.sendExecutionReport(this, bean);
   }

   @Override
   public void onConnection(Connection source) {
      LOGGER.info("FIX Gateway Connected");
      statConnected = true;
      if (connectionListener != null) {
         connectionListener.onConnection(this);
      }
   }

   @Override
   public void onDisconnection(Connection source, String reason) {
      LOGGER.info("FIX Gateway Disconnected");
      statConnected = false;
      if (connectionListener != null) {
         connectionListener.onDisconnection(source, reason);
      }
   }

   // commands
   @Override
   public void connect() {
      LOGGER.info("Connecting to FIX Gateway");
      try {
         SimpleTimerManager.getInstance().start();
      }
      catch (SchedulerException e) {
         LOGGER.error("Error while trying to start the timer manager.", e);
      }
      statLastStartup = DateService.newLocalDate();
      resetStats();
      connectionHelper.connect();
   }

   @Override
   public void disconnect() {
      LOGGER.info("Disconnecting from FIX Gateway");
      connectionHelper.disconnect();
   }

   // queries
   @Override
   public String getConnectionName() {
      return connectionHelper.getConnectionName();
   }

   @Override
   public boolean isConnected() {
      return statConnected;
   }

   // statistics
   @Override
   public long getUptime() {
      if (statLastStartup == null) {
         return 0L;
      }
      return (DateService.newLocalDate().getTime() - statLastStartup.getTime());
   }

   @Override
   public long getNumberOfExceptions() {
      return statExceptions.get();
   }

   @Override
   public double getAvgInputPerSecond() {
      if (statStart == null) {
         return 0.0;
      }
      long currentTime = DateService.currentTimeMillis();
      long startTime = statStart.getTime();
      return (double) (statTotalFixOrderNotifications.get() + statTotalFixRfqNotifications.get() + statTotalFixQuotesAcknowledged.get() + statTotalFixQuotesNotAcknowledged.get()
            + statTotalFixTradesAcknowledged.get() + statTotalFixTradesNotAcknowledged.get()) / (currentTime - startTime + 1) * 1000.0; // avoid div per zero
   }

   @Override
   public double getAvgOutputPerSecond() {
      if (statStart == null) {
         return 0.0;
      }
      long currentTime = DateService.currentTimeMillis();
      long startTime = statStart.getTime();
      return (double) (statRfqResponses.get() + statOrderRejects.get() + statRfqReject.get() + statRejectExecutionReport.get() + statFillExecutionReport.get()) / (currentTime - startTime + 1)
            * 1000.0; // avoid
      // div
      // per
      // zero
   }

   @Override
   public double getPctOfFailedInput() {
      long totInput = statTotalFixOrderNotifications.get() + statTotalFixRfqNotifications.get() + statTotalFixQuotesAcknowledged.get() + statTotalFixQuotesNotAcknowledged.get()
            + statTotalFixTradesAcknowledged.get() + statTotalFixTradesNotAcknowledged.get();
      if (totInput > 0) {
         return (double) (statFailedFixOrderNotifications.get() + statFailedFixRfqNotifications.get() + statFailedFixTradesAcknowledged.get() + statFailedFixTradesNotAcknowledged.get()
               + statFailedFixQuotesAcknowledged.get() + statFailedFixQuotesNotAcknowledged.get()) / totInput * 100;
      }
      else {
         return 0.0;
      }
   }

   @Override
   public long getInFixOrderNotifications() {
      return statTotalFixOrderNotifications.get();
   }

   @Override
   public long getInFixRfqNotifications() {
      return statTotalFixRfqNotifications.get();
   }

   public long getInFixQuotesAcknowledged() {
      return statTotalFixQuotesAcknowledged.get();
   }

   public long getInFixQuotesNotAcknowledged() {
      return statTotalFixQuotesNotAcknowledged.get();
   }

   @Override
   public long getInFixTradesAcknowledged() {
      return statTotalFixTradesAcknowledged.get();
   }

   @Override
   public long getInFixTradesNotAcknowledged() {
      return statTotalFixTradesNotAcknowledged.get();
   }

   @Override
   public long getOutRfqResponses() {
      return statRfqResponses.get();
   }

   @Override
   public long getOutOrderRejects() {
      return statOrderRejects.get();
   }

   @Override
   public long getOutRfqRejects() {
      return statRfqReject.get();
   }

   @Override
   public long getOutRejectExecutionReports() {
      return statRejectExecutionReport.get();
   }

   @Override
   public long getOutFillExecutionReports() {
      return statFillExecutionReport.get();
   }

   public long getOutOrderResponse() {
      return statOrderResponse.get();
   }

   @Override
   public void resetStats() {
      statTotalFixOrderNotifications = new AtomicLong(0L);
      statFailedFixOrderNotifications = new AtomicLong(0L);
      statTotalFixRfqNotifications = new AtomicLong(0L);
      statFailedFixRfqNotifications = new AtomicLong(0L);
      statTotalFixQuotesAcknowledged = new AtomicLong(0L);
      statFailedFixQuotesAcknowledged = new AtomicLong(0L);
      statTotalFixQuotesNotAcknowledged = new AtomicLong(0L);
      statFailedFixQuotesNotAcknowledged = new AtomicLong(0L);
      statTotalFixTradesAcknowledged = new AtomicLong(0L);
      statFailedFixTradesAcknowledged = new AtomicLong(0L);
      statTotalFixTradesNotAcknowledged = new AtomicLong(0L);
      statFailedFixTradesNotAcknowledged = new AtomicLong(0L);
      statRfqResponses = new AtomicLong(0L);
      statOrderRejects = new AtomicLong(0L);
      statRfqReject = new AtomicLong(0L);
      statRejectExecutionReport = new AtomicLong(0L);
      statFillExecutionReport = new AtomicLong(0L);
      statOrderResponse = new AtomicLong(0L);
      statExceptions = new AtomicLong(0L);
      statStart = DateService.newLocalDate();
   }

   @Override
   public void sendOrderResponseAck(Operation source, Order order, String orderId) throws BestXException {
      // Send Fix SessionID
      LOGGER.info("Sending Order Response Ack for OrderId: {}", orderId);
      statOrderResponse.incrementAndGet();
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      FixOrderResponseOutputLazyBean bean = new FixOrderResponseOutputLazyBean(fixSessionId, order, ErrorCode.OK, "Order Accepted");
      fixGatewayConnection.sendOrderResponse(this, bean);
   }

   @Override
   public void sendOrderResponseNack(Operation source, Order order, String orderId, String errorMsg) throws BestXException {
      // Send Fix SessionID
      LOGGER.info("Sending Order Response Nack for OrderId: {}", orderId);
      statOrderResponse.incrementAndGet();
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      FixOrderResponseOutputLazyBean bean = new FixOrderResponseOutputLazyBean(fixSessionId, order, ErrorCode.GENERIC_ERROR, errorMsg);
      fixGatewayConnection.sendOrderResponse(this, bean);
   }

   @Override
   public void onFixRevokeNotification(FixGatewayConnection fixCustomerConnector, String origClOrdId, final String revokeId, String fixSessionId) {
      long start_time = DateService.currentTimeMillis();
      LOGGER.info("New revoke from FIX interface - origClOrdId: {} - Revoke Id: {}", origClOrdId, revokeId);
      if (origClOrdId == null) {
         LOGGER.error("REVOKE without Order ID arrived! Discard it.");
         return;
      }
      try {
         final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, origClOrdId);
         long mid_time = DateService.currentTimeMillis();
         Runnable runnable = new Runnable() {

            String param = revokeId;

            public void run() {
               try {
                  operationRegistry.bindOperation(operation, OperationIdType.FIX_REVOKE_ID, param);
               }
               catch (BestXException e) {
                  LOGGER.error("An error occurred while retrieving Operation by order: {}", param, e);
                  return;
               }
               // 20101202 Ruggero : I-41 Revoche Automatiche
               // set the flag for a reception of a customer revoke for this operation
               operation.setCustomerRevokeReceived(true);

               operation.onFixRevoke(OMS1CustomerAdapter.this);
            }
         };
         this.ordersExecutor.execute(runnable);
         long stop_time = DateService.currentTimeMillis();
         LOGGER.info("[REGISTRYTIME]-STOP: registry Operation load time {}, Operation call time {}", mid_time - start_time, stop_time - mid_time);
      }
      catch (OperationNotExistingException e) {
         LOGGER.error("REVOKE received for unknown Order ID. Send a revoke nack.");
         LOGGER.debug("Send Revoke NACK - OrigClOrdID: {} - RevokeId: {} - Revocation State: {} - Comment: {}", origClOrdId, revokeId, RevocationState.NOT_ACKNOWLEDGED,
               "Cannot find an order with the origClOrdId " + origClOrdId);
         FixOutputLazyBean cancelReject = new CSOrderCancelRejectLazyBean(fixSessionId, origClOrdId, "Cancellation rejected: invalid origClOrdId " + origClOrdId);
         try {
            fixGatewayConnection.sendGenericRevokeReport(this, revokeId, cancelReject);
         }
         catch (BestXException e1) {
            LOGGER.error("Error while sending an order cancel reject: {} / {}", e1.getMessage(), cancelReject);
         }
         return;
      }
      catch (BestXException e) {
         LOGGER.error("An error occurred while retrieving Operation by order: {}", origClOrdId, e);
         return;
      }
   }

   @Override
   public void sendRevokeAck(Operation source, String orderId, String comment) throws BestXException {
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
      LOGGER.info("Sending Revoke ACK - OrderId: {} - RevokeId: {} - Comment: {}", orderId, revokeId, comment);
      FixRevokeResponseOutputLazyBean revokeResponse = new FixRevokeResponseOutputLazyBean(fixSessionId, orderId, true, comment);
      fixGatewayConnection.sendRevokeResponse(this, revokeId, revokeResponse);
   }

   @Override
   public void sendRevokeNack(Operation source, Order order, String comment) throws BestXException {
      String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
      LOGGER.info("Sending Revoke NACK - OrderId: {} - RevokeId: {} - Comment: {}", order.getFixOrderId(), revokeId, comment);
      sendRevokeReport(source, order, RevocationState.NOT_ACKNOWLEDGED, comment);
   }

   @Override
   public void sendRevokeReport(Operation source, Order order, RevocationState revocationState, String comment) throws BestXException {
      String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
      String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
      LOGGER.info("Sending Revoke ACK({}) - OrderId: {} - RevokeId: {} - Revocation State: {} - Comment: {}", revocationState, order.getFixOrderId(), revokeId, revocationState.name(), comment);
      FixRevokeReportLazyBean revokeReport = new CSFixRevokeReportLazyBean(fixSessionId, source, revocationState == RevocationState.MANUAL_ACCEPTED ? true : false, comment);
      fixGatewayConnection.sendRevokeReport(this, revokeId, revokeReport);
   }

   @Override
   public void sendPartialFillExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, BigDecimal cumQty) throws BestXException {
      // Intentionally left empty
   }

   @Override
   public boolean isFillsManager(Operation operation) {
      LOGGER.debug("{} is a NO FILL operation, no way for managing fills" + operation);
      return false;
   }

   public MICCodeService getMicCodeService() {
      return this.micCodeService;
   }

   public void setMicCodeService(MICCodeService micCodeService) {
      this.micCodeService = micCodeService;
   }

   /**
    * @param sqlInstrumentDao the sqlInstrumentDao to set
    */
   public void setSqlInstrumentDao(SqlInstrumentDao sqlInstrumentDao) {
      this.sqlInstrumentDao = sqlInstrumentDao;
   }

   @Override
   public String getFixGatewayConnectionStatus() {
      // TODO Monitoring-BX
      return "FixGatewayStatus";
   }

   @Override
   public void sendOrderCancelReject(Operation source, String comment) throws BestXException {
      // TODO Auto-generated method stub
   }

   /**
    * Spring setter
    * @param settlementDateCalculator
    */
   public void setSettlementDateCalculator(SettlementDateCalculator settlementDateCalculator) {
      this.settlementDateCalculator = settlementDateCalculator;
   }

	public void setOrdersExecutor(Executor ordersExecutor) {
		this.ordersExecutor = ordersExecutor;
	}
	
	public void setAcksExecutor(Executor acksExecutor) {
		this.acksExecutor = acksExecutor;
	}
  
   
}