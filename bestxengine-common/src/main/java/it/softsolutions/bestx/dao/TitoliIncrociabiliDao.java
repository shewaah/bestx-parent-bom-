/*
* Project Name : bestxengine-common
* First created by: matteo.salis
* Creation date: 10/mag/2012
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

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;

import java.sql.SQLException;
import java.util.List;

public interface TitoliIncrociabiliDao {

	/**
	 * Check if the ISIN is in the TitoliIncrociabili table
	 * 
	 * @param instrument the instrument
	 * @param customer the Customer
	 * 
	 * @return true iff the ISIN is in the TitoliIncrociabili table
	 * @throws Exception Throws SQL Exceptions
	 */
	public abstract boolean isAMatch(Instrument instrument, Customer customer)
		throws SQLException;

	//Needed by MultiMatchingServer
	/**
	 * Check if it's a Matched Order
	 */
	public abstract boolean isAMatch(Order order)
		throws SQLException;

	/**
	 * Set Matching Operation
	 * @param operation
	 * @throws SQLException
	 */
	public abstract void setMatchingOperation(Operation operation)
		throws SQLException;

	/**
	 * Reset Matching Operation
	 * @param operation
	 * @throws SQLException
	 */
	public abstract void resetMatchingOperation(Operation operation)
		throws SQLException;

	/**
	 * Get Match Id
	 * @param operation
	 * @return Match Id
	 * @throws SQLException
	 */
	public abstract String getMatchId(Operation operation)
		throws SQLException;
	
	/**
	 * Check if All Orders of match's arrived
	 * @param matchId
	 * @return true if all orders are arrived
	 * @throws SQLException
	 */
	public abstract boolean allOrdersArrives(String matchId) 
		throws SQLException;
	
	/**
	 * Get List of Orders by Match Id
	 * @param matchId
	 * @return List of orders linked to a match
	 * @throws SQLException
	 */
	public abstract List<String> getMatchOrdersList(String matchId)
		throws SQLException;
	
	/**
	 * Delete matching by Match Id
	 * @param matchId
	 * @throws SQLException
	 */
	public abstract void deleteMatching(String matchId)
		throws SQLException;
}