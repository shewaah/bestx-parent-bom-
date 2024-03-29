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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketConnection;
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
	private MarketConnection marketConnection;
	private static final Logger LOGGER = LoggerFactory.getLogger(TW_StartExecutionEventHandler.class);

    public TW_StartExecutionEventHandler(Operation operation, MarketConnection twConnection, long orderCancelDelay) {
        super(operation);
        this.marketConnection=twConnection;
    }

    @Override
    public void onNewState(OperationState currentState) {
    	if(CheckIfBuySideMarketIsConnectedAndEnabled())
    		operation.setStateResilient(new TW_SendOrderState(), ErrorState.class);
    	else
    		operation.setStateResilient(new TW_RejectedState(), ErrorState.class);
    }
    
	
	/**
	 * Check if buy side market is connected and enabled.
	 *
	 * @return true, if successful
	 */
	public boolean CheckIfBuySideMarketIsConnectedAndEnabled (){
		if(marketConnection == null) return false;
		if (!marketConnection.isBuySideConnectionEnabled()) {             
			LOGGER.info("Market Tradeweb is not enabled");
			return false;			
		}
		
		if (!marketConnection.isBuySideConnectionAvailable()) {             
			LOGGER.info("MarketCode Tradeweb is not available");
			return false;			
		}		

		return true;		
	}
}
