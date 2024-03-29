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
package it.softsolutions.bestx.finders;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 26/ott/2012 
* 
**/
public interface MarketMakerFinder {
    
    /**
     * @param code
     * @return
     * @throws BestXException
     */
    MarketMaker getMarketMakerByCode(String code) throws BestXException;

    /**
     * @param accountCode
     * @return
     * @throws BestXException
     */
    MarketMaker getMarketMakerByAccount(String accountCode) throws BestXException;

    /**
     * @param marketCode
     * @param code
     * @return
     * @throws BestXException
     */
    MarketMarketMaker getMarketMarketMakerByCode(MarketCode marketCode, String code) throws BestXException;
    
    /**
     * @param tsoxSpecificCode the market maker specific code on TSOX
     * @return the market maker associated to tsoxSpecificCode on TSOX
     * @throws BestXException
     */
    MarketMarketMaker getMarketMarketMakerByTSOXCode(String tsoxSpecificCode) throws BestXException;
    
    /**
     * Finds the MarketMarketMaker given the market code. Since Bloomberg has a different management
	 * with a different set of MarketMarketMakers for the BLP and TSOX, this method allows to encapsulate the logic behind this
     * @param marketCode the original marketCode
     * @param marketSpecificCode the string of the market specific code
     * @return the MarketMarketMaker associated to the marketSpecificCode for that MarketCode (or the associated MarketCode in case of Bloomberg)
     * @throws BestXException
     */
    MarketMarketMaker getSmartMarketMarketMakerByCode(MarketCode marketCode, String marketSpecificCode) throws BestXException;
    
}
