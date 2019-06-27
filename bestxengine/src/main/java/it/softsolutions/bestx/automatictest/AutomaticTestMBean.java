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
package it.softsolutions.bestx.automatictest;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;

// TODO: Auto-generated Javadoc
/**
 * Purpose: Main interface for interface components for communicating with operations.
 *
 * Project Name : bestxengine 
 * First created by: acquafresca/Gonzalez 
 * Creation date: 27/06/2019 
 * 
 **/
public interface AutomaticTestMBean {
    
   
    /**
     * Gets the simple order operation by id.
     *
     * @param id the id
     * @return the simple order operation by id
     * @throws OperationNotExistingException the operation not existing exception
     * @throws BestXException the best X exception
     */
    String getSimpleOrderOperationById(String id) throws OperationNotExistingException, BestXException;
    
    /**
     * Creates the new order.
     *
     * @param isin the isin
     * @param date the date
     * @param settlementDate the settlement date
     * @return the string
     */
    String createNewOrder(String isin, String date, String settlementDate);
  
}
