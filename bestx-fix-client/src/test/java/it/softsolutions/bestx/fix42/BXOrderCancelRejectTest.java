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
package it.softsolutions.bestx.fix42;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
import it.softsolutions.bestx.fix.field.CxlRejResponseTo;

import java.io.IOException;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;
import quickfix.fix42.OrderCancelReject;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 12, 2012 
 * 
 **/
public class BXOrderCancelRejectTest {
    
    
//    @Test
    public void fromFix() throws ConfigError, InvalidMessage, FieldNotFound {
        String messageData = "8=FIX.4.29=17135=934=449=SOFT152=20120612-13:33:39.77256=OMS111=10945260447312337=133950803068039=441=10945260447312358=Cancellation rejected: status cannot be cancelled434=110=056";
        DataDictionary dataDictionary = new DataDictionary("FIX42_BestX_CS.xml");
        
        OrderCancelReject message = new OrderCancelReject();
        message.fromString(messageData, dataDictionary, true);
        
        BXOrderCancelReject bxOrderCancelReject = BXOrderCancelReject.fromFIX42Message(message);
        
        System.out.println(bxOrderCancelReject);
        
        assertNotNull(bxOrderCancelReject);
        assertEquals(CxlRejResponseTo.OrderCancelRequest, bxOrderCancelReject.getCxlRejResponseTo());
    }

    public static void main(String[] args) throws IOException {

    }
}
