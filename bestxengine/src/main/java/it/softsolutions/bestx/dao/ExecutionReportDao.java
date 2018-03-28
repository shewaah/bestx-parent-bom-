/*
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
package it.softsolutions.bestx.dao;


import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport;

import java.util.List;

/**
 * Purpose : Persistence of Execution Report
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/

public interface ExecutionReportDao {
	/**
	 * Get Execution Report of Operation
	 * @param operation
	 * @return List of <ExecutionReport>
	 */
    List<ExecutionReport> getOperationExecutionReports(Operation operation);
    
    /**
     * Delete Execution report of operation
     * @param operation
     */
    void deleteOperationExecutionReports(Operation operation);
    
    /**
     * Save Execution Reports of Operation
     * @param operation
     */
    void saveOperationExecutionReports(Operation operation);
}
