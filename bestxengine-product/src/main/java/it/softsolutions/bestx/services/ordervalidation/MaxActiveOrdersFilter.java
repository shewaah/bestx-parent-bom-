/*
* Copyright 1997-2017 SoftSolutions! srl 
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.OrderCounter;


/**  
*
* Purpose: check the number of currently active orders and a threshold of max active orders.
* Reject the order if cross the threshold.
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 27 set 2017 
* 
**/
public class MaxActiveOrdersFilter implements OrderValidator {
   private static final Logger LOGGER = LoggerFactory.getLogger(MaxActiveOrdersFilter  .class);
   private OrderCounter activeOrdersDao;
   private int orderThreshold;
   
   
   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#validateOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.Order)
    */
   @Override
   public OrderResult validateOrder(Operation operation, Order order) {
      OrderResultBean result = new OrderResultBean();
      result.setOrder(order);
      result.setReason("");
      int actualActiveOrders = activeOrdersDao.countActiveOrders(DateService.newLocalDateMidnight());
      
      if (orderThreshold > 0 && actualActiveOrders > orderThreshold) {
         LOGGER.info("Order {} rejected because number of actual active orders exceeded max oactive order threshold {}", order.getFixOrderId(), orderThreshold);
         result.setReason(Messages.getString("MaxActiveOrdersThresholdExceeded.0", orderThreshold));
      } else {
         result.setValid(true);
      }
      return result;
   }
   
   /**
    * @param activeOrdersDao the activeOrdersDao to set
    */
   public void setActiveOrdersDao(OrderCounter activeOrdersDao) {
      this.activeOrdersDao = activeOrdersDao;
   }
   
   /**
    * @param orderThreshold the orderThreshold to set
    */
   public void setOrderThreshold(int orderThreshold) {
      this.orderThreshold = orderThreshold;
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isDbNeeded()
    */
   @Override
   public boolean isDbNeeded() {
      return false;
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isInstrumentDbCheck()
    */
   @Override
   public boolean isInstrumentDbCheck() {
      return false;
   }

}
