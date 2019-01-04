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
package it.softsolutions.bestx.test.fix;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.fix.BXBusinessMessageReject;
import it.softsolutions.bestx.fix.BXExecutionReport;
import it.softsolutions.bestx.fix.BXNewOrderSingle;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
import it.softsolutions.bestx.fix.BXOrderCancelRequest;
import it.softsolutions.bestx.fix.BXReject;
import it.softsolutions.bestx.fix.field.Currency;
import it.softsolutions.bestx.fix.field.HandlInst;
import it.softsolutions.bestx.fix.field.IDSource;
import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.SettlmntTyp;
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
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 14, 2012 
 * 
 **/
public class BloombergFIXClientTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BloombergFIXClientTestHelper.class);

    protected static BlockingQueue<BXExecutionReport> bxExecutionReports = new ArrayBlockingQueue<BXExecutionReport>(10);
    protected static BlockingQueue<BXOrderCancelReject> bXOrderCancelRejects = new ArrayBlockingQueue<BXOrderCancelReject>(10);
    protected static BlockingQueue<BXReject> bxRejects = new ArrayBlockingQueue<BXReject>(10);
    
    private static Semaphore semaphore;
    protected static FIXClient bloombergFixClient; 
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        bloombergFixClient = new FIXClientImpl();
        
        semaphore = new Semaphore(0);
        
        bloombergFixClient.init("bloomberg-unittest-bestx-fix-client.properties", "", new FIXClientCallback() {
            
            @Override
            public void onExecutionReport(BXExecutionReport bxExecutionReport) throws FIXClientException {
                LOGGER.debug("{}", bxExecutionReport);
                bxExecutionReports.add(bxExecutionReport);
            }

            @Override
            public void onLogon(SessionID sessionID) {
                LOGGER.info("{}", sessionID);
                semaphore.release();
            }

            @Override
            public void onLogout(SessionID sessionID) {
                LOGGER.info("{}", sessionID);                
            }

            @Override
            public void onOrderCancelReject(BXOrderCancelReject bxOrderCancelReject) throws FIXClientException {
                LOGGER.debug("{}", bxOrderCancelReject);
                bXOrderCancelRejects.add(bxOrderCancelReject);
            }

            @Override
            public void onReject(BXReject bxReject) throws FIXClientException {
                LOGGER.debug("{}", bxReject);
                bxRejects.add(bxReject);
            }

            @Override
            public void onBusinessMessageReject(BXBusinessMessageReject bxBusinessMessageReject) throws FIXClientException {
                LOGGER.debug("{}", bxBusinessMessageReject);
            }
        });
        
        boolean connected = semaphore.tryAcquire(10, TimeUnit.SECONDS);
        org.junit.Assert.assertTrue("Connection to FIX acceptor failed", connected);
    }
    
    
    static BXNewOrderSingle createNewOrderSingle(String clOrdID, String account, String securityID, Side side, Double price, OrdType ordType, Double orderQty, Date futSettDate)  {
        
        BXNewOrderSingle bxNewOrderSingle = new BXNewOrderSingle();
        bxNewOrderSingle.setAccount(account);
        bxNewOrderSingle.setCurrency(Currency.EUR);
        bxNewOrderSingle.setIdSource(IDSource.IsinNumber);
        bxNewOrderSingle.setOrderQty(orderQty);
        bxNewOrderSingle.setTimeInForce(TimeInForce.Day);
        bxNewOrderSingle.setTransactTime(new Date());
        bxNewOrderSingle.setSecurityID(securityID);
        bxNewOrderSingle.setClOrdID(clOrdID);
        bxNewOrderSingle.setHandlInst(HandlInst.AutomatedExecutionOrderPrivate);
        bxNewOrderSingle.setOrdType(ordType);
        bxNewOrderSingle.setPrice(price);
        bxNewOrderSingle.setSide(side);
        bxNewOrderSingle.setSymbol(securityID);
        bxNewOrderSingle.setSettlmntTyp(SettlmntTyp.Future);
        bxNewOrderSingle.setFutSettDate(futSettDate);
        
        return bxNewOrderSingle;
    }
    
    protected static BXOrderCancelRequest createOrderCancelRequest(String clOrdID, String orderID, String origClOrdID, String securityID, Side side, Double orderQty) {
        
        BXOrderCancelRequest bxOrderCancelRequest = new BXOrderCancelRequest();
        bxOrderCancelRequest.setClOrdID(clOrdID);
        bxOrderCancelRequest.setOrderID(orderID);
        bxOrderCancelRequest.setOrigClOrdID(origClOrdID);
        bxOrderCancelRequest.setSecurityID(securityID);
        bxOrderCancelRequest.setSymbol(securityID);
        bxOrderCancelRequest.setSide(side);
        bxOrderCancelRequest.setOrderQty(orderQty);
        bxOrderCancelRequest.setTransactTime(new Date());
        
        return bxOrderCancelRequest;
    }

}
