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
package it.softsolutions.bestx.handlers.tradeweb;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.tradeweb.TW_RejectedState;
import it.softsolutions.bestx.states.tradeweb.TW_SendOrderState;

/**
 *
 * Purpose: start execution on TW
 *
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class TW_StartExecutionEventHandler extends BaseOperationEventHandler {
    
    private static final long serialVersionUID = -9102220412689184343L;

    public TW_StartExecutionEventHandler(Operation operation, MarketBuySideConnection twConnection, long orderCancelDelay) {
        super(operation);
    }

    @Override
    public void onNewState(OperationState currentState) {
//    	if(this.operation != null && this.operation.getLastAttempt() != null && this.operation.getLastAttempt().getMarketOrder() != null &&
//    			this.operation.getLastAttempt().getMarketOrder().getMarket() != null &&
//    			this.operation.getLastAttempt().getMarketOrder().getMarket().isDisabled()) {
//    		// create a new MarketExecutionReport for the rejection and then go to rejected
//    		operation.setStateResilient(new TW_RejectedState("Tradeweb market is disabled"), ErrorState.class);
//    	}
//    	else
    		operation.setStateResilient(new TW_SendOrderState(), ErrorState.class);
    }
}
