/*
* Copyright 1997-2019 SoftSolutions! srl 
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

package it.softsolutions.bestx.services;

import it.softsolutions.bestx.services.DateService;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: gabriele.ghidoni 
* Creation date: 9 set 2019 
* 
**/
public class DateServiceTest {

   @Test
   public void test() throws Exception {
      Date date = UtcDateOnlyConverter.convert("20190909");
      System.out.println(date);
      Date time = UtcTimeOnlyConverter.convert("00:00:00.000");
      System.out.println(time);
      Date timestamp = DateService.convertUTCToLocal(date, time);
      System.out.println(timestamp);

      Date today = DateService.newLocalDate();
      System.out.println(DateUtils.isSameDay(timestamp, today));
   }

   @Test
   public void test2() throws Exception {
      Date date = UtcDateOnlyConverter.convert("20190909");
      System.out.println(date);
      Date timestamp = DateService.convertUTCToLocal(date, null);
      System.out.println(timestamp);

      Date today = DateService.newLocalDate();
      System.out.println(DateUtils.isSameDay(timestamp, today));
   }

}
