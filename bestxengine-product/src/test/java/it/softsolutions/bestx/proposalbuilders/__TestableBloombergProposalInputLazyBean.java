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

import it.softsolutions.bestx.connections.bloomberg.BloombergProposalInputLazyBean;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.protocol.XT2Msg;

public class __TestableBloombergProposalInputLazyBean extends BloombergProposalInputLazyBean
{

   public __TestableBloombergProposalInputLazyBean(XT2Msg msg, ProposalSide side, long timeZone)
   {
      super(msg, side, timeZone);
   }
}
