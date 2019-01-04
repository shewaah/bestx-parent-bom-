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
public class LoadRequestTest {
    
    @Test
    public void fromXml() throws IllegalArgumentException, Exception {
        String xmlMessage = "<?xml version='1.0' ?><ns0:LoadRequest xmlns:ns0='BestX'  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='grdLiteMessageSchema.xsd' ns0:InitialLoadTypeCd='true'><Instrument ns0:SecurityTypeCd='ISIN' SecurityId='IT0001976403'/></ns0:LoadRequest>";
        
        LoadRequest loadRequest = LoadRequest.fromXml(xmlMessage);
        assertNotNull(loadRequest);
        assertEquals("IT0001976403", loadRequest.getSecurityIDs()[0]);
        assertEquals(SecurityType.ISIN, loadRequest.getSecurityType());
        assertEquals(true, loadRequest.isInitialLoad());
    }
    
    @Test
    public void toXml() throws IllegalArgumentException, Exception {
        LoadRequest loadRequest = new LoadRequest(SecurityType.ISIN, true, "IT0001976403");
        
        String xmlMessage = loadRequest.toXml();
        assertNotNull(xmlMessage);
    }

    @Test
    public void constructor() throws IllegalArgumentException, Exception {
        constructor(SecurityType.ISIN, true, "IT0001976401");
        constructor(SecurityType.ISIN, false, "IT0001976402");
        constructor(SecurityType.CUSIP, true, "IT0001976403");
        constructor(SecurityType.VALOR, true, "IT0001976401", "IT0001976402", "IT0001976403");
    }
    
    @Test
    public void constructorMessageSchema() throws IllegalArgumentException, Exception {
        new LoadRequest("grdLiteMessageSchema.xsd", SecurityType.ISIN, true, "IT0001976401");
    }
    
    private void constructor(SecurityType securityType, boolean initialLoad, String... securityIDs) {
        LoadRequest loadRequest = new LoadRequest(securityType, initialLoad, securityIDs);
        assertNotNull(loadRequest);
        assertEquals(securityIDs.length, loadRequest.getSecurityIDs().length);
        assertEquals(securityIDs[0], loadRequest.getSecurityIDs()[0]);
        assertEquals(securityType, loadRequest.getSecurityType());
        assertEquals(initialLoad, loadRequest.isInitialLoad());
    }
}
