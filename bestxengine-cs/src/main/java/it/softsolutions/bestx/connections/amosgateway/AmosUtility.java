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

package it.softsolutions.bestx.connections.amosgateway;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Order;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class AmosUtility {
   private static final Logger LOGGER = LoggerFactory.getLogger(AmosUtility.class);
   private String orderIdPrefix;
   private Map<String, String> portfolioToAkrosInternalMM;
   static AmosUtility instance;
   private static String DEFAULT_PORTFOLIO = "0";

   /**
    * Inits the.
    */
   public void init()
   {
      if (instance == null)
      {
         instance = new AmosUtility();
      }
   }

   /**
    * Gets the single instance of AmosUtility.
    *
    * @return single instance of AmosUtility
    */
   public static AmosUtility getInstance(){
      if (instance == null)
      {
         instance = new AmosUtility();
      }
      return instance;
   }

   /**
    * Gets the order id prefix.
    *
    * @return the order id prefix
    */
   public String getOrderIdPrefix() {
      return orderIdPrefix;
   }

   /**
    * Sets the order id prefix.
    *
    * @param orderIdPref the new order id prefix
    */
   public void setOrderIdPrefix(String orderIdPref) {
      getInstance().orderIdPrefix = orderIdPref;
   }

   /**
    * Wrap string.
    *
    * @param originalStr the original str
    * @return the string
    */
   public String wrapString(String originalStr) {	
      if (originalStr == null)
      {
         return "";
      }
      return orderIdPrefix + originalStr;
   }

   /**
    * Unwrap string.
    *
    * @param originalStr the original str
    * @return the string
    */
   public String unwrapString(String originalStr) {
      if (originalStr == null)
      {
         return "";
      }
      if (originalStr.startsWith(orderIdPrefix))
      {
         return originalStr.substring(orderIdPrefix.length());
      }
      else
      {
         return originalStr;
      }
   }

   /**
    * Gets the internal mm.
    *
    * @param portfolioId the portfolio id
    * @return the internal mm
    */
   public String getInternalMM(int portfolioId){
      Integer portId = new Integer(portfolioId);
      String value = portfolioToAkrosInternalMM.get(portId.toString());
      if (value != null)
      {
         return value.split(",")[0];
      }
      else
      {
         return null;
      }
   }

   /**
    * Gets the sinfo code.
    *
    * @param portfolioId the portfolio id
    * @return the sinfo code
    */
   public String getSinfoCode(int portfolioId){
      Integer portId = new Integer(portfolioId);

      String value = portfolioToAkrosInternalMM.get(portId.toString());
      if (value != null)
      {
         return value.split(",")[1];
      }
      else
      {
         LOGGER.warn("No sinfocode found for the portfolio {}, extracting the sinfocode for the default portfolio with id {}", portfolioId, DEFAULT_PORTFOLIO);
         value = portfolioToAkrosInternalMM.get(DEFAULT_PORTFOLIO);
         return value;
      }
   }

   /**
    * Sets the portfolio to akros internal mm.
    *
    * @param portfolioToAkrosInternalMM the portfolio to akros internal mm
    */
   public void setPortfolioToAkrosInternalMM(
         Map<String, String> portfolioToAkrosInternalMM) {
      getInstance().portfolioToAkrosInternalMM = portfolioToAkrosInternalMM;
   }

   /**
    * Checks if is an amos order.
    *
    * @param order the order
    * @return true, if is an amos order
    */
   public boolean isAnAmosOrder(Order order)
   {
      String orderId = order.getFixOrderId();
      if (orderId != null && orderId.startsWith(getOrderIdPrefix()))
      {
         return true;
      }
      return false;
   }
}
