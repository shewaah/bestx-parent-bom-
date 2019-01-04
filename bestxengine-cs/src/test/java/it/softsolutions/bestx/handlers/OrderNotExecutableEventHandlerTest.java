/*
* Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Order.OrderType;
import it.softsolutions.bestx.model.Order.TimeInForce;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: ruggero.rizzo 
 * Creation date: 24/gen/2014 
 * 
 **/
public class OrderNotExecutableEventHandlerTest {
    private static long OVERTHRESHOLD_TIMER = 5000;
    private static long WITHINTHRESHOLD_TIMER = 1000;
    private static int GENERAL_TIMER = 40000;
    private Order order;
    private Customer customer;
    private CustomerAttributes customerAttributes;
    private CSOrderNotExecutableEventHandler orderNotExecutableEventHandler;
    
    @Before
    public void setUp() throws Exception {
        order = new Order();
        order.setType(OrderType.LIMIT);
        order.setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        order.setLimit(new Money("EUR", new BigDecimal(100.0)));
        order.setBestPriceDeviationFromLimit(5.0);
        order.setFixOrderId("TEST_ORDER");
        customer = new Customer();
        customerAttributes = new CustomerAttributes();
    }
    
    @Test
    public void testGetTimerIntervalDeltaWithinThreshold() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(10.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(WITHINTHRESHOLD_TIMER, timerInterval);
    }


    @Test
    public void testGetTimerIntervalDeltaOverThreshold() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(3.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(OVERTHRESHOLD_TIMER, timerInterval);
    }

    @Test
    public void testGetTimerIntervalDeviationNull() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(3.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        order.setBestPriceDeviationFromLimit(null);
        orderNotExecutableEventHandler.getTimerInterval(order, customer);
    }
    
    @Test
    public void testGetTimerIntervalMaximumDeviationDisabled() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(0.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(WITHINTHRESHOLD_TIMER, timerInterval);
    }
    
    @Test
    public void testGetTimerIntervalLimitPriceNotExistent() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        Order order = new Order();
        order.setBestPriceDeviationFromLimit(5.0);
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(0.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(WITHINTHRESHOLD_TIMER, timerInterval);
    }
    
    @Test
    public void testGetTimerIntervalMaxDeviationNotConfigured() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        CustomerAttributes customerAttributes = new CustomerAttributes();
        customerAttributes.setLimitPriceOffMarket(null);
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(GENERAL_TIMER, timerInterval);
    }

  
    @Test
    public void testGetTimerIntervalMaxDeviationNegative() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        CustomerAttributes customerAttributes = new CustomerAttributes();
        customerAttributes.setLimitPriceOffMarket(new BigDecimal(-5.0));
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(GENERAL_TIMER, timerInterval);
    }
    
    @Test
    public void testGetTimerIntervalCustomerAttributesNotAvailable() {
        orderNotExecutableEventHandler = new CSOrderNotExecutableEventHandler(null, null, GENERAL_TIMER, null, null, null);
        
        CustomerAttributes customerAttributes = null;
        customer.setCustomerAttributes(customerAttributes);
        order.setCustomer(customer);
        long timerInterval = orderNotExecutableEventHandler.getTimerInterval(order, customer);
        assertEquals(GENERAL_TIMER, timerInterval);
    }
}