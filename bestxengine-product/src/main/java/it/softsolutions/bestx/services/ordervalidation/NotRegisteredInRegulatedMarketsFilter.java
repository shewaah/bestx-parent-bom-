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
 * Date         : $Date: 2010-09-07 09:36:01 $
 * Header       : $Id: NotRegisteredInRegulatedMarketsFilter.java,v 1.1 2010-09-07 09:36:01 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/services/ordervalidation/NotRegisteredInRegulatedMarketsFilter.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public class NotRegisteredInRegulatedMarketsFilter implements OrderValidator
{
   private final RegulatedMktIsinsLoader regulatedMarketIsinsLoader;

   public NotRegisteredInRegulatedMarketsFilter(RegulatedMktIsinsLoader regulatedMarketIsinsLoader)
   {
      this.regulatedMarketIsinsLoader = regulatedMarketIsinsLoader;
   }

   public boolean isDbNeeded()
   {
      return true;
   }

   public boolean isInstrumentDbCheck()
   {
      return false;
   }

   public OrderResult validateOrder(Operation operation, Order order)
   {
      OrderResultBean result = new OrderResultBean();
      result.setOrder(order);
      result.setReason("");
      if(order.getSide().compareTo(OrderSide.BUY) == 0)
      {
         result.setValid(regulatedMarketIsinsLoader.isInstrumentInAutomaticCurandoMarketsList(order.getInstrument().getIsin()));
      }
      else
      {
         result.setValid(true);
      }
      
      if(!result.isValid()) {
         result.setReason(Messages.getString("NotRegisteredInRegulatedMarketsFilter.0"));
      }
      return result;
   }
}
