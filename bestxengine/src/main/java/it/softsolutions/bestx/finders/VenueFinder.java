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

import java.util.Set;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Venue;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 26/ott/2012 
* 
**/
public interface VenueFinder {
    
    /**
     * @return
     * @throws BestXException
     */
    Set<Venue> getAllVenues() throws BestXException;

    /**
     * @param market
     * @return
     * @throws BestXException
     */
    Venue getMarketVenue(Market market) throws BestXException;

    /**
     * @param marketMaker
     * @return
     * @throws BestXException
     */
    Venue getMarketMakerVenue(MarketMaker marketMaker) throws BestXException;
    
}
