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
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.proposalbuilders;

import it.softsolutions.xt2.protocol.XT2Msg;

public class __FakeXT2MsgBuilderForBBG
{
   
   public static XT2Msg getFakeBBGXT2Msg()
   {
      /*
       * PduName=BOOK,CLASS_NAME=BOOK,
       * SourceMarketName=BLOOMBERG,Date=20111110,
       * ErrorMsg=,TimeStr=6:36:08,
       * XT2TS=123609529,UserSessionName=MIFID,
       * LastPrice=92.406,DeltaTS=60001529,
       * Ask1Q=1.0E7,Ask1P=95.456,ErrorCode=0,
       * Time=63608000,Bid1Q=1.0E7,
       * Bid1P=92.406,Volume=5246.0,
       * $IBMessTy__=1,MARKET=BLOOM,
       * LastTime=6:36:08,ISIN=IT0004750409,
       * $IBSubj____=/BLOOMBERG/BOOK/IT0004750409@GS
       */
      
      XT2Msg fakeMsg = new XT2Msg();
      fakeMsg.setName("BOOK");
      fakeMsg.setSubject("/BLOOMBERG/BOOK/IT0004750409@GS");

      fakeMsg.setValue("CLASS_NAME", "BOOK");

      fakeMsg.setValue("TimeStr", "6:36:08");
      fakeMsg.setValue("XT2TS", 123609529);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("LastPrice", 92.406);
      fakeMsg.setValue("DeltaTS", "60001529");
      fakeMsg.setValue("Ask1Q", 1.0E7);
      fakeMsg.setValue("Ask1P", 95.456);
      fakeMsg.setValue("ErrorCode", 0);
      try
      {
         fakeMsg.setValue("Date", 20111110L);
         fakeMsg.setValue("Time", 63608000L);
      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      fakeMsg.setValue("Bid1Q", 1.0E7);
      fakeMsg.setValue("Bid1P", 92.406);
      fakeMsg.setValue("Volume", 5246.0);
      fakeMsg.setValue("$IBMessTy__", 1);
      fakeMsg.setValue("TimeStr", 1);
      fakeMsg.setValue("LastTime", "6:36:08");
      fakeMsg.setValue("ISIN", "IT0004750409");
      fakeMsg.setValue("TimeStr", "6:36:08");
      fakeMsg.setSourceMarketName("BLOOMBERG");
      
      return fakeMsg;
   }

}
