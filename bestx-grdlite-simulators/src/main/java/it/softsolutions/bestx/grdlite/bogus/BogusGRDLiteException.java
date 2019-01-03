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
package it.softsolutions.bestx.grdlite.bogus;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-mq-service 
 * First created by: davide.rossoni 
 * Creation date: 30/gen/2013 
 * 
 **/
public class BogusGRDLiteException extends Exception {

    private static final long serialVersionUID = -1030263094079357448L;
    
    public BogusGRDLiteException() {
        super();
    }
    
    public BogusGRDLiteException(String message) {
        super(message);
    }
    
    public BogusGRDLiteException(Throwable cause) {
        super(cause);
    }
    
    public BogusGRDLiteException(String message, Throwable cause) {
        super(message, cause);
    }

}
