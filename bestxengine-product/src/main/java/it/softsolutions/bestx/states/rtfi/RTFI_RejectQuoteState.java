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
package it.softsolutions.bestx.states.rtfi;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

/**  
*
* Purpose: RTFI rejected quote state  
*
* Project Name : bestxengine-product 
* First created by: ruggero.rizzo
* Creation date: 05/ott/2012 
* 
**/
public class RTFI_RejectQuoteState extends BaseState implements Cloneable {
	
   public RTFI_RejectQuoteState() {
      super(OperationState.Type.RejectQuote, MarketCode.RTFI);
   }
   
	public RTFI_RejectQuoteState(String rejectedReason) {
	    super(OperationState.Type.RejectQuote, MarketCode.RTFI);
	    setComment(rejectedReason);
	}

    @Override
	public OperationState clone() throws CloneNotSupportedException {
		return new RTFI_RejectQuoteState();
	}

	@Override
	public void validate() throws BestXException {
	}
}
