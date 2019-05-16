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
package it.softsolutions.bestx.connections.bloomberg.tsox;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.tradestac.api.TradeStacException;
import it.softsolutions.tradestac.fix50.TSBusinessMessageReject;
import quickfix.SessionID;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: davide.rossoni 
 * Creation date: 12/lug/2013
 * 
 **/
public interface TSOXConnection extends Connection {
    
    /**
     * Set the tsoxConnectionListener
     * 
     * @param tsoxConnectionListener the tsoxConnectionListener
     */
    void setTsoxConnectionListener(TSOXConnectionListener tsoxConnectionListener);
    
    void sendRfq(MarketOrder marketOrder) throws BestXException;

    void sendSubjectOrder(MarketOrder marketOrder) throws BestXException;
    
    void ackProposal(Proposal proposal) throws BestXException;

    void acceptProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException;

    void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException;
}
