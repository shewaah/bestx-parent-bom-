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
package it.softsolutions.bestx.services.ordervalidation;

import static org.junit.Assert.assertTrue;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.dao.SettlementLimitDao;
import it.softsolutions.bestx.finders.db.SetHolidayFinder;
import it.softsolutions.bestx.model.Country;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.financecalc.SettlementDateCalculator;
import it.softsolutions.bestx.services.ordervalidation.utils.FakeHolidaysDao;

import java.text.ParseException;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * 
 * Purpose: this class is the Unit Test of the class it.softsolutions.bestx.services.ordervalidation.SettlementDateLimitControl
 * 
 * Project Name : bestxengine-cs First created by: stefano.pontillo Creation date: 28/mag/2012
 * 
 **/
public class SettlementDateLimitControlTest {
    private SettlementDateLimitControl settlementDateLimitControlTest;

    @Before
    public void setUp() throws Exception {
        // Messages singleton initialization
        Messages messages = new Messages();
        messages.setBundleName("messages");
        messages.setLanguage("it");
        messages.setCountry("IT");

        SettlementLimitDao settlementLimitDao = new FakeSettlementLimitDao();
        SetHolidayFinder settlementHolidayFinder = new SetHolidayFinder();
        SettlementDateCalculator settlementDateCalculator = new SettlementDateCalculator();

        settlementHolidayFinder.setHolidayDao(new FakeHolidaysDao());
        settlementHolidayFinder.init();

        settlementDateLimitControlTest = new SettlementDateLimitControl();
        settlementDateLimitControlTest.setAllCountryCode("ALL");
        settlementDateLimitControlTest.setAllCurrencyCode("ALL");
        settlementDateLimitControlTest.setSettlementDateCalculator(settlementDateCalculator);
        settlementDateLimitControlTest.setSettlementHolidayFinder(settlementHolidayFinder);
        settlementDateLimitControlTest.setSettlementLimitDao(settlementLimitDao);
    }

    @After
    public void tearDown() throws Exception {
        settlementDateLimitControlTest = null;
    }

    @Test
    public void testValidateOrderNullInstrument() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Order order = new Order();
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("15/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(null);

        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderNullCountry() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Instrument intrument = new Instrument();
        intrument.setCountry(null);

        Order order = new Order();
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("15/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        // [RR20121023] Behaviour changed following the implementation of the jira request BXCRESUI-24 :
        // a null country leads to a Supranational instrument
        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderNullCurrency() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("IT");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("15/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency(null);
        order.setInstrument(intrument);

        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderLowerFiveDays() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("US");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 3 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("15/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("USD");
        order.setInstrument(intrument);

        // Five days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderFiveDays() throws ParseException {
        // This test doesn't have holidays between dates
        Country country = new Country();
        country.setCode("US");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 5 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("19/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("USD");
        order.setInstrument(intrument);

        // Five days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderGreaterFiveDays() throws ParseException {
        // This test doesn't have holidays between dates
        Country country = new Country();
        country.setCode("US");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 6 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("20/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("USD");
        order.setInstrument(intrument);

        // Five days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderLowerThreeDays() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("IT");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 3 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("15/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Three days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderGreaterThreeDays() throws ParseException {
        // This test doesn't have holidays between dates
        Country country = new Country();
        country.setCode("IT");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 4 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("18/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Three days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderLowerFourDays() throws ParseException {
        // This test doesn't have holidays between dates
        Country country = new Country();
        country.setCode("ES");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 4 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("18/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Four days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderGreaterFourDays() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("ES");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 5 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("19/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Four days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderLowerSixDays() throws ParseException {
        // This test doesn't have holidays between dates
        Country country = new Country();
        country.setCode("FR");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 6 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("20/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Six days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderGreaterSixDays() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("FR");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 7 days settlement
        order.setTransactTime(DateUtils.parseDate("12/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("21/06/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Six days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderHoliday() throws ParseException {
        // This test doesn't have holidays or week-end between dates
        Country country = new Country();
        country.setCode("IT");

        Instrument intrument = new Instrument();
        intrument.setCountry(country);

        Order order = new Order();
        // 3 days settlement
        order.setTransactTime(DateUtils.parseDate("13/08/2012", new String[] { "dd/MM/yyyy" }));
        order.setFutSettDate(DateUtils.parseDate("17/08/2012", new String[] { "dd/MM/yyyy" }));
        order.setCurrency("EUR");
        order.setInstrument(intrument);

        // Three days settlement limit
        OrderResult orderResult = settlementDateLimitControlTest.validateOrder(null, order);

        assertTrue(orderResult.isValid());
    }
}
