/*
 * Copyright 1997-2013 SoftSolutions! srl 
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import it.softsolutions.bestx.fix.BXBusinessMessageReject;
import it.softsolutions.bestx.fix.BXExecutionReport;
import it.softsolutions.bestx.fix.BXNewOrderSingle;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
import it.softsolutions.bestx.fix.BXOrderCancelRequest;
import it.softsolutions.bestx.fix.BXReject;
import it.softsolutions.bestx.fix.field.OrdStatus;
import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.Side;
import it.softsolutions.bestx.fix.field.TimeInForce;
import it.softsolutions.bestx.fixclient.FIXClient;
import it.softsolutions.bestx.fixclient.FIXClientCallback;
import it.softsolutions.bestx.fixclient.FIXClientException;
import it.softsolutions.bestx.fixclient.FIXClientImpl;
import quickfix.SessionID;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 12/nov/2013 
 * 
 **/
public class OrderStatesTestHelper {
    static FIXClient fixClient = null; 
    static Semaphore semaphore;
    static SessionID sessionId;

    static BlockingQueue<BXExecutionReport> bxExecutionReports = new ArrayBlockingQueue<BXExecutionReport>(10);
    static BlockingQueue<BXOrderCancelReject> bXOrderCancelRejects = new ArrayBlockingQueue<BXOrderCancelReject>(10);
    static BlockingQueue<BXReject> bxRejects = new ArrayBlockingQueue<BXReject>(10);
    static BlockingQueue<BXBusinessMessageReject> bxBusinessMessageRejects = new ArrayBlockingQueue<BXBusinessMessageReject>(10);

    static JdbcTemplate jdbcTemplate; 
    static ClassPathXmlApplicationContext context;

    public static String CFG_FILE = "order_states_test_oms1_bestx-fix-client.properties";
    public static String CFG_FOLDER = "/";
    static final double DEFAULT_ORDER_QTY = 10000.0;
    
    public OrderStatesTestHelper() {
        super();
    }

    @BeforeClass 
    public static void setupFixClientAndDB() {
        // jdbcTemplate is used to read the states an order has crossed.
        // it refers to pooledDataSource, which is configured according to 
        // hibernate.connection.url parameter in bestxengine-cs/src/test/config/hibernate.properties
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        
        org.junit.Assert.assertNull("Fix client not reset properly in other tests", fixClient);

        fixClient = new FIXClientImpl();
    
        semaphore = new Semaphore(0);

        try {
            fixClient.init(CFG_FILE, CFG_FOLDER, new FIXClientCallback() {

                //@Override
                public void onExecutionReport(BXExecutionReport bxExecutionReport) throws FIXClientException {
                    bxExecutionReports.add(bxExecutionReport);
                }

                //@Override
                public void onLogon(SessionID sessionID) {
                    sessionId = sessionID;
                    semaphore.release();
                }

                //@Override
                public void onLogout(SessionID sessionID) {
                }

                //@Override
                public void onOrderCancelReject(BXOrderCancelReject bxOrderCancelReject) throws FIXClientException {
                    bXOrderCancelRejects.add(bxOrderCancelReject);
                }

                //@Override
                public void onReject(BXReject bxReject) throws FIXClientException {
                    bxRejects.add(bxReject);
                }

                @Override
                public void onBusinessMessageReject(BXBusinessMessageReject bxBusinessMessageReject) throws FIXClientException {
                    bxBusinessMessageRejects.add(bxBusinessMessageReject);
                }
            });
        } catch (FIXClientException e) {
            org.junit.Assert.fail(e.getMessage());
        }

        boolean connected = false;
        try {
            connected = semaphore.tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            org.junit.Assert.fail(e.getMessage());
        }

        org.junit.Assert.assertTrue("Fix client could not connect", connected);
    }
    
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
        fixClient.stop();
        fixClient = null;   //keep fixClient running, it is used by more than one test

        bxExecutionReports.clear();
        bXOrderCancelRejects.clear();
        bxRejects.clear();
        bxBusinessMessageRejects.clear();

