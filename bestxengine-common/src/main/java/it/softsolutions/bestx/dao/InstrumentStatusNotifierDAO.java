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

/* 
 * Purpose: Interface for the DAO that must manage the volatility call event for
 * a given ISIN.
 * Here we require onlyl the two methods that realize the db update, the
 * choice of the data structures, procedures and so on is all left to
 * the implementor.
 * 
 */
public interface InstrumentStatusNotifierDAO {
	/**
	 * Update the instrument quoting status when start volatility call
	 * 
	 * @param orderId the order id
	 */
   public void updateStateDescForStartVolatilityCall(String orderId);
   /**
    * Update the instrument quoting status when end volatility call
    * 
    * @param orderId the order id
    */
	public void updateStateDescForEndVolatilityCall(String orderId);
}
