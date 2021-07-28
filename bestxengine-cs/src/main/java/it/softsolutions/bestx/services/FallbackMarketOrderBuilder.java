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

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.model.MarketOrder;


/**  
*
* Purpose: this class is mainly for choose wich builder use following these rules:
*  1) the heartbeat flows and system status is up. Request are answered in time with no ERROR. BestX! uses the response to define the execution attempt. If there is a WARNING, BestX! logs the warning.
*  2) the hearbeat flows and the system status is up. Request is timed out. BestX! goes to the fallback algo to define the execution attempt wheter it is a LF or algo order. Other orders (orders that are executed afterwards) are not affected.
*  3) the heartbeat flows and system status is up. Request are answered in time with ERROR. BestX rejects back each order when the service GetRoutingProposal is responding with 1 or more Errors. Each new order continues to call the service GetRoutingProposal. An error response from GetRoutingProposal does not say anything about the availability of the services, that’s what he heartbeat does.
*  4) the hearbeat flows and the system status is down. BestX! goes to fallback
*  5) the heartbeat is timed out. BestX! goes to fallback
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 27 lug 2021 
* 
**/
public class FallbackMarketOrderBuilder implements MarketOrderBuilder {

   private MarketOrderBuilder defaultMarketOrderBuilder;
   private MarketOrderBuilder csAlgoMarketOrderBuilder;
   
   
   @Override
   public MarketOrder getMarketOrder(Operation operation) {
      if (csAlgoMarketOrderBuilder.getServiceStatus()) {
         return csAlgoMarketOrderBuilder.getMarketOrder(operation);
      } else if (defaultMarketOrderBuilder.getServiceStatus()) {
         return defaultMarketOrderBuilder.getMarketOrder(operation);
      } else {
         return null;
      }
   }
   
   public MarketOrderBuilder getDefaultMarketOrderBuilder() {
      return defaultMarketOrderBuilder;
   }
   
   public void setDefaultMarketOrderBuilder(MarketOrderBuilder defaultMarketOrderBuilder) {
      this.defaultMarketOrderBuilder = defaultMarketOrderBuilder;
   }
   
   public MarketOrderBuilder getCsAlgoMarketOrderBuilder() {
      return csAlgoMarketOrderBuilder;
   }
   
   public void setCsAlgoMarketOrderBuilder(MarketOrderBuilder csAlgoMarketOrderBuilder) {
      this.csAlgoMarketOrderBuilder = csAlgoMarketOrderBuilder;
   }

   @Override
   public boolean getServiceStatus() {
      return true;
   }
}
