/*
* Copyright 1997-2020 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.datacollector;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Proposal;

/**  
*
* Purpose: this class is mainly for manage data collector services like Kafka datalake for CS
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 15 lug 2020 
* 
**/
public interface DataCollector {

   /**
    * Sends a single proposal
    * 
    * @param proposal
    */
   public void sendPrice(Proposal proposal);
   
   /**
    * Send a whole book
    */
   public void sendBook(Operation operation);
   
   /**
    * Sends executable prices coming from market execution reports
    */
   public void sendPobex();
   
}
