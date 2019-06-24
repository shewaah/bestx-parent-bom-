/*
 * Copyright 2019-2028 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.bondvision;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutablePriceAskComparator;
import it.softsolutions.bestx.model.ExecutablePriceBidComparator;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix.field.QuoteCondition;
import it.softsolutions.tradestac.fix50.TSMarketDataSnapshotFullRefresh;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSMDFullGrp.TSNoMDEntries;
import it.softsolutions.tradestac.fix50.component.TSParties;


/*
 * Copyright 2019-2028 SoftSolutions! srl 
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
public class BondvisionBookManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(BondvisionBookManager.class);	
	private static BondvisionBookManager instance;
	
	public final static String Counteroffer = "Counteroffer";
	public final static String Expired = "Expired";
	public final static String RejectedByBestx = "Rejected by BestX";
	public final static String RejectedByDealer = "RejectedByDealer";
	public final static String NoReason = "";
	
	final static PartyRole executingFirm = PartyRole.ExecutingFirm;
	final static PartyIDSource lei = PartyIDSource.LegalEntityIdentifier;
	
	private MarketMakerFinder marketMakerFinder;

	public MarketMakerFinder getMarketMakerFinder() {
		return marketMakerFinder;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public synchronized static BondvisionBookManager getInstance() {
		if(instance == null)
			instance = new BondvisionBookManager();
		return instance;
	}
	
	private Map<String,List<ExecutablePrice>> rFCQBook = new java.util.concurrent.ConcurrentHashMap<String,List<ExecutablePrice>>();
	private Map<String, Boolean> rFCQOpenClose = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();
	
	private List<ExecutablePrice> getList(String rFCQID) throws BestXException {
		if(!rFCQBook.containsKey(rFCQID)) {
			throw new BestXException("RFCQID not existent");
		} else {
			return rFCQBook.get(rFCQID);
		}

	}
	
	private String getOriginatorID(TSNoMDEntries entry) {
		TSParties parties = entry.getTsParties();
		List<TSNoPartyID> partiesList = parties.getTSNoPartyIDsList();
		for(TSNoPartyID party : partiesList) {
			if(executingFirm.equals(party.getPartyRole()) && !(lei.equals(party.getPartyIDSource())))
				return party.getPartyID();
		}
		return null;
	}
	
	/**
	 * Creates a sorted list of Executable Prices from the input newBook
	 * @param newBook
	 * @param bookMap return parameter. All the Executable Prices will be created and set in this parameter, with the OriginatorID as a key.
	 * @param attemptId
	 * @param side
	 * @return a list of all ExecutablePrice
	 * @throws BestXException 
	 */
	private List<ExecutablePrice> fillListFromMarketData(TSMarketDataSnapshotFullRefresh newBook, Map<String,ExecutablePrice> bookMap, int attemptId, ProposalSide side) throws BestXException {
		List<ExecutablePrice> list = new ArrayList<ExecutablePrice>();
		List<TSNoMDEntries> entries = newBook.getTSMDFullGrp().getTSNoMDEntriesList();
		for(TSNoMDEntries entry : entries) {
			ExecutablePrice ep = new ExecutablePrice(new ClassifiedProposal());
			String originatorId = getOriginatorID(entry);
			ep.setMarketMarketMaker(marketMakerFinder.getMarketMarketMakerByCode(Market.MarketCode.BV, originatorId));
			ep.setOriginatorID(originatorId);
			ep.setPriceType(entry.getPriceType() == PriceType.Percentage ? Proposal.PriceType.PRICE : 
								entry.getPriceType() == PriceType.Yield ? Proposal.PriceType.YIELD : Proposal.PriceType.SPREAD);
			ep.setPrice(new Money("EUR", new BigDecimal(entry.getMdEntryPx().toString())));
			ep.setQty(new BigDecimal(entry.getMdEntrySize().toString()));
			ep.setAuditQuoteState(NoReason);
			ep.setAttemptId(attemptId);
//			ep.setExpiration(entry.getValidUntil());
			ep.setRank(entry.getMDPriceLevel());
			ep.setType(QuoteCondition.NonFirm.compareTo(entry.getQuoteCondition()) == 0 ? ProposalType.INDICATIVE :
						QuoteCondition.CloseInactive.compareTo(entry.getQuoteCondition()) == 0 ? ProposalType.CLOSED : ProposalType.TRADEABLE);
			list.add(ep);
			if(originatorId != null)
				bookMap.put(originatorId, ep);
			else throw new InvalidParameterException("OriginatorID cannot be null. Attempt " + attemptId);
		}
		Comparator<ExecutablePrice> comparator = side==ProposalSide.BID ? new ExecutablePriceBidComparator() :  new ExecutablePriceAskComparator();
		list.sort(comparator);
		for(int i = 0; i< list.size();/*increment is in body*/) {
			ExecutablePrice ep = list.get(i);
			ep.setRank(++i);
		}
		return list;
	}
	
	/**
	 * return true iff the RFCQ is open
	 * @param rFCQID
	 * @return
	 */
	public boolean isOpen(String rFCQID) {
		return Boolean.TRUE.equals(rFCQOpenClose.get(rFCQID));
	}
	
	/**
	 * Used to refresh the current book image when a new book is received from the market.
	 * @param rFCQID
	 * @param newBook
	 * @param refreshCounters iff true Offers in the book marked as counter-offer (in the RFQ negotiation process) are refreshed.
	 * If false they are skipped and remain unchanged in the current book
	 */
	@SuppressWarnings("deprecation")
	public void updateBook(String rFCQID, TSMarketDataSnapshotFullRefresh newBook, boolean refreshCounters, int attemptId, ProposalSide side) throws BestXException {
		Map<String, ExecutablePrice> mapBook =  new HashMap<String, ExecutablePrice>();
		List<ExecutablePrice> newList = fillListFromMarketData(newBook, mapBook, attemptId, side);
		List<ExecutablePrice> oldList = this.rFCQBook.get(rFCQID);
		if(refreshCounters || oldList == null || oldList.size() == 0) {
			this.rFCQBook.replace(rFCQID, Collections.synchronizedList(newList));  // replace the entire list with the new one
		}
		else {
			int i = 0;
			// parse the original executable price list
			// for each non counter replace the current executable price with the one in the new book if there is one
			for(ExecutablePrice ep : oldList) {
				ExecutablePrice newEp = mapBook.get(ep.getOriginatorID());
				if(ProposalType.COUNTER != ep.getType() && newEp != null) {
					oldList.set(i, newEp); // replace old executable price with new executable price
				} else if(newEp == null)
					oldList.remove(i--);
				i++;
			}
		}
	}

	/**
	 * Used to tell the BondVisionBookManager that a quote is not valid anymore and has to be removed from the book
	 * @param rFCQID
	 * @param quote
	 * @param reason the reason that would be shown to the OMS when the RFCQ feedbak 
	 * is reported in POBEx execution report if the price is included in the report
	 */
	public void rejectQuote(String rFCQID, ClassifiedProposal quote, String reason) throws BestXException {
		List<ExecutablePrice> list = getList(rFCQID);
		MarketMarketMaker mmm = quote.getMarketMarketMaker();
		for(ExecutablePrice ep : list) {
			if(ep.getMarketMarketMaker().equals(mmm)) {
				ep.setProposalState(ProposalState.REJECTED); //FIXME Serve?
				ep.setReason(reason); //FIXME Serve?
				ep.setAuditQuoteState(reason);
				break;
			}
		}
	}

	/**
	 * Used to tell the BondVisionBookManager that a quote has been updated singularly (e.g. counter from dealer)
	 * @param rFCQID
	 * @param quote
	 * @param reason
	 */
	public void updateQuote(String rFCQID, ClassifiedProposal quote, String reason) throws BestXException {
		List<ExecutablePrice> list = getList(rFCQID);
		MarketMarketMaker mmm = quote.getMarketMarketMaker();
		for(ExecutablePrice ep : list) {
			if(ep.getMarketMarketMaker().equals(mmm)) {
				ep.setClassifiedProposal(quote);
				ep.setAuditQuoteState(reason);
				break;
			}
		}
	}

	/**
	 * Used to tell the BondVisionBookManager that a quote has been updated singularly (e.g. counter from dealer) with no auditQuoteProposalState
	 * @param rFCQID
	 * @param quote
	 */
	public void updateQuote(String rFCQID, ClassifiedProposal quote) throws BestXException {
		updateQuote(rFCQID, quote, NoReason);
	}
	
	/**
	 * get the best quote whether it is a counter or not, but still valid
	 * does never select rejected and expired quotes
	 * @param rFCQID
	 * @return
	 */
	public ClassifiedProposal getBest(String rFCQID) {
		try {
			for(int i = 0; i < rFCQBook.get(rFCQID).size(); i++) {
				ExecutablePrice level = getLevel(rFCQID, i);
				if(level == null)
					return null;
				String reason = level.getAuditQuoteState();
				if(reason == null)
					reason = NoReason;
				switch(reason) {
				case Expired:
				case RejectedByBestx:
				case RejectedByDealer:
					continue; // try next level, because this one is not suitable
				case Counteroffer:
				case NoReason:
					ClassifiedProposal quote = level.getClassifiedProposal();
//					if(DateService.newLocalDate().compareTo(quote.getExpiration()) > 0) { // not set as expired, but it is
//						level.setAuditQuoteState(Expired);
//						continue;// try next level, because this one is no more suitable
//					} else
						return quote;
				default:
				return null;
				}
			}
		} catch(BestXException e) {
			LOGGER.error("Request to get best on RFCQ {} while this RFCQ dodes not exist");
			return null;
		}
		return null;
	}

	
	/**
	 * get the i-th level of the book
	 * @param rFCQID
	 * @param level
	 * @return
	 */
	public ExecutablePrice getLevel(String rFCQID, int level)  throws BestXException {  //FIXME valutare se è necessario copiare la lista prima di iterarla
		List<ExecutablePrice> list = getList(rFCQID);
		if(list.size() > level)
			return list.get(level);
		else
			return null;
	}

	
	/**
	 * Open a new RFCQ and creates all the needed resources
	 * @param rFCQID
	 */
	public void open(String rFCQID) throws BestXException {
		if(rFCQBook.containsKey(rFCQID) && isOpen(rFCQID))
			throw new BestXException("RFCQID already existing.");
		rFCQBook.put(rFCQID, new ArrayList<ExecutablePrice>());
		rFCQOpenClose.put(rFCQID, Boolean.TRUE);
	}
	
	/**
	 * Close the RFCQID, but maintains the book until it is dismissed
	 * @param rFCQID
	 */
	public void close(String rFCQID) {
		rFCQOpenClose.put(rFCQID, Boolean.FALSE);
	}
	
	/**
	 * dismiss the RFCQ and frees the related resources
	 * @param rFCQID
	 */
	public void dismiss(String rFCQID) {
		rFCQBook.remove(rFCQID);
		rFCQOpenClose.remove(rFCQID);
	}
}
