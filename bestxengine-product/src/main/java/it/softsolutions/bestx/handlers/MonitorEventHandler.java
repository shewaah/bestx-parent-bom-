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
package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * 
 * 
 * Purpose: this class manages events in the Monitor state
 * 
 * Project Name : bestxengine-product First created by: acquafresca/Gonzalez Creation date: 10/07/2019
 * 
 **/
public class MonitorEventHandler extends BaseOperationEventHandler  {

   private static final long serialVersionUID = -1L;
   private static final Logger LOGGER = LoggerFactory.getLogger(MonitorEventHandler.class);
   
   private long timeoutInMilliseconds;

/**
    * Instantiates a new monitor event handler.
    *
    * @param operation the operation
    * @throws BestXException the best X exception
    */
   public MonitorEventHandler(Operation operation, long monitorTimeout)
         throws BestXException{
      super(operation);
      this.timeoutInMilliseconds = monitorTimeout;
   }

   @Override
   public void onNewState(OperationState currentState) {
      LOGGER.debug("MonitorState entry action");
      
      setupDefaultTimer(timeoutInMilliseconds, false);
   }

	
   @Override
   public void onTimerExpired(String jobName, String groupName) {

	   String handlerJobName = super.getDefaultTimerJobName();

      if (jobName.equals(handlerJobName)) {
         operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
      }
      else {
         super.onTimerExpired(jobName, groupName);
      }
   }
}
