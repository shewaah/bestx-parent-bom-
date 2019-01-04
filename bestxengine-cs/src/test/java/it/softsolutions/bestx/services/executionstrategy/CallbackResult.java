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

import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService.Result;

/**
 * Purpose: this class is mainly for ...   Project Name : bestxengine-akros  First created by: ruggero.rizzo  Creation date: 23/ago/2012 
 */
public class CallbackResult
{
   private Result result;
   private String message;

   public CallbackResult(Result result, String message)
   {
      this.result = result;
      this.message = message;
   }
   public Result getResult()
   {
      return result;
   }

   public void setResult(Result result)
   {
      this.result = result;
   }

   public String getMessage()
   {
      return message;
   }

   public void setMessage(String message)
   {
      this.message = message;
   }
}