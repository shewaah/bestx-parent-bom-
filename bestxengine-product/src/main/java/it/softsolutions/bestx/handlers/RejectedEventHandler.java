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
package it.softsolutions.bestx.handlers;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * 
 * Purpose: this class is mainly for managing RejectedState
 * 
 * Project Name : bestxengine-product 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class RejectedEventHandler extends BaseOperationEventHandler {

    /**
	 * 
	 */
	private static final long serialVersionUID = -828153138571045312L;
//	private static final Logger LOGGER = LoggerFactory.getLogger(RejectedEventHandler.class);

    /**
     * Instantiates a new rejected event handler.
     *
     * @param operation the operation
     * @param serialNumberService the serial number service
     */
    public RejectedEventHandler(Operation operation) {
        super(operation);
    }

    @Override
    public void onNewState(OperationState currentState) {
        // new price discovery
        operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
    }
}
