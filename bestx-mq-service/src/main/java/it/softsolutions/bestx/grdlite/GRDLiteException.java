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
package it.softsolutions.bestx.grdlite;

import it.softsolutions.bestx.services.customservice.CustomServiceException;

/**  
 *
 * Purpose: wrapper for exceptions thrown during GRDLite connection  
 *
 * Project Name : mq-service 
 * First created by: ruggero.rizzo 
 * Creation date: 24/gen/2013 
 * 
 **/
public class GRDLiteException extends CustomServiceException {
    
    private static final long serialVersionUID = -7538728401120297500L;

    public GRDLiteException() {
        super();
    }
    
    public GRDLiteException(String message) {
        super(message);
    }
    
    public GRDLiteException(Throwable cause) {
        super(cause);
    }
    
    public GRDLiteException(String message, Throwable cause) {
        super(message, cause);
    }
}
