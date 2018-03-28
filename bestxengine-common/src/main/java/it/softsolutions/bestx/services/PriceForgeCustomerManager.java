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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.dao.CustomerManagerDAO;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Order;

/**
 * This class manages the customers that can or cannot access the PriceForge
 * @author ruggero.rizzo
 *
 */
public class PriceForgeCustomerManager
{
   private CustomerManagerDAO customerManagerDAO;
   
   public void init()
   {
      checkPrerequisites();
   }

   private void checkPrerequisites()
   {
      if (customerManagerDAO == null)
         throw new ObjectNotInitializedException("SqlCustomerManagerDAO not set");
   }

   /**
    * If the customer IS a Price Forge allowed one, than IT IS NOT an unwanted one,
    * if the customer IS NOT a PRice FOrge allowed one, than IT IS an unwanted one 
    * @param order
    * @return
    */
   public boolean isUnwanted(Order order)
   {
      return !customerManagerDAO.isAPriceForgeCustomer(order);
   }

   public void setCustomerManagerDAO(CustomerManagerDAO customerManagerDAO)
   {
      this.customerManagerDAO = customerManagerDAO;
   }
   
}
