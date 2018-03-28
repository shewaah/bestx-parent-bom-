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
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-09-17 10:36:36 $
 * Header       : $Id: FilterOnPortfolioBasis.java,v 1.4 2010-09-17 10:36:36 anna.cochetti Exp $
 * Revision     : $Revision: 1.4 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/services/ordervalidation/FilterOnPortfolioBasis.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anna.cochetti
 *
 * Implements a validator which returns always a valid OrderResult if the ordered instrument is in a portfolio listed in portfolioList
 * Otherwise returns the OrderValidator result
 */
public class FilterOnPortfolioBasis implements OrderValidator
{
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterOnPortfolioBasis.class);
   private final OrderValidator validator;
   public static String INTERNAL_PRODUCT_STR = "@RODOTTO_INTERNO@";

   public FilterOnPortfolioBasis(List<String> portfolioList, OrderValidator validator)
   {
      super();
      this.portfolioList = portfolioList;
      this.validator = validator;
   }

   protected List<String> portfolioList;

   public OrderResult validateOrder(Operation unused, Order order)
   {
   	OrderResultBean result = new OrderResultBean();
   	result.setOrder(order);
   	result.setValid(false);
   	result.setReason("");		
   
       // 20090616 AMC On the portfolios in the list this filter returns always true
      if(portfolioList != null) {
         for(String currentPortfolio : portfolioList) {
            if(order.getInstrument().getInstrumentAttributes().getPortfolio().getDescription().equalsIgnoreCase(currentPortfolio))
            {
               result.setValid(true);
               result.setReason(INTERNAL_PRODUCT_STR);
               LOGGER.info("Valid because it is an internal product.");
               return result;
            }
         }
      }
      
      // 20090616 AMC Otherwise return the delegated ordervalidator result
      return validator.validateOrder(unused, order);

   }

   public boolean isDbNeeded()
   {
      return this.validator.isDbNeeded();
   }

   public boolean isInstrumentDbCheck()
   {
      return this.validator.isInstrumentDbCheck();
   }      
}