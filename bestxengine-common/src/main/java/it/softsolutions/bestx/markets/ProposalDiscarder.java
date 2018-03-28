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
 * Date         : $Date: 2010-02-16 13:24:27 $
 * Header       : $Id: ProposalDiscarder.java,v 1.3 2010-02-16 13:24:27 anna.cochetti Exp $
 * Revision     : $Revision: 1.3 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/markets/ProposalDiscarder.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;

public interface ProposalDiscarder {
    
    public boolean isProposalDiscarded(Instrument instrument, ClassifiedProposal proposal, int errorCode);

    public MarketCode getMarketCode();

    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMarketMaker);

    public boolean okToSendBook(String isin);

    public boolean isEnabled();
}
