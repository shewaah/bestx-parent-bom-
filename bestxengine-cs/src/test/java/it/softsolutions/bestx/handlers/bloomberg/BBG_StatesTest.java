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
 *  RTFI/-/TEST23       | 106.02000 x 500000.00000  | ASK | INDICATIVE
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

package it.softsolutions.bestx.handlers.bloomberg;

import org.junit.Test;

import it.softsolutions.bestx.fix.field.OrdStatus;
import it.softsolutions.bestx.fix.field.Side;
import it.softsolutions.bestx.handlers.OrderStatesTestHelper;
import it.softsolutions.bestx.states.BusinessValidationState;
import it.softsolutions.bestx.states.FormalValidationOKState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.OrderReceivedState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.SendExecutionReportState;
import it.softsolutions.bestx.states.StateExecuted;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_AcceptQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_ExecutedState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_AcceptBestIfStillValidState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_AcceptInternalAndRejectBestState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_RejectInternalAndAcceptBestState;
import it.softsolutions.bestx.states.bloomberg.BBG_INT_SendInternalRfqState;
import it.softsolutions.bestx.states.bloomberg.BBG_ReceiveQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectQuoteState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;
import it.softsolutions.bestx.states.bloomberg.BBG_StartExecutionState;

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
public class BBG_StatesTest  extends OrderStatesTestHelper{


    @Test (timeout=20000) // 20 secs max
    public void testBB0000000001()  throws Exception {
        // Il dealer BLOOM rifiuta la QuoteRequest.
        // Si invia (Un)ExecutionReport/Rejected, perche' il motivo del rifiuto 
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000001", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Rejected, null, null, null, null);

            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            BBG_StartExecutionState.class.getSimpleName(),
                            BBG_SendRfqState.class.getSimpleName(),
                            BBG_RejectedState.class.getSimpleName(),
                            // second price discovery
                            SendAutoNotExecutionReportState.class.getSimpleName(),
                            OrderNotExecutedState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=20000) // 20 secs max
    public void testBB0000000014()  throws Exception {
        // Il dealer BLOOM rifiuta la QuoteRequest.
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000014", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 106.01, "RTSL", "TEST24", null);

            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            BBG_StartExecutionState.class.getSimpleName(),
                            BBG_SendRfqState.class.getSimpleName(),
                            BBG_RejectedState.class.getSimpleName(),
                            RejectedState.class.getSimpleName(),
                            // second price discovery
                            WaitingPriceState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    

    @Test (timeout=20000) // 20 secs max
    public void testBB0000000002()  throws Exception {
        // Il dealer BLOOM manda una quota valida, il client la accetta --> chiusura su BLOOM
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000002", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "BLTD", "RDEU", null);

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

    @Test (timeout=30000) // 30 secs max (test deals with a timeout)
    public void testBB0000000003()  throws Exception {
        // Il dealer BLOOM non risponde entro due-in (dealer response time)
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000003", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(20, clOrdID, OrdStatus.Filled, 106.01, "RTSL", "TEST24", null);

            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            BBG_StartExecutionState.class.getSimpleName(),
                            BBG_SendRfqState.class.getSimpleName(),
                            BBG_RejectedState.class.getSimpleName(),
                            RejectedState.class.getSimpleName(),
                            // second price discovery
                            WaitingPriceState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }

    @Test (timeout=20000)
    public void testBB0000000004()  throws Exception {
        // Il dealer BLOOM risponde con una qty non accettabile, bestx rifiuta la quote, il dealer conferma con QuoteStatus/TradeEnded --> timeout 30 secs
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi

        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000004", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 106.01, "RTSL", "TEST24", null);

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
                            BBG_RejectQuoteState.class.getSimpleName(),
                            RejectedState.class.getSimpleName(),
                            // second price discovery
                            WaitingPriceState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=30000) // 30 secs max (test deals with a timeout)
    public void testBB0000000005()  throws Exception {
        // Il dealer BLOOM non risponde all'accettazione del client entro il Bloomberg.Tsox.due-in-time
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi

        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000005", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            // wait for order processing to be completed
            Thread.sleep(10000);

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
                            WarningState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=20000) // 20 secs max
    public void testBB0000000006()  throws Exception {
        // Il MM interno RTFI rifiuta l'rfq, si chiude positivamente con il best externo BLOOM a 105.88

        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000006", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "BLTD", "RDEU", null);

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
                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
                            // reject from internal MM, accept external quote
                            BBG_INT_AcceptBestIfStillValidState.class.getSimpleName(),
                            BBG_AcceptQuoteState.class.getSimpleName(),
                            BBG_ExecutedState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
            
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=2000000) // 20 secs max
    public void testBB0000000007()  throws Exception {
        // Il MM interno RTFI risponde all'rfq confermando il prezzo, poi rifiuta l'hit/lift, 
        // quindi si chiude sulla best BLOOM

        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000007", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "BLTD", "RDEU", null);

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
                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
                            BBG_INT_AcceptInternalAndRejectBestState.class.getSimpleName(),
                            // internal MM rejects hit/lift, accept external quote
                            BBG_INT_AcceptBestIfStillValidState.class.getSimpleName(),
                            BBG_AcceptQuoteState.class.getSimpleName(),
                            BBG_ExecutedState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
            
        }
        finally {
            cleanDb(clOrdID);
        }
    }    
    
    @Test (timeout=20000) // 20 secs max
    public void testBB0000000008()  throws Exception {
        // Il MM interno RTFI risponde all'rfq con prezzo molto peggiorativo, si chiude positivamente 
        // con il best esterno su BLOOM dopo reject all'interno 
         
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000008", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "BLTD", "RDEU", null);

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
                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
                            // reject internal MM's quote, accept external
                            BBG_INT_RejectInternalAndAcceptBestState.class.getSimpleName(),
                            BBG_INT_AcceptBestIfStillValidState.class.getSimpleName(),
                            BBG_AcceptQuoteState.class.getSimpleName(),
                            BBG_ExecutedState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
            
        }
        finally {
            cleanDb(clOrdID);
        }
    }    
    
