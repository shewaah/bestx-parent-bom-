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
 * Date         : $Date: 2009-03-06 12:24:16 $
 * Header       : $Id: HIMTFFokOrderOutputBean.java,v 1.1 2009-03-06 12:24:16 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/himtf/HIMTFFokOrderOutputBean.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.connections.himtf;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_EX_DESTINATION;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.MTF_DESTINATION_VALUE;
import it.softsolutions.bestx.connections.regulated.FokOrderOutputBean;
import it.softsolutions.bestx.model.MarketOrder;

public class HIMTFFokOrderOutputBean extends FokOrderOutputBean
{
   private static final String LABEL_PRICE_TYPE = "PriceType";
   private static final String PERCENTAGE_PRICE_TYPE = "1";
   private static final String LABEL_ORDER_CAPACITY = "OrderCapacity";
   private static final String INDIVIDUAL_ORDER_CAPACITY = "I";
   private static final String LABEL_SYMBOL = "Symbol";
   private static final String LABEL_HANDLE_INSTRUCTIONS = "HandlInst";
   private static final String THREE = "3";

   
   
   public HIMTFFokOrderOutputBean(MarketOrder marketOrder, String marketCode, String subMarketCode, String account, String sessionId) {
      super(marketOrder, marketCode, subMarketCode, account, sessionId);
      fokOrderMessage.setValue(LABEL_EX_DESTINATION, MTF_DESTINATION_VALUE);
      fokOrderMessage.setValue(LABEL_PRICE_TYPE, PERCENTAGE_PRICE_TYPE);
      fokOrderMessage.setValue(LABEL_ORDER_CAPACITY, INDIVIDUAL_ORDER_CAPACITY);
      fokOrderMessage.setValue(LABEL_SYMBOL, marketOrder.getInstrument().getIsin());
      fokOrderMessage.setValue(LABEL_HANDLE_INSTRUCTIONS, THREE);
   }
}
