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
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * @author Stefano
 *
 */
public class BV_RejectedEventHandler extends BaseOperationEventHandler {
	/**
	 * @param operation
	 */
	public BV_RejectedEventHandler(Operation operation) {
		super(operation);
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		//In caso di rifiuto della RFCQ da parte del MM ritorno alla creazione di un nuovo book
        operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
	}
}
