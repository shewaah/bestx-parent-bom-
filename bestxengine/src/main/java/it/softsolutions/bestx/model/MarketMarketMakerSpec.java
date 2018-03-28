
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
 
package it.softsolutions.bestx.model;
  
/**
 *
 * Purpose: this class has been created to model the different ways a counterparty code is defined in electronic platforms
 * Currently allows to define a code and a code source in a FIX way  
 *
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 08 feb 2017
 * 
 **/

public class MarketMarketMakerSpec {
	@Override
	public String toString() {
		return "marketMakerSpec [marketMakerCode=" + marketMakerCode + ", marketMakerCodeSource="
				+ marketMakerCodeSource + "]";
	}
	
	public String marketMakerCode;
	public String marketMakerCodeSource;
	
	public MarketMarketMakerSpec(String mmCode, String mmCodeSource) {
		this.marketMakerCode = mmCode;
		this.marketMakerCodeSource = mmCodeSource;
	}
	
	public String getMarketMakerMarketSpecificCode () {
		return this.marketMakerCode;
	}
	public String getMarketMakerMarketSpecificCodeSource () {
		return this.marketMakerCodeSource;
	}
}