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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;

/**  
*
* Purpose: this class is the execution strategy, that is to say the class where we decide if to apply
* specific behaviours like the automatic not execution or the choosing of a magnet market.
* <br><br>
* Decisions making methods will notify the results through callback methods exposed in the {@link ExecutionStrategyServiceCallback}
* interface.
* <br><br>
* The {@link Result} enumeration lists all the possible outcomes.
* <br><br>
* Project Name : bestxengine-akros 
* First created by: ruggero.rizzo 
* Creation date: 21/ago/2012 
* 
**/
public interface ExecutionStrategyService
{
   public enum Result
   {
      CustomerAutoNotExecution, CustomerPolicyNotAvailable, InstrumentNotQuotedOnMagnetMarkets, 
      WrongPolicyOrNotQuotedOnRegulatedMarkets, MagnetNotRequired, MagnetMarketNotFound, QuantityNotValid,
      Failure, Success, InstrumentNotNegotiableOnMarket, MaxDeviationLimitViolated, LimitFileNoPrice, LimitFile, USSingleAttemptNotExecuted, EODCalled
   }
   
   /**
    * Decide wether the order should go in the automatic not execution or some other alternative, like the
    * magnet.
    * 
    * @param order : the order to examine
    * @param customer : the customer who sent the order
    * @param message : an optional message to be shown in the order history audit
    * @throws BestXException : if something wrong happens
    */
   void manageAutomaticUnexecution(Order order, Customer customer, String message) throws BestXException;
   /**
    * The starting of an order execution has been centralized in this method.
    * 
    * @param operation
    *            : the operation that is going to be executed
    * @param currentAttempt
    *            : current attempt
    * @param serialNumberService
    *            : the serial number service
    */
   public abstract void startExecution(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService);
   /**
    * The choice about what to do next after a failed execution attempt
    * @param operation
    *            : the operation that is going to be executed
    * @param currentAttempt
    *            : current attempt
    * @param serialNumberService
    *            : the serial number service
    * @throws BestXException : if something wrong happens
    **/
   public abstract void manageMarketReject(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService) throws BestXException;
   
   /**
    * The choice about what to do when the user or the client has cancelled the order
    * @param operation
    *            : the operation that is going to be executed
    * @param currentAttempt
    *            : current attempt
    * @param serialNumberService
    *            : the serial number service
    **/
   public abstract void acceptOrderRevoke(Operation operation, Attempt currentAttempt,
			SerialNumberService serialNumberService);

}