/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: paolo.midali 
 * Creation date: 11/feb/2013 
 * 
 **/
public class MultipleQuotesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleQuotesHandler.class);

    public MultipleQuotesHandler() {
        ;
    }

    private String externalQuoteReqID;
    private ClassifiedProposal externalQuote;

    private void setExternalQuoteReqID(String quoteReqID) {
        this.externalQuoteReqID = quoteReqID;
    }

    public ClassifiedProposal getUpdatedExternalQuote() {
        return externalQuote;
    }

    public synchronized void manageQuoteStatusChange(MarketCode mkt, String quoteID, Proposal.ProposalType proposalStatus) throws BestXException {
        if ( (quoteID == null) || (quoteID.isEmpty()) ) {
            throw new BestXException("Null/empty quoteID");
        }
        if (mkt == null) {
            throw new BestXException("Null/empty Market");
        }

        ClassifiedProposal currentExternalQuote = getUpdatedExternalQuote();
        if ( (currentExternalQuote != null) && 
             (currentExternalQuote.getMarket().getMarketCode() == mkt) && 
             (currentExternalQuote.getSenderQuoteId() != null) && 
             (currentExternalQuote.getSenderQuoteId().equals(quoteID)) ) {
            LOGGER.info("[MktMsg] Updating status to {} for external quoteID {}, mkt {}", proposalStatus, quoteID, mkt);
            currentExternalQuote.setType(proposalStatus);
            return;
        }
                
        LOGGER.info("No match for quote status {} for quoteID {}, mkt {}", proposalStatus, quoteID, mkt);
        
    }

    public synchronized void manageQuote(ClassifiedProposal proposal, String callingStateName) throws BestXException {
        if (proposal.getMarket() == null) {
            throw new BestXException("Proposal has a null market");
        }

        MarketCode mkt = proposal.getMarket().getMarketCode();
        if ( !isMarketValid(mkt) ) {
            throw new BestXException("Unsupported market: " + mkt);
        }

        String quoteReqID = proposal.getQuoteReqId();
        String quoteID = proposal.getSenderQuoteId();

        verifyQuoteID(mkt, quoteID);
        verifyQuoteReqID(quoteReqID);

        LOGGER.info("Received new proposal, quoteReqID={}, quoteID={}, mkt={}", quoteReqID, quoteID, mkt);

        if (externalQuoteReqID == null) {
            // set first external quoteRedID
            LOGGER.debug("Received external quote, quoteReqID={}, quoteID={}, state={}, setting externalQuoteReqID={}", quoteReqID, quoteID, callingStateName, quoteReqID);
            setExternalQuoteReqID(quoteReqID);
        }

        if (!externalQuoteReqID.equals(quoteReqID)) {
            LOGGER.warn("Received external quote with quoteReqID={} different from expected={}, quoteID={}, state={}, ignoring it", quoteReqID, externalQuoteReqID, quoteID, callingStateName);
            return;
        }

        String currentExternalQuoteID = ( externalQuote == null ? null : externalQuote.getSenderQuoteId() );  
        if ( isNewer(mkt, quoteID, currentExternalQuoteID) ) {
            LOGGER.info("[MktMsg] Updating external quote, quoteReqID={}, quoteID={}, state={}", quoteReqID, quoteID, callingStateName);
            externalQuote = proposal;
        }
        else {
            LOGGER.info("External quote older than incoming, ignoring it, quoteReqID={}, quoteID={}, state={}", quoteReqID, quoteID, callingStateName);
        }
    }    

    private void verifyQuoteReqID(String quoteReqID) throws BestXException {
        if (quoteReqID == null) {
            throw new BestXException("QuoteReqID is null");
        }
        if (quoteReqID.isEmpty()) {
            throw new BestXException("QuoteReqID is empty");
        }
    }

    protected void verifyQuoteID(MarketCode mktCode, String quoteID) throws BestXException {
        if (mktCode == null) {
            throw new BestXException("MarketCode is null");
        }
        if (!isMarketValid(mktCode)) {
            throw new BestXException("MarketCode is not valid: " + mktCode);
        }
        if (quoteID == null) {
            throw new BestXException("QuoteID is null");
        }
        if (quoteID.isEmpty()) {
            throw new BestXException("QuoteID is empty");
        }
    }

    protected boolean isNewer(MarketCode mktCode, String newQuoteID, String oldQuoteID) throws BestXException {
        if (!isMarketValid(mktCode)) {
            throw new BestXException("MarketCode is not valid: " + mktCode);
        }

        if (newQuoteID == null) {
            throw new BestXException("New QuoteID is null");
        }
        if (newQuoteID.isEmpty()) {
            throw new BestXException("New QuoteID is empty");
        }
        // FIXME AMC manage this in the scenario of quote stream for BV
        return true;

    }

    private boolean isMarketValid(MarketCode mktCode) {
        if (mktCode == null) {
            return false;
        }

        switch (mktCode) {
        case BLOOMBERG:
            return true;
        default:
            return false;
        }

    }
}
