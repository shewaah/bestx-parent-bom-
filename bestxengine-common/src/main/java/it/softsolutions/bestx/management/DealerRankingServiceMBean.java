/**
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.management;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 06/nov/2013
 * 
 */
public interface DealerRankingServiceMBean {
	
	/**
	 * Expose the loading of the ranking and update of the currently one in use.
	 * 
	 * @throws Exception
	 */
	void loadRankingAndUpdate() throws Exception;

	/**
	 * Expose the loading of today ranking.
	 * 
	 * @param updateLiveRanking
	 *            : true to update the currently ranking in use.
	 * @throws Exception
	 */
	void loadRanking(boolean updateLiveRanking) throws Exception;

	/**
	 * Expose the loading of a given date ranking and the possible update of the currently one in use.
	 * 
	 * @date : date requested.
	 * @param updateLiveRanking
	 *            : true to update the currently ranking in use.
	 * @throws Exception
	 */
	void loadRanking(String date, boolean updateLiveRanking) throws Exception;

	/**
	 * Fetch the ranking of the given date.
	 * 
	 * @date : date requested.
	 * @return An array of market makers codes.
	 */
	String[] getRanking(String date) throws Exception;
}
