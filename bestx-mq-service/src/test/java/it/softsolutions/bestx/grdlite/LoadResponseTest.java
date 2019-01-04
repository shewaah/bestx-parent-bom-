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
package it.softsolutions.bestx.grdlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.softsolutions.bestx.grdlite.LoadResponse.Status;

import org.junit.Test;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-mq-service 
 * First created by: davide.rossoni 
 * Creation date: 31/gen/2013 
 * 
 **/
public class LoadResponseTest {

    @Test
    public void fromXml() throws IllegalArgumentException, Exception {
        String xmlMessage = "<ns0:LoadResponse xmlns:ns0='BestX'  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'  xsi:schemaLocation='BestX file:/H:/Trash/XSD/BestX%20%20Instrument%20Request%20Schema.xsd'><Instrument ns0:SecurityTypeCd='ISIN'  SecurityId='IT0001976403'  Status='Error' /></ns0:LoadResponse>";
        
        LoadResponse loadResponse = LoadResponse.fromXml(xmlMessage);
        assertNotNull(loadResponse);
        assertEquals("IT0001976403", loadResponse.getSecurityId());
        assertEquals(SecurityType.ISIN, loadResponse.getSecurityType());
        assertEquals(Status.Error, loadResponse.getStatus());
    }
    
    @Test
    public void toXml() throws IllegalArgumentException, Exception {
        LoadResponse loadResponse = new LoadResponse(SecurityType.ISIN, Status.Error, "IT0001976403");
        
        String xmlMessage = loadResponse.toXml();
        assertNotNull(xmlMessage);
    }
    
    @Test
    public void constructor() throws IllegalArgumentException, Exception {
        constructor(SecurityType.ISIN, LoadResponse.Status.Error, "IT0001976401");
        constructor(SecurityType.ISIN, LoadResponse.Status.Received, "IT0001976402");
        constructor(SecurityType.CUSIP, LoadResponse.Status.Received, "IT0001976403");
        constructor(SecurityType.VALOR, LoadResponse.Status.Received, "IT0001976401");
    }
    
    private void constructor(SecurityType securityType, LoadResponse.Status status, String securityID) {
        LoadResponse loadResponse = new LoadResponse(securityType, status, securityID);
        assertNotNull(loadResponse);
        assertEquals(securityID, loadResponse.getSecurityId());
        assertEquals(securityType, loadResponse.getSecurityType());
        assertEquals(status, loadResponse.getStatus());
    }

}
