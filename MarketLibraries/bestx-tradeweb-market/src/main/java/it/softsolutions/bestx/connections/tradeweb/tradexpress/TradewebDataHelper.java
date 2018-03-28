
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.connections.tradeweb.tradexpress;

import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.TradingCapacity;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: anna.cochetti
 * Creation date: 23 ago 2017
 * 
 **/

public class TradewebDataHelper {


	public static Character convertTradingCapacity(MarketOrder marketOrder) {
		TradingCapacity tc = marketOrder.getTradingCapacity();
		if(tc == null) return null;
        switch (tc.getValue()) {// returns MiFID requested value 
        case "DEAL":
        	return 'P';
        case "MATCH":
        	return 'R';
        case "AOTC":
        	return 'M';
    	default:
    		return null;
        }        
	}

	public static TradingCapacity convertTradingCapacity(Character tc) {
		if(tc == null) return null;
        switch (tc) {// returns MiFID requested value 
        case 'P':
        	return TradingCapacity.PRINCIPAL; // DEAL
        case 'R':
        	return TradingCapacity.CROSS_AS_PRINCIPAL; // MATCH
        case 'M':
        	return TradingCapacity.CROSS_AS_AGENT;  //AOTC
    	default:
    		return null;
        }        
	}

	public static int convertShortSellIndicator(MarketOrder marketOrder, int defaultShortSelling) {
		if (OrderSide.isBuy(marketOrder.getSide())) return 0; // is buy!
		OrderSide ssi = marketOrder.getShortSellIndicator();
		if(ssi == null && !OrderSide.isBuy(marketOrder.getSide())) return defaultShortSelling;
        switch (ssi) {
        case SELL_SHORT:
        	return 1;
        case SELL_SHORT_EXEMPT:
        	return 2;
        case SELL:
        	return 3;
        case SELL_UNDISCLOSED:
        	return 4;
    	default:
    		return 0; // do not add to the order
        }        
	}


}
