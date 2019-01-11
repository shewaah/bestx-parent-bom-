
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
 
package it.softsolutions.bestx.markets.marketaxess;

import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;

/**
 *
 * Purpose: this class is mainly extend standard MarketOrder to carry MarketAxess specificity
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 12 gen 2017
 * 
 **/

public class MarketAxessOrder extends MarketOrder {

	
	/**
	 * 
	 */
	public MarketAxessOrder() {
		super();
	}
	
	public MarketAxessOrder(MarketOrder marketOrder) {
		super();
		setValues(marketOrder);
	}

}
