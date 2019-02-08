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
package it.softsolutions.bestx.states.bloomberg;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

/**  
*
* Purpose: send rfq to internal RTFI market maker, passing the best BLOOMBERG quote (similar, but simpler, to RTFI_INT_StartExecution)   
*
* Project Name : bestxengine-product 
* First created by: paolo.midali
* Creation date: 05/ott/2012 
* 
**/
public class BBG_INT_SendInternalRfqState extends BaseState implements Cloneable {
	
	public BBG_INT_SendInternalRfqState() {
	   super(OperationState.Type.InternalSendRfqToInternal, MarketCode.BLOOMBERG);
	}

	public BBG_INT_SendInternalRfqState(String comment) {
	   super(OperationState.Type.InternalSendRfqToInternal, MarketCode.BLOOMBERG);
		setComment(comment);
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new BBG_INT_SendInternalRfqState(this.getComment());
	}
	@Override
	public void validate() throws BestXException {
	}
	@Override
	public boolean mustSaveBook() {
		return true;
	}

	@Override
    public boolean areMultipleQuotesAllowed() {
        return true;
    }
}
