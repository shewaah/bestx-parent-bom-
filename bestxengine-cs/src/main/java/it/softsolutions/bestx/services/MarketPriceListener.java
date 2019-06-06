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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.bestexec.BookSorter;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBean;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.services.price.PriceResult.PriceResultState;
import it.softsolutions.bestx.services.price.PriceResultBean;
import it.softsolutions.bestx.services.price.PriceServiceListener;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
@SuppressWarnings("unused")
public class MarketPriceListener implements MarketPriceConnectionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketPriceListener.class);

    private PriceResultState consolidatedBookState = PriceResultState.COMPLETE;
    final private String logIdentifier;
    final private StringBuffer reasonBuffer = new StringBuffer();
    
    // [DR20131114] @see http://stackoverflow.com/questions/11350723/collections-newsetfrommapconcurrenthashmap-vs-collections-synchronizedset
    private final Set<MarketPriceConnection> remainingMarketPriceConnections = Collections.newSetFromMap(new ConcurrentHashMap<MarketPriceConnection, Boolean>());
    
    private final long t0 = DateService.currentTimeMillis();
    final private BaseBook consolidatedBook;
    final private PriceServiceListener listener;
    final private Order order;
    final private List<Attempt> previousAttempts;
    final private Set<Venue> venues;
    private Date creationDate;
    private boolean active = true;

    private PriceForgeService priceForgeService;
    private PriceForgeInstrumentsManager priceForgeInstrManager;
	private PriceForgeCustomerManager priceForgeCustManager;

    private CSPriceService priceService;
    private Executor executor;
    private BookClassifier bookClassifier;
    private BookSorter bookSorter;
    private MarketMakerFinder marketMakerFinder;
    private BookProposalBuilder akrosBookProposalBuilder;
    private MarketFinder marketFinder;
    private String checkDisabledInternalMarketMaker;
    private VenueFinder venueFinder;
    private PriceResultBean priceResult;

    public MarketPriceListener(PriceServiceListener requestor, Instrument instrument, Order order, List<Attempt> previousAttempts, Set<Venue> venues,
            Collection<MarketPriceConnection> marketPriceConnectionsEnabled, CSPriceService priceService, Executor executor, BookClassifier bookClassifier, BookSorter bookSorter,
            MarketMakerFinder marketMakerFinder, BookProposalBuilder akrosBookProposalBuilder, MarketFinder marketFinder, String checkDisabledInternalMarketMaker, VenueFinder venueFinder,
            PriceForgeService priceForgeService, PriceForgeCustomerManager priceForgeCustManager, PriceForgeInstrumentsManager priceForgeInstrManager,
            SerialNumberService serialNumberService) {
        this.listener = requestor;
        this.order = order;
        this.previousAttempts = previousAttempts;
        this.venues = venues;
        this.remainingMarketPriceConnections.addAll(marketPriceConnectionsEnabled);
        this.consolidatedBook = new BaseBook();
        this.consolidatedBook.setInstrument(instrument);
        this.logIdentifier = "[PRICESRV],Operation=" + order.getFixOrderId() + ",ISIN=" + order.getInstrumentCode();
        LOGGER.debug("{} start", this.logIdentifier);
        creationDate = DateService.newLocalDate();
        this.priceService = priceService;
        this.executor = executor;
        this.bookClassifier = bookClassifier;
        this.bookSorter = bookSorter;
        this.akrosBookProposalBuilder = akrosBookProposalBuilder;
        this.marketFinder = marketFinder;
        this.marketMakerFinder = marketMakerFinder;
        this.checkDisabledInternalMarketMaker = checkDisabledInternalMarketMaker;
        this.venueFinder = venueFinder;
        this.priceForgeService = priceForgeService;
        this.priceForgeCustManager = priceForgeCustManager;
        this.priceForgeInstrManager = priceForgeInstrManager;
        this.priceResult = new PriceResultBean();
    }
    
    @Override
    public synchronized void onMarketBookComplete(MarketPriceConnection source, Book book) {
        LOGGER.debug("{} Market Book Complete: {}", logIdentifier, source.getClass().getName());
        
        // Book is complete, so clear the list of NotQuoting marketMaker
        order.clearMarketMakerNotQuotingInstr();

        bookArrived(source, book);
    }

    @Override
    public synchronized void onMarketBookNotAvailable(MarketPriceConnection source, String reason) {
        LOGGER.debug("{} Market Book Unavailable: {} ({})", logIdentifier, source.getClass().getName(), reason);
        this.consolidatedBookState = PriceResultState.ERROR;
        this.reasonBuffer.append(this.reasonBuffer.length() > 0 ? ", " + reason : reason);
        // intervento per Desk corporates per modificare il contenuto del book in accordo con il set di regole descritto in tabella
        // PriceForgeRuleTable
        if (priceForgeService != null && canUsePriceForge(PriceForgeService.getPriceForgeMarketCode(), order)) {
            if (source.getMarketCode().compareTo(PriceForgeService.getPriceForgeMarketCode()) == 0) {
                // se sul mercato di Price Forge non ho lo strumento disponibile, non potro' applicare le regole di price forge
                // percio' inserisco nell'apposita lista di PriceForgeService il titolo di cui non ho potuto ricevere i prezzi
                priceForgeService.addInstrumentToUnavailableList(this.consolidatedBook.getInstrument());
            }
        }
        this.priceResult.addError(source.getMarketCode()+" book not available");
        this.bookArrived(source, null);
    }

    private void bookArrived(MarketPriceConnection source, Book book) {
    	PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId()+"_"+source.getMarketCode(), "Market Book arrived");
        Date start = DateService.newLocalDate();
        if (book != null) {
            LOGGER.info("Order {}, book arrived for the market {}, adding proposals for both sides.", order.getFixOrderId(), source.getMarketCode());
            for (Proposal proposal : book.getAskProposals()) {
                this.consolidatedBook.addProposal(proposal);
            }
            for (Proposal proposal : book.getBidProposals()) {
                this.consolidatedBook.addProposal(proposal);
            }
            LOGGER.debug("Order {}, new proposal added to the consolidated book, now it is : {}", order.getFixOrderId(), consolidatedBook);
        }
        PriceDiscoveryPerformanceMonitor.finalize(order.getCustomerOrderId()+"_"+source.getMarketCode(), "Market Book managed");
        if (remainingMarketPriceConnections.remove(source)) {
            if (remainingMarketPriceConnections.size() == 0) {
                // MONITOR log
                LOGGER.debug("[MONITOR] BookClassification start.");
                deactivate();
                LOGGER.info("{},EndTime={}", logIdentifier, (DateService.currentTimeMillis() - t0));
                LOGGER.debug("Order {}, Process the prices received. Consolidated Book : {}", order.getFixOrderId(), consolidatedBook);
                PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Scheduling processPriceResult");
                processPriceResult();
                book = null;
                priceService.getMarketPriceListeners().remove(this);
                // MONITOR log
                LOGGER.debug("[MONITOR] BookClassification stop.");
                Date end = DateService.newLocalDate();
                long time = end.getTime() - start.getTime();
                LOGGER.info("[STATISTICS] Book Classification time in millis: {}", time);
            }
        }
    }

    private void processPriceResult() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    String logStart = "Order " + order.getFixOrderId() + ". ";
                    Thread.currentThread().setName(logStart + "Price-process-" + listener.toString());
                    PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "processPriceResult start");
                    /*
                     * Ho il book completo con le proposal di tutti i mercati Ora devo estrarre la strategia per l'isin oggetto dell'ordine
                     * IL punto chiave e' quando ho a che fare con una strategia NONE di default, in questo caso, se non sono presenti
                     * proposal sul mercato del Price Forge non devo fare nulla, altrimenti posso rimuovere quelle di banca Akros da questo
                     * mercato e da BBG.
                     */

