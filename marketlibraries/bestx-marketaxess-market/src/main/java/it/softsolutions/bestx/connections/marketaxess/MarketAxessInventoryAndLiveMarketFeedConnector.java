
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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

package it.softsolutions.bestx.connections.marketaxess;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketPriceDiscoveryHelper.LiteMarketDataRequest;
import it.softsolutions.bestx.connections.tradestac2.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac2.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.BusinessRejectRefID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.MDEntryType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.RefMsgType;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.BusinessMessageReject;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.MarketDataRequest;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.MarketDataSnapshotFullRefresh;
import it.softsolutions.marketlibraries.quickfixjrootobjects.fields.Text;
import it.softsolutions.marketlibraries.quickfixjrootobjects.groups.RootNoMDEntries;
import it.softsolutions.tradestac2.api.TradeStacApplicationCallback;
import it.softsolutions.tradestac2.api.TradeStacException;
import it.softsolutions.tradestac2.client.TradeStacSessionCallback;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;


/**
 *
 * Purpose: this class is to connect the price feed of MarketAxess
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 11 gen 2017
 * 
 **/

public class MarketAxessInventoryAndLiveMarketFeedConnector extends Tradestac2MarketAxessConnection implements TradeStacPreTradeConnection, TradeStacApplicationCallback, TradeStacSessionCallback {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketAxessInventoryAndLiveMarketFeedConnector.class);


	public MarketAxessInventoryAndLiveMarketFeedConnector() {
		super("MARKET_AXESS_BUY_SIDE#marketAxessFIX50");
	}


	/**
	 * @param connectionName ignored 
	 */
	public MarketAxessInventoryAndLiveMarketFeedConnector(String connectionName) {
		super(connectionName);
	}

	private TradeStacPreTradeConnectionListener preTradeConnectionListener;

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.TradeStacPreTradeConnection#getTradeStacPreTradeConnectionListener()
	 */
	@Override
	public TradeStacPreTradeConnectionListener getTradeStacPreTradeConnectionListener() {
		return this.preTradeConnectionListener;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.TradeStacPreTradeConnection#setTradeStacPreTradeConnectionListener(it.softsolutions.bestx.connections.TradeStacPreTradeConnectionListener)
	 */
	@Override
	public void setTradeStacPreTradeConnectionListener(TradeStacPreTradeConnectionListener connectionListener) {
		this.preTradeConnectionListener = connectionListener;

	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.connections.TradeStacPreTradeConnection#requestInstrumentPriceSnapshot(it.softsolutions.bestx.model.Instrument, java.util.List)
	 */
	@Override
	public void requestInstrumentPriceSnapshot(Instrument instrument, List<MarketMarketMakerSpec> marketMakerCodes)
			throws BestXException {
		LOGGER.info("{}, {}", instrument, marketMakerCodes);

		if (instrument == null || marketMakerCodes == null) {
			throw new IllegalArgumentException("Params can't be null");
		}

		if (!isConnected()) {
			throw new BestXException("Not connected");
		}

		MarketDataRequest marketDataRequest = this.marketAxessHelper.createMarketDataRequest(instrument, marketMakerCodes);
		try {
			this.marketAxessHelper.addLiteMarketDataRequest(marketDataRequest.getMDReqID().getValue(), marketDataRequest.getMDReqID().getValue(), marketMakerCodes, instrument);
			this.marketAxessHelper.addLiteMarketDataRequest(marketDataRequest.getMDReqID().getValue(), instrument.getIsin(), marketMakerCodes, instrument);
			tradeStacClientSession.manageMarketDataRequest(marketDataRequest);
		} catch (Exception e) {
			throw new BestXException(String.format("Error managing marketDataRequest [%s]", marketDataRequest), e);
		}
	}

	@Override
	public void onBusinessMessageReject(SessionID sessionID, Message message, Message relatedMessage)
			throws TradeStacException {
		LOGGER.info("received business message reject: {}", message);
		BusinessMessageReject tsBusinessMessageReject = (BusinessMessageReject) message;
		String errMsg = "";
		try {
			if(!"V".equalsIgnoreCase(tsBusinessMessageReject.getString(RefMsgType.FIELD)))
				super.onBusinessMessageReject(sessionID, tsBusinessMessageReject, relatedMessage);
			else {
				try {
					errMsg = tsBusinessMessageReject.getString(Text.FIELD);
				} catch(Exception e) {
				}
				String requestId = null;
				try {
					requestId = tsBusinessMessageReject.getString(BusinessRejectRefID.FIELD);
				} catch(Exception e) {}
				// get the related request
				if(requestId!= null) {
					LiteMarketDataRequest liteMarketDataRequest = marketAxessHelper.getLiteMarketDataRequest(requestId);
					// create all the ClassifiedProposals with this information
					if(liteMarketDataRequest == null) return;
					Instrument instrument = liteMarketDataRequest.getInstrument();
					List<String> marketMakerCodes = liteMarketDataRequest.getMarketMakerCodes();
					int len = errMsg.length() - 1;
					if (len < 0) len = 0;
					for(String marketMakerCode : marketMakerCodes) {
						sendClassifiedProposalReject(instrument, marketMakerCode, errMsg.substring(0, Math.min(199, len)));
					}
					String isinCode = liteMarketDataRequest.getInstrument().getIsin();
					marketAxessHelper.removeLiteMarketDataRequest(isinCode);
					marketAxessHelper.removeLiteMarketDataRequest(requestId);
				}
			}
		} catch (Exception e) {
			throw new TradeStacException(e);
		}
	}

	private void sendClassifiedProposalReject(Instrument instrument, String marketMakerCode, String reason) {
		try {
			ClassifiedProposal bidClassifiedProposal = marketAxessHelper.getClassifiedProposalReject(MDEntryType.BID, instrument, marketMakerCode, reason);
			ClassifiedProposal askClassifiedProposal = marketAxessHelper.getClassifiedProposalReject(MDEntryType.OFFER, instrument, marketMakerCode, reason);

			// finally invoke the MDS listener
			preTradeConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);

		} catch (TradeStacException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}


	@Override
	public void init() throws BestXException, ConfigError, TradeStacException {
		super.init();
		tradeStacClientSession = super.getTradeStacClientSession();
		if (instrumentFinder == null) {
			throw new ObjectNotInitializedException("InstrumentFinder not set");
		}
		if (marketMakerFinder == null) {
			throw new ObjectNotInitializedException("MarketMakerFinder not set");
		}
		if (venueFinder == null) {
			throw new ObjectNotInitializedException("VenueFinder not set");
		}
		if (marketFinder == null) {
			throw new ObjectNotInitializedException("MarketFinder not set");
		}
		this.marketAxessHelper = new MarketAxessHelper(instrumentFinder, marketMakerFinder, venueFinder, marketFinder);

	}

	
	// AMC BESTX-296
	// MA: Duplication of entries in map of LiteMarketDataRequest necessary because sometimes in business message reject there is no way to get the ISIN code,
	// while in snapshotFullRefresh there is no echo of the original RFQ req ID
	@Override
	public void onMarketDataSnapshotFullRefresh(SessionID sessionID, Message message) throws TradeStacException {
		LOGGER.debug("[{}] {}, {}", connectionName, sessionID, message);
		MarketDataSnapshotFullRefresh marketDataSnapshotFullRefresh;
		LiteMarketDataRequest lDataReq = null;
		try {
			marketDataSnapshotFullRefresh = (MarketDataSnapshotFullRefresh) message;

			Instrument instrument = this.marketAxessHelper.getInstrument(marketDataSnapshotFullRefresh);
			String reqId = instrument.getIsin();

			lDataReq = marketAxessHelper.getLiteMarketDataRequest(reqId);
			if(lDataReq == null) {
				LOGGER.info("MarketDataRequest not found for ISIN code {}. Don't know where save incoming proposal", reqId);
				return;
			}
			MarketMarketMaker mmm = null;
			String mmmStr = null;
			
			List<RootNoMDEntries> entries = marketDataSnapshotFullRefresh.getNoMDEntriesList();
			for(RootNoMDEntries entry : entries) {
				ClassifiedProposal proposal = this.marketAxessHelper.createProposal(instrument, (MarketDataSnapshotFullRefresh.NoMDEntries)entry);
				proposal.setFutSettDate(instrument.getDefaultSettlementDate());
				lDataReq.addProposal(proposal);
				mmm = proposal.getMarketMarketMaker();
				if(mmm != null) {
					mmmStr = mmm.getMarketSpecificCode();
				}
			}  // end for

			// end of proposals read
			try {
				ClassifiedProposal askClassifiedProposal = lDataReq.returnProposal(mmmStr, Proposal.ProposalSide.ASK);
				if(askClassifiedProposal == null) askClassifiedProposal = this.marketAxessHelper.createZeroProposal (instrument,mmm, Proposal.ProposalSide.ASK);
				ClassifiedProposal bidClassifiedProposal = lDataReq.returnProposal(mmmStr, Proposal.ProposalSide.BID);
				if(bidClassifiedProposal == null) bidClassifiedProposal = this.marketAxessHelper.createZeroProposal (instrument,mmm, Proposal.ProposalSide.BID);
				this.marketAxessHelper.removeLiteMarketDataRequest(reqId, mmmStr);
				this.marketAxessHelper.removeLiteMarketDataRequest(lDataReq.getMdReqID(), mmmStr);
				preTradeConnectionListener.onClassifiedProposal(instrument, askClassifiedProposal, bidClassifiedProposal);
			} catch (Exception e) {
				throw new BestXException(String.format("Error managing marketDataSnapshotFullRefresh [%s]", message), e);
			}

		} catch(@SuppressWarnings("unused") ClassCastException e){
			LOGGER.error("received a message of wrong type:expected a MarketDataSnapshotFullRefresh while message was {}", message.toString());
			return;
		} catch(FieldNotFound e1) {
			throw new TradeStacException(String.format("Error managing marketDataRequest [%s]", message), e1);
		} catch(BestXException e2) {
			LOGGER.error("Error managing MarketDataFullSnapshot [{}]", message, e2);
		}
	}

	@Override
	public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {
		throw new NotImplementedException();
		// not used
	}

	@Override
	public void requestInstrumentStatus() throws BestXException {
		throw new NotImplementedException();
		// not used
	}
}
