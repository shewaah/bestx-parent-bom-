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
package it.softsolutions.bestx.connections.bloomberg.blp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.bloomberg.BloombergConnection;
import it.softsolutions.bestx.connections.bloomberg.BloombergConnectionListener;
import it.softsolutions.bestx.connections.bloomberg.BloombergProposalInputLazyBean;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.tradestac.api.ConnectionStatus;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: fabrizio.aponte 
* Creation date: 22/mag/2012 
* 
**/
public class BloombergToBLPTranslator implements BloombergConnectionListener, TradeStacPreTradeConnection, ConnectionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(BloombergToBLPTranslator.class);

	private static final String connectionName = MarketCode.BLOOMBERG.toString();
	
	private TradeStacPreTradeConnectionListener blpConnectionListener;
	private InstrumentFinder instrumentFinder;
	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	private MarketFinder marketFinder;
	
	private BloombergConnection bloombergConnection;
	private ConnectionHelper bloombergConnectionHelper;
	private BLPHelper blpHelper;
	
	/**
	 * Constructor
	 */
	public BloombergToBLPTranslator() {
	    
    }

	/**
	 * Initializes the {@code BloombergToBLPTranslator} 
	 */
	public void init()  {
		// check prerequisites 
		if (blpConnectionListener == null) {
			throw new ObjectNotInitializedException("blpConnectionListener not set");
		}
		if (instrumentFinder == null) {
			throw new ObjectNotInitializedException("instrumentFinder not set");
		}
		if (marketMakerFinder == null) {
			throw new ObjectNotInitializedException("marketMakerFinder not set");
		}
		if (venueFinder == null) {
			throw new ObjectNotInitializedException("venueFinder not set");
		}
		if (marketFinder == null) {
			throw new ObjectNotInitializedException("marketFinder not set");
		}
		
		if (bloombergConnection == null) {
			throw new ObjectNotInitializedException("Bloomberg Connection not set");
		}
		if (bloombergConnectionHelper == null) {
			throw new ObjectNotInitializedException("Bloomberg Connection helper not set");
		}
		
		bloombergConnectionHelper.setConnection(bloombergConnection);
		bloombergConnectionHelper.setConnectionListener(this);
		
		blpHelper = new BLPHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);
	}
	
	@Override
	public void onConnectionStatusChange(boolean marketStatus, boolean userStatus) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onInstrumentPrice(BloombergProposalInputLazyBean askBloomProposal, BloombergProposalInputLazyBean bidBloomProposal) {
		LOGGER.trace("askProposal = {}, bidProposal = {}", askBloomProposal, bidBloomProposal);
		
		if (askBloomProposal == null || bidBloomProposal == null) {
			throw new IllegalArgumentException("bloomProposal can't be null");
		}

		if (askBloomProposal.getIsin() != null && !askBloomProposal.getIsin().equals(bidBloomProposal.getIsin())) {
			LOGGER.error("Inconsistent isin codes: ask {}, bid {}", askBloomProposal.getIsin(), bidBloomProposal.getIsin());
			throw new IllegalArgumentException("Inconsistent isin codes: ask " + askBloomProposal.getIsin() + ", bid " + bidBloomProposal.getIsin());
		}

		try {
			Instrument instrument = blpHelper.getInstrument(askBloomProposal.getIsin());

			ClassifiedProposal bidClassifiedProposal = blpHelper.getClassifiedProposal(bidBloomProposal, instrument);
			ClassifiedProposal askClassifiedProposal = blpHelper.getClassifiedProposal(askBloomProposal, instrument);

			// finally invoke the BLP listener
			blpConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

		} catch (BLPException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
    	this.instrumentFinder = instrumentFinder;
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

	@Override
    public String getConnectionName() {
	    return MarketCode.BLOOMBERG.toString();
    }

	@Override
    public void connect() throws BestXException {
		bloombergConnectionHelper.connect();	    
    }

	@Override
    public void disconnect() throws BestXException {
		bloombergConnectionHelper.disconnect();
    }

	@Override
    public boolean isConnected() {
	    return bloombergConnection.isConnected();
    }

	@Override
    public void setConnectionListener(ConnectionListener listener) {
		throw new UnsupportedOperationException();
    }

    @Override
    public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener blpConnectionListener) {
		this.blpConnectionListener = blpConnectionListener;
    }

	@Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {
		bloombergConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCode);	    
    }

	@Override
    public void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodes) throws BestXException {
		for (String marketMakerCode : marketMakerCodes) {
			bloombergConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCode);
        }
    }

	@Override
    public void onConnection(Connection source) {
		blpConnectionListener.onClientConnectionStatusChange(connectionName, ConnectionStatus.Connected);
		blpConnectionListener.onMarketConnectionStatusChange(connectionName, ConnectionStatus.Connected);
    }

	@Override
    public void onDisconnection(Connection source, String reason) {
		blpConnectionListener.onClientConnectionStatusChange(connectionName, ConnectionStatus.NotConnected);
    }

	public void setBloombergConnection(BloombergConnection bloombergConnection) {
    	this.bloombergConnection = bloombergConnection;
    }

	public void setBloombergConnectionHelper(ConnectionHelper bloombergConnectionHelper) {
    	this.bloombergConnectionHelper = bloombergConnectionHelper;
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