//                    PriceForgeRuleBean rule = null;
//                    // intervento per Desk corporates per modificare il contenuto del book in accordo con il set di regole descritto in
//                    // tabella PriceForgeRuleTable
//                    if (priceForgeService != null && canUsePriceForge(PriceForgeService.getPriceForgeMarketCode(), order)) {
//                        rule = priceForgeService.getRule(consolidatedBook, order);
//                    } else {
//                        if (priceForgeService == null) {
//                            LOGGER.debug("{} PriceForgeService not available.", logStart);
//                        } else {
//                            LOGGER.debug("{} PriceForgeService cannot be used.", logStart);
//                        }
//                    }
//
                    // MSA 20110322 Get OrderId
                    // se rule e' != null, vuol dire che anche il price Forge Service lo e' e
                    // e' possibile utilizzarlo
                    BaseBook processedBook = consolidatedBook;
//                    if (rule != null) {
//                        LOGGER.debug("{}, processing the rule found.", logStart);
//                        processedBook = (BaseBook) priceForgeService.processRule(rule, consolidatedBook, order);
//                        LOGGER.debug("{}, rule processed, working with the possibly modified book.", logStart);
//                    } else {
//                        LOGGER.debug(
//                                "{}, No rules available, no default rule could be determined, the ISIN {} is probably new and has no asset type and bond type defined. Leave the market proposals unchanged.",
//                                logStart, order.getInstrument().getIsin());
//                    }
                    LOGGER.debug("Classify Book");

                    processedBook.checkSides();
                    ClassifiedBook classifiedBook = bookClassifier.getClassifiedBook(processedBook, order, previousAttempts, venues);

                    Collection<? extends ClassifiedProposal> currentProposals;
                    // 20091118 AMC se ripristino la proposta originale del MM devo farlo e poi riclassificare il book per non avere uno
                    // stato inconsistente delle proposte
                    if (OrderSide.BUY.equals(order.getSide())) {
                        currentProposals = classifiedBook.getAskProposals();
                    } else {
                        currentProposals = classifiedBook.getBidProposals();
                    }
                    LOGGER.debug("Check that at least one price is valid for order side");
                    boolean atLeastOnePriceAvailable = false;
                    for (ClassifiedProposal currentProposal : currentProposals) {
                        if (Proposal.ProposalState.VALID.equals(currentProposal.getProposalState())) {
                            if (Proposal.ProposalType.SET_TO_ZERO != currentProposal.getType() && Proposal.ProposalType.SPREAD_ON_BEST != currentProposal.getType()) {
                                atLeastOnePriceAvailable = true;
                                break;
                            }
                        }
                    }
                    boolean replaced = false;
                    if (!atLeastOnePriceAvailable) {
                        LOGGER.info("Order {}, no valid price for side: {}", order.getFixOrderId(), order.getSide().name());
//                        if (priceForgeService != null && canUsePriceForge(PriceForgeService.getPriceForgeMarketCode(), order)) {
//                            LOGGER.debug("Order {}, putting back in the book the original proposal removed on behalf of the price forge best", order.getFixOrderId());
//                            priceForgeService.replacePriceForgeProposalsWithOriginalProposals(processedBook, order, rule);
//                            replaced = true;
//                            LOGGER.debug("Order {}, performing again the book classification.", order.getFixOrderId());
//                            classifiedBook = bookClassifier.getClassifiedBook(processedBook, order, previousAttempts, venues);
//                        }
                    }

                    LOGGER.debug("{} Sort Book", logStart);
                    SortedBook sortedBook = null;
                    try {
                    	sortedBook = bookSorter.getSortedBook(classifiedBook);
                    } catch (IllegalArgumentException e) {
                    	LOGGER.warn("IllegalArgumentException in sorting book for order {}: original book is {}", order.getFixOrderId(), classifiedBook == null? "null book" : classifiedBook.toString(), e);
                    	throw e;
        			}
                    // intervento per Desk corporates per modificare il contenuto del book in accordo con il set di regole descritto in
                    // tabella PriceForgeRuleTable
