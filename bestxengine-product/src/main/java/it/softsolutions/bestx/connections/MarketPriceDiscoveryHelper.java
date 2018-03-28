
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

package it.softsolutions.bestx.connections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.Proposal;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 30 gen 2017
 * 
 **/

public class MarketPriceDiscoveryHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketPriceDiscoveryHelper.class);

	public static final String REJECT_MARKET_MAKER_START_CONTAINER = "[";
	public static final String REJECT_MARKET_MAKER_END_CONTAINER = "]";
	public static final String PRICE_SOURCE_MARKET_MAKER_ASSOCIATOR = "@";
	public static final String PARAMS_CAN_NOT_BE_NULL = "params can't be null";

	protected InstrumentFinder instrumentFinder;
	protected MarketMakerFinder marketMakerFinder;
	protected VenueFinder venueFinder;
	protected MarketFinder marketFinder;
	protected Market market;
	
	public MarketPriceDiscoveryHelper(InstrumentFinder instrumentFinder, MarketMakerFinder marketMakerFinder, VenueFinder venueFinder, MarketFinder marketFinder) throws BestXException {
		this.instrumentFinder = instrumentFinder;
		this.marketMakerFinder = marketMakerFinder;
		this.venueFinder = venueFinder;
		this.marketFinder = marketFinder;
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

	public void setMarket(Market market) {
		this.market = market;
	}
	protected ConcurrentMap<String, LiteMarketDataRequest> mdReqIDMap = new ConcurrentHashMap<String, LiteMarketDataRequest>();


	/**
	 * Creates a new {@link LiteMarketDataRequest} and adds it to the concurrent
	 * Hash Map. Implementation useful for Tradestac2.
	 * 
	 * @param mdReqID
	 *            the requestId used in the message.
	 * @param key
	 *            the identifier of the LiteMarketDataRequest to be created.
	 * @param instrument
	 *            the instrument within the new LiteMarketDataRequest.
	 * @param marketMakerCodes
	 *            the list of market makers within the new
	 *            LiteMarketDataRequest.
	 */
	public void addLiteMarketDataRequest(String mdReqID, String key, List<MarketMarketMakerSpec> marketMakerCodes, Instrument instrument) {
		LOGGER.trace("mdReqID = {}, instrument = {}, marketMakerCodes = {}", mdReqID, instrument, marketMakerCodes);

		List<String> mmcodes =Collections.synchronizedList(new ArrayList<String>());
		for(MarketMarketMakerSpec	 dealer : marketMakerCodes) {
			mmcodes.add(dealer.marketMakerCode);
		}
		LiteMarketDataRequest liteMarketDataRequest = new LiteMarketDataRequest(mdReqID, instrument, mmcodes);
		mdReqIDMap.put(key, liteMarketDataRequest);
	}

	/**
	 * Creates a new {@link LiteMarketDataRequest} and adds it to the concurrent
	 * Hash Map. Implementation useful for Tradestac.
	 * 
	 * @param mdReqID
	 *            the identifier of the LiteMarketDataRequest to be created.
	 * @param instrument
	 *            the instrument within the new LiteMarketDataRequest.
	 * @param marketMakerCodes
	 *            the list of market makers within the new
	 *            LiteMarketDataRequest.
	 */
	public void addLiteMarketDataRequest(String mdReqID, Instrument instrument, List<String> marketMakerCodes) {
		LOGGER.trace("mdReqID = {}, instrument = {}, marketMakerCodes = {}", mdReqID, instrument, marketMakerCodes);

		LiteMarketDataRequest liteMarketDataRequest = new LiteMarketDataRequest(mdReqID, instrument, marketMakerCodes);
		mdReqIDMap.put(mdReqID, liteMarketDataRequest);
	}

	/**
	 * Returns the {@link LiteMarketDataRequest} associated to a given
	 * identifier. Because this is extracted from a map, this returns null if
	 * there is no mapping for the mdReqID.
	 * 
	 * @param mdReqID
	 *            the identifier of the LiteMarketDataRequest.
	 * @return the LiteMarketDataRequest associated to a given identifier.
	 */
	public LiteMarketDataRequest getLiteMarketDataRequest(String mdReqID) {
		return mdReqIDMap.get(mdReqID);
	}

	/**
	 * Simply removes a {@link LiteMarketDataRequest} from the map.
	 * 
	 * @param mdReqID
	 *            the identifier of the LiteMarketDataRequest to remove.
	 */
	public void removeLiteMarketDataRequest(String mdReqID) {
		LOGGER.trace("mdReqID = {}", mdReqID);

		mdReqIDMap.remove(mdReqID);
	}

	/**
	 * Removes a given market maker from the list of market makers associated to
	 * an an existing {@link LiteMarketDataRequest}.The LiteMarketDataRequest is
	 * removed from the map when its list of market makers is empty.
	 * 
	 * @param mdReqID
	 *            the identifier of the existing {@link LiteMarketDataRequest}.
	 *            This is used as a key in the removal process.
	 * @param marketMakerCode
	 *            the market maker to remove.
	 */
	public void removeLiteMarketDataRequest(String mdReqID, String marketMakerCode) {
		LOGGER.trace("mdReqID = {}, marketMakerCode = {}", mdReqID, marketMakerCode);

		LiteMarketDataRequest liteMarketDataRequest = mdReqIDMap.get(mdReqID);

		if (liteMarketDataRequest != null && liteMarketDataRequest.getMarketMakerCodes() != null) {
			liteMarketDataRequest.getMarketMakerCodes().remove(marketMakerCode);

			if (liteMarketDataRequest.getMarketMakerCodes().size() == 0) {
				mdReqIDMap.remove(mdReqID);
			}
		}
	}
	public class LiteMarketDataRequest {

		private String mdReqID;
		private Instrument instrument;
		private List<String> marketMakerCodes;
		private Map<String, ClassifiedProposal> askProposals;
		private Map<String, ClassifiedProposal> bidProposals;

		/**
		 * Creates a new LiteMarketDataRequest. It is fair to highlight that a
		 * LiteMarketDataRequest always corresponds to one MarketDataRequest. In
		 * practice, the LiteMarketDataRequest is created using the same
		 * identifier, instrument and list of dealers associated to an existing
		 * MarketDataRequest. More, the LiteMarketDataRequest is never sent to
		 * the MDServer but is managed internally using a concurrent hash map.
		 * 
		 * @param mdReqID
		 *            the identifier of this LiteMarketDataRequest.
		 * @param instrument
		 *            the instrument.
		 * @param marketMakerCodes
		 *            the list of market makers.
		 */
		public LiteMarketDataRequest(String mdReqID, Instrument instrument, List<String> marketMakerCodes) {
			super();
			this.mdReqID = mdReqID;
			this.instrument = instrument;
			this.marketMakerCodes = marketMakerCodes;
			this.askProposals = new HashMap<String, ClassifiedProposal>();  //TODO check trade safety
			this.bidProposals = new HashMap<String, ClassifiedProposal>();
		}

		/**
		 * Returns the identifier of this LiteMarketDataRequest.
		 * 
		 * @return the identifier of this LiteMarketDataRequest.
		 */
		public String getMdReqID() {
			return mdReqID;
		}

		/**
		 * Changes the identifier of this LiteMarketDataRequest.
		 * 
		 * @param mdReqID
		 *            the new identifier of this LiteMarketDataRequest.
		 */
		public void setMdReqID(String mdReqID) {
			this.mdReqID = mdReqID;
		}

		/**
		 * Returns the instrument associated to this LiteMarketDataRequest.
		 * 
		 * @return the instrument associated to this LiteMarketDataRequest.
		 */
		public Instrument getInstrument() {
			return instrument;
		}

		/**
		 * Changes the instrument associated to this LiteMarketDataRequest.
		 * 
		 * @param instrument
		 *            the new instrument associated to this
		 *            LiteMarketDataRequest.
		 */
		public void setInstrument(Instrument instrument) {
			this.instrument = instrument;
		}

		/**
		 * Returns the list of market makers of this LiteMarketDataRequest.
		 * 
		 * @return the list of market makers of this LiteMarketDataRequest.
		 */
		public List<String> getMarketMakerCodes() {
			return marketMakerCodes;
		}

		/**
		 * Changes the list of market makers of this LiteMarketDataRequest.
		 * 
		 * @param marketMakerCodes
		 *            the new list of market makers of this
		 *            LiteMarketDataRequest.
		 */
		public void setMarketMakerCodes(List<String> marketMakerCodes) {
			this.marketMakerCodes = marketMakerCodes;
		}
		
		public void addProposal(ClassifiedProposal proposal) {
			if(proposal.getSide().equals(Proposal.ProposalSide.ASK))
				askProposals.put(proposal.getMarketMarketMaker().getMarketSpecificCode(), proposal);
			else
				bidProposals.put(proposal.getMarketMarketMaker().getMarketSpecificCode(), proposal);
		}
		
		public boolean isProposalComplete(String marketMakerCode) {
			boolean res = true;
			if(askProposals.get(marketMakerCode) == null || bidProposals.get(marketMakerCode) == null)
				res = false;
			return res;
		}
		public ClassifiedProposal returnProposal(String marketMakerCode, Proposal.ProposalSide side) {
			if(side == Proposal.ProposalSide.ASK)
				return askProposals.remove(marketMakerCode);
			if(side == Proposal.ProposalSide.BID)
				return bidProposals.remove(marketMakerCode);
			return null;
		}

	}


}
