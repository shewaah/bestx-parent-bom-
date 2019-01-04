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
 * -------->>>>>>>>                                                        <<<<<<<<--------                                                         
 * -------->>>>>>>> Configurazione richiesta: come per BBG_StatesTest.java <<<<<<<<--------
 * -------->>>>>>>>                                                        <<<<<<<<--------
 * 
 * 2)
 * Book per gli ordini Limit File NoPrice(ovvere con OrdType = Limit e TimeInForce=GTC): è il book di default per i test, senza internalizzazione,
 *                                       ma con le qty=0 (per cui tutti i prezzio vengono scartati e il book rimane vuoto
 * Si ottiene per l'isin BB0000000101
 *   
 *  BLOOMBERG/-/RDEU    | 105.6     x 0  | ASK | INDICATIVE
 *  BLOOMBERG/-/TEST21  | 105.7     x 0  | ASK | INDICATIVE
 *  RTFI/-/TEST24       | 106.01000 x 0  | ASK | INDICATIVE
 *  RTFI/-/TEST21       | 106.02000 x 0  | ASK | INDICATIVE
 *  RTFI/-/TEST25       | 106.03000 x 0  | ASK | INDICATIVE
 *  
 * Si ottiene per l'isin BB0000000100
 *
 *  BLOOMBERG/-/RDEU    | 105.6     x 100000        | ASK | INDICATIVE
 *  BLOOMBERG/-/TEST21  | 105.7     x 0             | ASK | INDICATIVE
 *  RTFI/-/TEST24       | 106.01000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST21       | 106.02000 x 500000.00000  | ASK | INDICATIVE
 *  RTFI/-/TEST25       | 106.03000 x 500000.00000  | ASK | INDICATIVE

 *   

 * 
 */

package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.fix.field.OrdStatus;
import it.softsolutions.bestx.fix.field.OrdType;
import it.softsolutions.bestx.fix.field.Side;
import it.softsolutions.bestx.fix.field.TimeInForce;
import it.softsolutions.bestx.states.BusinessValidationState;
import it.softsolutions.bestx.states.FormalValidationOKState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.OrderReceivedState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;

import org.junit.Test;


/**  
 *
 * Purpose: this class is mainly for testing of TSOX flows, including Internalization on RTFI
 *
 * - setup required for tests:
 *   - the test launches a Fix Client which sends orders, so a OMS2FixGateway must be up&running
 *   - OMS2FixGateway sends orders to a running session of BestX
 *   - BestX uses BLPConnectorFake and RTFIBSConnectorFake to receive books, and sends QuoteRequest/NewOrderSingle to TSOX, so
 *   - TradeStac bloomberg must be running, connected to TSOXSimulator
 *   - TSOXSimulator must be up&running
 *   
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 5-mar-2013 
 * 
 **/
public class LimitFiles_StatesTest extends OrderStatesTestHelper {
    // FIX configuration for connection to Xt2FixGateway


    
    @Test //(timeout=20000) // 20 secs max
    public void testEmptyBookWithLimitFileOrder()  throws Exception {
        // Il book è vuoto (BB0000000101), ma l'ordine è di tipo LimitFile: viene accettato, e posto in stato LimitFileNoPriceState  
        //   OrdType = Limit
        //   TimeInForce = GoodTillCancel
        //   Price = 107.0
        System.out.println("Calling test");
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrderOfType("BB0000000101", Side.Buy, 3, OrdType.Limit, TimeInForce.GoodTillCancel, 107.0, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);
            
            Thread.sleep(5);

            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            LimitFileNoPriceState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }

    @Test //(timeout=20000) // 20 secs max
    public void testEmptyBookWithNoLimitFileOrder()  throws Exception {
        // Il book è vuoto (BB0000000101), l'ordine non è di tipo LimitFile: viene rifiutato dal BookDepthClassifier 
        //   OrdType = Limit
        //   TimeInForce = Day  <--
        //   Price = 107.0
        System.out.println("Calling test");
        
        String clOrdID = "";
        try {
            clOrdID = sendNewOrderOfType("BB0000000101", Side.Buy, 3, OrdType.Limit, TimeInForce.Day, 107.0, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.New, null, null, null, null);

            waitAndVerifyExecutionReport(5, clOrdID, OrdStatus.Rejected, null, null, null, null);

            verifyOrderStates(clOrdID, 
                            /*InitialState.class.getSimpleName(), not saved to db */
                            OrderReceivedState.class.getSimpleName(),
                            FormalValidationOKState.class.getSimpleName(),
                            BusinessValidationState.class.getSimpleName(),
                            // first price discovery
                            WaitingPriceState.class.getSimpleName(),
                            SendAutoNotExecutionReportState.class.getSimpleName(),
                            OrderNotExecutedState.class.getSimpleName());
        }
        finally {
            cleanDb(clOrdID);
        }
    }    
}
