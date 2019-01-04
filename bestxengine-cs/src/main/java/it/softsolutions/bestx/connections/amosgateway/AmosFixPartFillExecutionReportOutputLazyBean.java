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

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.services.MICCodeService;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 19-ott-2012 
 * 
 **/
public class AmosFixPartFillExecutionReportOutputLazyBean extends OMS2FixExecutionReportOutputLazyBean
{
   private static final Logger LOGGER = LoggerFactory.getLogger(AmosFixPartFillExecutionReportOutputLazyBean.class);

   /**
    * Instantiates a new amos fix part fill execution report output lazy bean.
    *
    * @param sessionId the session id
    * @param quote the quote
    * @param order the order
    * @param orderId the order id
    * @param attempt the attempt
    * @param executionReport the execution report
    * @param cumulativeQty the cumulative qty
    * @param micCodeService the mic code service
    * @param marketMaker the market maker
    */
   public AmosFixPartFillExecutionReportOutputLazyBean(String sessionId, Quote quote, Order order, String orderId,
         Attempt attempt, ExecutionReport executionReport, BigDecimal cumulativeQty, MICCodeService micCodeService,
         MarketMaker marketMaker)
   {
      super(sessionId, quote, order, orderId, attempt, executionReport, micCodeService, marketMaker);
      if (cumulativeQty.compareTo(order.getQty()) == 0)
      {
         msg.setValue(FixMessageFields.FIX_OrdStatus, ExecutionReportState.FILLED.getValue());
      }
      else
      {
         msg.setValue(FixMessageFields.FIX_OrdStatus, ExecutionReportState.PART_FILL.getValue());
      }

      if (executionReport.getLastPx() != null)
      {
         msg.setValue(FixMessageFields.FIX_Price, executionReport.getLastPx().doubleValue());
      }
      cumQty = cumulativeQty;
      leavesQty = order.getQty().subtract(cumQty);
      if (leavesQty != null)
      {
         msg.setValue(FixMessageFields.FIX_LeavesQty, leavesQty.doubleValue());
      }
      if (leavesQty != null)
      {
         msg.setValue(FixMessageFields.FIX_CumQty, cumQty.doubleValue());
      }
      /* 20110420 - Ruggero
       * AMOS needs the commissions on every partial fill
       * In the market execution reports the partial fill commissions amount has been stored in
       * the amountCommission (see the SendExecutionReportEventHandler)
       */
      boolean amountCommissionWanted = ((CustomerAttributes) order.getCustomer().getCustomerAttributes()).getAmountCommissionWanted();

      if(amountCommissionWanted)
      {
         msg.setValue(FixMessageFields.FIX_CommType, Commission.CommissionType.AMOUNT.getValue());
         if (executionReport.getAmountCommission() != null)
         {
            msg.setValue(FixMessageFields.FIX_Commission, executionReport.getAmountCommission().doubleValue());
         }
         else
         {
            LOGGER.warn("No commission amount found in the execution report, the customer requested it so we put 0 in the Commission fix field.");
            msg.setValue(FixMessageFields.FIX_Commission, BigDecimal.ZERO.doubleValue());
         }
      }
   }
}