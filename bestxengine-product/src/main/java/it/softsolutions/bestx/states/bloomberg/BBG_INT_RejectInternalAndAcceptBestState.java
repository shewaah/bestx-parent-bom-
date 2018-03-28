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
 * Purpose: Bloomberg reject internal quote state from rtfi, accept best quote from bloomberg if still good  
 *
 * Project Name : bestxengine-product 
 * First created by: paolo.midali
 * Creation date: 28/nov/2012 
 * 
 **/
public class BBG_INT_RejectInternalAndAcceptBestState extends BaseState implements Cloneable {

    /**
     * Instantiates a new RTFI internalized order state.
     */
    public BBG_INT_RejectInternalAndAcceptBestState() {
        super(OperationState.Type.InternalRejectInternalAndAcceptBestState, MarketCode.BLOOMBERG);
    }

    public BBG_INT_RejectInternalAndAcceptBestState(String rejectReason) {
        super(OperationState.Type.InternalRejectInternalAndAcceptBestState, MarketCode.BLOOMBERG);
        setComment(rejectReason);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.BaseState#clone()
     */
    @Override
    public OperationState clone() throws CloneNotSupportedException {
        BBG_INT_RejectInternalAndAcceptBestState state = new BBG_INT_RejectInternalAndAcceptBestState();
        
        return state;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.OperationState#validate()
     */
    @Override
    public void validate() throws BestXException {
    }
    
    
    @Override
    public boolean areMultipleQuotesAllowed() {
        return true;
    }
}
