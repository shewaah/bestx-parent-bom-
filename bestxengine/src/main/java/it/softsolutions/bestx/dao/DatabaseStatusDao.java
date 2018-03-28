/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao;

import java.util.Date;


/**
 * Purpose : instrument data access interface.
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 24/feb/2015
* 
**/

public interface DatabaseStatusDao {
	
	String getDatabaseStatus();

	int selectCountAs(String tableName);
	
	Date selectOldestRecordInProposal();
	
	Date selectOldestRecordInPriceTable();
	
	
}
