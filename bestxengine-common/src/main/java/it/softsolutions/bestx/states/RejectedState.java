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
package it.softsolutions.bestx.states;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;

/**  
*
* Purpose: rejected state  
*
* Project Name : bestxengine-common 
* First created by: paolo.midali
* Creation date: 20/giu/2013 
* 
**/
public class RejectedState extends BaseState implements Cloneable {
	
   public RejectedState() {
      super(OperationState.Type.Rejected, null);
   }
    
	public RejectedState(String reason) {
	   super(OperationState.Type.Rejected, null);
		setComment(reason);
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new RejectedState(this.getComment());
	}

	@Override
	public void validate() throws BestXException {
	}
}
