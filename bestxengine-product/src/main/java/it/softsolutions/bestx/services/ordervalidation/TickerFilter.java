/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.CustomerManagerDAO;
import it.softsolutions.bestx.model.Order;

public class TickerFilter implements OrderValidator
{
   private CustomerManagerDAO customerManagerDAO;
   
   @Override
   public OrderResult validateOrder(Operation operation, Order order)
   {
      OrderResultBean result = new OrderResultBean();
      result.setOrder(order);
      result.setReason("");  
      
      if (customerManagerDAO.isTheTickerNotAllowedForTheCustomer(order))
      {
         result.setReason(Messages.getString("TickerFilterNotPassed.0", order.getInstrument().getRTFITicker(), order.getCustomer().getName())); 
         result.setValid(false);
      } 
      else 
      {
         result.setValid(true);
      }
      
      return result;
   }

   @Override
   public boolean isDbNeeded()
   {
      return true;
   }

   @Override
   public boolean isInstrumentDbCheck()
   {
      return false;
   }

   public void setCustomerManagerDAO(CustomerManagerDAO customerManagerDAO)
   {
      this.customerManagerDAO = customerManagerDAO;
   }

   public CustomerManagerDAO getCustomerManagerDAO()
   {
      return customerManagerDAO;
   }
}
