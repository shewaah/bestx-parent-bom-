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

package it.softsolutions.bestx.connections.amosgateway;

import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class OMS1FixOrderInputLazyBean extends FixOrderInputLazyBean {
   private String oms1ClientId = null;
   private String clientId = null;

   /**
    * Instantiates a new amos fix order input lazy bean.
    *
    * @param msg the msg
    * @param dateFormat the date format
    * @param dateTimeFormat the date time format
    */
   public OMS1FixOrderInputLazyBean(XT2Msg msg, String dateFormat,
         String dateTimeFormat) {
      super(msg, dateFormat, dateTimeFormat);
      throw new UnsupportedOperationException();
   }

   /**
    * Instantiates a new amos fix order input lazy bean.
    *
    * @param msg the msg
    * @param dateFormat the date format
    * @param dateTimeFormat the date time format
    * @param oms1ClientId the amos client id
    */
   public OMS1FixOrderInputLazyBean(XT2Msg msg, String dateFormat,
         String dateTimeFormat, String oms1ClientId) {

      super(msg, dateFormat, dateTimeFormat);
      this.oms1ClientId = oms1ClientId;
      msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().wrapString(msg.getString(FixMessageFields.FIX_ClOrdID)));

      // 20100301 AMC sostituito il valore fisso di Codice Private con quanto arriva da AMOS (che e' il codice Sinfo del cliente, non il codice SABE
      this.clientId = msg.getString(FixMessageFields.FIX_ClientID);
      if(this.clientId == null || clientId.isEmpty())
      {
         this.clientId = msg.getString(FixMessageFields.FIX_Account);
      }
      else
      {
         this.clientId = oms1ClientId;
      }
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean#getClientId()
    */
   @Override
   public String getClientId() {
      return this.clientId;
   }   

   /**
    * Gets the amos client id.
    *
    * @return the amos client id
    */
   public String getAmosClientId() {
      return this.oms1ClientId;
   }   
}
