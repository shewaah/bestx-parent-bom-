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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.SettlementLimitDao;
import it.softsolutions.bestx.finders.db.SetHolidayFinder;
import it.softsolutions.bestx.model.Holiday;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.SettlementLimit;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.financecalc.SettlementDateCalculator;


/** 

*
* Purpose: this class is mainly for ... 
*
* Project Name : bestxengine-product
* First created by: stefano.pontillo
* Creation date: 28/mag/2012
*
**/
public class SettlementDateLimitControl implements OrderValidator
{
   private static final Logger LOGGER = LoggerFactory.getLogger(SettlementDateLimitControl.class);
   
   private SettlementLimitDao settlementLimitDao;
   private SetHolidayFinder settlementHolidayFinder;
   private SettlementDateCalculator settlementDateCalculator;
   
   private String allCountryCode;
   private String allCurrencyCode;

   public OrderResult validateOrder(Operation operation, Order order)
   {
      OrderResultBean result = new OrderResultBean();
      result.setOrder(order);
      result.setValid(true);
      result.setReason("");
      
      if (order.getInstrument() != null && order.getCurrency() != null) {
         String currencyCode = order.getCurrency();
         
         String countryCode = null;
         if (order.getInstrument().getCountry() != null) {
            countryCode = order.getInstrument().getCountry().getCode();
         }
         else {
            LOGGER.info("Order [{}] has a null country, assuming it is a Supranational", order.getFixOrderId());
         }
         
         Date orderDate = order.getTransactTime();
         if (orderDate == null) {
            orderDate = DateService.newLocalDate();
         }
         DateUtils.truncate(orderDate, Calendar.DAY_OF_MONTH);
            
         
         Date orderSettlementDate = order.getFutSettDate();
         
         List<Holiday> holidays = settlementHolidayFinder.getFilteredHolidays(currencyCode, countryCode);
         int days = settlementDateCalculator.getCalculatedSettlementDays(orderDate, orderSettlementDate, holidays);
         
         
         SettlementLimit settlementLimit = settlementLimitDao.getValidFilteredLimit(currencyCode, countryCode);
         if (settlementLimit == null || settlementLimit.getCurrencyCode() == null) {
            LOGGER.debug("settlement limit not found for currency {} and country {}. Try with country ALL", currencyCode, countryCode);
            settlementLimit = settlementLimitDao.getValidFilteredLimit(currencyCode, allCountryCode);
         }
         if (settlementLimit == null || settlementLimit.getCurrencyCode() == null) {
            LOGGER.debug("settlement limit not found for currency {} and country {}. Try with currency ALL", currencyCode, allCountryCode);
            
            settlementLimit = settlementLimitDao.getValidFilteredLimit(allCurrencyCode, countryCode);
         }
         if (settlementLimit == null || settlementLimit.getCurrencyCode() == null) {
            LOGGER.debug("settlement limit not found for currency {} and country {}. Try with currency ALL and country ALL", currencyCode, allCountryCode);
            
            settlementLimit = settlementLimitDao.getValidFilteredLimit(allCurrencyCode, allCountryCode);
         }
         
         if (settlementLimit != null && settlementLimit.getCurrencyCode() != null) {
            if (days > settlementLimit.getLimitDays()) {
               result.setValid(false);
               result.setReason(Messages.getString("SettlementDateLimit.0"));
            }
         } else {
            result.setValid(false);
            result.setReason(Messages.getString("SettlementDateLimit.1"));
         }
      } else {
         result.setValid(false);
         if (order.getInstrument() == null) {
             result.setReason(Messages.getString("SettlementDateLimit.2"));
         }
         else  {
             result.setReason(Messages.getString("SettlementDateLimit.3"));
         }
         
      }
   
      return result;
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isDbNeeded()
    */
   @Override
   public boolean isDbNeeded()
   {
      return false;
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isInstrumentDbCheck()
    */
   @Override
   public boolean isInstrumentDbCheck()
   {
      return false;
   }

   /**
    * Set the SettlementLimitDao to manage SettlementLimits table
    * 
    * @param settlementLimitDao the settlementLimitDao to set
    */
   public void setSettlementLimitDao(SettlementLimitDao settlementLimitDao)
   {
      this.settlementLimitDao = settlementLimitDao;
   }

   /**
    * Set the SetHolidayFinder to manage Holidays table
    * 
    * @param settlementHolidayFinder the settlementHolidayFinder to set
    */
   public void setSettlementHolidayFinder(SetHolidayFinder settlementHolidayFinder)
   {
      this.settlementHolidayFinder = settlementHolidayFinder;
   }

   /**
    * Set the calculator class to manage settlement dates
    * 
    * @param settlementDateCalculator the settlementDateCalculator to set
    */
   public void setSettlementDateCalculator(SettlementDateCalculator settlementDateCalculator)
   {
      this.settlementDateCalculator = settlementDateCalculator;
   }

   /**
    * Set the code for ALL countries
    * 
    * @param allCountryCode the allCountryCode to set
    */
   public void setAllCountryCode(String allCountryCode)
   {
      this.allCountryCode = allCountryCode;
   }

   /**
    * Set the code for ALL currency
    * 
    * @param allCurrencyCode the allCurrencyCode to set
    */
   public void setAllCurrencyCode(String allCurrencyCode)
   {
      this.allCurrencyCode = allCurrencyCode;
   }
}
