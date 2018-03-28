/*
 * Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.connections.tradestac2;

import java.util.Set;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;

/**
 * This interface contains methods relative to the events (from MDS platform)
 * that we want to intercept.
 * 
 * @author Davide Rossoni
 */

/*
 * 
 * Purpose: this class is mainly to contain the interface to tradestac implementation of market - pretrade messages
 * 
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 */
public interface TradeStacPreTradeConnectionListener extends TradeStacConnectionListener {

    // void OnInstrumentPrice(TSBusinessMessageReject tsBusinessMessageReject);

    /**
     * Notifies new marketDatas, identified by BID and ASK classifiedProposals
     * related to the specified instrument
     * 
     * @param instrument
     *            the instrument related to the notified marketData
     * @param askClassifiedProposal
     *            the ASK classifiedProposal
     * @param bidClassifiedProposal
     *            the BID classifiedProposal
     */
    void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal);

    
    /**
     * Notifies that we have received all the securities for all the authorized
     * product groups.
     * 
     * @param securityList
     *            the list of securities received from MDServer.
     */
    void onSecurityListCompleted(Set<String> securityList);
}
