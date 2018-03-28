/**
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
package it.softsolutions.bestx.fixclient;

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

import java.text.ParseException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.SessionID;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client
 * First created by: davide.rossoni
 * Creation date: 31/ott/2013
 * 
 */
public class MyFIXClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MyFIXClient.class);
	
    public static String CFG_FILE = "bestx-fix-client.properties";
	
    private static FIXClient fixClient; 
    private static Semaphore semaphore;
//    private static SessionID sessionId;

    private static final Random random = new Random(System.currentTimeMillis());
    private static final int NUM_THREADS = 1;
    private static final int NUM_ORDERS = 1;
    private static final int SLEEP = 100;
    
    // UNIT_TEST_CLIENT_CODE, 1994
    private static final String ACCOUNT = "1994"; 
    private static final String[] SECURITY_IDs = new String[]{ "XS0169888558", "XS0170558877", "XS0225369403", "XS0055498413" };
    private static final Double PRICE = 101.4;
    private static final Double ORDER_QTY = 1000.0;
    private static final Date FUT_SETT_DATE = DateUtils.addDays(new Date(), 3);
//    private static final Side SIDE = Side.Buy;
//    private static final OrdType ORD_TYPE = OrdType.Market;
    private static long lastOrderId = -1;
    
    private void init() throws FIXClientException, InterruptedException {
        fixClient = new FIXClientImpl();
        semaphore = new Semaphore(0); 

        fixClient.init(CFG_FILE, new FIXClientCallback() {
			
			@Override
			public void onReject(BXReject bxReject) throws FIXClientException {
				LOGGER.debug("bxReject: {}", bxReject);
			}
			
			@Override
			public void onOrderCancelReject(BXOrderCancelReject bxOrderCancelReject) throws FIXClientException {
				LOGGER.debug("bxOrderCancelReject: {}", bxOrderCancelReject);
                
			}
			
			@Override
			public void onLogout(SessionID sessionID) {
				LOGGER.debug("sessionID: {}", sessionID);
                
			}
			
			@Override
			public void onLogon(SessionID sessionID) {
				LOGGER.debug("sessionID = {}", sessionID);
                
//                sessionId = sessionID;
                semaphore.release();
			}
			
			@Override
			public void onExecutionReport(BXExecutionReport bxExecutionReport) throws FIXClientException {
				LOGGER.debug("bxExecutionReport = {}", bxExecutionReport);
                
			}
			
			@Override
			public void onBusinessMessageReject(BXBusinessMessageReject bxBusinessMessageReject) throws FIXClientException {
				LOGGER.debug("bxBusinessMessageReject: {}", bxBusinessMessageReject);
            	
			}
		});
        
        semaphore.acquire();
    }
    
    public void run(int count, int sleep) throws ParseException, FIXClientException, InterruptedException {
    	LOGGER.debug("count = {}, sleep = {}", count, sleep);
        
        
        for (int i = 0; i < count; i++) {
            if (sleep > 0) {
                Thread.sleep(random.nextInt(sleep));
            }
            Double orderQty = (random.nextInt(10) + 1) * ORDER_QTY;
//            OrdType ordType = random.nextInt(5) == 0 ? OrdType.Market : OrdType.Limit;
            OrdType ordType = OrdType.Market;
            Side side = random.nextInt(4) == 0 ? Side.Sell : Side.Buy;
            Double price = null;
            if (ordType == OrdType.Limit) {
//            	price = PRICE + (side == Side.Buy ? -1 : 1) * (random.nextInt(100) * 0.1); 
            	price = PRICE + (side == Side.Buy ? -1 : 1) * (10.0);
            }
            TimeInForce timeInForce = random.nextBoolean() ? TimeInForce.GoodTillCancel : TimeInForce.Day;
            String securityID = SECURITY_IDs[random.nextInt(SECURITY_IDs.length)];
            
            // Fixed values
            timeInForce = TimeInForce.GoodTillCancel;
//            timeInForce = TimeInForce.Day;
//            side = Side.Buy;
//            price = 88.05;
//            price = 106.05;
            ordType = OrdType.Limit;
//            orderQty = 1000.0;
            //TW Bogus ISINS
//            securityID = "XS0225369403";
//            securityID = "XS0451037062";
//            securityID = "DE000A0GMHG2";
//            securityID = "XS0169888558";
            //TW Test Env ISINS
            securityID = "XS0619547838";
//            securityID = "US40411EAB48";
//            securityID = "XS0546057570";
//            securityID = "XS0603832782";
            side = Side.Buy;
            //orderQty = 454000.0; //execution
            orderQty = 450000.0; //timeout
//            orderQty = 10000000.0;
            //price = 104.465;
            price = 106.4;
//            securityID = "GB0033280339";
            Currency currency = Currency.EUR;
            BXNewOrderSingle bxNewOrderSingle = createNewOrderSingle(securityID, side, price, ordType, orderQty, timeInForce, currency);
        	LOGGER.debug("{}", bxNewOrderSingle);            
            fixClient.manageNewOrderSingle(bxNewOrderSingle);
            
//            Thread.sleep(10000);
//            sendRevoke();
        }
    }
    
    public void sendRevoke() throws FIXClientException {
        BXOrderCancelRequest cancelRequest = new BXOrderCancelRequest();
        
        cancelRequest.setClOrdID("" + System.nanoTime());
        cancelRequest.setOrigClOrdID("" + lastOrderId);
//        cancelRequest.setOrigClOrdID("807571901904509");
        
//        cancelRequest.setOrderID("2910922699431256");
        cancelRequest.setSymbol("XS0225369403");
        cancelRequest.setSide(Side.Buy);
        cancelRequest.setSecurityID("XS0225369403");
        cancelRequest.setOrderQty(50000.0);
        cancelRequest.setTransactTime(new Date());
        LOGGER.debug("{}", cancelRequest);
        fixClient.manageOrderCancelRequest(cancelRequest);
        
    }
    public void paolo() throws ParseException, FIXClientException, InterruptedException {
    	BXNewOrderSingle bxNewOrderSingle = new BXNewOrderSingle();

    	lastOrderId = System.currentTimeMillis();
        bxNewOrderSingle.setClOrdID("" + lastOrderId);
        bxNewOrderSingle.setCurrency(Currency.EUR);
        bxNewOrderSingle.setHandlInst(HandlInst.AutomatedExecutionOrderPrivate);
        bxNewOrderSingle.setIdSource(IDSource.IsinNumber);
        bxNewOrderSingle.setOrderQty(10000.0);
        bxNewOrderSingle.setOrdType(OrdType.Market);
        bxNewOrderSingle.setTimeInForce(TimeInForce.Day);
        bxNewOrderSingle.setSettlmntTyp(SettlmntTyp.Future);
        bxNewOrderSingle.setFutSettDate(FUT_SETT_DATE);
        
        String securityID = "BB0000000010";
        bxNewOrderSingle.setSecurityID(securityID);
        bxNewOrderSingle.setSide(Side.Buy);
        bxNewOrderSingle.setSymbol(securityID);
        bxNewOrderSingle.setAccount("UNIT_TEST_CLIENT_CODE");
        bxNewOrderSingle.setTransactTime(new Date());
        
        LOGGER.debug("{}", bxNewOrderSingle);            
        
        fixClient.manageNewOrderSingle(bxNewOrderSingle);
    }
    
    private BXNewOrderSingle createNewOrderSingle(String securityID, Side side, Double price, OrdType ordType, Double orderQty, TimeInForce timeInForce, Currency currency) {
        BXNewOrderSingle order = new BXNewOrderSingle();

        lastOrderId = System.nanoTime();
        order.setClOrdID("" + lastOrderId);
        order.setSecurityID(securityID);
        order.setSide(side);
        order.setOrderQty(orderQty);
        order.setOrdType(ordType);
        order.setPrice(price);
        order.setTimeInForce(timeInForce);
        order.setAccount(ACCOUNT);
        order.setCurrency(currency);
        order.setHandlInst(HandlInst.AutomatedExecutionOrderPrivate);
        order.setIdSource(IDSource.IsinNumber);
        order.setSettlmntTyp(SettlmntTyp.Future);
        order.setTransactTime(new Date());
        order.setFutSettDate(FUT_SETT_DATE);
        order.setSymbol("[N/A]");
//        order.setTicketOwner("BEL");
//        order.setText("LFNP: qui dolorem ipsum, quia dolor sit, amet, consectetur, adipisci v'elit, sed quia non numquam eius modi tempora incidunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit, qui in ea voluptate velit esse, quam nihil molestiae consequatur, vel illum, qui dolorem eum fugiat, quo voluptas nulla pariatur?At vero eos et acc");
//        order.setText("LF: pota");

        return order;
    }
	
	public static void main(String[] args) {
		final MyFIXClient myFIXClient = new MyFIXClient();
	        try {
	            myFIXClient.init();
            } catch (FIXClientException | InterruptedException e1) {
            	LOGGER.error("{}", e1.getMessage(), e1);
            }

//	        try {
//	            myFIXClient.paolo();
//            } catch (ParseException | FIXClientException | InterruptedException e1) {
//            	LOGGER.error("{}", e1.getMessage(), e1);
//            }
	        
//	      try {
//          myFIXClient.sendRevoke();
//        } catch (FIXClientException e1) {
//          LOGGER.error("{}", e1.getMessage(), e1);
//        }
	        for (int i = 0; i < NUM_THREADS; i++) {
                new Thread() {
                    public void run() {
                        try {
                        	myFIXClient.run(NUM_ORDERS, SLEEP);
//                        	Thread.sleep(2000);
//                        	myFIXClient.sendRevoke();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
	    do {
	        try { Thread.sleep(10000); } catch (InterruptedException e) { }
        } while (true);
    }

	
}
