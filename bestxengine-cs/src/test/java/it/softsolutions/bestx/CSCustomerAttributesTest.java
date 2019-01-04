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
package it.softsolutions.bestx;

import it.softsolutions.bestx.model.CustomerAttributesIFC;

import java.util.Calendar;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: davide.rossoni 
 * Creation date: May 29, 2012 
 * 
 **/
public class CSCustomerAttributesTest {

    @Test
    public void testDate() {
        Calendar cleanCal = Calendar.getInstance();
        cleanCal.set(2012, 11, 2, 0, 0, 0);

        Calendar dirtyCal;
        dirtyCal = Calendar.getInstance();
        dirtyCal.add(Calendar.DAY_OF_YEAR, 3);

        System.out.println("clean settl: " + cleanCal.getTime());
        System.out.println("dirty settl: " + dirtyCal.getTime());

        System.out.println("old equal: " + (dirtyCal.getTime()).compareTo(cleanCal.getTime()) );        
        System.out.println("new equal: " + DateUtils.isSameDay(dirtyCal.getTime(), cleanCal.getTime()) );        

        int pippo = 0;
    }

    
    
	@Test(expected = ClassCastException.class)
	public void setAttributeNotBoolean() {
		CustomerAttributesIFC customerAttributes = new CustomerAttributes();
		customerAttributes.setAttribute(CustomerAttributes.AttributeName.amountCommissionWanted.toString(), "WhatAFuck!!!");
	}

}
