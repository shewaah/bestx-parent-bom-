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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public interface TitoliIncrociabiliService {
    /**
     * @param instrument
     * @param customer
     * @return
     * @throws BestXException
     */
    boolean isAMatch(Instrument instrument, Customer customer) throws BestXException;
    /**
     * @param order
     * @return
     * @throws BestXException
     */
    boolean isAMatch(Order order) throws BestXException;
    /**
     * @param operation
     * @throws BestXException
     */
    void setMatchingOperation(Operation operation) throws BestXException;
    /**
     * @param operation
     * @throws BestXException
     */
    void resetMatchingOperation(Operation operation) throws BestXException;
}
