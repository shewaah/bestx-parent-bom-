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

/*
 * -------->>>>>>>>                           <<<<<<<<--------                                                         
 * -------->>>>>>>> Configurazione richiesta: <<<<<<<<--------
 * -------->>>>>>>>                           <<<<<<<<--------
 * 
 * 1)
 * BestX.properties:  -->  Internal.MM.Codes = TEST23
 * 
 * 
 * 2)
 * Book per i test senza internalizzazione, generati dai connectors fake RTFI e BLOOM:
 *   
 *  BLOOMBERG/-/RDEU    | 105.6     x 100000        | ASK | INDICATIVE
 *  BLOOMBERG/-/TEST21  | 105.7     x 0             | ASK | INDICATIVE
 *  RTFI/-/TEST24       | 106.01000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST21       | 106.02000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST25       | 106.03000 x 500000.00000  | ASK | INDICATIVE
 *  
 *  Book per i test con internalizzazione:
 *  
 *  BLOOMBERG/-/RDEU    | 105.6     x 100000        | ASK | INDICATIVE
 *  BLOOMBERG/-/TEST21  | 105.7     x 0             | ASK | INDICATIVE
 *  RTFI/-/TEST24       | 106.01000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST23       | 106.02000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST25       | 106.03000 x 500000.00000  | ASK | INDICATIVE
 *  
 *   
 * 3)
 * BestX, tradestac bloomberg connector, tsox simulator e xt2fixgateway up & running
 * 
 * 
 * 4)
 * tsox bloomberg simulator deve essere configurato adeguatamente...
 * 
 * 5) i timeouts (BestX.properties) devono essere:
 * Market.state.timeout = 8000 
 * Price.state.timeout = 600000
 * Bloomberg.Tsox.due-in-time = 5
 * Bloomberg.Tsox.wiretime = 15
 * Bloomberg.Tsox.hitlift_reply_time = 6
 * 
 */

package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.CSStrategy;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.fix.field.OrdStatus;
import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.Side;
import it.softsolutions.bestx.fix.field.TimeInForce;
import it.softsolutions.bestx.states.BusinessValidationState;
import it.softsolutions.bestx.states.FormalValidationOKState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.OrderReceivedState;
import it.softsolutions.bestx.states.OrderRevocatedState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.StateExecuted;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.bloomberg.BBG_AcceptQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_ExecutedState;
import it.softsolutions.bestx.states.bloomberg.BBG_ReceiveQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;
import it.softsolutions.bestx.states.bloomberg.BBG_StartExecutionState;

import org.junit.Test;

/**  
 *
 * Purpose: this class is mainly for testing of TSOX flows, including Internalization on RTFI
 *
 * - setup required for tests:
 *   - the test launches a Fix Client which sends orders, so a OMS1FixGateway must be up&running
 *   - OMS1FixGateway sends orders to a running session of BestX
 *   - BestX uses BLPConnectorFake and RTFIBSConnectorFake to receive books, and sends QuoteRequest/NewOrderSingle to TSOX, so
 *   - TradeStac bloomberg must be running, connected to TSOXSimulator
 *   - TSOXSimulator must be up&running
 *   
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 5-mar-2013 
 * 
 **/
public class OrderRevoke_StatesTest  extends OrderStatesTestHelper{

    @Test (timeout=10000)
    public void test_LFTH_Revoke_Accepetd()  throws Exception {
        // L'ordine va in stato LimitFile Threshold, quindi receve una revoca  
        // e la accetta automaticamente 
        // Utilizza BB0000000100 (che ha il book normale), e' GTC + Limit (quindi Limit File),
        // ha un limit price di 104 (lontano dalla best ma entro il threshold) --> diventa un LFTH
        //
        // Il commento atteso e' "LF:" (non essendoci nessun commento nell'ordine iniziale)
         
        CSStrategy strategy = (CSStrategy)context.getBean("csStrategy");
        String lfPrefix = strategy.getLimitFileCommentPrefix();
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrderOfType("BB0000000100", Side.Buy, 3, OrdType.Limit, TimeInForce.GoodTillCancel, 104.0, null);

            // riceve execreport iniziale (New)
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);
            
            // wait, so it can go in OrderNotExecutableState
            Thread.sleep(2000);
            sendOrderCancelRequest(clOrdID, "BB0000000100", Side.Buy);

