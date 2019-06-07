/*
 * Copyright 2019-2028 SoftSolutions! srl 
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
package it.softsolutions.bestx.states.bondvision;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

/**
 *
 * Purpose: TW cancelled state
 *
 * Project Name : bestx-bondvision-market 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class BV_ReceiveQuoteState extends BaseState implements OperationState {

	public BV_ReceiveQuoteState() {
		super(OperationState.Type.Cancelled, MarketCode.BV);
	}

	public BV_ReceiveQuoteState(String comment) {
		super(OperationState.Type.Cancelled, MarketCode.BV);
		setComment(comment);
	}

	@Override
	public void validate() throws BestXException {
	}

	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new BV_ReceiveQuoteState(this.getComment());
	}
}