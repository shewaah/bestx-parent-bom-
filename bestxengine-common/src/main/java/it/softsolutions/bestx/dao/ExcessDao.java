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

import it.softsolutions.bestx.model.Customer;

/**
 * Purpose : excess towards buy or sell orders DAO interface
 * @author ruggero.rizzo
 *
 */
public interface ExcessDao {

   /**
    * Recover, for the given customer, the excess limit.
    * @param customer : customer whose exess limit we must find
    * @return the limit.
    * @throws Exception
    */
	public abstract double getLimit(Customer customer) throws Exception;

	/**
	 * Calculate the excess for the given customer.
	 * @param customer : customer whose excess we must calculate
	 * @return the excess.
	 * @throws Exception
	 */
	public abstract double getCurrentExcess(Customer customer) throws Exception;

}