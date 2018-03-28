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
package it.softsolutions.bestx.services.executionstrategy;

/**  
*
* Purpose: this is the execution strategy callback, these methods must be implemented by who should
* manage the result of the execution strategy.  
*
* Project Name : bestxengine-akros 
* First created by: ruggero.rizzo 
* Creation date: 21/ago/2012 
* 
**/
public interface ExecutionStrategyServiceCallback
{
   /**
    * Method called when the execution strategy reaches a result. The implementor will know
    * what to do depending on the kind of result received.
    * 
    * @param result : a value of the enumeration ExecutionStrategyService.Result
    * @param message : message explaining the result received
    */
   void onUnexecutionResult(ExecutionStrategyService.Result result, String message);
   
   /**
    * The execution strategy reached a success point, that is to say the market on which execute the order.
    * It is not mandatory that every execution strategy lasts on an execution.
    * @param executionMarket : the market on which we will execute the order
    */
   void onUnexecutionDefault(String executionMarket);
}
