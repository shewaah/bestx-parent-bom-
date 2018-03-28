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
package it.softsolutions.bestx.services.serial;

import static org.junit.Assert.assertEquals;
import it.softsolutions.bestx.BestXException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: Jun 28, 2012 
 * 
 **/
public class SerialNumberServiceTest {

    
    private static SerialNumberService serialNumberService;
    
    @BeforeClass
    public static void setUp() throws Exception {
        // Initialize finders using Spring
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

        serialNumberService = (SerialNumberService) context.getBean("serialNumberService");
        

    }
    
    @Test(expected = IllegalArgumentException.class)
    public void uniqueIdentifierNullIdentifier() throws BestXException {
        serialNumberService.getUniqueIdentifier(null, "P%013d");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void uniqueIdentifierNullPattern() throws BestXException {
        serialNumberService.getUniqueIdentifier("MOT_SESSION_ID", null);
    }
    
    @Test
    public void uniqueIdentifierValid() throws BestXException {
        assertEquals(14, serialNumberService.getUniqueIdentifier("MOT_SESSION_ID", "P%013d").length());
    }
    
}
