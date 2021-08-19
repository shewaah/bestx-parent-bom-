/*
* Copyright 1997-2021 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.jsscommon.Money;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 27 lug 2021 
* 
**/
public class BestXMarketOrderBuilder extends MarketOrderBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(BestXMarketOrderBuilder.class);

   private TargetPriceCalculator targetPriceCalculator;
   
   @Override
   public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) {
      MarketOrder marketOrder = null;
      Attempt currentAttempt = operation.getLastAttempt();
      if (currentAttempt.getExecutionProposal() != null) {
         marketOrder = new MarketOrder();
         marketOrder.setValues(operation.getOrder());
         marketOrder.setTransactTime(DateService.newUTCDate());
         marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());

         marketOrder.setMarketMarketMaker(currentAttempt.getExecutionProposal().getMarketMarketMaker());
         Money limitPrice = this.targetPriceCalculator.calculateTargetPrice(operation);
         marketOrder.setLimit(limitPrice);
         String cleanMarketName = marketOrder.getMarket().getName().indexOf("_HIST") >= 0 ? 
        		 			marketOrder.getMarket().getName().substring(0, marketOrder.getMarket().getName().indexOf("_HIST")):
        		 				marketOrder.getMarket().getName();
         LOGGER.info("Order={}, Selecting for execution market: {}, and price {}", 
        		 operation.getOrder().getFixOrderId(), 
        		 cleanMarketName, limitPrice == null? "null":limitPrice.getAmount().toString());
         marketOrder.setVenue(currentAttempt.getExecutionProposal().getVenue());
         marketOrder.setBuilder(this);
      }
      listener.onMarketOrderBuilt(this, marketOrder);
   }

	public TargetPriceCalculator getTargetPriceCalculator() {
		return targetPriceCalculator;
	}
	
	public void setTargetPriceCalculator(TargetPriceCalculator targetPriceCalculator) {
		this.targetPriceCalculator = targetPriceCalculator;
	}
   
}
