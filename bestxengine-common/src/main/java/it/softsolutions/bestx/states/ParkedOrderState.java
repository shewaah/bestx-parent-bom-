/*
* Copyright 1997-2018 SoftSolutions! srl 
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
* Purpose: this class is mainly for parking limit files order before first price discovery  
*
* Project Name : bestxengine-common 
* First created by: stefano.pontillo 
* Creation date: 12 lug 2018 
* 
**/
public class ParkedOrderState extends BaseState implements Cloneable {

   public ParkedOrderState() {
      super(OperationState.Type.LimitFileParkedOrder, null);
  }

  public ParkedOrderState(String comment) {
      super(OperationState.Type.LimitFileParkedOrder, null);
      setComment(comment);
  }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.OperationState#validate()
    */
   @Override
   public void validate() throws BestXException {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.BaseState#clone()
    */
   @Override
   public OperationState clone() throws CloneNotSupportedException {
      return new ParkedOrderState();
   }

}
