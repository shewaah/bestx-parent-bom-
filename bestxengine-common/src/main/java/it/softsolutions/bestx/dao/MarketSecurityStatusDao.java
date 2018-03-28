/*
* Project Name : bestxengine-common
* First created by: matteo.salis
* Creation date: 11/mag/2012
*
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
*
*/
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.markets.MarketSecurityStatus;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;

/**
* Purpose : Interface to load data about securities sent by Market
*
*/
public interface MarketSecurityStatusDao {
   
    /**
     * Get Market Data by Market Code and SubMarketCode
     * @param marketCode
     * @param subMarketCode
     * @param instrument
     * @return >MarketSecurityStatus>
     */
    MarketSecurityStatus getMarketSecurityStatus(MarketCode marketCode, SubMarketCode subMarketCode, String instrument);
   
    /**
     * Save Or Update Market Security Status data
     * @param marketSecurityStatus
     */
    void saveOrUpdateMarketSecurityStatus(MarketSecurityStatus marketSecurityStatus);
}
