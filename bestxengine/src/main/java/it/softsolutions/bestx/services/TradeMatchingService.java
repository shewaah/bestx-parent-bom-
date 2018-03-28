/*
* Copyright 1997-2012 SoftSolutions! srl
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

import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.TradeFill;
import it.softsolutions.jsscommon.Money;

import java.util.Date;


/** 
*
* Purpose: service storing all trade messages coming from Bloomberg Trade Feed 
* allowing clients to match their executed orders to trades stored
*
* Project Name : bestxengine-akros
* First created by: ruggero.rizzo
* Creation date: 25-set-2012
*
**/
public interface TradeMatchingService extends Service {
	/**
	 * Method used to asynchronously store trades
	 * @param trade A TradeFill representing the trade received
	 */
	void addTrade(TradeFill trade);
	
	/**
	 * Finder method used to find trades and match them to a given order
	 * @param order : the order
	 * @param executionPrice : order execution price
	 * @param marketMaker : order execution market maker
	 * @param minArrivalDate : minimum allowed arrival date
	 * @return A TradeFill, or null if no trade matches
	 */
	TradeFill matchTrade(Order order,
			Money executionPrice,
            MarketMaker marketMaker,
            Date minArrivalDate);
}
