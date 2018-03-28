
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
 * Purpose: this class is mainly for carrying the trading capacity of orders and executions 
 *
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 22 ago 2017
 * 
 **/

public enum TradingCapacity {
	NONE("NONE"), // None
	AGENT("AOTC"), //(agent) -> AOTC
	CROSS_AS_AGENT("AOTC"), //(cross as agent) -> AOTC
	CROSS_AS_PRINCIPAL("MTCH"), //(cross as principal) -> MTCH
	PRINCIPAL("DEAL"), //(principal) -> DEAL
	RISKLESS_PRINCIPAL("DEAL") //(riskless principal) -> DEAL
	;
    private final String value;

    private TradingCapacity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
