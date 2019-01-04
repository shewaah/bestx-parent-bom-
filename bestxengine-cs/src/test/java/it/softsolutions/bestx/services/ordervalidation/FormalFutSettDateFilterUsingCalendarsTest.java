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

import static org.junit.Assert.assertEquals;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
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
 * Purpose: this class is mainly for testing of FormalFutSettDateFilterUsingCalendars
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 28/mag/2012 
 * 
 **/
public class FormalFutSettDateFilterUsingCalendarsTest {
	private FormalFutSettDateFilterUsingCalendars formalFutSettDateFilterUsingCalendars;
	private SetHolidayFinder settlementHolidayFinder;
	private Messages messages;

	@Before
	public void setUp() throws Exception {
		messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");

		formalFutSettDateFilterUsingCalendars = new FormalFutSettDateFilterUsingCalendars();

		settlementHolidayFinder = new SetHolidayFinder();
		settlementHolidayFinder.setHolidayDao(new FakeHolidaysDao());
		settlementHolidayFinder.init();
		
		formalFutSettDateFilterUsingCalendars.setHolidayFinder(settlementHolidayFinder);
		formalFutSettDateFilterUsingCalendars.setSettlementDateCalculator(new SettlementDateCalculator());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test (expected=ObjectNotInitializedException.class)
	public void test_NullOrder_ExceptionExpected() {
		Operation op = null;
		Order order = null;

		formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
	}

	@Test (expected=ObjectNotInitializedException.class)
	public void test_NullInstrument_ExceptionExpected() {
		Operation op = null;
		Order order = new Order();

		formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
	}

	@Test
	public void test_SettlementDateAlreadySet() throws ParseException {
		Operation op = null;
		Order order = new Order();
		order.setFutSettDate(DateUtils.parseDate("15/08/2012", new String[]{ "dd/MM/yyyy" }));

		OrderResult result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect valid order, unchanged futSettDate
		assertEquals(result.isValid(), true);
		assertEquals(order.getFutSettDate(), DateUtils.parseDate("15/08/2012", new String[]{ "dd/MM/yyyy" }));
	}

	@Test
	public void test_SettlementDateNotSet_MissingStdSettlementDays() throws ParseException {
		Operation op = null;
		Order order = new Order();
		Instrument instrument = new Instrument();
		instrument.setCurrency("EUR");
		instrument.setIsin("IT0000000001");
		Country country = new Country();
		country.setCode("IT");
		instrument.setCountry(country);
		order.setInstrument(instrument);

		OrderResult result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect invalid order, reject message
		assertEquals(result.isValid(), false);
		assertEquals(result.getReason(), Messages.getString("FormalFutSettDate.1"));
	}
	
	@Test
	public void test_SettlementDateNotSet_InvalidStdSettlementDays() throws ParseException {
		Operation op = null;
		Order order = new Order();
		Instrument instrument = new Instrument();
		instrument.setCurrency("EUR");
		instrument.setIsin("IT0000000001");
		Country country = new Country();
		country.setCode("IT");
		instrument.setCountry(country);
		instrument.setStdSettlementDays(-1);
		order.setInstrument(instrument);

		OrderResult result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect invalid order, reject message
		assertEquals(result.isValid(), false);
		assertEquals(result.getReason(), Messages.getString("FormalFutSettDate.1"));
	}

	@Test
	public void test_SettlementDateNotSet_ValidStdSettlementDays() throws ParseException {
		Operation op = null;
		Order order = new Order();
		order.setTransactTime(DateUtils.parseDate("14/08/2012", new String[]{ "dd/MM/yyyy" }));	// tuesday agu 14, 2012 
		
		Instrument instrument = new Instrument();
		instrument.setCurrency("EUR");
		instrument.setIsin("IT0000000001");
		Country country = new Country();
		country.setCode("IT");
		instrument.setCountry(country);
		instrument.setStdSettlementDays(0);
		order.setInstrument(instrument);

		// zero days, same date expected (tuesday 14th)
		OrderResult result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect valid order + settlement date
		assertEquals(result.isValid(), true);
		assertEquals(DateUtils.parseDate("14/08/2012", new String[]{ "dd/MM/yyyy" }), order.getFutSettDate());

		// one day, thursday expected (as aug 15th is holiday)
		order.setFutSettDate(null);
		order.getInstrument().setStdSettlementDays(1);
		result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect valid order + settlement date
		assertEquals(result.isValid(), true);
		assertEquals(DateUtils.parseDate("16/08/2012", new String[]{ "dd/MM/yyyy" }), order.getFutSettDate());

		// two days, friday expected (as aug 15th is holiday)
		order.setFutSettDate(null);
		order.getInstrument().setStdSettlementDays(2);
		result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect valid order + settlement date
		assertEquals(result.isValid(), true);
		assertEquals(DateUtils.parseDate("17/08/2012", new String[]{ "dd/MM/yyyy" }), order.getFutSettDate());

		// three days, monday expected (as aug 15th is holiday + sat/sun )
		order.setFutSettDate(null);
		order.getInstrument().setStdSettlementDays(3);
		result = formalFutSettDateFilterUsingCalendars.validateOrder(op, order);
		// expect valid order + settlement date
		assertEquals(result.isValid(), true);
		assertEquals(DateUtils.parseDate("20/08/2012", new String[]{ "dd/MM/yyyy" }), order.getFutSettDate());
	}
}