            // riceve execreport Cancelled
            String expectedErrorMsg = Messages.getString("REVOKE_ACKNOWLEDGED");
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Canceled, null, null, null, lfPrefix);
            
            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            OrderNotExecutableState.class.getSimpleName(),
                            OrderRevocatedState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }        
    
    @Test (timeout=10000)
    public void test_LFTH_WithComment_Revoke_Accepetd()  throws Exception {
        // L'ordine va in stato LimitFile Threshold, quindi receve una revoca  
        // e la accetta automaticamente 
        // Utilizza BB0000000100 (che ha il book normale), e' GTC + Limit (quindi Limit File),
        // ha un limit price di 104 (lontano dalla best ma entro il threshold) --> diventa un LFTH
        //
        // Il commento atteso e' "LF:" seguito dal commento gia' presente nell'ordine in ingresso
        //   (attenzione, il commento puo' essere troncato a BestX.properties -> LimitFileCommentMaxLen)
         
        CSStrategy strategy = (CSStrategy)context.getBean("csStrategy");
        String lfPrefix = strategy.getLimitFileCommentPrefix();
        String orderComment = "Comment";
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrderOfType("BB0000000100", Side.Buy, 3, OrdType.Limit, TimeInForce.GoodTillCancel, 104.0, orderComment);

            // riceve execreport iniziale (New)
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);
            
            // wait, so it can go in OrderNotExecutableState
            Thread.sleep(2000);
            sendOrderCancelRequest(clOrdID, "BB0000000100", Side.Buy);

            // riceve execreport Cancelled
            String expectedErrorMsg = Messages.getString("REVOKE_ACKNOWLEDGED");
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Canceled, null, null, null, lfPrefix + " " + orderComment);
            
            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            OrderNotExecutableState.class.getSimpleName(),
                            OrderRevocatedState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=10000)
    public void test_LFNP_Revoke_Accepetd()  throws Exception {
        // L'ordine va in stato LimitFile Threshold, quindi receve una revoca  
        // e la accetta automaticamente 
        // Utilizza BB0000000101 (che ha il book normale ma con qty=0, per cui tutte le quote vengono scartate), e' GTC + Limit (quindi Limit File),
        //   --> diventa un LFNP
        //
        // Il commento atteso e' "LFNP:" (non essendoci nessun commento nell'ordine iniziale)
         
        CSStrategy strategy = (CSStrategy)context.getBean("csStrategy");
        String lfnpPrefix = strategy.getLimitFileNoPriceCommentPrefix();
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrderOfType("BB0000000101", Side.Buy, 3, OrdType.Limit, TimeInForce.GoodTillCancel, 104.0, null);

            // riceve execreport iniziale (New)
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);
            
            // wait, so it can go in OrderNotExecutableState
            Thread.sleep(2000);
            sendOrderCancelRequest(clOrdID, "BB0000000101", Side.Buy);

            // riceve execreport Cancelled
            String expectedErrorMsg = Messages.getString("REVOKE_ACKNOWLEDGED");
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Canceled, null, null, null, lfnpPrefix);
            
            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            LimitFileNoPriceState.class.getSimpleName(),
                            OrderRevocatedState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }    
    
    @Test (timeout=20000)
    public void test_Revoke_TerminalState_Rejected()  throws Exception {
        // L'ordine viene eseguito, quindi va in uno stato terminale.
        // La richiesta di revoca viene rifiutata (OrderCancelRequestReject)
        //
        // Riutilizzo il test BBG_StatesTest.testBB0000000002, a cui aggiungo la revoca
        // 
         
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000002", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "BLTD", "RDEU", null);

            sendOrderCancelRequest(clOrdID, "BB0000000002", Side.Buy);
            String expectedErrorMsg = Messages.getString("REVOKE_NOT_ACKNOWLEDGED");
            waitAndVerifyOrderCancelReject(5, expectedErrorMsg);
            
            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            BBG_StartExecutionState.class.getSimpleName(),
                            BBG_SendRfqState.class.getSimpleName(),
                            BBG_ReceiveQuoteState.class.getSimpleName(),
                            BBG_AcceptQuoteState.class.getSimpleName(),
                            BBG_ExecutedState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }        

}
