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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.bestexec.BookSorter;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
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
         SerialNumberService serialNumberService){
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
      this.priceResult.addError(source.getMarketCode() + " book not available");
      this.bookArrived(source, null);
   }

   private void bookArrived(MarketPriceConnection source, Book book) {
      PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId() + "_" + source.getMarketCode(), "Market Book arrived");
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
      PriceDiscoveryPerformanceMonitor.finalize(order.getCustomerOrderId() + "_" + source.getMarketCode(), "Market Book managed");
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
               PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "processPriceResult start");
               BaseBook processedBook = consolidatedBook;
               LOGGER.debug("Classify Book");

               processedBook.checkSides();
               ClassifiedBook classifiedBook = bookClassifier.getClassifiedBook(processedBook, order, previousAttempts, venues);

               Collection<? extends ClassifiedProposal> currentProposals;
               // 20091118 AMC se ripristino la proposta originale del MM devo farlo e poi riclassificare il book per non avere uno
               // stato inconsistente delle proposte
               if (classifiedBook == null) {
                  String error = "ClassifiedBook is null";
                  LOGGER.error(error);
                  throw new Exception(error);
               }
               if (OrderSide.BUY.equals(order.getSide())) {
                  currentProposals = classifiedBook.getAskProposals();
               }
               else {
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
               }

               LOGGER.debug("{} Sort Book", logStart);
               SortedBook sortedBook = null;
               try {
                  sortedBook = bookSorter.getSortedBook(classifiedBook);
               }
               catch (IllegalArgumentException e) {
                  LOGGER.warn("IllegalArgumentException in sorting book for order {}: original book is {}", order.getFixOrderId(), classifiedBook == null ? "null book" : classifiedBook.toString(), e);
                  throw e;
               }

               if (OrderSide.BUY.equals(order.getSide())) {
                  currentProposals = sortedBook.getAskProposals();
               }
               else {
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

               LOGGER.debug("{} Check the market makers that do not quote the instrument to build a not quoting proposal.", logStart);

               Collection<MarketMarketMaker> mmmNotQuotingInstr = order.getMarketMarketMakerNotQuotingInstr().keySet();
               if (mmmNotQuotingInstr != null) {
                  List<Venue> venuesAlreadyNotifiedList = new ArrayList<Venue>();
                  Map<MarketCode, List<Venue>> venuesAlreadyNotifiedByMarket = new HashMap<MarketCode, List<Venue>>();
                  for (MarketMarketMaker mmm : mmmNotQuotingInstr) {
                     MarketCode mktCode = mmm.getMarket().getMarketCode();
                     LOGGER.debug("{} Market Maker {}", logStart, mmm.getMarketSpecificCode());
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

                     }
                     else {
                        LOGGER.debug("{} Already built a not quoting proposal for the market maker {}, it is again the market maker {}", logStart, mmm.getMarketSpecificCode(),
                              mmm.getMarketMaker().getCode());
                     }
                  }
               }
               priceResult.setSortedBook(sortedBook);
               LOGGER.debug("Sorted Book state: {}", consolidatedBookState);
               priceResult.setState(consolidatedBookState);
               priceResult.setReason(reasonBuffer.toString());

               ApplicationStatisticsHelper.logStringAndUpdateOrderIds(order, "Order.Queue_" + priceService.getPriceServiceName() + ".Book." + order.getInstrument().getIsin(),
                     this.getClass().getName());

               listener.onPricesResult(priceService, priceResult);
            }
            catch (Exception e) {
               LOGGER.error("Exception while processing prices: " + e.getMessage(), e);
            }
         }

      });
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
               }
               else {
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
            }
            catch (SchedulerException e) {
               LOGGER.error("Order " + order.getFixOrderId() + ", cannot stop the timer " + timerName + "/" + marketPriceConnection.getClass().getSimpleName(), e);
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
            addErrorInReport(marketCode + " prices timed out");
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
