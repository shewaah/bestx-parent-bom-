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
package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.util.concurrent.ConcurrentSkipListMap;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;

/**
 * Singleton class, for every ISIN we have a corresponding list of orders that requested prices for it and a book with the prices received for it.
 * 
 * The orders are wrapped in a more complex object, the MarketPriceListener.
 * 
 * Upon reception of a proposal we always add it to the ISIN book, previously we remove the proposal from the same market maker and for the same side if exists. Then we notify all the interested
 * orders of the proposal arrival and check if all the venues every order waited for replied to us. If so the price request routine ends here and we send the book onward to the listener to have it
 * processed with those of the other markets.
 * 
 * 
 * We also have a list of the markets that request prices through the price connection of another one. When we receive a proposal we check if the market maker of one of those markets corresponds to
 * proposal one. If so, we must insert it also in the subordinate market book.
 * 
 * @author ruggero.rizzo
 * 
 */
public class SimpleMarketProposalAggregator implements MarketProposalAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMarketProposalAggregator.class);
    public static final String TIMER_NAME_SEPARATOR = "@";
    private OperationRegistry operationRegistry;
    private static SimpleMarketProposalAggregator instance = null;
    private ConcurrentMap<String, BaseBook> marketBookMap;
    private ConcurrentMap<String, List<MarketPriceConnectionListener>> bookListeners;

    private MarketFinder marketFinder;

    private SimpleMarketProposalAggregator() {
    	bookListeners = new ConcurrentSkipListMap<String, List<MarketPriceConnectionListener>>();
    	marketBookMap = new ConcurrentSkipListMap<String, BaseBook>();
    }

    public static SimpleMarketProposalAggregator getInstance() {
        if (instance == null) {
            instance = new SimpleMarketProposalAggregator();
        }

        return instance;
    }

    @Override
    public void addBookListener(Instrument instrument, MarketPriceConnectionListener bookListener) {
        String isin = instrument.getIsin();
        List<MarketPriceConnectionListener> isinBookListeners = bookListeners.get(isin);

        if (isinBookListeners == null) {
            isinBookListeners = new ArrayList<MarketPriceConnectionListener>();
            bookListeners.put(isin, isinBookListeners);
        }

        if (!isinBookListeners.contains(bookListener)) {
            isinBookListeners.add(bookListener);
        } else {
            LOGGER.debug("Isin {}, book listener already registered.", isin);
        }

        if (!marketBookMap.containsKey(isin)) {
            BaseBook marketBook = new BaseBook();
            marketBook.setInstrument(instrument);
            marketBookMap.put(isin, marketBook);
        }
    }

    /**
     * Manage the proposal notifying all the orders waiting for prices for that instrument. We check if the market maker replied for both sides and, for every order, if its book is complete or not.
     * 
     * @param instrument
     *            : the instrument subject of the price requests
     * @param proposal
     *            : the proposal just received
     * @param marketPriceConnection
     *            : the market from whom we asked prices
     * @param errorCode
     *            : possible error code sent from the market
     * @param errorCode
     *            : possible error msg sent from the market
     * @param isLastProposal
     *            : last proposal received, book complete for the specific market
     */
    @Override
    public void onProposal(Instrument instrument, ClassifiedProposal proposal, MarketPriceConnection marketPriceConnection, int errorCode, String errorMsg, boolean isLastProposal)
                    throws BestXException {
        LOGGER.debug("Proposal received from the market {}  : {}", marketPriceConnection.getMarketCode(), proposal);
        String isin = instrument.getIsin();
        Market proposalMarket = proposal.getMarket();
        ProposalSide proposalSide = proposal.getSide();
        LOGGER.debug("Proposal received from the market {}  : {}", marketPriceConnection.getMarketCode(), proposal);
        MarketMarketMaker proposalMarketMarketMaker = proposal.getMarketMarketMaker();
        MarketMaker proposalMarketMaker = proposal.getMarketMarketMaker().getMarketMaker();
        BaseBook marketBook = marketBookMap.get(isin);

        if (marketBook == null) {
            LOGGER.error("Book not found for isin {}", isin);
            return;
        }

        List<MarketPriceConnectionListener> realIsinBookListenersList = bookListeners.get(isin);

        List<MarketPriceConnectionListener> clonedIsinBookListenersList = new ArrayList<MarketPriceConnectionListener>();
        clonedIsinBookListenersList.addAll(realIsinBookListenersList);

        if (clonedIsinBookListenersList == null || clonedIsinBookListenersList.isEmpty()) {
            LOGGER.warn("[PRICESRV] Received an unexpected proposal [{}] for isin {}, no listeners registered!", proposal, isin);
            return;
        }

        if (marketBook.proposalExistsForMarketMarketMaker(proposalMarketMarketMaker, proposalSide)) {
            marketBook.removeProposalFromMarketMakerAndSide(proposalMarketMarketMaker, proposalSide);
        }

        marketBook.addProposal(proposal);

        LOGGER.debug("MarketBook with the new proposal is : {}", marketBook);

        List<MarketPriceConnectionListener> toBeDeleted = new ArrayList<MarketPriceConnectionListener>();
        for (MarketPriceConnectionListener isinBookListener : clonedIsinBookListenersList) {
            Order order = isinBookListener.getOrder();
            boolean allowableProposal = proposalMarketMarketMaker.canTrade(instrument, order.isBestExecutionRequired()) && proposalMarketMaker.isEnabled();
            // if the proposal cannot be processed due to the market maker configuration, remove it from the marketBook
            if (!allowableProposal) {
                LOGGER.info("Order {}: the market maker {} is configured to not trade on the instrument {}, ignore the proposal", order.getFixOrderId(), proposalMarketMaker, isin);

                // remove the proposal added at the beginning of this method
                proposal.setProposalState(ProposalState.DROPPED);
                // marketBook.removeProposal(proposal);
                // return;
            } else {
                if (errorCode != 0) {
                    LOGGER.info("Order {}: proposal received with an error, code = {}, msg = {}. Ignore it.", order.getFixOrderId(), errorCode, errorMsg);
                }
            }

            if (isinBookListener.isActive()) {
                String timerName = buildTimerName(isin, order.getFixOrderId(), proposalMarket.getMarketCode());
                LOGGER.debug("[PRICESRV] Order={}, Market={}, PricesReceivedIn={} ms", order.getFixOrderId(), proposalMarket.getMarketCode().name(),
                        (DateService.currentTimeMillis() - isinBookListener.getCreationDate().getTime()));
                List<MarketMarketMaker> orderMarketMarketMakers = isinBookListener.getMarketMarketMakersForMarket(proposalMarket.getMarketCode());
                LOGGER.debug("Order {}, market makers for the market {} : {}", order.getFixOrderId(), proposalMarket.getMarketCode(),
                        (orderMarketMarketMakers != null ? orderMarketMarketMakers : "NONE!"));
                boolean bookComplete = true;

                if(orderMarketMarketMakers != null) {
	                for (MarketMarketMaker mmm : orderMarketMarketMakers) {
	                    // reset the flag to true because the previous market maker could have set it to false
	                    boolean bothSidesArrived = true;
	                    if (mmm.getMarketMaker().isEnabled()) {
	                        // check if we received a proposal for the market market maker ...
	                        if (!marketBook.proposalExistsForMarketMarketMaker(mmm, ProposalSide.ASK)) {
	                            bookComplete = false;
	                            bothSidesArrived = false;
	                        }
	                        // ... now check if we received that of the other side, only if we did we can consider the book as complete
	                        if (!marketBook.proposalExistsForMarketMarketMaker(mmm, ProposalSide.BID)) {
	                            bookComplete = false;
	                            bothSidesArrived = false;
	                        }
	
	                    } else {
	                        LOGGER.debug("Order {}, market market maker {} is disabled by configuration or cannot trade the instrument always due to the configuration, do not consider it as a missing reply.",
	                                        order.getFixOrderId(), mmm);
	                    }
	
	                    // bloomberg could send us a proposal with error code -6 "No realtime available", but we do not have to ignore it
	                    // we keep processing it as a regular proposal because, for every -6 "No realtime available", we received a proposal
	                    // with price and quantity = 0. We can insert it in the book, it will be discarded during the classification.
	                    if (bothSidesArrived) {
	                        order.removeMarketMakerNotQuotingInstr(mmm);
	                    }
	                } 
                }else {
                	LOGGER.info("null orderMarketMarketMakers for order {}", order.getFixOrderId());
                }
                if (bookComplete || isLastProposal) {

                    MarketCode marketCode = proposalMarket.getMarketCode();
                    if (isinBookListener.getRemainingMarketPriceConnections() != null && isinBookListener.getRemainingMarketPriceConnections().contains(marketPriceConnection)) {
                        // remove duplicated market maker from the original book, we choose the best proposal
                        // for those market maker quoting with more than one market market maker in a market
                        
                        //copy the marketBook to leave it untouched
                        BaseBook modifiedMarketBook = marketBook.clone();

                        cleanMultipleMarketMakerProposals(marketPriceConnection.getMarketCode(), instrument, modifiedMarketBook, order);
                        boolean keepNotValidProposals = false;
                        BaseBook propMarketBook = fillCompleteMarketBook(modifiedMarketBook, marketCode, keepNotValidProposals);
                        // With a market down, thus no prices available, we might anyway find proposals stored
                        // in the aggregator book, but being the market not connected it will result as not quoting
                        // the instrument. If we are sending onward a complete book for such a market, we must clear it
                        // and let BestX add the not quoting fake proposal. Only this one must appear on the book.
                        if (order.getMarketNotQuotingInstr() != null && order.getMarketNotQuotingInstr().containsKey(marketCode)) {
                            LOGGER.info("Order {}, the market {} is temporarily unable to accept orders or it could not quote the instrument at the moment, empty the book, proposals found for this market cannot be sent onward. ",
                                            order.getFixOrderId(), marketCode);
                            propMarketBook.getAskProposals().clear();
                            propMarketBook.getBidProposals().clear();
                        }
                        LOGGER.info("Order {}, all the market makers replied with prices, the book is complete for the market {}, send it onward.", order.getFixOrderId(), marketCode);

                        // BV SDP is the only BondVision that could arrive here
                        if (marketCode.equals(MarketCode.BV)) {
                            isinBookListener.onMarketBookComplete(marketPriceConnection, propMarketBook);
                        } else {
                            // book complete for BBG
                            isinBookListener.onMarketBookComplete(marketPriceConnection, propMarketBook);
                            buildAndNotifyDependingMarketsBook(isinBookListener, order, modifiedMarketBook);
                        }

                        if (isinBookListener.getNumWaitingReplyMarketPriceConnection() == 0) {
                            LOGGER.info("Order {}, price discovery complete", order.getFixOrderId());
                            toBeDeleted.add(isinBookListener);
                            cleanOrderMarketMakersNotReplyingList(isinBookListener.getOrder(), marketBook);
                        }
                        modifiedMarketBook = null;
                        LOGGER.debug("Order {}, {} book notified. The complete marketBook is {}", order.getFixOrderId(), proposalMarket.getMarketCode(), marketBook);
                    } else {
                        LOGGER.debug("Order {}, {} book already notified.", order.getFixOrderId(), proposalMarket.getMarketCode());
                    }
                }
            } else {
                toBeDeleted.add(isinBookListener);
                LOGGER.info("Price Listener for the order {} not active, price discovery already completed, waiting to be removed from the listener list.", isinBookListener.getOrder().getFixOrderId());
            }
        }
        realIsinBookListenersList.removeAll(toBeDeleted);

        // TODO : capire se e' necessario :
        if (realIsinBookListenersList.isEmpty()) {
            cleanBook(isin, realIsinBookListenersList);
        }
    }

    public void buildAndNotifyDependingMarketsBook(MarketPriceConnectionListener isinBookListener, Order order, BaseBook marketBook) throws BestXException {
        Instrument instrument = order.getInstrument();
        BaseBook depMktMarketBook = null;
        Set<MarketPriceConnection> priceConns = isinBookListener.getRemainingMarketPriceConnections();
        MarketPriceConnection twMktPriceConn = null;
        MarketPriceConnection bvMktPriceConn = null;
        for (MarketPriceConnection priceConn : priceConns) {
            if (priceConn.getMarketCode().equals(MarketCode.TW)) {
                twMktPriceConn = priceConn;
            } else if (priceConn.getMarketCode().equals(MarketCode.BV)) {
                bvMktPriceConn = priceConn;
            }
        }

        // these two maps are never null, every order initialize them on instantiation
        Map<MarketCode, String> marketNotQuoting = order.getMarketNotQuotingInstr(); // market code, reason
        Map<MarketCode, String> marketNotNegotiating = order.getMarketNotNegotiatingInstr(); // market code, reason

        if (twMktPriceConn != null && twMktPriceConn.isInstrumentQuotedOnMarket(instrument)) {
            createDependingMarketsBook(MarketCode.TW, instrument, marketBook, isinBookListener, order, ProposalSide.BID);
            createDependingMarketsBook(MarketCode.TW, instrument, marketBook, isinBookListener, order, ProposalSide.ASK);
        } else {
            LOGGER.debug("Order {}, instrument {} not quoted on TW, do not add proposals for it in the TW book.", order.getFixOrderId(), instrument.getIsin());
        }

        if (!marketNotQuoting.containsKey(MarketCode.BV) && !marketNotNegotiating.containsKey(MarketCode.BV)) {
            if (bvMktPriceConn != null && bvMktPriceConn.isInstrumentQuotedOnMarket(instrument)) {
                createDependingMarketsBook(MarketCode.BV, instrument, marketBook, isinBookListener, order, ProposalSide.BID);
                createDependingMarketsBook(MarketCode.BV, instrument, marketBook, isinBookListener, order, ProposalSide.ASK);
            }
        }

        boolean keepNotValidProposals = false;
        // notify TW/BV book
        if (twMktPriceConn != null) {
            depMktMarketBook = fillCompleteMarketBook(marketBook, MarketCode.TW, keepNotValidProposals);
            isinBookListener.onMarketBookComplete(twMktPriceConn, depMktMarketBook);
        }

        if (bvMktPriceConn != null) {
            depMktMarketBook = fillCompleteMarketBook(marketBook, MarketCode.BV, keepNotValidProposals);
            isinBookListener.onMarketBookComplete(bvMktPriceConn, depMktMarketBook);
        }
    }

    /**
     * A market maker could have more than one market market maker, here we check the proposals received from the same market maker and keep only the best one.
     * 
     * Remember that the book must have the same market market maker quoting on both sides, what we do here is find out the best proposal for a market maker among its market market makers, but in the
     * meantime take the same market market maker proposal from the less interesting side.
     * 
     * (Customer request of the 27 Jan 2012)
     * 
     * @param marketCode
     * @param instrument
     * @param marketBook
     * @param isinBookListener
     * @param order
     * @param side
     */
    private void cleanMultipleMarketMakerProposals(MarketCode marketCode, Instrument instrument, BaseBook marketBook, Order order) {
        LOGGER.debug("Find best proposal for every market maker.");

        // the following maps store or will store:
        // best proposals for the new book
        Map<MarketMaker, Proposal> newBookBestSideProposals = new ConcurrentHashMap<MarketMaker, Proposal>();
        // proposals for the not best side of the new book
        Map<MarketMaker, Proposal> newBookOtherSideProposals = new ConcurrentHashMap<MarketMaker, Proposal>();
        // actual book not best side proposals
        Map<MarketMarketMaker, Proposal> bookOtherSideProposals = new ConcurrentHashMap<MarketMarketMaker, Proposal>();

        // load the not best side actual book proposals
        if (order.getSide().equals(OrderSide.BUY)) {
            for (Proposal bookProp : marketBook.getBidProposals()) {
                bookOtherSideProposals.put(bookProp.getMarketMarketMaker(), bookProp);
            }

            // clean multiple proposals for the same market maker and fetch the corresponding proposals
            // for the side which is not the best one
            for (Proposal bookProp : marketBook.getAskProposals()) {
                checkMarketMakerProposalInBook(newBookBestSideProposals, newBookOtherSideProposals, bookProp, bookOtherSideProposals);
            }
        } else {
            for (Proposal bookProp : marketBook.getAskProposals()) {
                bookOtherSideProposals.put(bookProp.getMarketMarketMaker(), bookProp);
            }

            // clean multiple proposals for the same market maker and fetch the corresponding proposals
            // for the side which is not the best one
            for (Proposal bookProp : marketBook.getBidProposals()) {
                checkMarketMakerProposalInBook(newBookBestSideProposals, newBookOtherSideProposals, bookProp, bookOtherSideProposals);
            }
        }

        // initialize the market book with the new ask and bid proposals
        LOGGER.info("Order {}, original book with multiple proposals for a mmm : {}", order.getFixOrderId(), marketBook);
        marketBook.getAskProposals().clear();
        marketBook.getBidProposals().clear();

        switch (order.getSide()) {
        case BUY:
            marketBook.getAskProposals().addAll(newBookBestSideProposals.values());
            marketBook.getBidProposals().addAll(newBookOtherSideProposals.values());
            break;
        case SELL:
            marketBook.getAskProposals().addAll(newBookOtherSideProposals.values());
            marketBook.getBidProposals().addAll(newBookBestSideProposals.values());
            break;
        default:
            break;
        }

        LOGGER.info("Order {}, cleaned book with only the best proposal for every mmm : {}", order.getFixOrderId(), marketBook);
        LOGGER.debug("Book cleaned, one proposal for every market maker.");
    }

    /**
     * Here we save in the best proposals map the best for every marker maker. We check the price for proposals received from the same market maker and keep the best one.
     * 
     * @param newBookBestSideProposals
     *            : map with a market maker with its best proposal, there is a different map for every side, thus this method will be called twice
     * @param newBookOtherSideProposals
     *            : map with the market maker proposal for the not best side
     * @param bookProp
     *            : proposal in the book
     * @param otherSideProposals
     *            : map containing the not best side proposals for eveery market maker taken from the original book
     */
    private void checkMarketMakerProposalInBook(Map<MarketMaker, Proposal> newBookBestSideProposals, Map<MarketMaker, Proposal> newBookOtherSideProposals, Proposal bookProp,
                    Map<MarketMarketMaker, Proposal> otherSideProposals) {
        MarketMaker bookPropMM = bookProp.getMarketMarketMaker().getMarketMaker();
        ProposalSide bookPropSide = bookProp.getSide();

        Proposal otherSideProp = otherSideProposals.get(bookProp.getMarketMarketMaker());
        // sanity check
        if (otherSideProp == null) {
            return;
        }

        boolean priceIsBetter = false;
        LOGGER.debug("Check proposal price for {}={}", bookPropMM, bookProp.getPrice().getAmount());

        // always add the first proposal because the map is empty
        if (newBookBestSideProposals.isEmpty()) {
            newBookBestSideProposals.put(bookPropMM, bookProp);
            newBookOtherSideProposals.put(bookPropMM, otherSideProp);
        } else {
            // map not empty, check if we already have a proposal for this market maker
            Proposal alreadyInBook = newBookBestSideProposals.get(bookPropMM);
            if (alreadyInBook == null) {
                // first proposal for this MM, put it in the map
                newBookBestSideProposals.put(bookPropMM, bookProp);
                newBookOtherSideProposals.put(bookPropMM, otherSideProp);
                LOGGER.debug("Best price for {}={}", bookPropMM, bookProp.getPrice().getAmount());
            } else {
                // already exists a proposal for MM, find out if the new one is better
                if (bookPropSide.equals(ProposalSide.ASK)) {
                    // a 0 price is always lower than other prices, but it is not the better if, for that market maker,
                    // we have also a valid price
                    // so, if the first proposal has a valid price we must ignore the following with a 0 price
                    priceIsBetter = !bookProp.getPrice().getAmount().equals(BigDecimal.ZERO) && bookProp.getPrice().compareTo(alreadyInBook.getPrice()) < 0;
                    // if the price already in the book is zero it is lower than every other price, but it is not correct
                    // so if it is zero, but the new proposal has a valid price, we must take the new proposal
                    if (alreadyInBook.getPrice().getAmount().equals(BigDecimal.ZERO)) {
                        priceIsBetter = !bookProp.getPrice().getAmount().equals(BigDecimal.ZERO);
                    }
                } else {
                    priceIsBetter = bookProp.getPrice().compareTo(alreadyInBook.getPrice()) > 0;
                }
                if (priceIsBetter) {
                    LOGGER.debug("Best price for {}={}", bookPropMM, bookProp.getPrice().getAmount());
                    newBookBestSideProposals.put(bookPropMM, bookProp);
                    newBookOtherSideProposals.put(bookPropMM, otherSideProp);
                }
            }
        }
    }

    // Creazione dei book per i mercati che dipendono da BBG per la richiesta prezzi (clona le proposal BBG necessarie)
    private void createDependingMarketsBook(MarketCode targetMarketCode, Instrument instrument, BaseBook marketBook, MarketPriceConnectionListener isinBookListener, Order order, ProposalSide propSide)
                    throws BestXException {
        // now build books for TW and/or BV
        List<MarketMarketMaker> depMktMarketMarketMakers = isinBookListener.getMarketMarketMakersForMarket(targetMarketCode);
        // loop on all the market makers of the given market
        for (MarketMarketMaker mmm : depMktMarketMarketMakers) {
            if (mmm.canTrade(instrument) && mmm.getMarketMaker().isEnabled()) {
                MarketMaker mm = mmm.getMarketMaker();
                List<MarketMarketMaker> bbgMarketMarketMakers = mm.getMarketMarketMakerForMarket(MarketCode.BLOOMBERG);
                ClassifiedProposal fakeProp = null;
                // loop on all the BBG market makers corresponding to the one of the given market
                for (MarketMarketMaker bbgMmm : bbgMarketMarketMakers) {
                    ClassifiedProposal bbgProp = marketBook.getAlreadyExistingProposal(bbgMmm, propSide);
                    if (bbgProp != null) {
                        if (fakeProp == null) {
                            fakeProp = getClonedProposal(bbgProp, targetMarketCode, mmm, instrument);
                        } else {
                            boolean priceIsBetter = false;
                            if (propSide.equals(ProposalSide.ASK)) {
                                // a 0 price is always lower than other prices, but it is not the better if, for that market maker,
                                // we have also a valid price
                                // so, if the first proposal has a valid price we must ignore the following with a 0 price
                                priceIsBetter = !bbgProp.getPrice().getAmount().equals(BigDecimal.ZERO) && bbgProp.getPrice().compareTo(fakeProp.getPrice()) < 0;
                                // if the first proposal has a 0 price, but the new proposal has a valid price,
                                // we must allow the substitution of the price with the new one
                                priceIsBetter = fakeProp.getPrice().getAmount().equals(BigDecimal.ZERO);
                            } else {
                                priceIsBetter = bbgProp.getPrice().compareTo(fakeProp.getPrice()) > 0;
                            }

                            if (priceIsBetter) {
                                fakeProp = getClonedProposal(bbgProp, targetMarketCode, mmm, instrument);
                            }
                        }
                    }
                }
                if (fakeProp != null) {
                    LOGGER.debug("Order {}, {} proposal added for the market maker {}, market {}", order.getFixOrderId(), propSide, mmm, targetMarketCode);

                    order.removeMarketMakerNotQuotingInstr(mmm);

                    if (marketBook.proposalExistsForMarketMarketMaker(fakeProp.getMarketMarketMaker(), propSide)) {
                        marketBook.removeProposal(marketBook.getAlreadyExistingProposal(fakeProp.getMarketMarketMaker(), propSide));
                    }

                    switch (propSide) {
                    case BID:
                        marketBook.getBidProposals().add(fakeProp);
                        break;
                    case ASK:
                        marketBook.getAskProposals().add(fakeProp);
                        break;
                    default:
                        break;
                    }
                }
            } else {
                LOGGER.debug("Order {}, the market maker {} cannot trade the instrument, do not add proposals for {}", order.getFixOrderId(), mmm, targetMarketCode);
            }
        }
    }

    private ClassifiedProposal getClonedProposal(ClassifiedProposal sourceProposal, MarketCode targetMarket, MarketMarketMaker targetMMM, Instrument instrument) throws BestXException {
        ClassifiedProposal fakeProposal = new ClassifiedProposal();
        Venue venue = new Venue(sourceProposal.getVenue());
        Market market = marketFinder.getMarketByCode(targetMarket, null);
        venue.setMarket(market);
        fakeProposal = new ClassifiedProposal();
        fakeProposal.setMarket(market);
        fakeProposal.setProposalState(ProposalState.NEW);
        fakeProposal.setSide(sourceProposal.getSide());
        fakeProposal.setType(ProposalType.INDICATIVE);
        fakeProposal.setTimestamp(sourceProposal.getTimestamp());
        fakeProposal.setPrice(sourceProposal.getPrice());
        fakeProposal.setQty(sourceProposal.getQty());
        fakeProposal.setVenue(venue);
        fakeProposal.setFutSettDate(instrument.getDefaultSettlementDate());
        fakeProposal.setNonStandardSettlementDateAllowed(sourceProposal.isNonStandardSettlementDateAllowed());
        fakeProposal.setMarketMarketMaker(targetMMM);
        fakeProposal.setReason("");

        return fakeProposal;
    }

    private BaseBook fillCompleteMarketBook(BaseBook marketBook, MarketCode marketCode, boolean keepNotValidProposals) {
        LOGGER.debug("Building the complete book for the market {}", marketCode);
        BaseBook newBook = new BaseBook();
        List<Proposal> marketProposals = new ArrayList<Proposal>();
        marketProposals.addAll(marketBook.getAskProposals());
        marketProposals.addAll(marketBook.getBidProposals());
        for (Proposal marketProposal : marketProposals) {
            ClassifiedProposal classProp = (ClassifiedProposal) marketProposal;

            // process the proposal if :
            // - its state is not dropped
            // - or its state is dropped but we must keep the proposals not valid
            // - the market is the one we are working with
            if ((classProp.getProposalState() != null && (!classProp.getProposalState().equals(ProposalState.DROPPED) || keepNotValidProposals))
                            && marketProposal.getMarket().getMarketCode().equals(marketCode)) {
                // the addProposal method check the side and put the proposal in the
                // right one
                newBook.addProposal(marketProposal);
            }
        }
        LOGGER.debug("{} complete book : {}", marketCode, newBook);
        newBook.setInstrument(marketBook.getInstrument());
        return newBook;

    }

    public void cleanMarketBookOnNewPrices(Instrument instrument, MarketCode marketCode) {
        String isin = instrument.getIsin();
        LOGGER.debug("New prices received from the market {} for the instrument {}, cleaning the book to remove old prices.", marketCode, isin);

        // [DR20121119] Questo controllo sembra superfluo, commenterei il tutto
        List<MarketPriceConnectionListener> isinBookListeners = bookListeners.get(isin);
        if (isinBookListeners == null) {
            LOGGER.warn("No quotation available event cannot be managed because there are no listeners registered for the isin " + isin);
            return;
        }

        BaseBook marketBook = marketBookMap.get(isin);
        if (marketBook == null) {
            LOGGER.warn("No quotation available event cannot be managed because there are no books registered for the isin " + isin);
            return;
        }

        cleanMarketBook(marketBook, marketCode);
    }

    private BaseBook cleanMarketBook(BaseBook marketBook, MarketCode marketCode) {

        synchronized (marketBook) {
            LOGGER.debug("Cleaning the book for the market {}", marketCode);

            Collection<Proposal> removeAskProposals = new ArrayList<Proposal>();
            Collection<Proposal> removeBidProposals = new ArrayList<Proposal>();
            for (Proposal prop : marketBook.getAskProposals()) {
                if (prop.getMarket() != null && prop.getMarket().getMarketCode().equals(marketCode)) {
                    LOGGER.debug("Removing proposal {}", prop);
                    removeAskProposals.add(prop);
                }
            }
            if (marketBook.getAskProposals() != null) {
                marketBook.getAskProposals().removeAll(removeAskProposals);
            }

            for (Proposal prop : marketBook.getBidProposals()) {
                if (prop.getMarket() != null && prop.getMarket().getMarketCode().equals(marketCode)) {
                    LOGGER.debug("Removing proposal {}", prop);
                    removeBidProposals.add(prop);
                }
            }
            if (marketBook.getBidProposals() != null) {
                marketBook.getBidProposals().removeAll(removeBidProposals);
            }

            LOGGER.debug("{} cleaned book : {}", marketCode, marketBook);
        }
        return marketBook;
    }

    /**
     * When a proposal arrives we also remove the market market maker from the list of those that did not reply to the price request for a given order.
     * 
     * Keeping a book for all the orders waiting on replies over a single ISIN may cause the side effect that, even if a market market maker did not reply, we already have its price in the book. Thus
     * completing the book without the need of a proposal reception. This way the aforementioned list will not be correctly cleaned.
     * 
     * Here we clean it because the book is complete and we can remove the market makers in the book still available in the list.
     * 
     * @param order
     * @param marketBook
     */
    private void cleanOrderMarketMakersNotReplyingList(Order order, BaseBook marketBook) {
        Map<MarketMarketMaker, String> mmmNotQuoting = order.getMarketMarketMakerNotQuotingInstr();
        Collection<Proposal> proposals = null;
        if (OrderSide.BUY.equals(order.getSide())) {
            proposals = marketBook.getAskProposals();
        } else {
            proposals = marketBook.getBidProposals();
        }

        for (Proposal prop : proposals) {
            MarketMarketMaker propMmm = prop.getMarketMarketMaker();
            if (mmmNotQuoting.containsKey(propMmm)) {
                LOGGER.info("Order " + order.getFixOrderId() + ", the market market maker " + propMmm + " replied and is already in the book, remove it from the list of those not replying.");
                mmmNotQuoting.remove(propMmm);
            }
        }
    }

    /**
     * When there are no more listeners on a given isin we can also clean the stored book.
     * 
     * @param isin
     * @param isinBookListeners
     */
    private void cleanBook(String isin, List<MarketPriceConnectionListener> isinBookListeners) {
        LOGGER.debug("Instrument {}, cleaning the book associated with it, no more listeners waiting for prices.", isin);
        if (isinBookListeners.isEmpty()) {
            BaseBook book = marketBookMap.get(isin);
            book.getAskProposals().clear();
            book.getBidProposals().clear();
        }
    }

    @Override
    public void onBookError(String isin, MarketPriceConnection marketPriceConnection) {
        List<MarketPriceConnectionListener> isinBookListeners = bookListeners.get(isin);

        if (isinBookListeners == null) {
            LOGGER.warn("Error while processing prices replies for the isin " + isin + ", no listeners registered!");
            return;
        }
        for (MarketPriceConnectionListener isinBookListener : isinBookListeners) {
            String error = "The price processing routine did not send us a book";
            isinBookListener.onMarketBookNotAvailable(marketPriceConnection, error);
        }
        bookListeners.remove(isin);
    }

    public void onNoQuotationAvailable(String isin, MarketPriceConnection marketPriceConnection) {
        List<MarketPriceConnectionListener> realIsinBookListenersList = bookListeners.get(isin);
        if (realIsinBookListenersList == null) {
            LOGGER.warn("No quotation available event cannot be managed because there are no listeners registered for the isin " + isin);
            return;
        }

        BaseBook marketBook = marketBookMap.get(isin);
        if (marketBook == null) {
            LOGGER.warn("No quotation available event cannot be managed because there are no books registered for the isin " + isin);
            return;
        }

        List<MarketPriceConnectionListener> clonedIsinBookListenersList = new ArrayList<MarketPriceConnectionListener>();
        clonedIsinBookListenersList.addAll(realIsinBookListenersList);

        boolean keepNotValidProposals = false;
        for (MarketPriceConnectionListener isinBookListener : clonedIsinBookListenersList) {
            Order order = isinBookListener.getOrder();
            String orderId = order.getFixOrderId();
            if (order.getFixOrderId() != null) {
                // price discovery for the given market price connection ended : remove the timer
                LOGGER.info("Order " + order.getFixOrderId() + ", no quotation available from the market " + marketPriceConnection.getMarketCode()
                                + ", fill the book with not quoting instrument proposal for each market maker.");
                // remove from the list of the not quoting market makers those already in the book
                cleanMarketBook(marketBook, marketPriceConnection.getMarketCode());
                BaseBook propMarketBook = fillCompleteMarketBook(marketBook, marketPriceConnection.getMarketCode(), keepNotValidProposals);
                isinBookListener.onMarketBookComplete(marketPriceConnection, propMarketBook);
            }
            // done processing the listener, remove it from the original list
            realIsinBookListenersList.remove(isinBookListener);
        }
        clonedIsinBookListenersList = null;
        realIsinBookListenersList = null;
    }

    @Override
    public void onTimerExpired(MarketPriceConnection marketPriceConnection, String jobName) {
        if (!jobName.contains("@")) {
            LOGGER.warn("Timer " + jobName + " expired, but cannot be managed by the Proposal Aggregator.");
            return;
        }
        LOGGER.info("Timer " + jobName + " expired, start to manage it.");
        String isin = jobName.split("@")[0];
        String orderId = jobName.split("@")[1];

        Operation operation = null;
        try {
            operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
        } catch (Exception e) {
            StringBuilder message = new StringBuilder(512);
            message.append("Error while managing Operation Registry: ").append(e.getMessage());
            LOGGER.error(message.toString(), e);
            return;
        }

        LOGGER.info("The timer is related to the order " + orderId + " and isin " + isin);
        try {
            List<MarketPriceConnectionListener> realIsinBookListenersList = bookListeners.get(isin);

            if (realIsinBookListenersList == null) {
                StringBuilder message = new StringBuilder(512);
                message.append("The timer ").append(jobName).append(" cannot be managed because there are no listeners registered for the isin ").append(isin);
                LOGGER.warn(message.toString());
                operation.onApplicationError(operation.getState(), null, message.toString());
                return;
            }

            List<MarketPriceConnectionListener> clonedIsinBookListenersList = new ArrayList<MarketPriceConnectionListener>();
            clonedIsinBookListenersList.addAll(realIsinBookListenersList);

            BaseBook originalMarketBook = marketBookMap.get(isin);
            if (originalMarketBook == null) {
                String message = "The timer " + jobName + " cannot be managed because there are no books registered for the isin " + isin;
                LOGGER.warn(message);
                operation.onApplicationError(operation.getState(), null, message);
                return;
            }

            BaseBook marketBook = new BaseBook();
            marketBook.setInstrument(originalMarketBook.getInstrument());
            marketBook.getAskProposals().addAll(originalMarketBook.getAskProposals());
            marketBook.getBidProposals().addAll(originalMarketBook.getBidProposals());

            String priceTimeoutReason = Messages.getString("PRICE_TIMEOUT.0");

            boolean rightOrderFound = false;

            // find out the order to whom the timer is linked, loop through the market makers left not
            // quoting and build fake proposals with the reason read from the property PRICE_TIMEOUT.0
            for (MarketPriceConnectionListener isinBookListener : clonedIsinBookListenersList) {
                Order order = isinBookListener.getOrder();

                if (order.getFixOrderId() != null && order.getFixOrderId().equals(orderId)) {
                    if (!isinBookListener.isActive()) {
                        LOGGER.warn("Order " + orderId + ", timer expire " + jobName + " but isinbook listener not active!");
                        continue;
                    }

                    rightOrderFound = true;

                    // disactivate the listener the stop processing incoming proposals in the onProposal method
                    LOGGER.debug("Order {} disactivating listener, timer expired : {}", order.getFixOrderId(), jobName);
                    isinBookListener.deactivate();

                    LOGGER.info("Order {}, the timer expired, we filled the book, but it is not technically complete.", order.getFixOrderId());

                    boolean keepNotValidProposals = true;
                    // book with all the proposals, even the not valid, needed later to build TW and BV books because
                    // mmm not enabled for BBG could be enabled for the other markets
                    BaseBook allProposalsMarketBook = fillBookWithMarketMakersNotReplying(order, marketBook, priceTimeoutReason, marketPriceConnection, keepNotValidProposals);
                    keepNotValidProposals = false;
                    // book with the valid proposals only, this is the BBG complete book
                    BaseBook validProposalsMarketBook = fillBookWithMarketMakersNotReplying(order, marketBook, priceTimeoutReason, marketPriceConnection, keepNotValidProposals);
                    cleanMultipleMarketMakerProposals(marketPriceConnection.getMarketCode(), order.getInstrument(), allProposalsMarketBook, order);
                    cleanMultipleMarketMakerProposals(marketPriceConnection.getMarketCode(), order.getInstrument(), validProposalsMarketBook, order);
                    // Bloomberg book complete
                    isinBookListener.onMarketBookComplete(marketPriceConnection, validProposalsMarketBook);

                    // if the timer expired is that of bloomberg, we must simulate the expiration of the TW and BV timer
                    if (marketPriceConnection.getMarketCode().equals(MarketCode.BLOOMBERG)) {
                        Set<MarketPriceConnection> mktPriceConns = isinBookListener.getRemainingMarketPriceConnections();
                        Set<MarketPriceConnection> availableMktPriceConnections = new HashSet<MarketPriceConnection>();
                        availableMktPriceConnections.addAll(mktPriceConns);
                        for (MarketPriceConnection mktPriceConn : availableMktPriceConnections) {
                            MarketCode mktPriceConnCode = mktPriceConn.getMarketCode();
                            if (mktPriceConnCode != null && (mktPriceConnCode.equals(MarketCode.TW) || mktPriceConnCode.equals(MarketCode.BV))) {
                                LOGGER.debug("Order {}, building book on timer {} expiration for the market {}", order.getFixOrderId(), jobName, mktPriceConnCode);
                                try {
                                    createDependingMarketsBook(mktPriceConnCode, order.getInstrument(), allProposalsMarketBook, isinBookListener, order, ProposalSide.ASK);
                                    createDependingMarketsBook(mktPriceConnCode, order.getInstrument(), allProposalsMarketBook, isinBookListener, order, ProposalSide.BID);
                                } catch (BestXException e) {
                                    LOGGER.error("Exception while creating depending book", e);
                                }
                                keepNotValidProposals = false;
                                BaseBook priceConnBook = fillBookWithMarketMakersNotReplying(order, allProposalsMarketBook, priceTimeoutReason, mktPriceConn, keepNotValidProposals);
                                isinBookListener.onMarketBookComplete(mktPriceConn, priceConnBook);
                                priceConnBook = null;
                            }
                        }
                    }
                    // listener processing done, remove it from the real list
                    realIsinBookListenersList.remove(isinBookListener);

                    allProposalsMarketBook = null;
                    validProposalsMarketBook = null;
                }

                order = null;
            }

            // if we have never found the order for the expired timer send all in application error should never happen
            if (!rightOrderFound) {
                StringBuilder message = new StringBuilder(512);
                message.append("The timer ").append(jobName).append(" cannot be managed because the order ").append(orderId).append(" does not exist");
                LOGGER.error(message.toString());
                operation.onApplicationError(operation.getState(), null, message.toString());
                return;
            }

            clonedIsinBookListenersList = null;
        } catch (Exception e) {
            StringBuilder message = new StringBuilder(512);
            message.append("Generic Error while managing the timer ").append(jobName).append(": ").append(e.getMessage());
            LOGGER.error(message.toString(), e);
            operation.onApplicationError(operation.getState(), e, message.toString());
        }
    }

    /**
     * Remember that this method will remove the DROPPED proposals from the book
     * 
     * @param order
     * @param marketBook
     * @param reason
     * @param marketPriceConnection
     * @param keepNotValidProposals
     * @return
     */
    public BaseBook fillBookWithMarketMakersNotReplying(Order order, BaseBook marketBook, String reason, MarketPriceConnection marketPriceConnection, boolean keepNotValidProposals) {
        // remove from the list of the not quoting market makers those already in the book
        cleanOrderMarketMakersNotReplyingList(order, marketBook);

        // updating the reason of all the market makers
        Map<MarketMarketMaker, String> marketMarketMakersNotQuotingMap = order.getMarketMarketMakerNotQuotingInstr();
        for (MarketMarketMaker mmm : marketMarketMakersNotQuotingMap.keySet()) {
            LOGGER.info("Order {}, the market market maker {} did not reply, set the reason as {}", order.getFixOrderId(), mmm, reason);
            marketMarketMakersNotQuotingMap.put(mmm, reason);
        }

        return fillCompleteMarketBook(marketBook, marketPriceConnection.getMarketCode(), keepNotValidProposals);
    }

    public static String buildTimerName(String isin, String orderId, MarketCode marketCode) {
        return isin + TIMER_NAME_SEPARATOR + orderId + TIMER_NAME_SEPARATOR + marketCode;
    }

    public MarketFinder getMarketFinder() {
        return marketFinder;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    public void clearBooks() {
        for (BaseBook book : marketBookMap.values()) {
            book.getAskProposals().clear();
            book.getBidProposals().clear();
        }
    }

    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

}
