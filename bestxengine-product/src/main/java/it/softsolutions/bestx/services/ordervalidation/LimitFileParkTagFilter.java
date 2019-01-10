/*
* Copyright 1997-2018 SoftSolutions! srl 
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

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 12 lug 2018 
* 
**/
public class LimitFileParkTagFilter implements OrderValidator {

   public static String TO_PARK_VAL = "Order will be parked.";
   private String custOrderHandlingInstrValue;
   

   @Override
   public OrderResult validateOrder(Operation operation, Order order) {
      OrderResultBean result = new OrderResultBean();
      result.setOrder(order);
      result.setValid(true);
      
      if (order.isLimitFile() &&  this.custOrderHandlingInstrValue != null && 
            order.getCustOrderHandlingInstr() != null &&
            this.custOrderHandlingInstrValue.equalsIgnoreCase(order.getCustOrderHandlingInstr())) {
         
         //Order is always valid but if TMO force go in ParkedOrderState
         result.setReason(TO_PARK_VAL);    
      }
      return result;
   }

   @Override
   public boolean isDbNeeded() {
      return false;
   }

   @Override
   public boolean isInstrumentDbCheck() {
      return false;
   }

   
   /**
    * @param custOrderHandlingInstrValue the custOrderHandlingInstrValue to set
    */
   public void setCustOrderHandlingInstrValue(String custOrderHandlingInstrValue) {
      this.custOrderHandlingInstrValue = custOrderHandlingInstrValue;
   }

}
