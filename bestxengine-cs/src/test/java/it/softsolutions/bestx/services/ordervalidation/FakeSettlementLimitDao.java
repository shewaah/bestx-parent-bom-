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

import it.softsolutions.bestx.dao.SettlementLimitDao;
import it.softsolutions.bestx.model.SettlementLimit;

/** 

*
* Purpose: this class is mainly for ... 
*
* Project Name : bestxengine-cs
* First created by: stefano.pontillo
* Creation date: 28/mag/2012
*
**/
public class FakeSettlementLimitDao implements SettlementLimitDao
{
   /* (non-Javadoc)
    * @see it.softsolutions.bestx.dao.SettlementLimitDao#getValidFilteredLimit(java.lang.String, java.lang.String)
    */
   @Override
   public SettlementLimit getValidFilteredLimit(String currencyCode, String countryCode)
   {
      SettlementLimit settlementLimit = null;
      
      //Simulation of behavior based on given currency and country codes
      if (countryCode == null){
          //country code null, supranational, country accepted 'ALL'
          settlementLimit = new SettlementLimit();
          settlementLimit.setCurrencyCode(currencyCode);
          settlementLimit.setCountryCode("ALL");
          settlementLimit.setEnabled(true);
          settlementLimit.setLimitDays(3);
      }else if (currencyCode.equalsIgnoreCase("EUR") && countryCode.equalsIgnoreCase("IT")) {
         settlementLimit = new SettlementLimit();
         settlementLimit.setCurrencyCode("EUR");
         settlementLimit.setCountryCode("IT");
         settlementLimit.setEnabled(true);
         settlementLimit.setLimitDays(3);
      } else if (currencyCode.equalsIgnoreCase("EUR") && countryCode.equalsIgnoreCase("ALL")) {
         settlementLimit = new SettlementLimit();
         settlementLimit.setCurrencyCode("EUR");
         settlementLimit.setCountryCode("ALL");
         settlementLimit.setEnabled(true);
         settlementLimit.setLimitDays(4);
      } else if (currencyCode.equalsIgnoreCase("ALL") && countryCode.equalsIgnoreCase("ALL")) {
         settlementLimit = new SettlementLimit();
         settlementLimit.setCurrencyCode("ALL");
         settlementLimit.setCountryCode("ALL");
         settlementLimit.setEnabled(true);
         settlementLimit.setLimitDays(5);
      } else if (currencyCode.equalsIgnoreCase("EUR") && countryCode.equalsIgnoreCase("FR")) {
         settlementLimit = new SettlementLimit();
         settlementLimit.setCurrencyCode("EUR");
         settlementLimit.setCountryCode("FR");
         settlementLimit.setEnabled(true);
         settlementLimit.setLimitDays(6);
      }
      
      return settlementLimit;
   }
}
