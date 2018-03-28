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

import it.softsolutions.bestx.fix.BXBusinessMessageReject;
import it.softsolutions.bestx.fix.BXExecutionReport;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
import it.softsolutions.bestx.fix.BXReject;
import quickfix.SessionID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 11, 2012 
 * 
 **/
public interface FIXClientCallback {
    
    void onLogon(SessionID sessionID);
    
    void onLogout(SessionID sessionID);
    
    void onExecutionReport(BXExecutionReport bxExecutionReport) throws FIXClientException;

    void onOrderCancelReject(BXOrderCancelReject bxOrderCancelReject) throws FIXClientException;
    
    void onReject(BXReject bxReject) throws FIXClientException;

    void onBusinessMessageReject(BXBusinessMessageReject bxBusinessMessageReject) throws FIXClientException;
}
