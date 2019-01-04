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
package it.softsolutions.bestx.finders;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Holiday;

import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
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
 * Creation date: 14/nov/2012 
 * 
 **/
public class HolidayFinderTest {
    private static HolidayFinder holidayFinder;
    private static ClassPathXmlApplicationContext context;

    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        holidayFinder = (HolidayFinder) context.getBean("dbBasedHolidayFinder");
    }

    @Test
    public void isHolidayForCurrencyNull() throws BestXException {
        boolean res = holidayFinder.isAnHoliday(null, null);
        assertFalse(res);
        
        res = holidayFinder.isAnHoliday("EUR", null);
        assertFalse(res);
    }

    @Test
    public void isHolidayForCurrencyInvalid() throws BestXException, ParseException {
        String currency = "#INVALID#";
        boolean res = holidayFinder.isAnHoliday(currency, DateUtils.parseDate("20121225", new String[]{ "yyyyMMdd" })); 
        assertFalse(res);
    }
    
    /*
    @Test
    public void isHolidayForCurrencyValid() throws BestXException, ParseException {
        String currency = "EUR";
        boolean res = holidayFinder.isAnHoliday(currency, DateUtils.parseDate("20120604", new String[]{ "yyyyMMdd" })); 
        assertTrue(res);
        
        res = holidayFinder.isAnHoliday(currency, DateUtils.parseDate("20121228", new String[]{ "yyyyMMdd" })); 
        assertFalse(res);
    }
    */
    @Test
    public void getFilteredHolidaysNull() throws BestXException {
        List<Holiday> holidays = holidayFinder.getFilteredHolidays(null, null);
        System.out.println(holidays);
        assertNotNull(holidays);
        
        holidays = holidayFinder.getFilteredHolidays("EUR", null);
        assertNotNull(holidays);
    }

    @Test
    public void getFilteredHolidaysInvalid() throws BestXException, ParseException {
        String currency = "#INVALID#";
        String countryCode = "IT";
        List<Holiday> holidays = holidayFinder.getFilteredHolidays(currency, countryCode); 
        System.out.println(holidays);
    }
    
    @Test
    public void getFilteredHolidaysValid() throws BestXException, ParseException {
        String currency = "EUR";
        String countryCode = "IT";
        List<Holiday> holidays = holidayFinder.getFilteredHolidays(currency, countryCode);
        
        System.out.println(holidays);
        for (Holiday holiday : holidays) {
            System.out.println(holiday);
        }
        
    }

    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
    }
    
}
