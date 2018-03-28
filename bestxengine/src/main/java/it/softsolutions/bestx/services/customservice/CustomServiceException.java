
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
 
package it.softsolutions.bestx.services.customservice;

  
/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 10/ago/2015
 * 
 **/

public class CustomServiceException extends Exception {

	private static final long serialVersionUID = 2395891661935443298L;

	public CustomServiceException() {
        super();
    }
    
    public CustomServiceException(String message) {
        super(message);
    }
    
    public CustomServiceException(Throwable cause) {
        super(cause);
    }
    
    public CustomServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
