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

import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.Side;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 14, 2012 
 * 
 **/
public class OrderCancelRequestTest extends FIXClientTestHelper {
    
    private static final String ACCOUNT = "1994";
    private static final String SECURITY_ID = "XS0170558877";
    private static final Side SIDE = Side.Buy;
    private static final Double PRICE = 103.5;
    private static final OrdType ORD_TYPE = OrdType.Limit;
    private static final Double ORDER_QTY = 30000.0;
    private static final Date FUT_SETT_DATE = DateUtils.addDays(new Date(), 3);
    
//    @Test
//    public void fake() {
//        assertTrue(1 + 1 == 2);
//    }
    /*
    @Test//(timeout = 15000)
    public void orderCancelRequestValid() throws ParseException, FIXClientException, InterruptedException {
        String clOrdID = "" + System.currentTimeMillis();
        
        BXNewOrderSingle bxNewOrderSingle = createNewOrderSingle(clOrdID, ACCOUNT, SECURITY_ID, SIDE, PRICE, ORD_TYPE, ORDER_QTY, FUT_SETT_DATE);
        fixClient.manageNewOrderSingle(bxNewOrderSingle);
        
        BXExecutionReport bxExecutionReportNew = bxExecutionReports.take();
        assertNotNull(bxExecutionReportNew);

        Thread.sleep(5000);
        
        clOrdID = "" + System.currentTimeMillis();
        String orderID = bxExecutionReportNew.getOrderID();
        String origClOrdID = bxExecutionReportNew.getClOrdID();
        BXOrderCancelRequest bxOrderCancelRequest = createOrderCancelRequest(clOrdID, orderID, origClOrdID, SECURITY_ID, SIDE, ORDER_QTY);
        fixClient.manageOrderCancelRequest(bxOrderCancelRequest);
        
        BXExecutionReport bxExecutionReport2 = bxExecutionReports.take();
        assertNotNull(bxExecutionReport2);
    }
    */
    /**
     * 
     * - send Order will null Account
     * - verify that execution report with OrdStatus=rejected is received
     * 
     * - send order cancel request
     * - verify that order cancel reject is receievd (OrdStatus = Canceled)
     * 
     */
    /*
    @Test(timeout = 15000)
    public void nullAccount() throws ParseException, FIXClientException, InterruptedException {
        String clOrdID = "" + System.currentTimeMillis();
        
        BXNewOrderSingle bxNewOrderSingle = createNewOrderSingle(clOrdID, ACCOUNT, SECURITY_ID, SIDE, PRICE, ORD_TYPE, ORDER_QTY, FUT_SETT_DATE);
        fixClient.manageNewOrderSingle(bxNewOrderSingle);
        
        BXExecutionReport bxExecutionReportNew = bxExecutionReports.take();
        assertNotNull(bxExecutionReportNew); 
        assertEquals(OrdStatus.Rejected, bxExecutionReportNew.getOrdStatus());

        Thread.sleep(5000);
        
        clOrdID = "" + System.currentTimeMillis();
        String orderID = bxExecutionReportNew.getOrderID();
        String origClOrdID = bxExecutionReportNew.getClOrdID();
        BXOrderCancelRequest bxOrderCancelRequest = createOrderCancelRequest(clOrdID, orderID, origClOrdID, SECURITY_ID, SIDE, ORDER_QTY);
        fixClient.manageOrderCancelRequest(bxOrderCancelRequest);
        
        BXOrderCancelReject bxOrderCancelReject = bXOrderCancelRejects.take();
        assertNotNull(bxOrderCancelReject);
        assertEquals(OrdStatus.Canceled, bxOrderCancelReject.getOrdStatus());
    }
    */
    /*
    @Test(timeout = 15000)
    public void orderCancelReject() throws ParseException, FIXClientException, InterruptedException {
        String clOrdID = "" + System.currentTimeMillis();
        
        BXNewOrderSingle bxNewOrderSingle = createNewOrderSingle(clOrdID, ACCOUNT + "#AAA", SECURITY_ID, SIDE, PRICE, ORD_TYPE, ORDER_QTY, FUT_SETT_DATE);
        fixClient.manageNewOrderSingle(bxNewOrderSingle);
        
        BXExecutionReport bxExecutionReportNew = bxExecutionReports.take();
        assertNotNull(bxExecutionReportNew);
        
        clOrdID = "" + System.currentTimeMillis();
        String orderID = bxExecutionReportNew.getOrderID();
        String origClOrdID = bxExecutionReportNew.getClOrdID();
        BXOrderCancelRequest bxOrderCancelRequest = createOrderCancelRequest(clOrdID, orderID, origClOrdID, SECURITY_ID, SIDE, ORDER_QTY);
        fixClient.manageOrderCancelRequest(bxOrderCancelRequest);
        
        BXOrderCancelReject bxOrderCancelReject = bXOrderCancelRejects.take();
        assertNotNull(bxOrderCancelReject);
        assertEquals(clOrdID, bxOrderCancelReject.getClOrdID());
        assertEquals(orderID, bxOrderCancelReject.getOrderID());
        assertEquals(origClOrdID, bxOrderCancelReject.getOrigClOrdID());
        // [DR20120614] Controllo OrdStatus = "Canceled" come da specifiche SS!, ma non è standard FIX
        assertEquals(OrdStatus.Canceled, bxOrderCancelReject.getOrdStatus());
        assertEquals(CxlRejResponseTo.OrderCancelRequest, bxOrderCancelReject.getCxlRejResponseTo());
        assertTrue(bxOrderCancelReject.getText().startsWith("Cancellation rejected"));
    }
    */
}
