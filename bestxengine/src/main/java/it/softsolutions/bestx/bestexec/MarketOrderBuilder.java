/*
* Copyright 1997-2021 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.bestexec;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.MarketOrder;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: stefano.pontillo 
* Creation date: 27 lug 2021 
* 
**/
public interface MarketOrderBuilder {

   /**
    * This method take informations from operation to create the market order for the current attempt
    * 
    * @param operation
    * @return null if is not possible to create the market order
    * @throws Exception
    */
   public MarketOrder getMarketOrder(Operation operation);
   
}
