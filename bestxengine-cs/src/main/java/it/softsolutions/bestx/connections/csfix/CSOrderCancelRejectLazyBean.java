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
package it.softsolutions.bestx.connections.csfix;

import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixOutputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

/**  
*
* Purpose: this class is a Credit Suisse specific bean used to create an OrderCancelReject fix message. 
*
* Project Name : bestxengine-cs 
* First created by: ruggero.rizzo 
* Creation date: 14/giu/2012 
* 
**/
public class CSOrderCancelRejectLazyBean extends FixOutputLazyBean
{
   private String origClOrdId;
   private String comment;
   private XT2Msg msg;
   
   /**
    * Constructor
    * 
    * @param fixSessionId : session whom send the message to
    * @param origClOrdId : id of the rejected cancel
    * @param comment : reject comment
    */
   public CSOrderCancelRejectLazyBean(String fixSessionId, String origClOrdId, String comment)
   {
      super(fixSessionId);
      this.origClOrdId = origClOrdId;
      this.comment = comment;
   }
   
   /**
    * Build the xt2 message with the required fix fields
    * @return the xt2msg
    */
   public XT2Msg buildMsg() {
      msg = super.buildMsg();
      msg.setName(FixMessageFields.FIX_OrderRevokeReject); 
      msg.setValue(FixMessageFields.FIX_SessionID, fixSessionId);
      //[RR20120614] This is not the correct value for the OrderID, but at the moment it is enough
      msg.setValue(FixMessageFields.FIX_OrderID, origClOrdId);
      //[RR20120614] The ClOrdId is not correct, but it is overwritten by the gateway
      msg.setValue(FixMessageFields.FIX_ClOrdID, origClOrdId);
      msg.setValue(FixMessageFields.FIX_OrigClOrdID, origClOrdId);
      msg.setValue(FixMessageFields.FIX_Text, comment == null ? "" : comment);
      msg.setValue(FixMessageFields.FIX_OrdStatus, "" + quickfix.field.OrdStatus.CANCELED);
      msg.setValue(FixMessageFields.FIX_ExecType, "" + quickfix.field.ExecType.CANCELED);
      return msg;
  }

   @Override
   public String toString()
   {
      return (msg != null? msg.toString() : "empty message");
   }
}
