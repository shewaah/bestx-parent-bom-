/*
* Copyright 1997-2016 SoftSolutions! srl 
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
package it.softsolutions.bestx.mq.messages;

import java.util.HashMap;
import java.util.Map;

public abstract class MQMessage {

   private final Map<String, String> messageProperties = new HashMap<String, String>();

   public final Map<String, String> getMessageProperties() {
      return messageProperties;
   }

   public abstract String toTextMessage();
}
