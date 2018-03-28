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
package it.softsolutions.bestx.services.financecalc;

import static org.junit.Assert.assertEquals;
import it.softsolutions.bestx.finders.db.SetHolidayFinder;
import it.softsolutions.bestx.model.Holiday;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** 

*
* Purpose: this class is mainly for ... 
*
* Project Name : bestxengine-product
* First created by: stefano.pontillo
* Creation date: 24/mag/2012
*
**/
public class SettlementDateCalculatorTest
{
   private SimpleDateFormat simpleDateFormat;
   private SimpleDateFormat fullDateFormat;
   private SettlementDateCalculator settlementDateCalculator;
   private SetHolidayFinder settlementHolidayFinder = null;
   private List<Holiday> holidays = null;

   @Before
   public void setUp() throws Exception
   {
      settlementHolidayFinder = new SetHolidayFinder();
      settlementHolidayFinder.setHolidayDao(new FakeHolidaysDao());
      simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
      fullDateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
      settlementDateCalculator = new SettlementDateCalculator();
   }

   @After
   public void tearDown() throws Exception
   {
      settlementHolidayFinder = null;
   }

   @Test
   public void getCalculatedSettlementDateNullHolidays() throws ParseException
   {
      //Test from monday to Thursday without holidays
      Date calculatedSettlementDate = settlementDateCalculator.getCalculatedSettlementDate(3, null, simpleDateFormat.parse("11/06/2012"));
      assertEquals(simpleDateFormat.parse("14/06/2012").getTime(), calculatedSettlementDate.getTime());
   }

   @Test
   public void getCalculatedSettlementDateEmptyHolidays() throws ParseException
   {
      //Test from monday to Thursday with empty holidays
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, new ArrayList(), simpleDateFormat.parse("11/06/2012"));
      assertEquals(simpleDateFormat.parse("14/06/2012").getTime(), date.getTime());
   }

   @Test
   public void getCalculatedSettlementDateWithoutHolidays() throws ParseException
   {
      //Test from monday to Thursday without holidays in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, holidays, simpleDateFormat.parse("11/06/2012"));
      assertEquals(simpleDateFormat.parse("14/06/2012").getTime(), date.getTime());
   }

   @Test
   public void getCalculatedSettlementDateWeekEnd() throws ParseException
   {
      //Test from friday to wednesday without holidays in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, holidays, simpleDateFormat.parse("08/06/2012"));
      assertEquals(simpleDateFormat.parse("13/06/2012").getTime(), date.getTime());
   }

//   @Test
   public void getCalculatedSettlementDateWithHoliday() throws ParseException
   {
      //Test from monday to friday with one holiday in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, holidays, simpleDateFormat.parse("13/08/2012"));
      assertEquals(simpleDateFormat.parse("17/08/2012").getTime(), date.getTime());
   }

//   @Test
   public void getCalculatedSettlementDateWithHolidayWeekEnd() throws ParseException
   {
      //Test from Tuesday to monday with one holiday and week-end in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, holidays, simpleDateFormat.parse("14/08/2012"));
      assertEquals(simpleDateFormat.parse("20/08/2012").getTime(), date.getTime());
   }

//   @Test
   public void getCalculatedSettlementDateWithTwoHolidayWeekEnd() throws ParseException
   {
      //Test from monday to monday with two holidays and week-end in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(3, holidays, simpleDateFormat.parse("24/12/2012"));
      assertEquals(simpleDateFormat.parse("31/12/2012").getTime(), date.getTime());
   }

   @Test
   public void getCalculatedSettlementDateSevenDays() throws ParseException
   {
      //Test from monday to monday without holidays, with a week-end in the period
      Date date = settlementDateCalculator.getCalculatedSettlementDate(7, holidays, simpleDateFormat.parse("25/06/2012"));
      assertEquals(simpleDateFormat.parse("04/07/2012").getTime(), date.getTime());
   }

   @Test
   public void getCalculatedSettlementDateNullStart()
   {
      //Test with null startDate 
      Assert.assertNull(settlementDateCalculator.getCalculatedSettlementDate(-1, holidays, null));
   }

   @Test
   public void getCalculatedSettlementDaysNullHolidays() throws ParseException
   {
      //Test from monday to Thursday with null holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("11/06/2012"), simpleDateFormat.parse("14/06/2012"), null);
      assertEquals(3, days);
   }

   @Test
   public void getCalculatedSettlementDaysEmptyHolidays() throws ParseException
   {
      //Test from monday to Thursday with empty holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("11/06/2012"), simpleDateFormat.parse("14/06/2012"), new ArrayList());
      assertEquals(3, days);
   }

   @Test
   public void getCalculatedSettlementDaysThreeDays() throws ParseException
   {
      //Test from monday to Thursday without holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("11/06/2012"), simpleDateFormat.parse("14/06/2012"), holidays);
      assertEquals(3, days);
   }

   @Test
   public void getCalculatedSettlementDaysWeekEnd() throws ParseException
   {
      //Test from friday to wednesday without holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("15/06/2012"), simpleDateFormat.parse("20/06/2012"), holidays);
      assertEquals(3, days);
   }

   @Test
   public void getCalculatedSettlementDaysWeekEndWithHourMinSec() throws ParseException
   {
      //from [Thu Dec 06 11:15:00 CET 2012] to [Tue Dec 11 03:00:00 CET 2012] returned 4 days --> wrong, should be 3!

      //Test from friday to wednesday without holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(fullDateFormat.parse("06/12/2012 - 11:15:00"), fullDateFormat.parse("11/12/2012 - 03:00:00"), holidays);
      assertEquals(3, days);
   }
   
//   @Test
   public void getCalculatedSettlementDaysHoliday() throws ParseException
   {
      //Test from monday to friday with holiday
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("13/08/2012"), simpleDateFormat.parse("17/08/2012"), holidays);
      assertEquals(3, days);
   }

//   @Test
   public void getCalculatedSettlementDaysHolidaysWeekEnd() throws ParseException
   {
      //Test from monday to monday with 2 holidays and week end
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("24/12/2012"), simpleDateFormat.parse("31/12/2012"), holidays);
      assertEquals(3, days);
   }

   @Test
   public void getCalculatedSettlementDaysSevenDays() throws ParseException
   {
      //Test from friday to wednesday without holidays
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("25/06/2012"), simpleDateFormat.parse("04/07/2012"), holidays);
      assertEquals(7, days);
   }

   @Test
   public void getCalculatedSettlementDaysNullStart() throws ParseException
   {
      //Test with null startDate
      int days = settlementDateCalculator.getCalculatedSettlementDays(null, simpleDateFormat.parse("20/06/2012"), holidays);
      assertEquals(-1, days);
   }

   @Test
   public void getCalculatedSettlementDaysNullEnd() throws ParseException
   {
      //Test with null startDate
      int days = settlementDateCalculator.getCalculatedSettlementDays(simpleDateFormat.parse("20/06/2012"), null, holidays);
      assertEquals(-1, days);
   }
}
