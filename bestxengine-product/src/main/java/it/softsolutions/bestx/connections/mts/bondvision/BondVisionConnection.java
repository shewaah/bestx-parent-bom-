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
package it.softsolutions.bestx.connections.mts.bondvision;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.mts.MTSConnection;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: anna.cochetti
 * Creation date: 13-nov-2012 
 * 
 **/

public interface BondVisionConnection extends MTSConnection
{
    void sendRfq(String bvSessionId, BondVisionRFQOutputLazyBean rfq) throws BestXException;
}
