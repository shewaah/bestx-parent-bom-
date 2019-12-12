/*
* Copyright 1997-2019 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.mqclient;
 

 

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-mq-jmx-client-app 
* First created by: stefano.pontillo 
* Creation date: 12 dic 2019 
* 
**/
public interface MQtestMBean {
   
   // commands
   String connectMQService();

   void disconnect();

   boolean isConnected();

   String sendPriceDiscoveryRequest(String instrumentCode, String side, double qty);   

   String sendMessage(String messageStr);
   
   String readMessage();
}
