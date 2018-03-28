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

import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class BloombergProposalInputLazyBeanTest
{
   @Test
   public void testGetTimeStamp()
   {
      XT2Msg fakeMsg = __FakeXT2MsgBuilderForBBG.getFakeBBGXT2Msg();
      long nyTimeZone = 21600000;
      __TestableBloombergProposalInputLazyBean bbgProposal = new __TestableBloombergProposalInputLazyBean(fakeMsg, ProposalSide.ASK, nyTimeZone);
      
      Calendar testCalendar = Calendar.getInstance();
      //months start from 0, so 10 = November
      testCalendar.set(2011, 10, 10, 12, 36, 8);
      Date orderDate = testCalendar.getTime();
      System.out.println("(BBG timestamp) Order time " + orderDate);
      
      Date proposalDate = bbgProposal.getTimeStamp();
      System.out.println("(BBG timestamp) Proposal time " + proposalDate);
      long deltaInMillis = Math.abs(proposalDate.getTime() - orderDate.getTime());
      //tolerate 5 seconds of difference between MOT price and order timestamps
      boolean propAfterOrder = deltaInMillis <= 5000;
      
      org.junit.Assert.assertTrue("(BBG timestamp) Proposal timestamp differs from the order timestamp for more than 5 seconds", propAfterOrder);
   }
}

