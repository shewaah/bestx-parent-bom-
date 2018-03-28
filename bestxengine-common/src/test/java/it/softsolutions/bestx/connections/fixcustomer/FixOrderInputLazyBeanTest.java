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
package it.softsolutions.bestx.connections.fixcustomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: ruggero.rizzo 
* Creation date: 22/mag/2012 
* 
**/
public class FixOrderInputLazyBeanTest
{
   private FixOrderInputLazyBean fixInLazyBean;
   private XT2Msg msg;
   private String dateFormat;
   private String dateTimeFormat;
   private Date expectedDate;
   private SimpleDateFormat modifiedDateTimeFormatter;
   private String dateWithMillisNewFormat = "20120511-12:47:28.941";
   @Before
   public void setUp() throws Exception
   {
      msg = new XT2Msg();
      
      modifiedDateTimeFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
      modifiedDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
      expectedDate = modifiedDateTimeFormatter.parse(dateWithMillisNewFormat);
      dateFormat = "yyyyMMdd";
      msg.setValue(FixMessageFields.FIX_TransactTime, dateWithMillisNewFormat);
   }

   @Test
   public void testGetTransactTime_withMillis_traditionalDateFormatter()
   {
      dateTimeFormat = "yyyyMMddHHmmss";
      fixInLazyBean = new FixOrderInputLazyBean(msg, dateFormat, dateTimeFormat);
      Date transactTime = fixInLazyBean.getTransactTime();
      
      assertNotSame(expectedDate, transactTime);
   }
   
   @Test
   public void testGetTransactTime_withMillis_modifiedDateFormatter()
   {
      dateTimeFormat = "yyyyMMdd-HH:mm:ss.SSS";
      fixInLazyBean = new FixOrderInputLazyBean(msg, dateFormat, dateTimeFormat);
      Date transactTime = fixInLazyBean.getTransactTime();
      assertEquals(expectedDate, transactTime);
   }
   
   @Test(expected = IllegalArgumentException.class)
   public void constructorNull() 
   {
      new FixOrderInputLazyBean(null, null, null);
   }
   
}
