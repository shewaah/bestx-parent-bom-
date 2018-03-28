/*
 * Project Name : BestXEngine_Akros
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
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;

public interface MarketProposalAggregator {
    
    void addBookListener(Instrument instrument, MarketPriceConnectionListener bookListener);

    void onProposal(Instrument instrument, ClassifiedProposal proposal, MarketPriceConnection marketPriceConnection, int errorCode, String errorMsg, boolean forceCompleteBook) throws BestXException;

    void onBookError(String isin, MarketPriceConnection marketPriceConnection);

    void onTimerExpired(MarketPriceConnection marketPriceConnection, String jobName);

}