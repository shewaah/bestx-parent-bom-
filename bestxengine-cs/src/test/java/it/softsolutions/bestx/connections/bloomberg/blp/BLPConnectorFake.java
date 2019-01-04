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
package it.softsolutions.bestx.connections.bloomberg.blp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.api.ConnectionStatus;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 22/lug/2013 
 * 
 **/
public class BLPConnectorFake implements TradeStacPreTradeConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(BLPConnectorFake.class);
    private MarketFinder marketFinder;
    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;
    
    private TradeStacPreTradeConnection blpConnection;
    private TradeStacPreTradeConnectionListener blpConnectionListener;
    
    /**
	 * @return the blpConnection
	 */
	public TradeStacPreTradeConnection getBlpConnection() {
		return blpConnection;
	}

	/**
	 * @param blpConnection the blpConnection to set
	 */
	public void getTradeStacPreTradeConnectionListener(TradeStacPreTradeConnection blpConnection) {
		this.blpConnection = blpConnection;
		this.blpConnectionListener = blpConnection.getTradeStacPreTradeConnectionListener();
	}

	@Override
    public String getConnectionName() {
        LOGGER.info("");
        return Market.MarketCode.BLOOMBERG.name();
    }

    @Override
    public void connect() throws BestXException {
        LOGGER.info("");
        blpConnectionListener.onMarketConnectionStatusChange(Market.MarketCode.BLOOMBERG.name(), ConnectionStatus.Connected);
        
        blpConnection.connect();
    }

    @Override
    public void disconnect() throws BestXException {
        LOGGER.info("");
        
        blpConnection.disconnect();
    }

    @Override
    public boolean isConnected() {
        LOGGER.info("");
        return true;
    }

    @Override
    public void setConnectionListener(ConnectionListener listener) {
        LOGGER.info("************ unexpected call");
        LOGGER.info("");
    }

    @Override
    public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener blpConnectionListener) {
        this.blpConnectionListener = blpConnectionListener;
        
        blpConnection.setTradeStacPreTradeConnectionListener(blpConnectionListener);
    }

    @Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {
        LOGGER.info("************ unexpected call");

    }

    @Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodes) throws BestXException {
        LOGGER.debug("instrument = {}, marketMakerCodes = {}", instrument, marketMakerCodes);
        
        ClassifiedProposal bidp1 = null;
        ClassifiedProposal askp1 = null;
        ClassifiedProposal bidp2 = null;
        ClassifiedProposal askp2 = null;
        String isin = instrument.getIsin();

        // uso policy BPipeTest, su cui sono abilitati DAB (Bloomberg) , TEST21 (Bloomberg e RTFI), TEST22..25 (RTFI)
        // to make Bloomberg best
        // to make bloomberg best, far from rtfi, so that rtfi is discarded and no new price discovery is executed
        if (isin.startsWith("BB000"))                        
        {
            int instrumentNumber = Integer.parseInt(isin.substring(2));
            double qtyToUse = 100000;
            // BB0000000101 : needs books with null qtys
            if (instrumentNumber == 101) {
                qtyToUse = 0.0;
            }
            
            bidp1 = getProposal("RDEU", new BigDecimal(qtyToUse), BigDecimal.valueOf(105.5001234), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            askp1 = getProposal("RDEU", new BigDecimal(qtyToUse), BigDecimal.valueOf(105.6), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            
            bidp2 = getProposal("TSB21", BigDecimal.ZERO, BigDecimal.valueOf(105.4), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            askp2 = getProposal("TSB21", BigDecimal.ZERO, BigDecimal.valueOf(105.7), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            
            // to make bloomberg best, close enough to rtfi, so that rtfi is accepted and new price discovery is executed
            //        ClassifiedProposal bidp1 = getProposal("DAB", new BigDecimal(1000000), new BigDecimal(105.1), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            //        ClassifiedProposal askp1 = getProposal("DAB", new BigDecimal(1000000), new BigDecimal(106.0), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            //        ClassifiedProposal bidp2 = getProposal("TSB21", BigDecimal.ZERO, new BigDecimal(105.4), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            //        ClassifiedProposal askp2 = getProposal("TSB21", BigDecimal.ZERO, new BigDecimal(105.7), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            // to make bloomberg worst
            //        ClassifiedProposal bidp1 = getProposal("DAB", new BigDecimal(1000000), new BigDecimal(205.5), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            //        ClassifiedProposal askp1 = getProposal("DAB", new BigDecimal(1000000), new BigDecimal(205.6), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            //      ClassifiedProposal bidp2 = getProposal("TSB21", BigDecimal.ZERO, new BigDecimal(05.4), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            //      ClassifiedProposal askp2 = getProposal("TSB21", BigDecimal.ZERO, new BigDecimal(05.7), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            
            blpConnectionListener.onClassifiedProposal(instrument, bidp1, askp1);
            blpConnectionListener.onClassifiedProposal(instrument, bidp2, askp2);
        } else if (isin.equals("DE0001135267")) {
        	double quantity = 5000000.0;
        	
        	// 104.151 - 104.169
            bidp1 = getProposal("D1", BigDecimal.valueOf(quantity), BigDecimal.valueOf(104.151), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            askp1 = getProposal("D1", BigDecimal.valueOf(quantity), BigDecimal.valueOf(104.169), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
            
            // 104.153 - 104.172
            bidp2 = getProposal("D2", BigDecimal.valueOf(quantity), BigDecimal.valueOf(104.153), ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
            askp2 = getProposal("D2", BigDecimal.valueOf(quantity), BigDecimal.valueOf(104.172), ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
        	
            blpConnectionListener.onClassifiedProposal(instrument, bidp1, askp1);
            blpConnectionListener.onClassifiedProposal(instrument, bidp2, askp2);
        } else if (isin.equals("XS0169888558")) {
        	Double quantity = 50000.0;
        	cycle = (cycle++ >= 10) ? 0 : cycle;
        	Double price = 95.0 + cycle;
        	
        	for (String marketMakerCode : marketMakerCodes) {
        		BigDecimal bidPrice = BigDecimal.valueOf(price + 0.063 * (1 + random.nextInt(10))).setScale(3, RoundingMode.HALF_UP);
        		BigDecimal askPrice = BigDecimal.valueOf(bidPrice.doubleValue() + 0.054 * (1 + random.nextInt(10))).setScale(3, RoundingMode.HALF_UP);
        		BigDecimal bidQty = BigDecimal.valueOf(quantity * (1 + random.nextInt(20))).setScale(0, RoundingMode.HALF_UP);
        		BigDecimal askQty = BigDecimal.valueOf(quantity * (1 + random.nextInt(20))).setScale(0, RoundingMode.HALF_UP);
        		LOGGER.info("[{}] {}@{} - {}@{}", marketMakerCode, bidPrice, bidQty, askPrice, askQty);
        		
        		bidp1 = getProposal(marketMakerCode, bidQty, bidPrice, ProposalSide.BID, instrument.getBBSettlementDate(), instrument.getCurrency());
        		askp1 = getProposal(marketMakerCode, askQty, askPrice, ProposalSide.ASK, instrument.getBBSettlementDate(), instrument.getCurrency());
        		
        		blpConnectionListener.onClassifiedProposal(instrument, bidp1, askp1);
            }
        	
        } else {
        	LOGGER.info("Request sent to proxy: {}, {}", instrument, marketMakerCodes);
        	blpConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCodes);
            return;
        }
    }
    
    private Random random = new Random(System.currentTimeMillis());
    private int cycle = 0;


    private ClassifiedProposal getProposal(String mmm, BigDecimal qty, BigDecimal price, ProposalSide side, Date settlDate, String currency) throws BestXException {
        Market market = marketFinder.getMarketByCode(MarketCode.BLOOMBERG, null);
        MarketMarketMaker marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.BLOOMBERG, mmm);
        Venue venue = getVenueFinder().getMarketMakerVenue(marketMarketMaker.getMarketMaker());
        Venue bloombergVenue = new Venue(venue);
        bloombergVenue.setMarket(market);

        ClassifiedProposal classifiedProposal =  new ClassifiedProposal();
        classifiedProposal.setMarket(market);
        classifiedProposal.setMarketMarketMaker(marketMarketMaker);
        classifiedProposal.setVenue(bloombergVenue);

        classifiedProposal.setSide(side);
        classifiedProposal.setProposalState(ProposalState.NEW);
        classifiedProposal.setQty(qty);
        classifiedProposal.setType(ProposalType.INDICATIVE);
        //        if (qty.equals(BigDecimal.ZERO)) {
        //            classifiedProposal.setType(ProposalType.INDICATIVE);
        //        }
        //        else {
        //            classified Proposal.setType(ProposalType.TRADEABLE);
        //        }

        if (settlDate != null) {
            classifiedProposal.setFutSettDate(settlDate);
        }
        else {
            classifiedProposal.setFutSettDate(DateUtils.addDays(new Date(), 5));
        }
        classifiedProposal.setPrice(new Money(currency, price));

        return classifiedProposal;
    }

    public MarketFinder getMarketFinder() {
        return marketFinder;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    public MarketMakerFinder getMarketMakerFinder() {
        return marketMakerFinder;
    }

    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    public VenueFinder getVenueFinder() {
        return venueFinder;
    }

    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

	@Override
    public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
	    return blpConnectionListener;
    }

	@Override
	public void requestInstrumentStatus() throws BestXException {
		LOGGER.error("call to unimplemented method requestInstrumentStatus");
	}

}
 
 
