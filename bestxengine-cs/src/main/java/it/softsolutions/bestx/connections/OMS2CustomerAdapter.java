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
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.amosgateway.AmosFixCalculatedExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.amosgateway.AmosFixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.amosgateway.AmosFixPartFillExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.amosgateway.AmosFixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.amosgateway.AmosFixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.amosgateway.OMS1FixOrderInputLazyBean;
import it.softsolutions.bestx.connections.amosgateway.OMS2FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqInputLazyBean;
import it.softsolutions.bestx.dao.sql.SqlInstrumentDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.management.FixCustomerAdapterMBean;
import it.softsolutions.bestx.management.statistics.CustomerAdapterStatistics;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.LazyExecutionReport;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.MICCodeService;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;

/** 

 *
 * Purpose: this class is mainly for manage the messages exchange between BestX and the customer connection
 *
 * Project Name : bestxengine-cs
 * First created by: ruggero.rizzo
 * Creation date: 15/giu/2012
 *
 **/
public class OMS2CustomerAdapter extends CustomerAdapterStatistics implements CustomerConnection, FixGatewayConnectionListener, ConnectionListener, FixCustomerAdapterMBean {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OMS2CustomerAdapter.class);
    
    private OperationRegistry operationRegistry;
    private Executor executor;
    private CustomerFinder customerFinder;
    private MarketMakerFinder marketMakerFinder;
    private InstrumentFinder instrumentFinder;
    private FixGatewayConnection fixGatewayConnection;
    private ConnectionListener connectionListener;
    private ConnectionHelper connectionHelper;
    // Monitorable statistics variables
    private AtomicLong statTotalFixOrderNotifications;
    private AtomicLong statFailedFixOrderNotifications;
    private AtomicLong statTotalFixRfqNotifications;
    private AtomicLong statFailedFixRfqNotifications;
    private AtomicLong statTotalFixQuotesAcknowledged;
    private AtomicLong statFailedFixQuotesAcknowledged;
    private AtomicLong statTotalFixQuotesNotAcknowledged;
    private AtomicLong statFailedFixQuotesNotAcknowledged;
    private AtomicLong statTotalFixTradesAcknowledged;
    private AtomicLong statFailedFixTradesAcknowledged;
    private AtomicLong statTotalFixTradesNotAcknowledged;
    private AtomicLong statFailedFixTradesNotAcknowledged;
    private AtomicLong statRfqResponses;
    private AtomicLong statOrderRejects;
    private AtomicLong statRfqReject;
    private AtomicLong statRejectExecutionReport;
    private AtomicLong statFillExecutionReport;
    private AtomicLong statExceptions;
    private volatile boolean statConnected;
    private AtomicLong statOrderResponse;
    private Date statStart;
    private Date statLastStartup;
    private MICCodeService micCodeService;
    private SqlInstrumentDao sqlInstrumentDao;
    
    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
        if (executor == null) {
            throw new ObjectNotInitializedException("Executor not set");
        }
        if (customerFinder == null) {
            throw new ObjectNotInitializedException("Operation finder not set");
        }
        if (instrumentFinder == null) {
            throw new ObjectNotInitializedException("Instrument finder not set");
        }
        if (connectionHelper == null) {
            throw new ObjectNotInitializedException("Connection helper not set");
        }
        if (fixGatewayConnection == null) {
            throw new ObjectNotInitializedException("Fix gateway connection not set");
        }
        if (marketMakerFinder == null) {
            throw new ObjectNotInitializedException("Market maker finder not set");
        }
        if (micCodeService == null) {
            throw new ObjectNotInitializedException("micCodeService not set");
        }
    }

    /**
     * Set the operation registry
     * 
     * @param operationRegistry
     *            : the registry to be set
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * Set the executor
     * 
     * @param executor
     *            : the executor to be set
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Set the customer finder
     * 
     * @param customerFinder
     *            : the customer finder to be set
     */
    public void setCustomerFinder(CustomerFinder customerFinder) {
        this.customerFinder = customerFinder;
    }

    /**
     * Set the instrument finder
     * 
     * @param instrumentFinder
     *            : the instrument finder to be set
     */
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    /**
     * Set the connection helper
     * 
     * @param connectionHelper
     *            : the connection helper to be set
     */
    public void setConnectionHelper(ConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    /**
     * Set the amos gateway connection
     * 
     * @param fixGatewayConnection
     *            : the connection to be set
     */
    public void setFixGatewayConnection(FixGatewayConnection fixGatewayConnection) {
        this.fixGatewayConnection = fixGatewayConnection;
        this.fixGatewayConnection.setFixGatewayListener(this);
    }

    /**
     * Set the connection listener
     * 
     * @param connectionListener
     *            : the listener to be set
     */
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    /**
     * Adapter initialization
     * 
     * @throws BestXException
     *             : thrown in case of error
     */
    public void init() throws BestXException {
        checkPreRequisites();
        connectionHelper.setConnection(fixGatewayConnection);
        connectionHelper.setConnectionListener(this);
        orderReceived = 0;
        setChannelName("OMS2");
        String timerName = RECEIVED_ORDERS_STATISTICS_LABEL + "_" + getChannelName();
        try {
            createTimer(timerName, OMS2CustomerAdapter.class.getSimpleName());
        } catch (SchedulerException e) {
            LOGGER.error("Error while scheduling customer connection statistics timer {}!", timerName, e);
        }
    }

    private String translateCustomerType(String customerType) {
        String retVal = null;
        if (customerType == null || customerType.isEmpty()) {
            return null;
        }
        if (customerType.equalsIgnoreCase("W")) {
            retVal = "P";
        } else if (customerType.equalsIgnoreCase("P")) {
            retVal = "T";
        } else if (customerType.equalsIgnoreCase("I")) {
            retVal = "W";
        } else if (customerType.equalsIgnoreCase("G")) {
            retVal = "G";
        } else if (customerType.equalsIgnoreCase("N")) {
            retVal = null;
        } else {
            retVal = null;
        }
        return retVal;
    }

    @Override
    public void onFixOrderNotification(FixGatewayConnection source, final FixOrderInputLazyBean fixOrder) {
        LOGGER.info("New order from FIX interface - OrderId: {} - {}", fixOrder.getFixOrderId(), fixOrder);

        statTotalFixOrderNotifications.incrementAndGet();
        orderReceived++;
        ApplicationMonitor.onNewOrder(source.getConnectionName());	// for statistics logging
        String orderId = fixOrder.getFixOrderId();
        
        if (orderId == null || orderId.isEmpty()) {
            LOGGER.error("ORDER without Order ID arrived! Discard it.");
            statFailedFixRfqNotifications.incrementAndGet();
            return;
        }
        
        try {
            if (operationRegistry.operationExistsById(OperationIdType.ORDER_ID, orderId, false)) {
                LOGGER.error("An order notification with the same orderId is already existing: order id ({})", orderId);

                return;
            }
            
            final Operation operation = operationRegistry.getNewOperationById(OperationIdType.ORDER_ID, orderId, false);

            String sessionId = fixOrder.getSessionId();
            LOGGER.debug("SESSION ID : {}", sessionId);
            if (sessionId != null) {
                operationRegistry.bindOperation(operation, OperationIdType.FIX_SESSION, sessionId);
                operation.addIdentifier(OperationIdType.CUSTOMER_CHANNEL, "OMS2");
            }

            fixOrder.setCustomerFinder(customerFinder);
            fixOrder.setSqlInstrumentDao(sqlInstrumentDao);
            fixOrder.setInstrumentFinder(instrumentFinder);
            OMS1FixOrderInputLazyBean amosFixOrder = (OMS1FixOrderInputLazyBean) fixOrder;
            LOGGER.info("Assigned internal OrderId {} to new order {}", amosFixOrder.getFixOrderId(), orderId);
            //the clientId is the Account field of the order
            //the amosClientId is by default the akros Private code in the spring config, 0800,
            //in a AMOS order the clientId is read from the fix fields ClientID or Account
            if(fixOrder.getClientId() == null || !amosFixOrder.getAmosClientId().equals(fixOrder.getClientId())) 
            {  
                //no value in the Account fix field or amosClientId different from the clientId (it happens 
                //when the AMOS order has a value in the ClientID fix field that is not the Akros Private one,
                //the ClientID in this situations is the customer SinfoCode. A SinfoCode could be mapped on
                //more than one clientCode, save for the use of the customerType field that can point out the
                //right clientCode to be used to find the Customer object)
                fixOrder.setCustomer(customerFinder.getCustomerBySinfoCodeAndFlag(fixOrder.getClientId(), translateCustomerType(fixOrder.getCustomerType())));
            } 
            else 
            {
                fixOrder.setCustomer(customerFinder.getCustomerByFixId(fixOrder.getClientId()));
            }
            Thread.currentThread().setName("onFixOrderNotification," + operation.toString().replace('-','=') +
                            ",ISIN=" + fixOrder.getIsin()+",");			

            // Save order ID to operation data
            operation.addIdentifier(OperationIdType.ORDER_ID, orderId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onCustomerOrder(OMS2CustomerAdapter.this, fixOrder);
                }
            });
        } catch (BestXException e) {
            statFailedFixOrderNotifications.incrementAndGet();
            statExceptions.incrementAndGet();
            LOGGER.error("Error occurred while retrieving operation by order id ({})", orderId, e);
        }  catch (Exception e) {
            //[RR20121126] The CustomerFilterTable was not accessible due to missing privileges, the exception
            //was not catched and the gateway connection went down. Catching here the exception keeps the
            //customer connection active allowing us to log the error
            statFailedFixOrderNotifications.incrementAndGet();
            statExceptions.incrementAndGet();
            LOGGER.error("Error while processing new order {} : {}", orderId, e.getMessage(), e);
        }
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
            Operation op = null;
            try {
                op = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
            } catch (OperationNotExistingException e) {
                // probably it could be a ClOrdID related to a OrderCancelRequest 
                op = operationRegistry.getExistingOperationById(OperationIdType.FIX_REVOKE_ID, orderId);
            }

            final Operation operation = op;			// move all work to child thread, while preserving object-orientation, sending around only full featured objects, not IDs            
            final LazyExecutionReport executionReport = new LazyExecutionReport(operation);
            LOGGER.debug("Forward event to operation in new thread");          
            executor.execute(new Runnable() {
                public void run() {
                    operation.onCustomerExecutionReportAcknowledged(OMS2CustomerAdapter.this, executionReport);
                }
            });
        } catch (OperationNotExistingException e) {
            statFailedFixTradesAcknowledged.incrementAndGet();
            LOGGER.error("Execution Report Ack received for order: {}. But order is not bound to any operation.", orderId, e);
        } catch (BestXException e) {
            statFailedFixTradesAcknowledged.incrementAndGet();
            statExceptions.incrementAndGet();
            LOGGER.error("Error occurred while retrieving operation by order id ({}): ", orderId, e);
        }
    }
    @Override
    public void onFixTradeNotAcknowledged(FixGatewayConnection fixCustomerConnector, final String orderId,
                    String executionReportId, final Integer errorCode, final String errorMessage) {
        LOGGER.info("New Nack to Execution Report from FIX interface - OrderId: {}", orderId);
        statTotalFixTradesNotAcknowledged.incrementAndGet();
        if (orderId == null) {
            LOGGER.error("Nack to Execution Report without Order ID arrived! Discard it.");
            statFailedFixTradesNotAcknowledged.incrementAndGet();
            return;
        }
        try {
            Operation op = null;
            try {
                op = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
            } catch (OperationNotExistingException e) {
                // probably it could be a ClOrdID related to a OrderCancelRequest 
                op = operationRegistry.getExistingOperationById(OperationIdType.FIX_REVOKE_ID, orderId);
            }

            final Operation operation = op;            // move all work to child thread, while preserving object-orientation, sending around only full featured objects, not IDs
            final LazyExecutionReport executionReport = new LazyExecutionReport(operation);
            LOGGER.debug("Forward event to operation in new thread");          
            executor.execute(new Runnable() {
                public void run() {
                    operation.onCustomerExecutionReportNotAcknowledged(OMS2CustomerAdapter.this, executionReport, errorCode, errorMessage);
                }
            });
        } catch (OperationNotExistingException e) {
            statFailedFixTradesNotAcknowledged.incrementAndGet();
            LOGGER.error("Execution Report Nack received for order: {}. But order is not bound to any operation.", orderId, e);
        } catch (BestXException e) {
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
        statFillExecutionReport.incrementAndGet();
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);

        // **********************************************************************
        // [DR20121018] 
        // -- Ricezione OrderCancelRequest -----------------------------------------
        // OrderId:   A122912D000276 >> origClOrdID >> OperationIdType.ORDER_ID
        // Revoke Id: A122912F000055 >> clOrdID     >> OperationIdType.FIX_REVOKE_ID

        // -- Spedizione ExecutionReport.Canceled ----------------------------------
        // clOrdID (11) = 122912F000055 >> recuperato dall'operation
        // orderID (37) = 122912D000276 >> orderID
        // origClOrdID (41) = 122912D000276 >> orderID
        // -------------------------------------------------------------------------

        String clOrdID = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
        OMS2FixExecutionReportOutputLazyBean bean = new OMS2FixExecutionReportOutputLazyBean(fixSessionId, order, clOrdID, orderId, executionReport, errorCode.getCode(), rejectReason, micCodeService);
        fixGatewayConnection.sendExecutionReport(this, bean);    
    }

    /** 
     * Method used to send partial fills execution reports
     */
    @Override
    public void sendPartialFillExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, BigDecimal cumQty) throws BestXException {
        // Send Fix SessionID

        LOGGER.info("Sending partial fill={}", ((MarketExecutionReport) executionReport));		
        LOGGER.debug("Sending partial fill to customer {} - Fix ID: {}", source.getOrder().getCustomer().getName(),
                        source.getOrder().getCustomer().getFixId());
//        setNonRegulatedCounterPart(order, executionReport, micCodeService);
        LOGGER.debug("Cum Qty: {}", cumQty.doubleValue());
        LOGGER.debug("Ord Qty: {}", order.getQty().doubleValue());
        statFillExecutionReport.incrementAndGet();
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode(executionReport.getCounterPart());
        if (marketMaker == null)
        {
            marketMaker = marketMakerFinder.getMarketMakerByCode(executionReport.getExecBroker());
        }
        OMS2FixExecutionReportOutputLazyBean bean = new AmosFixPartFillExecutionReportOutputLazyBean(fixSessionId, quote, order, orderId, attempt, executionReport, cumQty, micCodeService, marketMaker);
        fixGatewayConnection.sendExecutionReport(this, bean);    
    }

    /**
     * Method used to send the closure message report, which is the execution report to be sent
     * after all the partial fills
     */
    @Override
    public void sendExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport) throws BestXException {
        // Send Fix SessionID
        LOGGER.debug("Sending Execution Report for OrderId: {}", orderId);
        LOGGER.info("Sending fill={}", executionReport);		
//        setNonRegulatedCounterPart(order, executionReport, micCodeService);
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode(executionReport.getCounterPart());
        if (marketMaker == null)
        {
            marketMaker = marketMakerFinder.getMarketMakerByCode(executionReport.getExecBroker());
        }
        OMS2FixExecutionReportOutputLazyBean bean = new AmosFixCalculatedExecutionReportOutputLazyBean(fixSessionId, quote, order, orderId, attempt, executionReport, micCodeService, marketMaker);
        fixGatewayConnection.sendExecutionReport(this, bean);    
    }

    /**
     * @param order
     * @param executionReport
     * @throws BestXException
     */
    @Deprecated
    private void setNonRegulatedCounterPart(Order order, ExecutionReport executionReport, MICCodeService micCodeService) throws BestXException
    {
        if (micCodeService.isOTCMarket(executionReport.getMarket())) {
            MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode(executionReport.getExecBroker());
            if (marketMaker != null && marketMaker.getSinfoCode() != null){
                executionReport.setCounterPart("01" + marketMaker.getSinfoCode().replaceFirst("^00*", ""));
            }
        }
    }
    @Override
    public void onConnection(Connection source) {
        LOGGER.info("FIX Gateway Connected");
        statConnected = true;
        if (connectionListener != null)
        {
            connectionListener.onConnection(this);
        }
    }
    @Override
    public void onDisconnection(Connection source, String reason) {
        LOGGER.info("FIX Gateway Disconnected");
        statConnected = false;
        if (connectionListener != null)
        {
            connectionListener.onDisconnection(source, reason);
        }
    }
    // commands
    @Override
    public void connect() {
        LOGGER.info("Connecting to FIX Gateway");
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
        if (statLastStartup == null)
        {
            return 0L;
        }
        return (DateService.currentTimeMillis() - statLastStartup.getTime());
    }
    @Override
    public long getNumberOfExceptions() {
        return statExceptions.get();
    }
    @Override
    public double getAvgInputPerSecond() {
        if (statStart == null)
        {
            return 0.0;
        }
        long currentTime = DateService.currentTimeMillis();
        long startTime = statStart.getTime();
        return (double)(statTotalFixOrderNotifications.get() +
                        statTotalFixRfqNotifications.get() +
                        statTotalFixQuotesAcknowledged.get() +
                        statTotalFixQuotesNotAcknowledged.get() +
                        statTotalFixTradesAcknowledged.get() +
                        statTotalFixTradesNotAcknowledged.get()) / (currentTime - startTime + 1) * 1000.0; // avoid div per zero
    }
    @Override
    public double getAvgOutputPerSecond() {
        if (statStart == null)
        {
            return 0.0;
        }
        long currentTime = DateService.currentTimeMillis();
        long startTime = statStart.getTime();
        return (double)(statRfqResponses.get() +
                        statOrderRejects.get() +
                        statRfqReject.get() +
                        statRejectExecutionReport.get() +
                        statFillExecutionReport.get()) / (currentTime - startTime + 1) * 1000.0; // avoid div per zero
    }
    @Override
    public double getPctOfFailedInput() {
        long totInput = statTotalFixOrderNotifications.get() +
                        statTotalFixRfqNotifications.get() +
                        statTotalFixQuotesAcknowledged.get() +
                        statTotalFixQuotesNotAcknowledged.get() +
                        statTotalFixTradesAcknowledged.get() +
                        statTotalFixTradesNotAcknowledged.get();
        if (totInput > 0) 
        {
            return (double)(statFailedFixOrderNotifications.get() +
                            statFailedFixRfqNotifications.get() +
                            statFailedFixTradesAcknowledged.get() +
                            statFailedFixTradesNotAcknowledged.get() +
                            statFailedFixQuotesAcknowledged.get() +
                            statFailedFixQuotesNotAcknowledged.get()) / totInput * 100;
        } 
        else
        {
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
        FixOrderResponseOutputLazyBean bean = new AmosFixOrderResponseOutputLazyBean(fixSessionId, order, ErrorCode.OK, "Order accepted.");
        fixGatewayConnection.sendOrderResponse(this, bean);    
    }
    @Override
    public void sendOrderResponseNack(Operation source, Order order, String orderId, String errorMsg) throws BestXException {
        // Send Fix SessionID
        LOGGER.info("Sending Order Response Nack for OrderId: {}", orderId);
        statOrderResponse.incrementAndGet();
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        FixOrderResponseOutputLazyBean bean = new AmosFixOrderResponseOutputLazyBean(fixSessionId, order, ErrorCode.GENERIC_ERROR, errorMsg);
        fixGatewayConnection.sendOrderResponse(this, bean);    
    }

    /**
     * The field fixSessionId is needed only as interface implementation, but it is not used yet
     */
    @Override
    public void onFixRevokeNotification(
                    FixGatewayConnection fixCustomerConnector, String orderId, final String revokeId, String fixSessionId) {
        LOGGER.info("New revoke from FIX interface - OrderId: {} - Revoke Id: {}", orderId, revokeId);
        if (orderId == null) {
            LOGGER.error("REVOKE without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
            Runnable runnable = new Runnable() {
                String param = revokeId;
                public void run() {
                    try {
                        operationRegistry.bindOperation(operation, OperationIdType.FIX_REVOKE_ID, param);
                    } catch (BestXException e) {
                        LOGGER.error("An error occurred while retrieving Operation by order: {}", param, e);
                        return;
                    }
                    //20101202 Ruggero : I-41 Revoche Automatiche
                    //set the flag for a reception of a customer revoke for this operation
                    operation.setCustomerRevokeReceived(true);

                    operation.onFixRevoke(OMS2CustomerAdapter.this);
                }
            };
            executor.execute(runnable);
        } catch (OperationNotExistingException e) {
            LOGGER.error("REVOKE received for unknown Order ID. Discard it.");
            return;
        } catch (BestXException e) {
            LOGGER.error("An error occurred while retrieving Operation by order: {}", orderId, e);
            return;
        }
    }
    @Override
    public void sendRevokeAck(Operation source, String orderId, String comment) throws BestXException {
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
        LOGGER.info("Sending Revoke ACK - OrderId: " + orderId + " - RevokeId: " + revokeId + " - Comment: " + comment);
        FixRevokeResponseOutputLazyBean revokeResponse = new AmosFixRevokeResponseOutputLazyBean(fixSessionId, orderId, true, comment, source);
        fixGatewayConnection.sendRevokeResponse(this, revokeId, revokeResponse);
    }
    @Override
    public void sendRevokeNack(Operation source, Order order, String comment) throws BestXException {
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
        LOGGER.info("Sending Revoke NACK - OrderId: {} - RevokeId: {} - Comment: {}", order.getFixOrderId(), revokeId, comment);
        FixRevokeResponseOutputLazyBean revokeResponse = new AmosFixRevokeResponseOutputLazyBean(fixSessionId, order.getFixOrderId(), false, comment, source);
        fixGatewayConnection.sendRevokeResponse(this, revokeId, revokeResponse);
        //20101221 - Ruggero : there is no need to send another revoke reject
        //sendRevokeReport(source, order, RevocationState.NOT_ACKNOWLEDGED, comment);
    }

    @Override
    public void sendRevokeReport(Operation source, Order order, RevocationState revocationState, String comment) throws BestXException {
        String fixSessionId = source.getIdentifier(OperationIdType.FIX_SESSION);
        String revokeId = source.getIdentifier(OperationIdType.FIX_REVOKE_ID);
        LOGGER.info("Sending Revoke Report - OrderId: {} - RevokeId: {} - Revocation State: {} - Comment: {}", order.getFixOrderId(), revokeId, revocationState.name(), comment);
        FixRevokeReportLazyBean revokeReport = new AmosFixRevokeReportLazyBean(fixSessionId, source, revocationState == RevocationState.MANUAL_ACCEPTED ? true : false, comment);
        fixGatewayConnection.sendRevokeReport(this, revokeId, revokeReport);
    }

    public MarketMakerFinder getMarketMakerFinder() {
        return marketMakerFinder;
    }

    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    @Override
    public boolean isFillsManager(Operation operation) {
        LOGGER.debug("{} is an AMOS operation, ok for managing fills", operation.toString());
        return true;
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
	public void sendOrderCancelReject(Operation source, String comment)
			throws BestXException {
		// TODO Auto-generated method stub
		
	}
}

