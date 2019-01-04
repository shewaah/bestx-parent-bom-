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
package it.softsolutions.bestx.mq;

import it.softsolutions.bestx.mq.messages.MQMessage;

public interface MQConnection {

   /**
    * Publish the given MQMessage
    * 
    * @param mqMessage
    *            the message to be published
    * @throws Exception
    *            if something goes wrong
    */
   void publish(MQMessage mqMessage) throws Exception;

   /**
    * Close.
    *
    * @throws Exception the exception
    */
   void close() throws Exception;
}
