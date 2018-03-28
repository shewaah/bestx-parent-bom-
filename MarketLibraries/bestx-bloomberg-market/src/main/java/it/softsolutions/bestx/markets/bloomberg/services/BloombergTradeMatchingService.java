/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.markets.bloomberg.services;

import it.softsolutions.bestx.markets.bloomberg.model.BloombergFeedTrade;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.Service;
import it.softsolutions.jsscommon.Money;

import java.util.Date;

/**  
 *
 * Purpose: Service storing all trade messages coming from Bloomberg TradeStac connection
 * allowing clients to match their executed orders to trades stored  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: paolo.midali 
 * Creation date: 20/feb/2013 
 * 
 **/
public interface BloombergTradeMatchingService extends Service {

   /**
    * Method used to asynchronously store trades
    * @param trade A BloombergFeedTrade representing the trade received
    */
   void addTrade(BloombergFeedTrade trade);
	
	/**
	 * Finder method used to find trades and match them to a given order
	 * @param order
	 * @param executionPrice
	 * @param marketMaker
	 * @param minArrivalDate
	 * @return A BloombergFeedTrade, or null if no trade matches
	 */
   MarketExecutionReport matchTrade(Order order,
			                        Money executionPrice,
			                        MarketMaker marketMaker,
			                        Date minArrivalDate);
}
