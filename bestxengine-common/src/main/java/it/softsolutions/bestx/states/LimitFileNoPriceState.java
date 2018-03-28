/*
* Copyright 1997-2013 SoftSolutions! srl 
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
 * Purpose: state for Limit Price Order without prices  
 *
 * Project Name : bestxengine-common 
 * First created by: ruggero.rizzo 
 * Creation date: 29/ott/2013 
 * 
 **/
public class LimitFileNoPriceState extends BaseState implements Cloneable {

    public LimitFileNoPriceState() {
        super(OperationState.Type.LimitFileNoPrice, null);
    }

    public LimitFileNoPriceState(String comment) {
        super(OperationState.Type.LimitFileNoPrice, null);
        setComment(comment);
    }

    @Override
    public void validate() throws BestXException {
    }

    @Override
    public OperationState clone() throws CloneNotSupportedException {
        return new LimitFileNoPriceState();
    }

    @Override
    public boolean mustSaveBook() {
        return true;
    }

    @Override
    public boolean isRevocable() {
        return true;
    }
//    
//    @Override
//    public boolean isTerminal() {
//        return true;
//    }
    
}
