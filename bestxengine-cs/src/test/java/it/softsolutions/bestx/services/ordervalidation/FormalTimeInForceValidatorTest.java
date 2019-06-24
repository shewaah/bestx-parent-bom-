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
package it.softsolutions.bestx.services.ordervalidation;

import static org.junit.Assert.assertTrue;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Order.OrderType;
import it.softsolutions.bestx.model.Order.TimeInForce;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: ruggero.rizzo 
 * Creation date: 06/nov/2013 
 * 
 **/
public class FormalTimeInForceValidatorTest {
    private static FormalTimeInForceValidator formalTimeInForceValidator;
    private static ClassPathXmlApplicationContext context;
    
    
    
    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        formalTimeInForceValidator = new FormalTimeInForceValidator();
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
    }
    
    @Test
    public void testValidateOrderTimeInForceNotExpectedValue() {
        Order order = new Order();
        order.setTimeInForce(TimeInForce.AT_THE_CLOSE);
        order.setType(OrderType.LIMIT);
        
        OrderResult orderResult = formalTimeInForceValidator.validateOrder(null, order);
        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderGoodTillCancelOrderNotLimit() {
        Order order = new Order();
        order.setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        order.setType(OrderType.MARKET);
        
        OrderResult orderResult = formalTimeInForceValidator.validateOrder(null, order);
        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderGoodTillCancelOrderLimitNotValid() { 
        Order order = new Order();
        order.setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        order.setType(OrderType.LIMIT);
        Money limitPrice = new Money("EUR", BigDecimal.ZERO);
        order.setLimit(limitPrice);
        
        OrderResult orderResult = formalTimeInForceValidator.validateOrder(null, order);
        assertTrue(!orderResult.isValid());
    }

    @Test
    public void testValidateOrderGoodTillCancelOrderLimitValid() {
        Order order = new Order();
        order.setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        order.setType(OrderType.LIMIT);
        Money limitPrice = new Money("EUR", BigDecimal.valueOf(100.2));
        order.setLimit(limitPrice);
        
        OrderResult orderResult = formalTimeInForceValidator.validateOrder(null, order);
        assertTrue(orderResult.isValid());
    }

    @Test
    public void testValidateOrderDayOrSession() {
        Order order = new Order();
        order.setTimeInForce(TimeInForce.DAY_OR_SESSION);
        order.setType(OrderType.LIMIT);
        Money limitPrice = new Money("EUR", BigDecimal.valueOf(100.2));
        order.setLimit(limitPrice);
        
        OrderResult orderResult = formalTimeInForceValidator.validateOrder(null, order);
        assertTrue(orderResult.isValid());
    }
}
