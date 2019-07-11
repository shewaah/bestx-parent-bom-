/*
 * Copyright 1997-2019 SoftSolutions! srl 
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
* Purpose: this class is mainly for managing orders that are in monitor state , 
* i.e. if the book is successfully completed a rejection must be sent to customer, 
* otherwise the order tries periodically to get a correct book  
*
* Project Name : bestxengine-common 
* First created by: acquafresca/gonzalez
* Creation date: 10/07/2019 
* 
**/
public class MonitorState extends BaseState implements Cloneable {
	
	public MonitorState () {
        super(OperationState.Type.Monitor, null);
	}

	public MonitorState (String comment) {
        super(OperationState.Type.Monitor, null);
		setComment(comment);
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new MonitorState(getComment());
	}

	@Override
	public void validate() throws BestXException {
	}
	
    @Override
    public boolean mustSaveBook() {
        return true;
    }	
}