//                    if (priceForgeService != null && !replaced && canUsePriceForge(PriceForgeService.getPriceForgeMarketCode(), order)) {
//                        priceForgeService.changeBestProposals(processedBook, sortedBook, order, rule);
//                    }

                    if (OrderSide.BUY.equals(order.getSide())) {
                        currentProposals = sortedBook.getAskProposals();
                    } else {
                        currentProposals = sortedBook.getBidProposals();
                    }

                    LOGGER.debug("Check that at least one price is valid for order side");
                    atLeastOnePriceAvailable = false;
                    for (ClassifiedProposal currentProposal : currentProposals) {
                        if (Proposal.ProposalState.VALID.equals(currentProposal.getProposalState())) {
                            atLeastOnePriceAvailable = true;
                            break;
                        }
                    }
                    if (!atLeastOnePriceAvailable) {
                        LOGGER.info("No valid price for side: {}", order.getSide().name());
                        consolidatedBookState = PriceResultState.ERROR;
                    }

                    /*
                     * 20110301 - Ruggero For every market that does not list the instrument, build a "fake" proposal to let the trader know
                     * what happened to this trading venue. We must build a proposal for both sides.
                     */
                    // MSA 20110322: Moved Up
                    /*
                     * String fixOrderId = order.getFixOrderId(); marketNotQuotingOrderInstr =
                     * marketNotQuotingOrderInstrMap.get(fixOrderId);
                     */

                    /*
                     * add empty BID & ASK proposals for market not quoting instrument
                     */
                    Map<MarketCode, String> marketsAndReasons = order.getMarketNotQuotingInstr();
                    Collection<MarketCode> marketCodes = marketsAndReasons.keySet();
                    LOGGER.debug("{} markets not quoting instrument : {}", logStart, marketCodes);

                    for (MarketCode mktCode : marketCodes) {
                        LOGGER.debug("{} Creating ASK and BID proposals for market not quoting instrument, mkt : {}", logStart, mktCode.name());

                        String reason = marketsAndReasons.get(mktCode);

                        ClassifiedProposal newAskProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, null, order, reason);
                        newAskProp.setSide(ProposalSide.ASK);
                        List<ClassifiedProposal> askProps = sortedBook.getAskProposals();
                        if (askProps != null) {
                            int lastIdx = sortedBook.getAskProposals().size();
                            askProps.add(lastIdx, newAskProp);
                        }

                        ClassifiedProposal newBidProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, null, order, reason);
                        newBidProp.setSide(ProposalSide.BID);
                        List<ClassifiedProposal> bidProps = sortedBook.getBidProposals();
                        if (bidProps != null) {
                            int lastIdx = sortedBook.getBidProposals().size();
                            bidProps.add(lastIdx, newBidProp);
                        }

                        LOGGER.debug("{} Descriptive proposals for market not quoting instrument, mkt {}, built and added to the book.", logStart, mktCode.name());
                    }
                    /*
                     * add empty BID & ASK proposals for market not negotiating instrument
                     */
                    marketsAndReasons.clear();
                    marketsAndReasons = order.getMarketNotNegotiatingInstr();
                    marketCodes.clear();
                    marketCodes = marketsAndReasons.keySet();
                    LOGGER.debug("{} markets not negotiating instrument : {}", logStart, marketCodes);

                    for (MarketCode mktCode : marketCodes) {
                        LOGGER.debug("{} Creating ASK and BID proposals for market not negotiating instrument, mkt : {}", logStart, mktCode.name());

                        String reason = marketsAndReasons.get(mktCode);

                        ClassifiedProposal newAskProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, null, order, reason);
                        newAskProp.setSide(ProposalSide.ASK);
                        List<ClassifiedProposal> askProps = sortedBook.getAskProposals();
                        if (askProps != null) {
                            int lastIdx = sortedBook.getAskProposals().size();
                            askProps.add(lastIdx, newAskProp);
                        }

                        ClassifiedProposal newBidProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, null, order, reason);
                        newBidProp.setSide(ProposalSide.BID);
                        List<ClassifiedProposal> bidProps = sortedBook.getBidProposals();
                        if (bidProps != null) {
                            int lastIdx = sortedBook.getBidProposals().size();
                            bidProps.add(lastIdx, newBidProp);
                        }

                        LOGGER.debug("{} Descriptive proposals for market not negotiating instrument, mkt {}, built and added to the book.", logStart, mktCode.name());
                    }

                    /*
                     * 20110303 - Ruggero For every market connection check if there are market makers that do not quote the isin and build
                     * a suitable proposal
                     * 
                     * With the PriceFOrge generating a BEST proposal for the Akros internal MM we must not build one if it is one of those
                     * not quoting the instrument.
                     */
                    MarketMaker priceForgeMM = PriceForgeService.getOwnDeskMarketMaker();
                    MarketCode priceForgeMktCode = PriceForgeService.getPriceForgeMarketCode();

                    LOGGER.debug("{} Check the market makers that do not quote the instrument to build a not quoting proposal.", logStart);

                    Collection<MarketMarketMaker> mmmNotQuotingInstr = order.getMarketMarketMakerNotQuotingInstr().keySet();
                    if (mmmNotQuotingInstr != null) {
                        List<Venue> venuesAlreadyNotifiedList = new ArrayList<Venue>();
                        Map<MarketCode, List<Venue>> venuesAlreadyNotifiedByMarket = new HashMap<MarketCode, List<Venue>>();
                        for (MarketMarketMaker mmm : mmmNotQuotingInstr) {
                            MarketCode mktCode = mmm.getMarket().getMarketCode();
                            LOGGER.debug("{} Market Maker {}", logStart, mmm.getMarketSpecificCode());
//                            if (mktCode != null && mktCode.equals(priceForgeMktCode) && rule != null && rule.getAction() != null) {
//
//                                LOGGER.debug(
//                                        "{} Market maker of the price forge market ({}), check if the market maker is the internal one and the price forge strategy is BEST, if so we cannot build a market maker not quoting instrument proposal.",
//                                        logStart, priceForgeMktCode.name());
//                                MarketMaker mm = mmm.getMarketMaker();
//                                if (mm != null && mm.equals(priceForgeMM) && rule.getAction().equals(PriceForgeRuleBean.ActionType.ADD_SPREAD)) {
//                                    LOGGER.debug("{} Internal market maker ({}) and the price forge strategy is BEST, do not build a market maker not quoting instrument proposal.", logStart,
//                                            priceForgeMM.getCode());
//                                    continue;
//                                }
//                            }

                            //
                            // Some market market makers are the same trading venue, there is no need
                            // to insert n proposals because the trader will see the venue name.
                            //
                            // e.g. we can have:
                            //
                            // market1 --> mmm1, mmm2
                            // market2 --> mmm3
                            //
                            // all corresponding to the same mm
                            //
                            // and we must create ONE fake, empty proposal for market1,
                            // and ONE for market2
                            // so we keep a different list for each market
                            //
                            // previously we had only one list,
                            // but if mmm3 did receive no price (so a fake proposal was created for market2),
                            // and mmm1/mmm2 received no price, no fake proposal was created for market1
                            // ("Already built a not quoting proposal for the market maker ...")
                            //
                            Venue mmmVenue = venueFinder.getMarketMakerVenue(mmm.getMarketMaker());

                            if (venuesAlreadyNotifiedByMarket.get(mktCode) == null) {
                                venuesAlreadyNotifiedByMarket.put(mktCode, new ArrayList<Venue>());
                            }
                            venuesAlreadyNotifiedList = venuesAlreadyNotifiedByMarket.get(mktCode);
                            if (!venuesAlreadyNotifiedList.contains(mmmVenue)) {
                                venuesAlreadyNotifiedList.add(mmmVenue);
                                String reason = order.getReason(mmm);
                                ClassifiedProposal newAskProp = null;
                                if (reason == null) {
                                    reason = Messages.getString("RejectProposalISINNotQuotedByMM");
                                }
                                newAskProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, mmm, order, reason);

                                newAskProp.setSide(ProposalSide.ASK);
                                LOGGER.debug("{} Market Maker {}, created not quoting ASK proposal {}", logStart, mmm.getMarketSpecificCode(), newAskProp);

                                List<ClassifiedProposal> askProps = sortedBook.getAskProposals();
                                if (askProps != null) {
                                    int lastIdx = sortedBook.getAskProposals().size();
                                    askProps.add(lastIdx, newAskProp);
                                }

                                ClassifiedProposal newBidProp = null;
                                if (reason == null) {
                                    reason = Messages.getString("RejectProposalISINNotQuotedByMM");
                                }
                                newBidProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, mmm, order, reason);

                                newBidProp.setSide(ProposalSide.BID);
                                LOGGER.debug("{} Market Maker {}, created not quoting BID proposal {}", logStart, mmm.getMarketSpecificCode(), newAskProp);

                                List<ClassifiedProposal> bidProps = sortedBook.getBidProposals();
                                if (bidProps != null) {
                                    int lastIdx = sortedBook.getBidProposals().size();
                                    bidProps.add(lastIdx, newBidProp);
                                }

                            } else {
                                LOGGER.debug("{} Already built a not quoting proposal for the market maker {}, it is again the market maker {}", logStart, mmm.getMarketSpecificCode(), mmm.getMarketMaker().getCode());
                            }
                        }
                    }

                    if (priceForgeService != null && canUsePriceForge(PriceForgeService.getPriceForgeMarketCode(), order)) {
                        Venue priceForgeVenue = null;
                        try {
                            priceForgeVenue = venueFinder.getMarketMakerVenue(priceForgeMM);
                        } catch (BestXException e) {
                            LOGGER.error("{} Error while finding the venue corresponding to the price forge market maker {} and finding out if it is in the customer policy ({}).", logStart, priceForgeMM, venues, e);
                        }
                        Market priceForgeMarket = marketFinder.getMarketByCode(priceForgeMktCode, null);
                        sortedBook = fixPriceForgeBookPosition(sortedBook, priceForgeVenue, order, priceForgeMarket, priceForgeMM);
                    }

                    priceResult.setSortedBook(sortedBook);
                    LOGGER.debug("Sorted Book state: {}", consolidatedBookState);
                    priceResult.setState(consolidatedBookState);
                    priceResult.setReason(reasonBuffer.toString());

                    ApplicationStatisticsHelper.logStringAndUpdateOrderIds(order, "Order.Queue_" + priceService.getPriceServiceName() + ".Book." + order.getInstrument().getIsin(), this.getClass()
                            .getName());

                    listener.onPricesResult(priceService, priceResult);
                } catch (Exception e) {
                    LOGGER.error("Exception while processing prices: {}", e.getMessage(), e);
                }
            }

        });
    }

    /**
     * Check the book for a proposal of the PriceForge market and market maker. Find out if the market is in policy and fix the book if it
     * is not.
     * 
     * If the market is in policy and the PriceForge market is not the Best Execution, build a fake proposal stating that the market maker
     * does not quote the ISIN.
     * 
     * If the market is not in policy and the PriceForge market is the Best Execution, remove the proposal from the book. This behaviour is
     * already managed in the book classification phase, check the DiscardUnavailableVenueProposalClassifier classifier.
     * 
     * If the market is in policy and the PriceForge market is the Best Execution : no action.
     * 
     * @param sortedBook
     * @return the same sortedBook eventually fixed
     */
    public SortedBook fixPriceForgeBookPosition(SortedBook sortedBook, Venue priceForgeVenue, Order order, Market priceForgeMkt, MarketMaker priceForgeMM) {
        String logStart = "Order " + order.getFixOrderId() + ". ";
        LOGGER.debug("{} check if the book should be fixed for there could be the price forge market out of the customer policy.", logStart);

        if (sortedBook != null && sortedBook.getValidSideProposals(order.getSide()) != null && sortedBook.getValidSideProposals(order.getSide()).size() > 0) {
            boolean isPriceForgeMMInPolicy = true;
            isPriceForgeMMInPolicy = venues.contains(priceForgeVenue);

            ClassifiedProposal bestProposal = sortedBook.getBestProposalBySide(order.getSide());
            Market bestPropMkt = bestProposal.getMarket();
            if (isPriceForgeMMInPolicy && priceForgeMkt.equals(bestPropMkt)) {
                LOGGER.debug("{} PriceForge market in policy AND best execution. No change applied to the book.", logStart);
            } else if (isPriceForgeMMInPolicy && !priceForgeMkt.equals(bestPropMkt)) {
                LOGGER.debug("{} PriceForge market in policy AND not best execution. Insert a fake empty proposal in the book.", logStart);
                if (!order.isBestExecutionRequired()) {
                    LOGGER.info("{} It is an order for the Systematic Internalizer, do not insert the PriceForge market fake proposal.", logStart);
                } else {
                    MarketCode mktCode = priceForgeMkt.getMarketCode();
                    // For the priceForge market maker we have only one market market maker
                    List<MarketMarketMaker> marketMarketMakers = priceForgeVenue.getMarketMaker().getMarketMarketMakerForMarket(mktCode);
                    MarketMarketMaker priceForgeMarketMarketMaker = marketMarketMakers.get(0);
                    List<ClassifiedProposal> bookProposals = null;
                    if (ProposalSide.ASK.equals(bestProposal.getSide())) {
                        bookProposals = sortedBook.getBidProposals();
                    } else {
                        bookProposals = sortedBook.getAskProposals();
                    }
                    // Check if we already have a proposal for the price forge market maker in the book,
                    // if so do not build an empty one.
                    // This could happen when a previous attempt went on the price forge and was rejected
                    if (!proposalForPriceForgeMarketMakerAlreadyExistant(bookProposals, priceForgeMkt, priceForgeMarketMarketMaker, logStart)) {
                        LOGGER.debug("{} No proposals for the price forge market and market maker already exists in the book, build the empty one.", logStart);
                        String reason = Messages.getString("RejectProposalISINNotQuotedByMM");
                        // Build the empty proposals to show that we did not go on the price forge market
                        // because the market maker did not quote the instrument
                        ClassifiedProposal newAskProp = null;
                        try {
                            newAskProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, priceForgeMarketMarketMaker, order, reason);
                            newAskProp.setSide(ProposalSide.ASK);
                            List<ClassifiedProposal> askProps = sortedBook.getAskProposals();
                            if (askProps != null) {
                                int lastIdx = sortedBook.getAskProposals().size();
                                askProps.add(lastIdx, newAskProp);
                            }
                        } catch (Exception e) {
                            LOGGER.error("{} Error while building the empty ASK proposal.", logStart, e);
                        }

                        ClassifiedProposal newBidProp = null;
                        try {
                            newBidProp = akrosBookProposalBuilder.buildEmptyProposalWithReason(mktCode, priceForgeMarketMarketMaker, order, reason);
                            newBidProp.setSide(ProposalSide.BID);
                            List<ClassifiedProposal> bidProps = sortedBook.getBidProposals();
                            if (bidProps != null) {
                                int lastIdx = sortedBook.getBidProposals().size();
                                bidProps.add(lastIdx, newBidProp);
                            }
                        } catch (Exception e) {
                            LOGGER.error("{} Error while building the empty BID proposal.", logStart, e);
                        }
                    }
                }
            } else {
                if (!isPriceForgeMMInPolicy) {
                    LOGGER.info("{} The price forge market maker {} is not in policy, nothing to do with the book, the proposal has been market as DROPPED during the classification process.",
                            logStart, priceForgeMM);
                }
            }
        } else {
            LOGGER.info("{} Book empty or without valid proposals, nothing to check and fix.", logStart);
        }
        return sortedBook;
    }

    private boolean proposalForPriceForgeMarketMakerAlreadyExistant(List<ClassifiedProposal> bookProposals, Market priceForgeMarket, MarketMarketMaker priceForgeMarketMarketMaker, String logStart) {
        boolean exists = false;

        for (ClassifiedProposal proposal : bookProposals) {
            Market propMarket = proposal.getMarket();
            MarketMarketMaker propMarketMarketMaker = proposal.getMarketMarketMaker();

            if (propMarket.equals(priceForgeMarket) && propMarketMarketMaker.equals(priceForgeMarketMarketMaker)) {
                LOGGER.info("{} In the book already exists a proposal for the price forge market maker, keep it and do not build an empty one.", logStart);
                exists = true;
                break;
            }
        }

        return exists;
    }

    @Override
    public Order getOrder() {
        return this.order;
    }

    public boolean hasVenue(String marketMakerCode) {
        boolean hasVenue = false;

        for (Venue venue : venues) {
            if (venue.getCode().equals(marketMakerCode)) {
                hasVenue = true;
                break;
            }
        }

        return hasVenue;
    }

    public Set<Venue> getVenues() {
        return venues;
    }

    /**
     * Loads a list of market market makers for the given market. CSBESTXPOC-107 : CS customization, loads only those that canTrade the
     * instrument and that are enabled.
     * 
     */
    @Override
    public List<MarketMarketMaker> getMarketMarketMakersForMarket(MarketCode marketCode) {
        List<MarketMarketMaker> marketMarketMakers = null;
        Instrument instrument = order.getInstrument();
        boolean bestExecutionRequired = order.isBestExecutionRequired();
        for (Venue venue : venues) {
            if (VenueType.MARKET_MAKER.equals(venue.getVenueType())) {
                if (marketMarketMakers == null) {
                    marketMarketMakers = new ArrayList<MarketMarketMaker>();
                }
                List<MarketMarketMaker> mmms = venue.getMarketMaker().getMarketMarketMakerForMarket(marketCode);
                // 20120521 Ruggero : check trading capabilities of the market market maker before
                // adding it to the list of the market marker makers whose prices we should wait for.
                for (MarketMarketMaker mmm : mmms) {
                    boolean canTrade = mmm.canTrade(instrument, bestExecutionRequired);
                    if (canTrade) {
                        marketMarketMakers.add(mmm);
                    } else {
                        LOGGER.debug("Order {} , the market market maker {} cannot trade the instrument {}, do not expect prices from him.", order.getFixOrderId(), mmm, instrument);
                    }
                }

            }
        }

        return marketMarketMakers;
    }

    @Override
    public List<MarketMarketMaker> getMarketMarketMakersForEnabledMarkets() {
        List<MarketMarketMaker> marketMarketMakers = null;

        for (Venue venue : venues) {
            for (MarketPriceConnection marketPriceConn : remainingMarketPriceConnections) {
                if (VenueType.MARKET_MAKER == venue.getVenueType()) {
                    if (venue.getMarketMaker().isEnabled()) {
                        if (marketMarketMakers == null) {
                            marketMarketMakers = new ArrayList<MarketMarketMaker>();
                        }
                        List<MarketMarketMaker> mmms = venue.getMarketMaker().getMarketMarketMakerForMarket(marketPriceConn.getMarketCode());
                        marketMarketMakers.addAll(mmms);
                    }
                }
            }
        }

        return marketMarketMakers;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public int getNumWaitingReplyMarketPriceConnection() {
        int remainingConn = remainingMarketPriceConnections.size();
        return remainingConn;
    }

    @Override
    public Set<MarketPriceConnection> getRemainingMarketPriceConnections() {
        return remainingMarketPriceConnections;
    }

    @Override
    public synchronized boolean isActive() {
        return active;
    }

    private boolean canUsePriceForge(MarketCode marketCode, Order order) {
        LOGGER.debug("Order {}, Price Forge not used in CS.", order.getFixOrderId());
        return false;
    }

    @Override
    public synchronized boolean deactivate() {
        active = false;
        return active;
    }

    @Override
    public void onMarketBookComplete(MarketCode marketCode, Book book) {
        Iterator<MarketPriceConnection> iter = remainingMarketPriceConnections.iterator();
        while (iter.hasNext()) {
            MarketPriceConnection marketPriceConnection = (MarketPriceConnection) iter.next();
            
            if (marketPriceConnection.getMarketCode() == marketCode) {
                //remove the market specific timer for price request, we received all the prices requested
                String timerName = SimpleMarketProposalAggregator.buildTimerName(order.getInstrument().getIsin(), order.getFixOrderId(), marketCode);
                try {
                	SimpleTimerManager.getInstance().stopJob(timerName, marketPriceConnection.getClass().getSimpleName());
                } catch (SchedulerException e) {
                    LOGGER.error("Order {}, cannot stop the timer {}/{}", order.getFixOrderId(), timerName, marketPriceConnection.getClass().getSimpleName(), e);
                }

                onMarketBookComplete(marketPriceConnection, book);
                
                break;
            }
        }
    }
    
    @Override
    public synchronized void onMarketBookPartial(MarketCode marketCode, Book book, String reason, List<String> marketMakersOnBothSides) {
        // Book is partial, so remove the marketMakers quoting on both sides
        order.removeMarketMakerNotQuotingInstr(marketCode, marketMakersOnBothSides);
        
        Iterator<MarketPriceConnection> iter = remainingMarketPriceConnections.iterator();
        while (iter.hasNext()) {
            MarketPriceConnection marketPriceConnection = (MarketPriceConnection) iter.next();
            
            if (marketPriceConnection.getMarketCode() == marketCode) {
                
                if (consolidatedBookState.equals(PriceResultState.COMPLETE)) {
                    consolidatedBookState = PriceResultState.INCOMPLETE;
                }
                this.reasonBuffer.append(this.reasonBuffer.length() > 0 ? ", " + reason : reason);
                addErrorInReport(marketCode+" prices timed out");
                onMarketBookComplete(marketPriceConnection, book);
                break;
            }
        }

    }
    
    public void addErrorInReport(String error) {
    	priceResult.addError(error);
    }

	public void addErrorsInReport(List<String> errors) {
		priceResult.addErrors(errors);
	}
}
