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

import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;

/**
 * 
 * Purpose: interface for all the MIC code retrieval services
 * 
 * Project Name : bestxengine 
 * First created by: ruggero.rizzo 
 * Creation date: 26/ott/2012
 * 
 **/
public interface MICCodeService
{
    /**
     * Find out if the given market is an OTC one
     * @param m : the market to check
     * @return true if the market is OTC, false otherwise
     */
   public boolean isOTCMarket(Market m);
   /**
    * Fetch the MIC Code of the given market
    * @param m : the market whose MIC Code we must find
    * @param i : the instrument that may be needed if we must look in tables with market/submarkets 
    * dependant from the instrument (i.e. the MarketSecurityStatus)
    * @return a MIC Code
    */
   public String getMICCode(Market m, Instrument i);
}
