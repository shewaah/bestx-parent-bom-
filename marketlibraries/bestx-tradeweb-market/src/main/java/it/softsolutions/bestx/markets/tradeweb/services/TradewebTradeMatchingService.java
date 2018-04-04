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
package it.softsolutions.bestx.markets.tradeweb.services;

import java.util.Date;

import it.softsolutions.bestx.markets.tradeweb.model.TradewebFeedTrade;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.Service;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: Service storing all trade messages coming from Tradeweb TradeStac connection
 * allowing clients to match their executed	 orders to trades stored
 * 
 * @author  Paolo Midali 
 *
 */
/*
 * Project Name : bestx-tradeweb-market 
 * First created by: paolo.midali 
 * Creation date: 20/feb/2013  
 */
public interface TradewebTradeMatchingService extends Service {

   /**
    * Method used to asynchronously store trades
    * @param trade A TradewebFeedTrade representing the trade received
    */
   void addTrade(TradewebFeedTrade trade);
	
	/**
	 * Finder method used to find trades and match them to a given order
	 * @param order the order
	 * @param executionPrice the executed price
	 * @param marketMaker the counterpart
	 * @param minArrivalDate the minimum date and time to check the matching trades
	 * @return A TradewebFeedTrade, or null if no trade matches
	 */
   MarketExecutionReport matchTrade(Order order,
			                        Money executionPrice,
			                        MarketMaker marketMaker,
			                        Date minArrivalDate);
}
