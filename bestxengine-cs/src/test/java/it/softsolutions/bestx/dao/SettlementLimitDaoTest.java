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
package it.softsolutions.bestx.dao;

import static org.junit.Assert.assertNull;
import it.softsolutions.bestx.model.SettlementLimit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: davide.rossoni 
 * Creation date: 28/nov/2012 
 * 
 **/
public class SettlementLimitDaoTest {

    private static SettlementLimitDao settlementLimitDao;
    private static ClassPathXmlApplicationContext context;

    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        settlementLimitDao = (SettlementLimitDao) context.getBean("hibernateSettlementLimitDao");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValidFilteredLimitNullCurrencyCode() {
        settlementLimitDao.getValidFilteredLimit(null, "IT");
    }
    
    @Test
    public void getValidFilteredLimitInvalid() {
        SettlementLimit settlementLimit = settlementLimitDao.getValidFilteredLimit("#INVALID#", "#INVALID#");
        assertNull(settlementLimit);
    }

    @Test
    public void getValidFilteredLimitNullContryCode() {
        SettlementLimit settlementLimit = settlementLimitDao.getValidFilteredLimit("ALL", null);
        assertNull(settlementLimit);
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
    }
    
    /*
    @Test
    public void getValidFilteredLimitValid() {
        String currencyCode = "EUR";
        String countryCode = "AD";
        SettlementLimit settlementLimit = settlementLimitDao.getValidFilteredLimit(currencyCode, countryCode);
        assertNotNull(settlementLimit);
        assertEquals(currencyCode, settlementLimit.getCurrencyCode());
        assertEquals(countryCode, settlementLimit.getCountryCode());
        assertEquals(3, settlementLimit.getLimitDays());
    }
    */
}