        // need to close jdbcTemplate? seems not... 
        //jdbcTemplate.?
    }

    protected void waitAndVerifyExecutionReport(int waitTimeSecs, String expectedClOrdID, OrdStatus expectedOrdStatus, Double expectedPrice, String expectedMkt, String expectedMM, String expectedText) {
        BXExecutionReport executionReport = null;
        try {
            executionReport = getExecutionReport(waitTimeSecs);
        } catch (InterruptedException e) {
            ;
        }
        
        assertNotNull(executionReport);
    
        assertEquals(expectedClOrdID, executionReport.getClOrdID());
        assertEquals(expectedOrdStatus, executionReport.getOrdStatus());
        
        if (expectedPrice != null) assertEquals((Double)expectedPrice, (Double)executionReport.getPrice());
        if (expectedMkt != null) assertEquals(expectedMkt, executionReport.getLastMkt());
        if (expectedMM != null) assertEquals(expectedMM, executionReport.getExecBroker());
        if (expectedText != null) assertEquals(expectedText, executionReport.getText());
    
        //        // get executionMM from DB
        //        String query = "select BancaRiferimento from TabHistoryOrdini where NumOrdine like ?";
        //        final Object[] params = new Object[] { "%" + executionReport.getClOrdID()};
        //
        //        final List<String> results = new ArrayList<String>();
        //        jdbcTemplate.query(query, params, new RowCallbackHandler() {
        //            public void processRow(ResultSet rset) throws SQLException {
        //                results.add(rset.getString(1));
        //            }});
        //
        //        org.junit.Assert.assertEquals(expectedMM, results.get(0));
    }

    private BXExecutionReport getExecutionReport(long maxSecTimeout) throws InterruptedException {
        BXExecutionReport bxExecReport = null;
    
        long startTime = System.currentTimeMillis();
        long nowTime;
    
        while (true) {
            nowTime = System.currentTimeMillis();
            if ( (maxSecTimeout > 0) && (nowTime - startTime) >= (maxSecTimeout*1000.0) ) {
                break;
            }
    
            Thread.sleep(50);
    
            bxExecReport = bxExecutionReports.poll();
            if (bxExecReport != null) {
                System.out.println("ExecReport received: " + bxExecReport);
                break;
            }
    
            BXReject bxReject = bxRejects.poll();
            if (bxReject != null) {
                System.out.println("Reject received: " + bxReject);
                org.junit.Assert.fail();
            }
    
            BXBusinessMessageReject bxBusinessMessageReject = bxBusinessMessageRejects.poll();
            if (bxBusinessMessageReject != null) {
                System.out.println("BusinessMessageReject received: " + bxBusinessMessageReject);
                org.junit.Assert.fail();
            }
        }
    
        return bxExecReport;
    }

    protected void waitAndVerifyOrderCancelReject(int waitTimeSecs, String expectedComment) {
        BXOrderCancelReject orderCancelReject = null;
        try {
            orderCancelReject = getOrderCancelReject(waitTimeSecs);
        } catch (InterruptedException e) {
            ;
        }
        
        assertNotNull(orderCancelReject);
    
        assertEquals(expectedComment, orderCancelReject.getText());
    }
    
    private BXOrderCancelReject getOrderCancelReject(long maxSecTimeout) throws InterruptedException {
        BXOrderCancelReject bxOrderCancelReject = null;
    
        long startTime = System.currentTimeMillis();
        long nowTime;
    
        while (true) {
            nowTime = System.currentTimeMillis();
            if ( (maxSecTimeout > 0) && (nowTime - startTime) >= (maxSecTimeout*1000.0) ) {
                break;
            }
    
            Thread.sleep(50);
    
            bxOrderCancelReject = bXOrderCancelRejects.poll();
            if (bxOrderCancelReject != null) {
                System.out.println("OrderCancelReject received: " + bxOrderCancelReject);
                break;
            }

            BXExecutionReport bxExecutionReport = bxExecutionReports.poll();
            if (bxExecutionReport != null) {
                System.out.println("ExecutionReport received: " + bxExecutionReport);
                org.junit.Assert.fail();
            }
            
            BXReject bxReject = bxRejects.poll();
            if (bxReject != null) {
                System.out.println("Reject received: " + bxReject);
                org.junit.Assert.fail();
            }
    
            BXBusinessMessageReject bxBusinessMessageReject = bxBusinessMessageRejects.poll();
            if (bxBusinessMessageReject != null) {
                System.out.println("BusinessMessageReject received: " + bxBusinessMessageReject);
                org.junit.Assert.fail();
            }
        }
    
        return bxOrderCancelReject;
    }
    
    protected String sendNewOrder(String isin, Side side, int settlementDays) throws FIXClientException {
        bxExecutionReports.clear();
        bXOrderCancelRejects.clear();
        bxRejects.clear();
        bxBusinessMessageRejects.clear();
    
        BXNewOrderSingle order = getNewOrder(isin, side, settlementDays);
    
        System.out.println("Sending order: " + order);
    
        fixClient.manageNewOrderSingle(order);
        
        return order.getClOrdID();
    }

    protected String sendNewOrderOfType(String isin, Side side, int settlementDays, OrdType orderType, TimeInForce timeInForce, Double price, String comment) throws FIXClientException {
        bxExecutionReports.clear();
        bXOrderCancelRejects.clear();
        bxRejects.clear();
        bxBusinessMessageRejects.clear();
    
        BXNewOrderSingle order = getNewOrder(isin, side, settlementDays);
        
        order.setOrdType(orderType);
        order.setTimeInForce(timeInForce);
        if (price != null && price > 0.0) {
            order.setPrice(price);
        }
        
        if ( (comment != null) && (!comment.isEmpty()) ) {
            order.setText(comment);
        }
    
        System.out.println("Sending order: " + order);
    
        fixClient.manageNewOrderSingle(order);
        
        return order.getClOrdID();
    }
    
    protected String sendOrderCancelRequest(String ordID, String isin, Side side) throws FIXClientException {
        BXOrderCancelRequest cancel  = new BXOrderCancelRequest();

        cancel.setClOrdID(ordID);
        cancel.setOrigClOrdID(ordID);
        cancel.setSymbol(isin);
        cancel.setSecurityID(isin);
        cancel.setSide(side);
        cancel.setOrderQty(DEFAULT_ORDER_QTY);
        cancel.setTransactTime(new Date());
        
        System.out.println("Sending order cancel request: " + cancel);
    
        fixClient.manageOrderCancelRequest(cancel);
        
        return cancel.getClOrdID();
    }    
    
    private BXNewOrderSingle getNewOrder(String isin, Side side, int settlementDays) {
        BXNewOrderSingle order = new BXNewOrderSingle();
    
        order.setAccount("UNIT_TEST_CLIENT_CODE");
        String clOrdID = "" + System.currentTimeMillis();
        order.setClOrdID(clOrdID);
        order.setCurrency(it.softsolutions.bestx.fix.field.Currency.EUR);
        order.setHandlInst(it.softsolutions.bestx.fix.field.HandlInst.AutomatedExecutionOrderPrivate);
        order.setIdSource(it.softsolutions.bestx.fix.field.IDSource.IsinNumber);
        order.setOrderQty(DEFAULT_ORDER_QTY);
        order.setOrdType(it.softsolutions.bestx.fix.field.OrdType.Market);
        order.setTimeInForce(it.softsolutions.bestx.fix.field.TimeInForce.Day);
        order.setSettlmntTyp(it.softsolutions.bestx.fix.field.SettlmntTyp.Future);
    
        order.setFutSettDate(DateUtils.addDays(new Date(), settlementDays));
    
        order.setSecurityID(isin);
        order.setSymbol(isin);
    
        order.setSide(side);
        
        order.setTransactTime(new Date());
    
        return order;
    }

    protected void verifyOrderStates(final String clOrdID, String ... expectedStates) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
             org.junit.Assert.fail(e.getMessage());
        }
        Collection<String> expectedStatesList = new ArrayList<String>();
        Collections.addAll(expectedStatesList, expectedStates);
    
        String query = "Select Stato from TabHistoryStati where NumOrdine like ?";
        final Object[] params = new Object[] { "%" + clOrdID};
    
        final Collection<String> results = new ArrayList<String>();
        jdbcTemplate.query(query, params, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                results.add(rset.getString(1));
            }});
    
        assertEquals(expectedStatesList, results);
    }

    protected void cleanDb(String clOrdID) {
        try {
            if (!clOrdID.isEmpty()) {
                String sql = "delete from PriceTable where NumOrdine like '%" + clOrdID + "'";
                jdbcTemplate.execute(sql);
    
                sql = "delete from TentativiStatoMercato where NumOrdine like '%" + clOrdID + "'";            
                jdbcTemplate.execute(sql);
    
                sql = "delete from TabTentativi where NumOrdine like '%" + clOrdID + "'"; 
                jdbcTemplate.execute(sql);
    
                sql = "delete from TabHistoryStati where NumOrdine like '%" + clOrdID + "'"; 
                jdbcTemplate.execute(sql);
    
                sql = "delete from TabHistoryOrdini where NumOrdine like '%" + clOrdID + "'";
                jdbcTemplate.execute(sql);
    
                sql = "delete from OperationState where OperationStateId in " +  
                                "(select OperationStateId from Operation where OrderId = " + 
                                "(select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "' ))";
                jdbcTemplate.execute(sql);
    
                sql = "delete from Proposal where BookId in (" +
                                "select BookId from attempt where OperationId =" + 
                                "(select OperationId from Operation where OrderId =" + 
                                " (select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "'))" +
                                ") and BookId IS NOT NULL";
                jdbcTemplate.execute(sql);
    
                sql = "delete from Book where BookId in (" +
                                "select BookId from attempt where OperationId =" + 
                                "(select OperationId from Operation where OrderId =" + 
                                "(select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "')))";
                jdbcTemplate.execute(sql);
    
                sql = "delete from Attempt where OperationId =" + 
                                "(select OperationId from Operation where OrderId =" + 
                                "(select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "'))";
                jdbcTemplate.execute(sql);
    
                sql = "delete from OperationBinding where OperationId =" + 
                                "(select OperationId from Operation where OrderId =" + 
                                "(select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "'))";
                jdbcTemplate.execute(sql);
    
                sql = "delete from Operation where OrderId =" + 
                                "(select OrderId from Rfq_Order where FixOrderId like '%" + clOrdID + "')";
                jdbcTemplate.execute(sql);
    
                sql = "delete from Rfq_Order where FixOrderId like '%" + clOrdID + "'";
                jdbcTemplate.execute(sql);
            }
        }
        catch (Exception e) {
            org.junit.Assert.fail(e.getMessage());
        }
    }

}