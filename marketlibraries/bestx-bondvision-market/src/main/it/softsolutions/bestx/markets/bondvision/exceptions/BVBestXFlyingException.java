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
package it.softsolutions.bestx.markets.bondvision.exceptions;

import it.softsolutions.bestx.BestXException;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class BVBestXFlyingException extends BestXException {

    private static final long serialVersionUID = 1895121521658998235L;

    public BVBestXFlyingException(String message) {
        super(message);
    }
    
    public BVBestXFlyingException(Throwable cause) {
        super(cause);
    }

    public BVBestXFlyingException(String message, Throwable cause) {
        super(message, cause);
    }
}
