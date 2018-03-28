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
package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.ConfigurableBookClassifier;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.dao.PriceForgeRuleDAO;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBean;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBean.ActionType;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.markets.ConfigurableMarketConnectionRegistry;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.Book;
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
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.jsscommon.exceptions.CurrencyFormatException;
import it.softsolutions.jsscommon.exceptions.CurrencyMismatchException;

/**
 * 
 * Purpose: this class manages the sending of orders towards the internal bank desks working on the best price found in the price discovery
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 06/lug/2012
 * 
 **/
public class PriceForgeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceForgeService.class);
    private List<MarketMaker> marketMakersToRemove;
    static MarketMaker akrosDeskMarketMaker;
    static MarketCode priceForceMarketCode;

    private PriceForgeRuleDAO priceForgeRuleDao;

    private MarketMakerFinder marketMakerFinder;
    private VenueFinder venueFinder;

    private String marketMakerListStr;

    private MarketFinder marketFinder;
    private Market bestMarket;

    private ConfigurableMarketConnectionRegistry marketConnectionRegistry;
    private MarketConnection priceForgeMarket = null;

    private final List<Instrument> unavailableInstrumentsOnMarketist = new ArrayList<Instrument>();
    private Map<Order, PriceForgeRuleBean> ruleMap;
    private Venue bestVenue;
    private BigDecimal quantity;
    private ConfigurableBookClassifier bookClassifier;

    private Map<Order, QuotePresent> quotePresentMap;
    private String priceForgeMarketCfg;

    /**
     * Inner class used to manage the internal market maker quotes found in the price discovery
     * 
     * @author ruggero.rizzo
     * 
     *         AMC 20091113 X EUSEBIO
     */
    class QuotePresent {
        /**
         * Constructor
         * 
         * @param quote
         *            : the internal market maker quote
         */
        QuotePresent(ClassifiedProposal quote) {
            ProposalSide side = quote.getSide();
            if (side.equals(ProposalSide.ASK)) {
                this.askProposals = new ArrayList<ClassifiedProposal>();
                this.askProposals.add(quote);
            } else if (side.equals(ProposalSide.BID)) {
                this.bidProposals = new ArrayList<ClassifiedProposal>();
                this.bidProposals.add(quote);
            }
        }

        /**
         * Add a quote to those of the internal market maker
         * 
         * @param quote
         */
        void add(ClassifiedProposal quote) {
            ProposalSide side = quote.getSide();
            if (side.equals(ProposalSide.ASK)) {
                if (this.askProposals == null) {
                    this.askProposals = new ArrayList<ClassifiedProposal>();
                }
                this.askProposals.add(quote);
            } else if (side.equals(ProposalSide.BID)) {
                if (this.bidProposals == null) {
                    this.bidProposals = new ArrayList<ClassifiedProposal>();
                }
                this.bidProposals.add(quote);
            }
        }

        List<ClassifiedProposal> askProposals;
        List<ClassifiedProposal> bidProposals;

        /**
         * Check if we already have a quote for the given side
         * 
         * @param side
         *            : the proposal side
         * @return true if the given side proposals list exists
         */
        boolean isQuotePresent(ProposalSide side) {
            return (side.equals(ProposalSide.ASK) ? askProposals != null : bidProposals != null);
        }

        /**
         * Get the list of available quotes for the given side
         * 
         * @param side
         *            : the required side
         * @return a list of proposals
         */
        List<ClassifiedProposal> getPresentQuotes(ProposalSide side) {
            return (side.equals(ProposalSide.ASK) ? askProposals : bidProposals);
        }

        /**
         * Find the best proposal among those available for the given side
         * 
         * @param side
         *            : the side of the required best
         * @return the best proposal if available, null otherwise
         */
        ClassifiedProposal getBestPresentQuote(ProposalSide side) {
            ClassifiedProposal best;
            if (side.equals(ProposalSide.ASK)) {
                if (askProposals == null) {
                    return null;
                }
                best = askProposals.get(0);
                if (askProposals.size() > 1) {
                    for (ClassifiedProposal currentProposal : askProposals) {
                        if (currentProposal.getPrice().compareTo(best.getPrice()) < 0) {
                            best = currentProposal;
                        }
                    }
                }
            } else {
                if (bidProposals == null) {
                    return null;
                }
                best = bidProposals.get(0);
                if (bidProposals.size() > 1) {
                    for (ClassifiedProposal currentProposal : bidProposals) {
                        if (currentProposal.getPrice().compareTo(best.getPrice()) > 0) {
                            best = currentProposal;
                        }
                    }
                }
            }
            return best;
        }
    }

    /**
     * Set the market makers list string
     * 
     * @param marketMakerListStr
     */
    public void setMarketMakerListStr(String marketMakerListStr) {
        this.marketMakerListStr = marketMakerListStr;
    }

    /**
     * Initialization method
     */
    public void init() {
        if (quantity == null) {
            throw new ObjectNotInitializedException("quantity property not set");
        }
        if (marketConnectionRegistry == null) {
            throw new ObjectNotInitializedException("market Connection Registry property not set");
        }
        priceForgeMarket = marketConnectionRegistry.getMarketConnection(priceForceMarketCode);
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("market finder property not set");
        }
        try {
            bestMarket = marketFinder.getMarketsByCode(priceForceMarketCode).get(0);
        } catch (BestXException e) {
            LOGGER.error("Could not identify the market for Price Forge Service - " + e.getMessage());
            throw new ObjectNotInitializedException("market finder property set not correctly");
        }

        if (marketMakerFinder == null) {
            throw new ObjectNotInitializedException("market maker finder property not set");
        }
        if (venueFinder == null) {
            throw new ObjectNotInitializedException("venue finder property not set");
        }

        if (marketMakersToRemove == null) {
            marketMakersToRemove = new ArrayList<MarketMaker>();

            if (marketMakerListStr == null) {
                throw new ObjectNotInitializedException("market maker list property not set");
            }
        }
        if (marketMakerListStr.length() > 0) {
            int lastIndex = 0;
            int index = marketMakerListStr.indexOf(',', lastIndex);
            try {
                while (index != -1) {
                    String marketMakerStr = marketMakerListStr.substring(lastIndex, index).trim();
                    MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode(marketMakerStr);
                    lastIndex = index + 1;
                    index = marketMakerListStr.indexOf(',', lastIndex);
                    marketMakersToRemove.add(marketMaker);
                }
                // ultimo market maker
                String marketMakerStr = marketMakerListStr.substring(lastIndex).trim();
                MarketMaker marketMaker = marketMakerFinder.getMarketMakerByCode(marketMakerStr);
                marketMakersToRemove.add(marketMaker);

                // statico a disposizione degli handlers del mercato Price Forge
                akrosDeskMarketMaker = marketMakersToRemove.get(0);
            } catch (BestXException e) {
                LOGGER.info("Unable to find market maker in list " + marketMakerListStr);
            }
        }

        ruleMap = new HashMap<Order, PriceForgeRuleBean>();
        quotePresentMap = new HashMap<Order, QuotePresent>();
        try {
            bestVenue = venueFinder.getMarketMakerVenue(marketMakersToRemove.get(0));
        } catch (BestXException e) {
            LOGGER.error("Unable to find a valid Venue for market maker: " + marketMakersToRemove.get(0).getName());
        }
    }

    /**
     * Set the market maker finder
     * 
     * @param marketMakerFinder
     */
    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    /**
     * Remove the internal market maker proposals to prepare the building of the new best
     * 
     * @param book
     *            : the book to change
     * @param order
     *            : the order we are executing
     * @param rule
     *            : the rule set by the traders
     * @return a modified proposals list
     * @throws BestXException
     */
    public List<ClassifiedProposal> removeProposals(Book book, Order order, PriceForgeRuleBean rule) throws BestXException {
        if (book == null) {
            return null;
        }
        // PriceForgeRuleBean rule = getRule(book, order);
        List<ClassifiedProposal> removedProposals = null;
        if (rule != null) {
            LOGGER.info("OrderId " + order.getFixOrderId() + ": price forge rule found, rule is: " + rule.toString());
            removedProposals = removeProposals(book.getAskProposals());
            int i = removedProposals.size();
            LOGGER.info("removed " + i + " proposal(s) from the ask proposals in book");
            List<ClassifiedProposal> removedBidProposals = removeProposals(book.getBidProposals());
            i = removedBidProposals.size();
            LOGGER.info("removed " + i + " proposal(s) from the bid proposals in book");
            removedProposals.addAll(removedBidProposals);
        }
        return removedProposals;
    }

    /**
     * 
     * @param proposals
     * @return the number of proposals removed
     */
    private List<ClassifiedProposal> removeProposals(Collection<? extends Proposal> proposals) throws BestXException {
        int i = 0;
        ArrayList<ClassifiedProposal> proposalsToRemove = new ArrayList<ClassifiedProposal>();
        if (proposals == null) {
            return proposalsToRemove;
        }
        for (MarketMaker marketMaker : marketMakersToRemove) {
            for (ClassifiedProposal proposal : (Collection<ClassifiedProposal>) proposals) {
                if (proposal.getMarketMarketMaker() != null && proposal.getMarketMarketMaker().getMarketMaker() != null && proposal.getMarketMarketMaker().getMarketMaker().equals(marketMaker)) {
                    proposalsToRemove.add(proposal);
                    i++;
                }
            }
        }
        int j = 0;
        for (ClassifiedProposal proposal : proposalsToRemove) {
            proposals.remove(proposal);
            LOGGER.info("removed proposal: price " + proposal.getPrice().getAmount().setScale(3, BigDecimal.ROUND_HALF_DOWN).toPlainString() + ", quantity "
                    + proposal.getQty().setScale(2, BigDecimal.ROUND_HALF_DOWN).toPlainString() + ", side " + proposal.getSide().getFixCode() + ", market maker "
                    + proposal.getMarketMarketMaker().getMarketMaker().getCode());
            j++;
        }
        if (i != j) {
            throw new BestXException("Unable to remove all proposals form Book");
        }
        return proposalsToRemove;
    }

    public void setPriceForgeRuleDao(PriceForgeRuleDAO priceForgeRuleDao) {
        this.priceForgeRuleDao = priceForgeRuleDao;
    }

    /**
     * 
     * @param bestProposal
     *            a proposal which serves as the basis for the new created best proposal
     * @param side
     *            the order side
     * @param rule
     *            the rule specifying how to build the new best proposal.
     * @return a new best proposal created from the given rule or null if the rule does not specify how to create a new best proposal.
     */
    public ClassifiedProposal createBestProposal(Order order, ProposalSide side, PriceForgeRuleBean rule) {
        if (rule == null || rule.getAction().compareTo(PriceForgeRuleBean.ActionType.ADD_SPREAD) != 0) {
            return null;
        }

        ClassifiedProposal proposal = new ClassifiedProposal();

        proposal.setSpread(rule.getSpread());
        proposal.setProposalState(ProposalState.NEW);
        proposal.setSide(side);
        proposal.setType(Proposal.ProposalType.SPREAD_ON_BEST);

        proposal.setFutSettDate(order.getFutSettDate());
        proposal.setNonStandardSettlementDateAllowed(true);
        proposal.setPrice(new Money(order.getCurrency(), BigDecimal.ZERO));
        proposal.setQty(BigDecimal.ZERO);

        proposal.setTimestamp(DateService.newLocalDate());
        proposal.setMarket(bestMarket);
        MarketMaker marketMaker = marketMakersToRemove.get(0);
        MarketMarketMaker mm = null;
        for (MarketMarketMaker m : marketMaker.getMarketMarketMakers()) {
            if (m.getMarket().getMarketCode().compareTo(bestMarket.getMarketCode()) == 0) {
                mm = m;
                continue;
            }
        }
        proposal.setMarketMarketMaker(mm);
        proposal.setVenue(bestVenue);
        return proposal;
    }

    /**
     * Creates a zero quantity zero price proposal to be set as the other sid of the newly created proposal with createBestProposal Use it
     * always after createBestProposal and set otherSideProposal with it
     * 
     * @param otherSideProposal
     * @return
     */
    public ClassifiedProposal createOtherSideProposal(ClassifiedProposal otherSideProposal) {
        ClassifiedProposal proposal = otherSideProposal.clone();
        proposal.setSide(otherSideProposal.getSide().compareTo(ProposalSide.ASK) == 0 ? ProposalSide.BID : ProposalSide.ASK);
        proposal.setQty(BigDecimal.ZERO);
        proposal.setType(Proposal.ProposalType.SET_TO_ZERO);
        try {
            proposal.setPrice(proposal.getPrice().subtract(proposal.getPrice()));
        } catch (Exception e) {
            // non puo' esserci
            LOGGER.error("currency mismatch");
        }
        return proposal;
    }

    public PriceForgeRuleBean getRule(Book book, Order order) {
        PriceForgeRuleBean rule = null;
        if (ruleMap.containsKey(order)) {
            rule = ruleMap.get(order);
        } else {
            if (priceForgeRuleDao != null) {
                PriceForgeRuleBean.ProposalSide side = order.getSide() == OrderSide.BUY ? PriceForgeRuleBean.ProposalSide.ASK : PriceForgeRuleBean.ProposalSide.BID;
                rule = priceForgeRuleDao.getRule(order.getInstrument(), side, order);
            }

            if (rule != null && ActionType.CHECK_PROPOSAL.equals(rule.getAction())) {
                LOGGER.debug("Order {}, the rule found is a CHECK_PROPOSAL, managing it.", order.getFixOrderId());
                try {
                    checkProposal(book, rule);
                } catch (BestXException e) {
                    LOGGER.error("Order {}, error while checking bloomberg proposals for a found rule with action CHECK_PROPOSAL.", order.getFixOrderId(), e);
                }
                LOGGER.debug("Order {}, CHECK_PROPOSAL rule managed.", order.getFixOrderId());

            }
            ruleMap.put(order, rule);
        }
        return rule;
    }

    public void disposeQuotePresent(Order order) {
        quotePresentMap.remove(order);
    }

    public void setQuotePresent(Order order, ClassifiedProposal proposal) {
        if (quotePresentMap.containsKey(order)) {
            QuotePresent quotePresent = quotePresentMap.get(order);
            quotePresent.add(proposal);
        } else {
            quotePresentMap.put(order, new QuotePresent(proposal));
        }
    }

    public ClassifiedProposal getBestQuotePresent(Order order, ProposalSide side) {
        if (quotePresentMap.containsKey(order)) {
            QuotePresent quotePresent = quotePresentMap.get(order);
            return quotePresent.getBestPresentQuote(side);
        } else {
            return null;
        }
    }

    public boolean isQuotePresent(Order order, ProposalSide side) {
        if (quotePresentMap.containsKey(order)) {
            return quotePresentMap.get(order).isQuotePresent(side);
        } else {
            return false;
        }
    }

    public void disposeRule(Order order) {
        ruleMap.remove(order);
    }

    private ClassifiedProposal getAskBest(SortedBook book, PriceForgeRuleBean rule) {
        ClassifiedProposal best = null;
        for (ClassifiedProposal proposal : book.getAskProposals()) {
            if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0
                    && (proposal.getProposalState().compareTo(ProposalState.VALID) == 0 || proposal.getProposalState().compareTo(ProposalState.NEW) == 0)) {
                if (best == null) {
                    best = proposal;
                    continue;
                }
            }
        }
        return best;
    }

    private ClassifiedProposal getBidBest(SortedBook book, PriceForgeRuleBean rule) {
        ClassifiedProposal best = null;
        for (ClassifiedProposal proposal : book.getBidProposals()) {
            if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0
                    && (proposal.getProposalState().compareTo(ProposalState.VALID) == 0 || proposal.getProposalState().compareTo(ProposalState.NEW) == 0)) {
                if (best == null) {
                    best = proposal;
                    continue;
                }
            }
        }
        return best;
    }

    public void replacePriceForgeProposalsWithOriginalProposals(BaseBook book, Order order, PriceForgeRuleBean rule) {
        LOGGER.debug("replacePFProposalsWithOriginalProposals START");
        LOGGER.info("Replacing original proposals because ConsolidatedBook is empty - needs one more Proposal and Book Classification");

        /*
         * 20110124 Ruggero If the rule action is NONE, the Akros proposals must be removed from the book. Because we removed these
         * proposals upon reception of the book, what we must do here is not restore them if the rule is NONE.
         */
        // PriceForgeRuleBean rule = getRule(book, order);
        boolean canRemove = true;
        if (rule != null) {
            String ruleAction = rule.getAction().getString();
            LOGGER.debug("Rule action is {}", ruleAction);
            canRemove = !ruleAction.equals(PriceForgeRuleBean.ActionType.NO_PRICE.getString());
        }
        if (canRemove) {
            removeBothSidesPriceForgeRules(book);
            if (isQuotePresent(order, ProposalSide.ASK)) {
                // lato ASK
                ClassifiedProposal best = getBestQuotePresent(order, ProposalSide.ASK);
                Collection<Proposal> askProps = book.getAskProposals();
                if (askProps == null) {
                    askProps = new ArrayList<Proposal>();
                    book.setAskProposals(askProps);
                }
                askProps.add(best);

                // lato BID
                MarketMarketMaker mmm = best.getMarketMarketMaker();
                QuotePresent quotePresent = quotePresentMap.get(order);
                List<ClassifiedProposal> bidPresentProps = quotePresent.getPresentQuotes(ProposalSide.BID);
                ClassifiedProposal otherSideProp = null;
                for (ClassifiedProposal prop : bidPresentProps) {
                    if (prop.getMarketMarketMaker().equals(mmm)) {
                        otherSideProp = prop;
                        break;
                    }
                }
                Collection<Proposal> bidProps = book.getBidProposals();
                if (bidProps == null) {
                    bidProps = new ArrayList<Proposal>();
                    book.setBidProposals(bidProps);
                }
                bidProps.add(otherSideProp);
            }
        } else {
            LOGGER.info("Rule action for this order is NONE, no need to restore original proposals.");
        }
        LOGGER.debug("replacePFProposalsWithOriginalProposals STOP");
    }

    private long getCurrentTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        return now;
    }

    // Monitorable
    private long pfReplacementTime = 0;

    public boolean changeBestProposals(BaseBook book, SortedBook sortedBook, Order order, PriceForgeRuleBean rule) {
        LOGGER.info("Change Proposals with type SET_TO_ZERO and SPREAD_ON_BEST - START");
        long startTime = getCurrentTimeInMillis();
        // PriceForgeRuleBean rule = getRule(book, order);
        boolean setTradeable = true;

        if (rule != null) {
            LOGGER.info("OrderID {}: price forge rule found, rule is: {}", order.getFixOrderId(), rule);
        }
        // creiamo liste che possono contenere anche le proposal da rimuovere, infatti ora si puo' arrivare
        // qua anche senza che siano presenti proposal sul mercato Price Forge e quindi nulla e' stato rimosso, ma sono state
        // aggiunte le best SPREAD_ON_BEST
        ArrayList<Proposal> newProposalList = null;
        ArrayList<Proposal> removeProposalList = null;
        ArrayList<ClassifiedProposal> newClassifiedProposalList = null;
        ArrayList<ClassifiedProposal> oldClassifiedProposalList = null;

        ClassifiedProposal bestAsk = getAskBest(sortedBook, rule);
        ClassifiedProposal bestBid = getBidBest(sortedBook, rule);
        if (bestAsk != null) {
            LOGGER.debug("bestAsk = {}, {}, {}, {}, {}", bestAsk.getSide(), (bestAsk.getPrice() != null ? bestAsk.getPrice().getAmount() : bestAsk.getPrice()), bestAsk.getQty(),
                    bestAsk.getMarket(), bestAsk.getMarketMarketMaker());
        } else {
            LOGGER.debug("bestAsk = {}", bestAsk);
        }

        if (bestBid != null) {
            LOGGER.debug("bestBid = {}, {}, {}, {}, {}", bestBid.getSide(), (bestBid.getPrice() != null ? bestBid.getPrice().getAmount() : bestBid.getPrice()), bestBid.getQty(),
                    bestBid.getMarket(), bestBid.getMarketMarketMaker());
        } else {
            LOGGER.debug("bestBid = {}", bestBid);
        }

        if ((bestAsk == null && bestBid == null) || (bestAsk == null && order.getSide().compareTo(OrderSide.BUY) == 0) || (bestBid == null && order.getSide().compareTo(OrderSide.SELL) == 0)) {
            removeBothSidesPriceForgeRules(book, sortedBook);

            LOGGER.info("Change Proposals with type SET_TO_ZERO and SPREAD_ON_BEST - STOP");

            disposeRule(order);
            disposeQuotePresent(order);
            return true;
        }

        if ((bestAsk != null && bestBid != null) || (bestAsk == null && bestBid != null && order.getSide().compareTo(OrderSide.SELL) == 0)
                || (bestAsk != null && bestBid == null && order.getSide().compareTo(OrderSide.BUY) == 0)) {
            newProposalList = new ArrayList<Proposal>();
            removeProposalList = new ArrayList<Proposal>();

            for (Proposal proposal : book.getAskProposals()) {
                ClassifiedProposal clProposal = (ClassifiedProposal) proposal;
                ClassifiedProposal newProposal = clProposal.clone();
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0) {
                    newProposal = spreadProposalToNormalProposal(bestAsk, (ClassifiedProposal) newProposal, setTradeable);
                } else if (proposal.getType().compareTo(ProposalType.SET_TO_ZERO) == 0) {
                    newProposal = completeZeroProposal(order, bestAsk, (ClassifiedProposal) newProposal);
                }
                newProposalList.add(newProposal);
                removeProposalList.add(proposal);
            }

            book.setAskProposals(newProposalList);
            book.getAskProposals().removeAll(removeProposalList);

            newClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            oldClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            for (ClassifiedProposal proposal : ((Collection<? extends ClassifiedProposal>) sortedBook.getAskProposals())) {
                ClassifiedProposal newProposal = proposal.clone();

                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0) {
                    newProposal = spreadProposalToNormalProposal(bestAsk, newProposal, setTradeable);
                } else if (proposal.getType().compareTo(ProposalType.SET_TO_ZERO) == 0) {
                    newProposal = completeZeroProposal(order, bestAsk, newProposal);
                }
                newClassifiedProposalList.add(newProposal);
                oldClassifiedProposalList.add(proposal);
            }
            sortedBook.setAskProposals(newClassifiedProposalList);
            sortedBook.getAskProposals().removeAll(oldClassifiedProposalList);

            newProposalList = new ArrayList<Proposal>();
            removeProposalList = new ArrayList<Proposal>();

            for (Proposal proposal : book.getBidProposals()) {
                ClassifiedProposal clProposal = (ClassifiedProposal) proposal;
                ClassifiedProposal newProposal = clProposal.clone();

                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0) {
                    newProposal = spreadProposalToNormalProposal(bestBid, newProposal, setTradeable);
                } else if (proposal.getType().compareTo(ProposalType.SET_TO_ZERO) == 0) {
                    newProposal = completeZeroProposal(order, bestBid, newProposal);
                }
                newProposalList.add(newProposal);
                removeProposalList.add(proposal);
            }
            book.setBidProposals(newProposalList);
            book.getBidProposals().removeAll(removeProposalList);

            newClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            oldClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            for (ClassifiedProposal proposal : ((Collection<? extends ClassifiedProposal>) sortedBook.getBidProposals())) {
                ClassifiedProposal newProposal = proposal.clone();

                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0) {
                    newProposal = spreadProposalToNormalProposal(bestBid, newProposal, setTradeable);
                } else if (proposal.getType().compareTo(ProposalType.SET_TO_ZERO) == 0) {
                    newProposal = completeZeroProposal(order, bestBid, newProposal);
                }
                newClassifiedProposalList.add(newProposal);
                oldClassifiedProposalList.add(proposal);

            }
            sortedBook.setBidProposals(newClassifiedProposalList);
            sortedBook.getBidProposals().removeAll(oldClassifiedProposalList);
        }

        pfReplacementTime = getCurrentTimeInMillis() - startTime;
        LOGGER.info("[STATISTICS] Price Forge replacement of proposals time in millis: {}", pfReplacementTime);
        LOGGER.info("Change Proposals with type SET_TO_ZERO and SPREAD_ON_BEST - STOP");

        disposeRule(order);
        disposeQuotePresent(order);
        return true;
    }

    /**
     * @param book
     * @param sortedBook
     */
    private void removeBothSidesPriceForgeRules(BaseBook book, SortedBook sortedBook) {
        ArrayList<Proposal> newProposalList;
        ArrayList<ClassifiedProposal> newClassifiedProposalList;
        boolean removedClassifiedAsk = false;
        boolean removedClassifiedBid = false;
        List<ClassifiedProposal> lastAskClassifiedProposals = sortedBook.getAskProposals();
        List<ClassifiedProposal> lastBidClassifiedProposals = sortedBook.getBidProposals();
        {
            newProposalList = new ArrayList<Proposal>();
            // tira via le proposte aggiunte con PriceForge, ossia quelle con tipo SPREAD_ON_BEST e SET_TO_ZERO
            // AMC 20090917 Modificato in modo da togliere la proposta SPREAD sul lato senza best che la proposta ZERO sul lato opposto
            for (Proposal proposal : book.getAskProposals()) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from ask proposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                }
            }
            book.setAskProposals(newProposalList);

            newClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            for (ClassifiedProposal proposal : sortedBook.getAskProposals()) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newClassifiedProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from ask classifiedProposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                    removedClassifiedAsk = true;
                }
            }
            sortedBook.setAskProposals(newClassifiedProposalList);

            newProposalList = new ArrayList<Proposal>();
            for (Proposal proposal : book.getBidProposals()) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from bid proposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                }
            }
            book.setBidProposals(newProposalList);

            newClassifiedProposalList = new ArrayList<ClassifiedProposal>();
            for (ClassifiedProposal proposal : ((Collection<? extends ClassifiedProposal>) sortedBook.getBidProposals())) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newClassifiedProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from bid classifiedProposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                    removedClassifiedBid = true;
                }
            }
            sortedBook.setBidProposals(newClassifiedProposalList);
        }
        /*
         * L'unico caso noto in cui po' essere stata rimossa la proposta da uno solo dei due lati e' quando l'originale proposta PriceForge
         * sia stata sostituita da un ProposalClassifier, in particolare con la proposta counter del MarketMaker sul lato a cardinalita'
         * maggiore. Cio' provoca, se non gestito, un book squilibrato
         */
        if (removedClassifiedAsk && !removedClassifiedBid) {
            LOGGER.info("Removed classified ask and not bid: reset Ask book to last");
            sortedBook.setAskProposals(lastAskClassifiedProposals); // ripristino il lato che ha cardinalita' inferiore
        } else if (!removedClassifiedAsk && removedClassifiedBid) {
            LOGGER.info("Removed classified bid and not ask: reset Bid book to last");
            sortedBook.setBidProposals(lastBidClassifiedProposals); // ripristino il lato che ha cardinalita' inferiore
        }
    }

    /**
     * @param book
     */
    private void removeBothSidesPriceForgeRules(BaseBook book) {
        ArrayList<Proposal> newProposalList;
        {
            newProposalList = new ArrayList<Proposal>();
            // tira via le proposte aggiunte con PriceForge, ossia quelle con tipo SPREAD_ON_BEST e SET_TO_ZERO
            for (Proposal proposal : book.getAskProposals()) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from ask proposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                }
            }
            book.setAskProposals(newProposalList);

            newProposalList = new ArrayList<Proposal>();
            for (Proposal proposal : book.getBidProposals()) {
                if (proposal.getType().compareTo(ProposalType.SPREAD_ON_BEST) != 0 && proposal.getType().compareTo(ProposalType.SET_TO_ZERO) != 0) {
                    newProposalList.add(proposal);
                } else {
                    LOGGER.info("Removed proposal: {}, {}, {}, {}, {} from bid proposalList", proposal.getSide(),
                            (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
                }
            }
            book.setBidProposals(newProposalList);
        }
    }

    private ClassifiedProposal spreadProposalToNormalProposal(ClassifiedProposal best, ClassifiedProposal proposal, boolean setTradeable) {
        if (proposal.getSpread() == null) {
            proposal.setSpread(BigDecimal.ZERO);
        }
        if (best != null) {
            proposal.setCustomerAdditionalExpenses(best.getCustomerAdditionalExpenses());
            proposal.setAccruedDays(best.getAccruedDays());
            proposal.setAccruedInterest(best.getAccruedInterest());
            proposal.setCommission(best.getCommission());
            proposal.setCommissionTick(best.getCommissionTick());
            proposal.setCommissionType(best.getCommissionType());
            proposal.setOrderPrice(best.getOrderPrice());
            try {
                if (best.getSide().compareTo(Proposal.ProposalSide.BID) == 0) {
                    proposal.setPrice(best.getPrice().sum(new Money(best.getPrice().getStringCurrency(), proposal.getSpread())));
                } else {
                    proposal.setPrice(best.getPrice().subtract(new Money(best.getPrice().getStringCurrency(), proposal.getSpread())));
                }
                proposal.setPriceTelQuel(proposal.getPrice());
            } catch (CurrencyMismatchException ce) {
                // non puo' esserci
                LOGGER.error("currency mismatch", ce);
            } catch (CurrencyFormatException e) {
                // should never happen
                LOGGER.error("currency format mismatch", e);
            }
        } else {
            LOGGER.info("null best proposal as parameter. Returning null proposal...");
            return null;
        }
        proposal.setQty(quantity);
        if (proposal.getProposalState().compareTo(ProposalState.DROPPED) != 0 && proposal.getProposalState().compareTo(ProposalState.REJECTED) != 0) {
            proposal.setProposalState(ProposalState.VALID);
        }
        if (setTradeable) {
            proposal.setType(ProposalType.TRADEABLE);
        } else {
            proposal.setType(ProposalType.INDICATIVE);
        }

        LOGGER.info(
                "Spread proposal changed to best proposal: {}, {}, {}, {}, {}",
                proposal.getSide(), (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(), proposal.getMarketMarketMaker());
        return proposal;
    }

    private ClassifiedProposal completeZeroProposal(Order order, ClassifiedProposal best, ClassifiedProposal proposal) {

        if (best != null) {
            proposal.setCustomerAdditionalExpenses(best.getCustomerAdditionalExpenses());
            proposal.setAccruedDays(best.getAccruedDays());
            proposal.setAccruedInterest(best.getAccruedInterest());
            proposal.setCommission(best.getCommission());
            proposal.setCommissionTick(best.getCommissionTick());
            proposal.setCommissionType(best.getCommissionType());
            proposal.setOrderPrice(best.getOrderPrice());
        } else {
            LOGGER.info("null best proposal as parameter. Not adding commission and accrued paramters...");
        }
        proposal.setType(ProposalType.INDICATIVE);

        LOGGER.info(
                "Zero proposal completed: {}, {}, {}, {}, {}",
                proposal.getSide(), (proposal.getPrice() != null ? proposal.getPrice().getAmount() : proposal.getPrice()), proposal.getQty(), proposal.getMarket(),
                        proposal.getMarketMarketMaker());
        return proposal;
    }

    public boolean addBestProposal(Book book, Order order, PriceForgeRuleBean rule) {
        if (unavailableInstrumentsOnMarketist.contains(order.getInstrument())) {
            LOGGER.info("Instrument unavailable on market due to previous empty book");
            return false;
        }
        if (!priceForgeMarket.isBuySideConnectionAvailable() || !priceForgeMarket.isBuySideConnectionEnabled()) {
            LOGGER.info("market unavailable/disabled.");
            return false;
        }

        // PriceForgeRuleBean rule = getRule(book, order);
        if (rule != null) {
            LOGGER.info("OrderID {}, price forge rule found, rule is: {}", order.getFixOrderId(), rule);
        }
        /*
         * try { removeProposals(book, order); } catch (BestXException e) { LOGGER.info("Unable to remove proposals from the book"); }
         */
        ProposalSide side = order.getSide().compareTo(OrderSide.BUY) == 0 ? ProposalSide.ASK : ProposalSide.BID;
        ClassifiedProposal proposal = createBestProposal(order, side, rule);
        if (proposal == null) {
            return false;
        } else {
            LOGGER.info("Created Price Forge Best proposal is {}", proposal);
            book.addProposal(proposal);
            proposal = createOtherSideProposal(proposal);
            LOGGER.info("Created Price Forge  Other Side proposal is {}", proposal.toString());
            book.addProposal(proposal);
        }
        return true;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    public void addInstrumentToUnavailableList(Instrument instrument) {
        if (!unavailableInstrumentsOnMarketist.contains(instrument)) {
            unavailableInstrumentsOnMarketist.add(instrument);
        }
    }

    public void setMarketConnectionRegistry(ConfigurableMarketConnectionRegistry marketConnectionRegistry) {
        this.marketConnectionRegistry = marketConnectionRegistry;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setBookClassifier(ConfigurableBookClassifier bookClassifier) {
        this.bookClassifier = bookClassifier;
    }

    public MarketConnection getPriceForgeMarket() {
        return priceForgeMarket;
    }

    public static MarketMaker getOwnDeskMarketMaker() {
        return akrosDeskMarketMaker;
    }

    public static MarketCode getPriceForgeMarketCode() {
        return priceForceMarketCode;
    }

    public String getPriceForgeMarketCfg() {
        return priceForgeMarketCfg;
    }

    public void setPriceForgeMarketCfg(String priceForgeMarketCfg) {
        this.priceForgeMarketCfg = priceForgeMarketCfg;
        priceForceMarketCode = MarketCode.valueOf(priceForgeMarketCfg);
    }

    public Book processRule(PriceForgeRuleBean rule, Book originalBook, Order order) {
        String ruleAction = rule.getAction().getString();

        LOGGER.debug("Order {}. The rule action is {}", order.getFixOrderId(), ruleAction);

        // rimuovo le proposal se:
        // - strategia NONE
        // - esiste una proposal akros sul mercato Price Forge e strategia NONE DEFAULT
        // - stategia BEST o BEST Default
        if (ruleAction.equals(PriceForgeRuleBean.ActionType.NO_PRICE.getString()) || ruleAction.equals(PriceForgeRuleBean.ActionType.NO_PRICE_DEFAULT.getString())
                || ruleAction.equals(PriceForgeRuleBean.ActionType.ADD_SPREAD.getString())) {
            LOGGER.debug("Order {}. The rule found is a NONE or is a Default NONE and we have an Akros proposal on the Price Forge market or is a BEST one.", order.getFixOrderId());
            // strategia NONE, elimino le proposals
            try {
                LOGGER.debug("Order {}. Removing the internal market maker proposals.", order.getFixOrderId());

                List<ClassifiedProposal> removedProposals = removeProposals(originalBook, order, rule);
                if (removedProposals != null) {
                    for (ClassifiedProposal removedProposal : removedProposals) {
                        // salvo solo le proposal Akros sul mercato del Price Forge
                        if (removedProposal.getMarket().getMarketCode().equals(PriceForgeService.getPriceForgeMarketCode())) {
                            setQuotePresent(order, removedProposal);
                        }
                    }
                }
            } catch (BestXException e) {
                LOGGER.info("Unable to remove proposals: ", e);
            } catch (Exception e1) {
                LOGGER.info("Unable to remove proposals: ", e1);
            }
        }

        // se la strategia lo richiede (BEST o BEST Default), creo la nuova best (o meglio, predispongo
        // una proposal che verra' migliorata in base alla strategia dopo aver classificato il book)
        if (ruleAction.equals(PriceForgeRuleBean.ActionType.ADD_SPREAD.getString())) {
            // aggiungi la finta best
            LOGGER.debug("Order {}. Trying to add the best proposal because the strategy is BEST.", order.getFixOrderId());

            // APPLY BEST STRATEGY
            if (!addBestProposal(originalBook, order, rule)) {
                LOGGER.error("Order {}. Unable to add the Price Forge Proposals!", order.getFixOrderId());
            }
        }
        return originalBook;
    }

    private PriceForgeRuleBean checkProposal(Book book, PriceForgeRuleBean rule) throws BestXException {
        // load both sides proposals
        LOGGER.debug("Checking the book for the existence of bloomberg Akros proposals.");
        List<Collection<Proposal>> proposalsSides = new ArrayList<Collection<Proposal>>();
        proposalsSides.add((Collection<Proposal>) book.getAskProposals());
        proposalsSides.add((Collection<Proposal>) book.getBidProposals());

        boolean ownProp = false;
        for (Collection<Proposal> proposals : proposalsSides) {
            for (Proposal proposal : proposals) {
                LOGGER.debug("Processing side : {}", proposal.getSide());
                MarketMarketMaker proposalMMM = null;
                MarketMaker proposalMM = null;
                if (proposal.getMarketMarketMaker() != null) {
                    LOGGER.debug("Checking proposal {}", proposal);
                    proposalMMM = marketMakerFinder.getMarketMarketMakerByCode(proposal.getMarket().getMarketCode(), proposal.getMarketMarketMaker().toString());
                    if (proposalMMM != null) {
                        proposalMM = proposalMMM.getMarketMaker();
                    }
                }
                // mercato del Price Forge, mm interno di Akros
                if (proposal.getMarket().getMarketCode().equals(MarketCode.BLOOMBERG) && proposalMM != null && proposalMM.equals(PriceForgeService.getOwnDeskMarketMaker())) {
                    LOGGER.debug("Own {} proposal on {} found!", proposal.getSide(), PriceForgeService.getPriceForgeMarketCode());
                    ownProp = true;
                    break;
                }
            }
        }
        if (ownProp) {
            LOGGER.debug("We have an internal proposal on Bloomberg, changing the rule to the None Default.");
            return priceForgeRuleDao.getDefNoneRule();
        } else {
            LOGGER.debug("We have not an internal proposal on Bloomberg, no action on the book.");
            return null;
        }

    }
}