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
package it.softsolutions.bestx.fixclient;

import java.io.File;
import java.io.InputStream;

import it.softsolutions.bestx.fix.BXNewOrderSingle;
import it.softsolutions.bestx.fix.BXOrderCancelRequest;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 11, 2012 
 * 
 **/
public interface FIXClient {
    
	void init(InputStream settings, File settingFolder, FIXClientCallback fixClientCallback) throws FIXClientException;
	
    void init(String filename, FIXClientCallback fixClientCallback) throws FIXClientException;
    
    void manageNewOrderSingle(BXNewOrderSingle bxNewOrderSingle) throws FIXClientException;
    
    void manageOrderCancelRequest(BXOrderCancelRequest bxOrderCancelRequest) throws FIXClientException;
    
    void stop();
}
