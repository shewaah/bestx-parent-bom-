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
package it.softsolutions.bestx.services.grdlite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.customservice.CustomServiceException;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: davide.rossoni Creation date: 13/feb/2013
 * 
 **/
public class GRDLiteServiceTest {

    private static CustomService grdLiteService;
    private static ClassPathXmlApplicationContext context;

    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        grdLiteService = (CustomService) context.getBean("grdLiteService");

        // context.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void sendRequestNullOperation() throws CustomServiceException {
        grdLiteService.sendRequest(null, false, "securityId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sendRequestNullSecurityID() throws CustomServiceException {
        grdLiteService.sendRequest("OP#12345", false, null);
    }
    
    /*
    @Test
    public void sendRequestValid() throws GRDLiteException {
        grdLiteService.sendRequest("OP#12345", false, "IT0123456789");
    }
    */
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();
        Thread.sleep(1000);
    }
    
}
