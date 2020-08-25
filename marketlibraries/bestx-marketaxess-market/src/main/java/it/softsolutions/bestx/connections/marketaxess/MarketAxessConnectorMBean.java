
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

package it.softsolutions.bestx.connections.marketaxess;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 11 set 2017
 * 
 **/

public interface MarketAxessConnectorMBean {

	/**
	 * @return the trader id used to send orders
	 */
	String getTraderPartyID();

	/**
	 * @param traderPartyID the trader id used to send orders
	 */
	void setTraderPartyID(String traderPartyID);
	
	void setIncludeDealers(int minDealers);
	
	int getIncludeDealers();

}