/**
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.connections.tradeweb.simulated;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.connections.tradeweb.mds.MDSException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.MDEntryType;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: davide.rossoni
 * Creation date: 30/gen/2015
 * 
 */
public class BogusMDSConnection implements TradeStacPreTradeConnection {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(BogusMDSConnection.class);
	
	private TradeStacPreTradeConnectionListener mdsConnectionListener;
	
    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;
    private MarketFinder marketFinder;
    
    public void init() {
    	
    	new Thread() {
    		@Override
    		public void run() {
    			try { Thread.sleep(400); } catch (InterruptedException e) { }
    			
    			mdsConnectionListener.onMarketConnectionStatusChange(getConnectionName(), ConnectionStatus.Connected);    	
    		}
    	}.start();
    }

	@Override
    public String getConnectionName() {
		return Market.MarketCode.TW + "#mds";
    }

	@Override
    public void connect() throws BestXException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public void disconnect() throws BestXException {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public boolean isConnected() {
		return true;
    }

	@Override
    public void setConnectionListener(ConnectionListener listener) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
	    return mdsConnectionListener;
    }

	@Override
    public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener mdsConnectionListener) {
		this.mdsConnectionListener = mdsConnectionListener;	    
    }

	@Override
    public void requestInstrumentStatus() throws BestXException {
		LOGGER.debug("");
    }

	@Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {
		LOGGER.debug("instrument = {}, marketMakerCode = {}", instrument, marketMakerCode);
        
		ClassifiedProposal askClassifiedProposal = getClassifiedProposal(MDEntryType.Bid, marketMakerCode, instrument);
		ClassifiedProposal bidClassifiedProposal = getClassifiedProposal(MDEntryType.Offer, marketMakerCode, instrument);
		
		mdsConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);
    }

	@Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodes) throws BestXException {
		LOGGER.debug("instrument = {}, marketMakerCodes = {}", instrument, marketMakerCodes);
        
	    for (String marketMakerCode : marketMakerCodes) {
	    	requestInstrumentPriceSnapshot(instrument, marketMakerCode);
        }
    }

	private ClassifiedProposal getClassifiedProposal(MDEntryType mdEntryType, String marketMakerCode, Instrument instrument) {
		LOGGER.debug("mdEntryType = {}, dealerCode = {}, instrument = {}", mdEntryType, marketMakerCode, instrument);
        
        ClassifiedProposal classifiedProposal = null;

        try {

            MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.TW, marketMakerCode);
            if (marketMarketMaker == null) {
                throw new MDSException("No marketMarketMaker found for dealerCode [" + marketMakerCode + "]");
            }

            Venue venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                throw new MDSException("No venue found for dealerCode [" + marketMakerCode + "]");
            }
            if (venue.getMarket() == null) {
                throw new MDSException("market is null for the venue retrieved with dealerCode [" + marketMakerCode + "]");
            }

            SubMarketCode subMarketCode = null;
            Market market = marketFinder.getMarketByCode(MarketCode.TW, subMarketCode);
            if (market == null) {
                throw new MDSException("No market found for dealerCode [" + MarketCode.TW + "]");
            }

            // --- Fill the proposal ---
            ProposalSide proposalSide = null;
            switch (mdEntryType) {
            case Bid:
                proposalSide = ProposalSide.BID;
                break;
            case Offer:
                proposalSide = ProposalSide.ASK;
                break;
            default:
                throw new MDSException("Unsupported mdEntryType [" + mdEntryType + "]");
            }

            BigDecimal qty = new BigDecimal(1000000.0);
            BigDecimal amount = new BigDecimal(new Double(100.35));

            ProposalType proposalType = ProposalType.INDICATIVE;
            String reason = null;

            // [DR20120504] TODO is the venue.copyOf necessary ?
            Venue bloombergVenue = new Venue(venue);
            bloombergVenue.setMarket(market);

            classifiedProposal = new ClassifiedProposal();
            classifiedProposal.setMarket(market);
            classifiedProposal.setMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setVenue(bloombergVenue);
            classifiedProposal.setProposalState(ProposalState.NEW);
            classifiedProposal.setType(proposalType);
            classifiedProposal.setSide(proposalSide);
            classifiedProposal.setQty(qty);
            classifiedProposal.setFutSettDate(instrument.getBBSettlementDate());
            classifiedProposal.setNonStandardSettlementDateAllowed(false);
            classifiedProposal.setNativeMarketMarketMaker(marketMarketMaker);

            Date timestamp = new Date();
            classifiedProposal.setTimestamp(timestamp);

            Money price = new Money(instrument.getCurrency(), amount);
            classifiedProposal.setPrice(price);

            LOGGER.info("{} {} {} {} {} {} {} {} {}", instrument.getIsin(), market.getMarketCode(), marketMarketMaker.getMarketSpecificCode(), classifiedProposal.getProposalState().name(), proposalType.name(), mdEntryType, amount, qty, reason);
        } catch (Exception e) {
            LOGGER.error("{}", e.getMessage(), e);
        }

        return classifiedProposal;
    }
	
    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

}
