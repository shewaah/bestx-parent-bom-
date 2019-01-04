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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * Purpose: Lazy bean needed to obtain new Amos protocol fields
 * 
 * Project Name : bestxengine-cs
 * First created by:
 * Creation date: 19-ott-2012.
 */
public class AmosFixRevokeResponseOutputLazyBean extends FixRevokeResponseOutputLazyBean {

   /**
    * Instantiates a new amos fix revoke response output lazy bean.
    *
    * @param fixSessionId the fix session id
    * @param orderId the order id
    * @param accept the accept
    * @param comment the comment
    * @param source the source
    */
   public AmosFixRevokeResponseOutputLazyBean(String fixSessionId, String orderId, boolean accept, String comment, Operation source) {
      super(fixSessionId, orderId, accept, comment);
      XT2Msg msg = super.getMsg();

      SimpleDateFormat amosDateTimeFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
      amosDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
      String commType = "3";
      String ordQty = source.getOrder().getQty().toPlainString();
      String execID = AmosUtility.getInstance().unwrapString(source.getIdentifier(OperationIdType.FIX_REVOKE_ID));
      String ordType = source.getOrder().getType().getFixCode();
      String description = source.getOrder().getInstrument().getDescription();
      orderId =  AmosUtility.getInstance().unwrapString(orderId);
      Date transactTime = DateService.newLocalDate();
      BigDecimal price = BigDecimal.ZERO;
      if (source.getOrder().getLimit() != null) {
         price = source.getOrder().getLimit().getAmount();
      }

      if (accept) {
         msg.setValue(FixMessageFields.FIX_AvgPx, "0.0");
         msg.setValue(FixMessageFields.FIX_Commission, "0");
         msg.setValue(FixMessageFields.FIX_CommType, commType);
         msg.setValue(FixMessageFields.FIX_CumQty, ordQty);
         msg.setValue(FixMessageFields.FIX_OrderQty, ordQty);
         msg.setValue(FixMessageFields.FIX_ExecID, execID);
         msg.setValue(FixMessageFields.FIX_ExecTransType, "0");
         msg.setValue(FixMessageFields.FIX_IDSource, "4");
         msg.setValue(FixMessageFields.FIX_LastPx, "0.0");
         msg.setValue(FixMessageFields.FIX_LastShares, "0.0");
         msg.setValue(FixMessageFields.FIX_OrdStatus, "4");
         msg.setValue(FixMessageFields.FIX_OrdType, ordType);
         msg.setValue(FixMessageFields.FIX_Symbol, description);
         msg.setValue(FixMessageFields.FIX_Text, "Revoca accettata");
         msg.setValue(FixMessageFields.FIX_TimeInForce, "0");
         msg.setValue(FixMessageFields.FIX_TransactTime, amosDateTimeFormatter.format(transactTime));
         msg.setValue(FixMessageFields.FIX_ExecType, "4");
         msg.setValue(FixMessageFields.FIX_LeavesQty, "0.0");
         msg.setValue(FixMessageFields.FIX_Price, price.toPlainString());  
         msg.setValue(FixMessageFields.FIX_SecurityID, source.getOrder().getInstrument().getIsin());
         msg.setValue(FixMessageFields.FIX_Side, source.getOrder().getSide().getFixCode());
         msg.setValue(FixMessageFields.FIX_OrigClOrdID, orderId);
      } else {
         msg.setValue(FixMessageFields.FIX_Text, "Revoca non accettata - Order not in a revocable state");
      }
      msg.setValue(FixMessageFields.FIX_OrderID, orderId);
      msg.setValue(FixMessageFields.FIX_ClOrdID, orderId);

   }

}
