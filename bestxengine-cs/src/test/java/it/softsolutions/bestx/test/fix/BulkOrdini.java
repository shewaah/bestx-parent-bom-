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
package it.softsolutions.bestx.test.fix;

import java.text.ParseException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.fix.BXBusinessMessageReject;
import it.softsolutions.bestx.fix.BXExecutionReport;
import it.softsolutions.bestx.fix.BXNewOrderSingle;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
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
 * Project Name : bestxengine-akros 
 * First created by: davide.rossoni 
 * Creation date: 05/gen/2013 
 * 
 **/
public class BulkOrdini {
     
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkOrdini.class);
    
    private static final int NUM_THREADS = 30;
    private static final int NUM_ORDERS = 50;
    private static final int SLEEP = 10;
    
    private static final Random random = new Random(System.currentTimeMillis());
    
    private static final String ACCOUNT = "1994";
    
    // --------------------------------------------------------
    // Good!!! DR20130829
    // --------------------------------------------------------
//    private static final String SECURITY_ID = "XS0169888558"; 
//    private static final String SECURITY_ID = "XS0170558877";
//    private static final String SECURITY_ID = "XS0225369403";
//    private static final String SECURITY_ID = "XS0055498413";
    
    private static final String[] SECURITY_IDs = new String[]{ "XS0169888558", "XS0170558877", "XS0225369403", "XS0055498413" };

//    private static final String SECURITY_ID = "EXE225369403";
//    private static final String SECURITY_ID =   "EXE000000001";
//    private static final String SECURITY_ID = "XX0000000003";
//    private static final String SECURITY_ID = "CUR000000000";
    private static final Side SIDE = Side.Buy;
    private static final Double PRICE = 109.4;
//    private static final OrdType ORD_TYPE = OrdType.Limit;
    private static final OrdType ORD_TYPE = OrdType.Market;
    private static final Double ORDER_QTY = 1000.0;
    private static final Date FUT_SETT_DATE = DateUtils.addDays(new Date(), 3);
    
    private static Semaphore semaphore;  
    protected static FIXClient fixClient;

    
    public synchronized void init() throws FIXClientException, InterruptedException {
        fixClient = new FIXClientImpl();
        
        semaphore = new Semaphore(0);
        
        fixClient.init("bestx-fix-client-OMS1.properties", "", new FIXClientCallback() {
            
            @Override
            public void onExecutionReport(BXExecutionReport bxExecutionReport) throws FIXClientException {
                LOGGER.debug("{}", bxExecutionReport);
            }

            @Override
            public void onOrderCancelReject(BXOrderCancelReject bxOrderCancelReject) throws FIXClientException {
                LOGGER.debug("{}", bxOrderCancelReject);
            }

            @Override
            public void onReject(BXReject bxReject) throws FIXClientException {
                LOGGER.debug("{}", bxReject);
            }

            @Override
            public void onLogon(SessionID sessionID) {
                LOGGER.info("");
                semaphore.release();
            }

            @Override
            public void onLogout(SessionID sessionID) {
                LOGGER.info("");                
            }

            @Override
            public void onBusinessMessageReject(BXBusinessMessageReject bxBusinessMessageReject) throws FIXClientException {
                // TODO Auto-generated method stub
                
            }
        });
        
        boolean connected = semaphore.tryAcquire(10, TimeUnit.SECONDS);
        System.out.println("Connected = " + connected);
    }
    
    public void run(int count, int sleep) throws ParseException, FIXClientException, InterruptedException {
        
        for (int i = 0; i < count; i++) {
            if (sleep > 0) {
                Thread.sleep(random.nextInt(sleep));
            }
            String clOrdID = "" + System.nanoTime();
            Double orderQty = (random.nextInt(10) + 1) * ORDER_QTY;
            OrdType ordType = random.nextInt(4) == 0 ? OrdType.Limit : OrdType.Market;
            BXNewOrderSingle bxNewOrderSingle = createNewOrderSingle(clOrdID, ACCOUNT, SECURITY_IDs[random.nextInt(SECURITY_IDs.length)], SIDE, PRICE, ordType, orderQty, FUT_SETT_DATE);
            
            clOrdID = "" + System.nanoTime();
            clOrdID = clOrdID.substring(clOrdID.length() - 11);
//            clOrdID = "CCC" + ("" + System.nanoTime()).substring(clOrdID.length() - 7) + "X";
            bxNewOrderSingle.setClOrdID(clOrdID);
            fixClient.manageNewOrderSingle(bxNewOrderSingle);
System.out.println(new Date() + " " + bxNewOrderSingle);
        }
    }
    
    public static void main(String[] args) {
        try {
            final BulkOrdini bulkOrdini = new BulkOrdini();
            bulkOrdini.init();
            
            for (int i = 0; i < NUM_THREADS; i++) {
                new Thread() {
                    public void run() {
                        try {
                            bulkOrdini.run(NUM_ORDERS, SLEEP);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            
            do {
                Thread.sleep(3000);
            } while (true);
            
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    
    private static BXNewOrderSingle createNewOrderSingle(String clOrdID, String account, String securityID, Side side, Double price, OrdType ordType, Double orderQty, Date futSettDate) throws ParseException {
        
        BXNewOrderSingle bxNewOrderSingle = new BXNewOrderSingle();
        bxNewOrderSingle.setAccount(account);
        bxNewOrderSingle.setCurrency(Currency.EUR);
        bxNewOrderSingle.setIdSource(IDSource.IsinNumber);
        bxNewOrderSingle.setOrderQty(orderQty);
        bxNewOrderSingle.setTimeInForce(TimeInForce.Day);
        bxNewOrderSingle.setSecurityID(securityID);
        bxNewOrderSingle.setClOrdID(clOrdID);
        bxNewOrderSingle.setHandlInst(HandlInst.AutomatedExecutionOrderPrivate);
        bxNewOrderSingle.setOrdType(ordType);
        if (ordType == OrdType.Limit) {
            bxNewOrderSingle.setPrice(price);
        }
        bxNewOrderSingle.setSide(side);
        bxNewOrderSingle.setSymbol(securityID);
        bxNewOrderSingle.setSettlmntTyp(SettlmntTyp.Future);
        bxNewOrderSingle.setFutSettDate(futSettDate);
        bxNewOrderSingle.setTransactTime(new Date());
        
        return bxNewOrderSingle;
    }
}
