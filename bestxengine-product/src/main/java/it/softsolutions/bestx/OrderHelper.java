/*
* Copyright 1997-2019 SoftSolutions! srl 
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
 
package it.softsolutions.bestx;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.bestexec.MarketOrderBuilder.BuilderType;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.PriceController;

/**  
*
* Purpose: this class collects stateless methods handling order 
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 04 apr 2019 
* 
**/
public class OrderHelper {

   private static final Logger LOGGER = LoggerFactory.getLogger(OrderHelper.class);

   public static void setOrderBestPriceDeviationFromLimit(Operation operation) {
      if (operation != null && operation.getLastAttempt() != null 
    		  && operation.getLastAttempt().getSortedBook() != null) {
         Order order = operation.getOrder();
         if (order.getLimit() != null) {
            Customer customer = order.getCustomer();
            SortedBook sortedBook = operation.getLastAttempt().getSortedBook();
            List<ClassifiedProposal> bookProposals = sortedBook.getSideProposals(order.getSide());

            BigDecimal limitPrice = order.getLimit().getAmount();
            LOGGER.debug("Order {} limit price {}, starting calculating delta from limit monitor price.", order.getFixOrderId(), limitPrice.doubleValue());
            try {
            	if (operation.getLastAttempt().getMarketOrder() != null && operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice() != null) {
            		order.setBestPriceDeviationFromLimit(
            				Math.abs(operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice().getAmount().doubleValue() - order.getLimit().getAmount().doubleValue()));
            	} else if (operation.getLastAttempt().getMarketOrder() != null && 
            	      operation.getLastAttempt().getMarketOrder().getBuilder() != null &&
            			operation.getLastAttempt().getMarketOrder().getBuilder().getType() != BuilderType.CUSTOM && 
            			operation.getLastAttempt().getExecutionProposal() == null) {
            		order.setBestPriceDeviationFromLimit(
            		   PriceController.INSTANCE.getBestProposalDelta(
            				   limitPrice.doubleValue() > 0.0 ? limitPrice : BigDecimal.ZERO, bookProposals, customer));
            	} else {
            		LOGGER.error("limitMonitorPrice value not available for order {}, using default", order.getFixOrderId());
            		order.setBestPriceDeviationFromLimit(null);
            	}
            } catch (BestXException e) {
               order.setBestPriceDeviationFromLimit(null);
            }
         }
      }
   }
}
