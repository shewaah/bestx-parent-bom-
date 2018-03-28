/*
* Project Name : bestengine-common 
* First created by: ruggero.rizzo 
* Creation date: 10/mag/2012 
* 
* Copright 1997-2012 SoftSolutions! srl 
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

import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Purpose : access to the regulated and regulated like markets instruments data.
 * @author ruggero.rizzo
 *
 */
public interface MarketSecurityIsinDao
{
   /**
    * Load all the instruments for every market.
    * @return a map with the market as key and its list of instruments as value.
    */
   Map<String, List<String>> loadInstruments();
   /**
    * Add to the map in memory the instruments of the given market.
    * @param market : the market whose instruments we want to load
    * @param argMap : currently map in use
    * @return the new map, the one passed as argument merged with the new data loaded.
    */
   Map<String, List<String>> addInstruments(String market, Map<String, List<String>> argMap);
   /**
    * Load submarket codes for specified market and isin.
    * @param isin : the instrument to look for
    * @param market : the market whose submarkets we must find
    * @return the list of submarket codes.
    */
   List<SubMarketCode> getSubmarketCodes(String isin, String market);
   public Date getBOTSettlementDate(String isin);
}
