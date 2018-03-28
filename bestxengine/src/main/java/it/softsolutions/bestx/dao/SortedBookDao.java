/*
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
import it.softsolutions.bestx.model.SortedBook;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface SortedBookDao {
	
	/**
	 * Get Sorted Book by ID
	 * @param id
	 * @return  <SortedBook>
	 */
    SortedBook getSortedBookById(Long id);
    
    /**
     * Save sorted book
     * @param sortedBook
     * @param operationId
     */
    void saveSortedBook(SortedBook sortedBook, long operationId);
    
    /**
     * Get Sorted book by Operation
     * @param operation
     * @return <SortedBook>
     */
    SortedBook getOperationSortedBook(Operation operation);
    
    /**
     * Delete SortedBook from Operation
     * @param operation
     */
    void deleteOperationSortedBook(Operation operation);
    
    /**
     * SaveSorted Book from Operation
     * @param operation
     */
    void saveOperationSortedBook(Operation operation);
}
