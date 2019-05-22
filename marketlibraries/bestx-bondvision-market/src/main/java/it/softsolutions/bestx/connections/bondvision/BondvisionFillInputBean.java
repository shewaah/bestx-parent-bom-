/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-01-15 08:59:28 $
 * Header       : $Id: BondvisionFillInputBean.java,v 1.1 2010-01-15 08:59:28 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/bondvision/BondvisionFillInputBean.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.connections.bondvision;

import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;

public class BondvisionFillInputBean extends XT2InputLazyBean
{
   public long getExecutionReportId() throws Exception
   {
      return msg.getLong("TradeNo");
   }
   public BigDecimal getPrice() throws Exception
   {
      return new BigDecimal(msg.getDouble("Price"));
   }
   public String getCounterpartMember()
   {
      return msg.getString("CounterpartMember");
   }
   
   BondvisionFillInputBean(XT2Msg msg)
   {
      this.msg = msg;
   }
}
