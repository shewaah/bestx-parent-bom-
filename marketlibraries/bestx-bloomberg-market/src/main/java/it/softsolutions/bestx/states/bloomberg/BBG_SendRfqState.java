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
package it.softsolutions.bestx.states.bloomberg;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Market.MarketCode;

/**  
*
* Purpose: send rfq request to BBG TSOX  
*
* Project Name : bestxengine-product 
* First created by: paolo.midali 
* Creation date: 24-lug-2013 
* 
**/
public class BBG_SendRfqState extends BaseState implements Cloneable {

   public BBG_SendRfqState() {
      super(OperationState.Type.SendRfq, MarketCode.BLOOMBERG);
   }
   
	@Override
	public OperationState clone() throws CloneNotSupportedException {
		return new BBG_SendRfqState();
	}

	@Override
	public void validate() throws BestXException {
	}
	

    @Override
    public boolean areMultipleQuotesAllowed() {
        return true;
    }

}