    @Test (timeout=20000) // 20 secs max
    public void testBB0000000009()  throws Exception {
        // Il MM interno risponde all'rfq confermando il prezzo, si chiude positivamente 
        // con l'interno RTFI e mando reject all'esterno --> chiusura su RTFI 
         
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000009", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Filled, 105.88, "RTSL", "TEST23", null);

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
                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
                            BBG_INT_AcceptInternalAndRejectBestState.class.getSimpleName(),
                            BBG_ExecutedState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
            
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
    @Test (timeout=20000) // 20 secs max (timeout for execution report must be set to 5 secs in BestX.Properties: Market.state.timeout = 5000)
    public void testBB0000000010()  throws Exception {
        // Il MM interno risponde all'rfq confermando il prezzo, timeout 
        // sull'attesa dell'execution report --> warning 
         
        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000010", Side.Buy, 3);

            // riceve solo execreport iniziale (New)
            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            // wait for exec report timeout to occur
            // Thread.sleep(> 20000) causes weird problems
            Thread.sleep(10000);
            
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
                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
                            BBG_INT_AcceptInternalAndRejectBestState.class.getSimpleName(),
                            WarningState.class.getSimpleName());
            
        }
        finally {
            cleanDb(clOrdID);
        }
    }            
    
    
// this test cannot be executed, as the tsox simulator cannot support two rfqs for the same orderid    
    @Test (timeout=30000) // 30 secs max (timeout)
    public void testBB0000000011()  throws Exception {
//        // Il MM interno risponde all'rfq confermando il prezzo, si chiude positivamente 
//        // con l'interno RTFI e mando reject all'esterno --> chiusura su RTFI 
//         
//        String clOrdID = "";
//        try {
//            clOrdID = sendNewOrder("BB0000000011", Side.Buy, 3);
//
//            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null);
//
//            waitAndVerifyExecutionReport(20, clOrdID, OrdStatus.Filled, 105.88, "RTSL", "TEST23");
//
//            verifyOrderStates(clOrdID, 
//                            /*InitialState.class.getSimpleName(), not saved to db */
//                            OrderReceivedState.class.getSimpleName(),
//                            FormalValidationOKState.class.getSimpleName(),
//                            BusinessValidationState.class.getSimpleName(),
//                            // first price discovery
//                            WaitingPriceState.class.getSimpleName(),
//                            BBG_StartExecutionState.class.getSimpleName(),
//                            BBG_SendRfqState.class.getSimpleName(),
//                            BBG_ReceiveQuoteState.class.getSimpleName(),
//                            BBG_INT_SendInternalRfqState.class.getSimpleName(),
//                            BBG_INT_AcceptInternalAndRejectBestState.class.getSimpleName(),
//                            BBG_ExecutedState.class.getSimpleName(),
//                            SendExecutionReportState.class.getSimpleName(),
//                            StateExecuted.class.getSimpleName());
//            
//        }
//        finally {
//            cleanDb(clOrdID);
//        }
    }    
    
    @Test (timeout=45000)
    public void testBB0000000013()  throws Exception {
        // Simile a testBB0000000004, ma dopo il QuoteResponse/Pass di BestX il dealer non invia QuoteStatus/TradeEnded --> timeout 30 secs
        // RTFI TS24 risponde a nuova rfq con prezzo valido --> chiusura su Rtfi

        String clOrdID = "";
        try {
            clOrdID = sendNewOrder("BB0000000013", Side.Buy, 3);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(40, clOrdID, OrdStatus.Filled, 106.01, "RTSL", "TEST24", null);

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
                            BBG_RejectQuoteState.class.getSimpleName(),
                            RejectedState.class.getSimpleName(),
                            // second price discovery
                            WaitingPriceState.class.getSimpleName(),
                            SendExecutionReportState.class.getSimpleName(),
                            StateExecuted.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }
    
}
