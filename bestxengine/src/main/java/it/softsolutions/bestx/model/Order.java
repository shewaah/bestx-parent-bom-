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
package it.softsolutions.bestx.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: Models the order object extending the RFQ  
*
* Project Name : bestxengine 
* First created by: ruggero.rizzo
* Creation date: 05/set/2012 
* 
**/
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Rfq.OrderSide.OrderCapacity;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.jsscommon.Money;
import quickfix.fix50sp2.component.Parties;

public class Order extends Rfq {

   private static final Logger LOGGER = LoggerFactory.getLogger(Order.class);

   public static final String IS_EXECUTION_DESTINATION = "SI";

   public static enum OrderStatus {
      UNKNOWN, RECEIVED, CANCELLED, REJECTED, COUNTER_SENT, FILLED_PARTIAL, FILLED_TOTAL
   }

   // [DR20120613] Adopted the standard FIX constant instead of an anonymous "1" and "2"
   public static enum OrderType {
      NOT_RECOGNIZED(""), MARKET("" + quickfix.field.OrdType.MARKET), LIMIT("" + quickfix.field.OrdType.LIMIT), STOP("" + quickfix.field.OrdType.STOP_STOP_LOSS), STOP_LIMIT(
            "" + quickfix.field.OrdType.STOP_LIMIT), MARKET_ON_CLOSE("" + quickfix.field.OrdType.MARKET_ON_CLOSE), WITH_OR_WITHOUT("" + quickfix.field.OrdType.WITH_OR_WITHOUT), LIMIT_OR_BETTER(
                  "" + quickfix.field.OrdType.LIMIT_OR_BETTER), LIMIT_WITH_OR_WITHOUT("" + quickfix.field.OrdType.LIMIT_WITH_OR_WITHOUT), ON_BASIS("" + quickfix.field.OrdType.ON_BASIS), ON_CLOSE(
                        "" + quickfix.field.OrdType.ON_CLOSE), LIMIT_ON_CLOSE("" + quickfix.field.OrdType.LIMIT_ON_CLOSE), FOREX_MARKET("" + quickfix.field.OrdType.FOREX_MARKET), ATQUOTE(
                              "" + quickfix.field.OrdType.PREVIOUSLY_QUOTED), FOREX_LIMIT("" + quickfix.field.OrdType.FOREX_LIMIT), FOREX_SWAP(
                                    "" + quickfix.field.OrdType.FOREX_SWAP), FOREX_PREVIOUSLY_QUOTED("" + quickfix.field.OrdType.FOREX_PREVIOUSLY_QUOTED), FUNARI(
                                          "" + quickfix.field.OrdType.FUNARI), MARKET_IF_TOUCHED("" + quickfix.field.OrdType.MARKET_IF_TOUCHED), MARKET_WITH_LEFT_OVER_AS_LIMIT(
                                                "" + quickfix.field.OrdType.MARKET_WITH_LEFT_OVER_AS_LIMIT), PREVIOUS_FUND_VALUATION_POINT(
                                                      "" + quickfix.field.OrdType.PREVIOUS_FUND_VALUATION_POINT), NEXT_FUND_VALUATION_POINT(
                                                            "" + quickfix.field.OrdType.NEXT_FUND_VALUATION_POINT), PEGGED(
                                                                  "" + quickfix.field.OrdType.PEGGED), COUNTER_ORDER_SELECTION("" + quickfix.field.OrdType.COUNTER_ORDER_SELECTION);

      /**
       * Get the order side fix code
       * 
       * @return fix code for the side
       */
      public String getFixCode() {
         return mFIXValue;
      }

      private final String mFIXValue;

      private OrderType(String inFIXValue){
         mFIXValue = inFIXValue;
      }
   }

   public static enum TimeInForce {
      DAY_OR_SESSION(quickfix.field.TimeInForce.DAY), GOOD_TILL_CANCEL(quickfix.field.TimeInForce.GOOD_TILL_CANCEL), AT_THE_OPENING(quickfix.field.TimeInForce.AT_THE_OPENING), IMMEDIATE_OR_CANCEL(
            quickfix.field.TimeInForce.IMMEDIATE_OR_CANCEL), FILL_OR_KILL(quickfix.field.TimeInForce.FILL_OR_KILL), GOOD_TILL_CROSSING(
                  quickfix.field.TimeInForce.GOOD_TILL_CROSSING), GOOD_TILL_DATE(quickfix.field.TimeInForce.GOOD_TILL_DATE), AT_THE_CLOSE(quickfix.field.TimeInForce.AT_THE_CLOSE);

      public char getFixCode() {
         return mFIXValue;
      }

      /**
       * GEt the time force starting from the fix code
       * @param fixCode of the time in force tag
       * @return the BestX! enum value
       */
      public static TimeInForce getByFixCode(char fixCode) {
         TimeInForce timeInForce = mFIXValueTable.get(fixCode);
         return timeInForce == null ? null : timeInForce;
      }

      private TimeInForce(char inFIXValue){
         mFIXValue = inFIXValue;
      }

      private final char mFIXValue;
      private static final Map<Character, TimeInForce> mFIXValueTable;
      static {
         Map<Character, TimeInForce> table = new HashMap<Character, TimeInForce>();
         for (TimeInForce timeInForce : values()) {
            table.put(timeInForce.getFixCode(), timeInForce);
         }
         mFIXValueTable = Collections.unmodifiableMap(table);
      }
   }

   public static enum IsMatchingFromQueryStates {
      NONE, TRUE, FALSE
   }

   private IsMatchingFromQueryStates isMatchingFromQuery;
   protected String customerOrderId = null; //clOrdId
   protected Order.OrderType type = OrderType.NOT_RECOGNIZED;
   protected Money limit = null;
   protected String currency = null;
   private String fIXOrderId = null;
   protected Venue venue = null;
   protected TimeInForce timeInForce = null;
   protected Date timeInForceDate = null;
   private boolean matchingOrder = false;
   private boolean law262Passed = true;
   private boolean addCommissionToCustomerPrice;
   protected String executionDestination;

   /* AMC 20170822 MiFID II Related fields */
   protected Boolean miFIDRestricted = null;
   protected TradingCapacity tradingCapacity = null;
   protected Parties decisionMaker = null;
   protected Parties executionDecisor = null;
   /** this is the client which is presented to the markjet, not to be confused with the Customer
   */
   protected Parties client = null;
   protected OrderSide shortSellIndicator = null;
   protected OrderCapacity orderCapacity = null;

   // [RR20110328] Maps where we save market/mmm not quoting the order instrument
   protected Map<MarketCode, String> marketNotQuotingInstr = new TreeMap<MarketCode, String>();
   protected Map<MarketCode, String> marketNotNegotiatingInstr = new TreeMap<MarketCode, String>(); // instrument quoted on market, but currently non negotiated (QuotingStatus!= NEG)
   protected Map<MarketMarketMaker, String> mmmNotQuotingInstr = new ConcurrentHashMap<MarketMarketMaker, String>(64);
   private PriceDiscoveryType priceDiscoveryType;
   private int logIdCounter = 0;
   private int bestExecutionVenueFlag;
   protected int priceType;
   private String orderSource = null;
   //[RR20130916]BXMNT-300: field used to store the ISIN or whatever is used as a code for the order instrument
   protected String instrumentCode;
   //[RR20131205] Store the comment sent by the customer, it will be used if the order is a limit file
   protected String text;
   private Double bestPriceDeviationFromLimit;
   protected String ticketOwner;

   //[SP20180712]BESTX-335 Limit file orders TMO manage
   protected String custOrderHandlingInstr;
   private int tryAfterMinutes;
   protected Date effectiveTime;

   protected String handlInst;

   public TradingCapacity getTradingCapacity() {
      return this.tradingCapacity;
   }

   public void setTradingCapacity(TradingCapacity tradingCapacity) {
      this.tradingCapacity = tradingCapacity;
   }

   /**
    * Get the execution destination
    * 
    * @return execution destination
    */
   public String getExecutionDestination() {
      return this.executionDestination;
   }

   /**
    * Set the execution destination
    * 
    * @param executionDestination
    *            the execution destination
    */
   public void setExecutionDestination(String executionDestination) {
      this.executionDestination = executionDestination;
   }

   /**
    * Check if the order requires the best execution
    * 
    * @return true or false
    */
   public boolean isBestExecutionRequired() {
      return (executionDestination == null || !executionDestination.equalsIgnoreCase(IS_EXECUTION_DESTINATION));
   }

   /**
    * Init the order starting from another order
    * 
    * @param source
    *            the source order
    */
   public void setValues(Order source) {
      super.setValues(source);

      customerOrderId = source.getCustomerOrderId();
      type = source.getType();
      currency = source.getCurrency();
      fIXOrderId = source.getFixOrderId();
      matchingOrder = source.isMatchingOrder();
      priceDiscoveryType = source.getPriceDiscoverySelected();

      this.setTradingCapacity(source.getTradingCapacity());
      this.setDecisionMaker(source.getDecisionMaker());

      this.setExecutionDecisor(source.getExecutionDecisor());
      this.setClient(source.getClient());
      this.setShortSellIndicator(source.getShortSellIndicator());
      this.setMiFIDRestricted(source.isMiFIDRestricted());

      this.setCustOrderHandlingInstr(source.getCustOrderHandlingInstr());
      this.setHandlInst(source.getHandlInst());
      this.setEffectiveTime(source.getEffectiveTime());
      this.setTryAfterMinutes(source.getTryAfterMinutes());
      
      if (source.getLimit() != null) {
         setLimit(source.getLimit());
      }
      if (source.getTimeInForce() != null) {
         timeInForce = source.getTimeInForce();
      }
      if (source.getVenue() != null) {
         venue = source.getVenue();
      }
      if (source.getExecutionDestination() != null) {
         executionDestination = source.getExecutionDestination(); // solo per debug setExecutionDestination("IS");
      }
      if (source.getBestExecutionVenueFlag() != null) {
         bestExecutionVenueFlag = source.getBestExecutionVenueFlag();
      }
      if (source.getPriceType() != null) {
         priceType = source.getPriceType();
      }
      if (source.getInstrumentCode() != null) {
         instrumentCode = source.getInstrumentCode();
      }
      if (source.getText() != null) {
         text = source.getText();
      }
      if (source.getTicketOwner() != null) {
         ticketOwner = source.getTicketOwner();
      }
      orderSource = source.getOrderSource();
   }

   /**
    * Set the customer order id
    * 
    * @param customerOrderId
    *            the customer order id
    */
   public void setCustomerOrderId(String customerOrderId) {
      this.customerOrderId = customerOrderId;
   }

   /**
    * Get the customer order id
    * 
    * @return customer order id
    */
   public String getCustomerOrderId() {
      return customerOrderId;
   }

   /**
    * Set the order type
    * 
    * @param type
    *            order type
    */
   public void setType(Order.OrderType type) {
      this.type = type;
   }

   /**
    * Get the order type
    * 
    * @return order type
    */
   public Order.OrderType getType() {
      return type;
   }

   /**
    * Set the limit price
    * 
    * @param limit
    *            price limit
    */
   public void setLimit(Money limit) {
      this.limit = limit;
   }

   /**
    * Get the limit price
    * 
    * @return limit price
    */
   public Money getLimit() {
      return limit;
   }

   /**
    * Set the currency
    * 
    * @param currency
    *            order currency
    */
   public void setCurrency(String currency) {
      this.currency = currency;
   }

   /**
    * Return the currency
    * 
    * @return currency
    */
   public String getCurrency() {
      return currency;
   }

   /**
    * Get the fix order id
    * 
    * @return the fIXOrderId
    */
   public String getFixOrderId() {
      return fIXOrderId;
   }

   /**
    * Set the fix order id
    * 
    * @param fIXorderId
    *            the fIXOrderId to set
    */
   public void setFixOrderId(String fIXorderId) {
      this.fIXOrderId = fIXorderId;
   }

   /**
    * Get the time in force fix value
    * 
    * @return time in force
    */
   public TimeInForce getTimeInForce() {
      return timeInForce;
   }

   /**
    * Set the time in force
    * 
    * @param timeInForce
    *            the time in force tag value
    */
   public void setTimeInForce(TimeInForce timeInForce) {
      this.timeInForce = timeInForce;
   }

   /**
    * Get the time in force related date
    * 
    * @return time in force date
    */
   public Date getTimeInForceDate() {
      return timeInForceDate;
   }

   /**
    * Set the time in force related date
    * 
    * @param timeInForceDate
    *            time in force date
    */
   public void setTimeInForceDate(Date timeInForceDate) {
      this.timeInForceDate = timeInForceDate;
   }

   /**
    * Check if the order is a matching
    * 
    * @return true or false
    */
   public boolean isMatchingOrder() {
      return matchingOrder;
   }

   /**
    * Set the order as matching or not
    * 
    * @param matchingOrder
    *            true or false
    */
   public void setMatchingOrder(boolean matchingOrder) {
      this.matchingOrder = matchingOrder;
   }

   /**
    * Get the venue
    * 
    * @return the venue
    */
   public Venue getVenue() {
      return venue;
   }

   /**
    * Set the venue
    * 
    * @param venue
    *            the venue to set
    */
   public void setVenue(Venue venue) {
      this.venue = venue;
   }

   /**
    * Check if the order passes the 262 law
    * 
    * @return true or false
    */
   public boolean isLaw262Passed() {
      return law262Passed;
   }

   /**
    * Set the order as compliant to the 262 law or not
    * 
    * @param law262Passed
    *            true or false
    */
   public void setLaw262Passed(boolean law262Passed) {
      this.law262Passed = law262Passed;
   }

   @Override
   public String toString() {
      return "ORDER -> OrderId: " + getFixOrderId() + "- CustomerOrderId: " + getCustomerOrderId() + "- Customer: " + (getCustomer() == null ? "" : getCustomer().getName()) + "- ISIN: "
            + (getInstrument() == null ? "" : getInstrument().getIsin()) + "- Venue: "
            + (getVenue() != null && getVenue().getMarketMaker() != null && getVenue().getMarket() != null
                  ? (getVenue().getMarketMaker().getCode() + "-" + getVenue().getMarket().getMarketCode().name()) : "")
            + "- TimeInForce: " + (getTimeInForce() == null ? "" : getTimeInForce().toString()) + "- TimeInForce Date: " + (getTimeInForceDate() == null ? "" : getTimeInForceDate().toString())
            + "- Price: " + (getLimit() == null ? "market price" : getLimit().getAmount().toPlainString()) + "- Currency: " + (getCurrency() == null ? "" : getCurrency()) + "- Quantity: "
            + (getQty() == null ? "" : getQty() + "- ExecutionDestination: " + (getExecutionDestination() == null ? "" : getExecutionDestination()) + "- Side: " + getSide()) + " - HandlInst: " + getHandlInst();
   }

   /**
    * Check if we have already done the query for knowing if the order is a matching or not
    * 
    * @return true or false
    */
   public IsMatchingFromQueryStates getIsMatchingFromQuery() {
      return isMatchingFromQuery;
   }

   /**
    * Set the variable to tell if we have already done the query for knowing if the order is a matching or not
    * 
    * @param isMatchingFromQuery
    *            true or false
    */
   public void setIsMatchingFromQuery(IsMatchingFromQueryStates isMatchingFromQuery) {
      this.isMatchingFromQuery = isMatchingFromQuery;
   }

   /**
    * Check if in this order we must add commissions to the customer price
    * 
    * @return true or false
    */
   public boolean isAddCommissionToCustomerPrice() {
      return addCommissionToCustomerPrice;
   }

   /**
    * Set the add commission to customer price flag
    * 
    * @param addCommissionToCustomerPrice
    *            true or false
    */
   public void setAddCommissionToCustomerPrice(boolean addCommissionToCustomerPrice) {
      this.addCommissionToCustomerPrice = addCommissionToCustomerPrice;
   }

   /**
    * Get the market not quoting the order instrument
    * 
    * @return market with reason
    */
   public Map<MarketCode, String> getMarketNotQuotingInstr() {
      return marketNotQuotingInstr;
   }

   /**
    * Add a market to those not quoting the order instrument
    * 
    * @param mCode
    *            not quoting market
    * @param reason
    *            reason
    */
   public synchronized void addMarketNotQuotingInstr(MarketCode mCode, String reason) {
      marketNotQuotingInstr.put(mCode, reason);
   }

   /**
    * Remove the market from the list of those not quoting the order instrument
    * 
    * @param mCode
    *            market to remove
    */
   public synchronized void removeMarketNotQuotingInstr(MarketCode mCode) {
      marketNotQuotingInstr.remove(mCode);
   }

   /**
    * Clear the map of the market not quoting the order instrument
    */
   public synchronized void clearMarketNotQuotingInstr() {
      marketNotQuotingInstr.clear();
   }

   /**
    * Get the markets not negotiating the instrument with the reason
    * 
    * @return markets and reason
    */
   public Map<MarketCode, String> getMarketNotNegotiatingInstr() {
      return marketNotNegotiatingInstr;
   }

   /**
    * Add a market not negotiating the instrument
    * 
    * @param mCode
    *            market not negotiating
    * @param reason
    *            reason
    */
   public synchronized void addMarketNotNegotiatingInstr(MarketCode mCode, String reason) {
      marketNotNegotiatingInstr.put(mCode, reason);
   }

   /**
    * Remove the market from those not negotiating
    * 
    * @param mCode
    */
   public synchronized void removeMarketNotNegotiatingInstr(MarketCode mCode) {
      marketNotNegotiatingInstr.remove(mCode);
   }

   /**
    * Clear the map of the market not negotiating the instrument
    */
   public synchronized void clearMarketNotNegotiatingInstr() {
      marketNotNegotiatingInstr.clear();
   }

   /**
    * Get the map of market market makers not quoting the instrument with its reason
    * 
    * @return market market maker with reason
    */
   public Map<MarketMarketMaker, String> getMarketMarketMakerNotQuotingInstr() {
      return mmmNotQuotingInstr;
   }

   /**
    * Add a market maker not quoting the instrument
    * 
    * @param mmm
    *            market market maker
    * @param reason
    *            reason
    */
   public void addMarketMakerNotQuotingInstr(MarketMarketMaker mmm, String reason) {
      mmmNotQuotingInstr.put(mmm, reason);
   }

   /**
    * Add a list of market market makers not quoting instrument with a common reason
    * 
    * @param mmmList
    *            a list of mmm
    * @param reason
    *            common reason
    */
   public void addMarketMakerNotQuotingInstr(List<MarketMarketMaker> mmmList, String reason) {
      for (MarketMarketMaker mmm : mmmList) {
         mmmNotQuotingInstr.put(mmm, reason);
      }
   }

   /**
    * Remove the market market maker from those not quoting the instrument
    * 
    * @param mmm
    *            market market maker to remove
    */
   public void removeMarketMakerNotQuotingInstr(MarketMarketMaker mmm) {
      if (mmmNotQuotingInstr.containsKey(mmm)) {
         mmmNotQuotingInstr.remove(mmm);
         LOGGER.debug("Order {}, mmm {} removed, market makers not quoting remained : {}", getFixOrderId(), mmm, mmmNotQuotingInstr.size());
      }
   }

   public void removeMarketMakerNotQuotingInstr(MarketCode marketCode, List<String> marketMakersOnBothSides) {

      for (String marketMaker : marketMakersOnBothSides) {
         Iterator<MarketMarketMaker> iter = mmmNotQuotingInstr.keySet().iterator();
         while (iter.hasNext()) {
            MarketMarketMaker marketMarketMaker = (MarketMarketMaker) iter.next();
            if (marketMarketMaker.getMarket().getMarketCode() == marketCode && marketMarketMaker.getMarketSpecificCode().equals(marketMaker)) {
               mmmNotQuotingInstr.remove(marketMarketMaker);
            }
         }
      }
   }

   /**
    * Clear the map of the market market makers not quoting the instrument
    */
   public void clearMarketMakerNotQuotingInstr() {
      mmmNotQuotingInstr.clear();
   }

   /**
    * Get the not quoting reason for the given market market maker
    * 
    * @param mmm
    *            market market maker whose reason we must look for
    * @return the reason
    */
   public synchronized String getReason(MarketMarketMaker mmm) {
      return mmmNotQuotingInstr.get(mmm);
   }

   /**
    * Get the reason for a market not quoting the instrument
    * 
    * @param marketCode
    *            market whose reason we are looking for
    * @return the reason
    */
   public synchronized String getReason(MarketCode marketCode) {
      return marketNotQuotingInstr.get(marketCode);
   }

   /**
    * Check if the given market market maker does not quote the instrument
    * 
    * @param mmm
    *            market market maker we are verifying
    * @return true or false
    */
   public boolean isMarketMakerNotQuotingInstr(MarketMarketMaker mmm) {
      return mmmNotQuotingInstr.containsKey(mmm);
   }

   /**
    * Get the price discovery type for this order
    * 
    * @return the price discovery type
    */
   public PriceDiscoveryType getPriceDiscoverySelected() {
      return priceDiscoveryType;
   }

   public PriceDiscoveryType getPriceDiscoveryType() {
      return priceDiscoveryType;
   }

   public void setPriceDiscoveryType(PriceDiscoveryType priceDiscType) {
      priceDiscoveryType = priceDiscType;
   }

   /**
    * Get the log id
    * 
    * @return log id
    */
   public String getLogId() {
      if (logIdCounter == 0) {
         return null;
      }
      else {
         return fIXOrderId + "_" + logIdCounter;
      }
   }

   /**
    * Increment the log id counter
    */
   public void generateNextLogId() {
      logIdCounter++;
   }

   /*
    * Get the current log id counter
    */
   public int getLogIdCounter() {
      return logIdCounter;
   }

   /**
    * Set the log id counter
    * 
    * @param logIdCounter
    *            log id counter
    */
   public void setLogIdCounter(int logIdCounter) {
      this.logIdCounter = logIdCounter;
   }

   /**
    * Set the best execution venue flag
    * 
    * @param bestExecutionVenueFlag
    *            true or false
    */
   public void setBestExecutionVenueFlag(int bestExecutionVenueFlag) {
      this.bestExecutionVenueFlag = bestExecutionVenueFlag;
   }

   /**
    * Get the best execution venue flag
    * 
    * @return the flag
    */
   public Integer getBestExecutionVenueFlag() {
      return bestExecutionVenueFlag;
   }

   /**
    * Set the price type
    * 
    * @param priceType
    *            type
    */
   public void setPriceType(int priceType) {
      this.priceType = priceType;
   }

   /**
    * Get the price type
    * 
    * @return price type
    */
   public Integer getPriceType() {
      return priceType;
   }

   /**
    * Sets the order source.
    *
    * @param orderSource the new order source
    */
   public void setOrderSource(String orderSource) {
      this.orderSource = orderSource;
   }

   /**
    * Gets the order source.
    *
    * @return the order source
    */
   public String getOrderSource() {
      return orderSource;
   }

   /**
    * @return the instrumentCode
    */
   public String getInstrumentCode() {
      return instrumentCode;
   }

   /**
    * @param instrumentCode the instrumentCode to set
    */
   public void setInstrumentCode(String instrumentCode) {
      this.instrumentCode = instrumentCode;
   }

   /**
    * Check if the order is a LimitFile order:
    * - type must be LIMIT
    * - timeInForce must be GOOD_TILL_CANCEL
    * 
    * It is a CS specific request
    * 
    * @return true if both the conditions have been met
    */
   public boolean isLimitFile() {
      return (type == OrderType.LIMIT && timeInForce == TimeInForce.GOOD_TILL_CANCEL);
   }

   /**
    * @return the text
    */
   public String getText() {
      return text;
   }

   /**
    * @param text the text to set
    */
   public void setText(String text) {
      this.text = text;
   }

   /**
    * @return the bestPriceDeviationFromLimit
    */
   public Double getBestPriceDeviationFromLimit() {
      return bestPriceDeviationFromLimit;
   }

   /**
    * @param bestPriceDeviationFromLimit the bestPriceDeviationFromLimit to set
    */
   public void setBestPriceDeviationFromLimit(Double bestPriceDeviationFromLimit) {
      this.bestPriceDeviationFromLimit = bestPriceDeviationFromLimit;
   }

   /**
    * @return the ticketOwner
    */
   public String getTicketOwner() {
      return ticketOwner;
   }

   /**
    * @param ticketOwner the ticketOwner to set
    */
   public void setTicketOwner(String ticketOwner) {
      this.ticketOwner = ticketOwner;
   }

   public Parties getDecisionMaker() {
      return decisionMaker;
   }

   public void setDecisionMaker(Parties decisionMaker) {
      this.decisionMaker = decisionMaker;
   }

   public Parties getExecutionDecisor() {
      return executionDecisor;
   }

   public void setExecutionDecisor(Parties executionDecisor) {
      this.executionDecisor = executionDecisor;
   }

   public Parties getClient() {
      return client;
   }

   public void setClient(Parties client) {
      this.client = client;
   }

   public OrderSide getShortSellIndicator() {
      return shortSellIndicator;
   }

   public void setShortSellIndicator(OrderSide shortSellIndicator) {
      this.shortSellIndicator = shortSellIndicator;
   }

   public Boolean isMiFIDRestricted() {
      return miFIDRestricted;
   }

   public void setMiFIDRestricted(Boolean miFIDRestricted) {
      this.miFIDRestricted = miFIDRestricted;
   }

   public OrderCapacity getOrderCapacity() {
      return orderCapacity;
   }

   public void setOrderCapacity(OrderCapacity orderCapacity) {
      this.orderCapacity = orderCapacity;
   }

   /**
    * @return the custOrderHandlingInstr
    */
   public String getCustOrderHandlingInstr() {
      return custOrderHandlingInstr;
   }

   /**
    * @param custOrderHandlingInstr the custOrderHandlingInstr to set
    */
   public void setCustOrderHandlingInstr(String custOrderHandlingInstr) {
      this.custOrderHandlingInstr = custOrderHandlingInstr;
   }

   /**
    * @return the handlInst
    */
   public String getHandlInst() {
      return handlInst;
   }

   /**
    * @param handlInst the handlInst to set
    */
   public void setHandlInst(String handlInst) {
      this.handlInst = handlInst;
   }

   /**
    * @return the tryAfterMinutes
    */
   public int getTryAfterMinutes() {
      return tryAfterMinutes;
   }

   /**
    * @param tryAfterMinutes the tryAfterMinutes to set
    */
   public void setTryAfterMinutes(int tryAfterMinutes) {
      this.tryAfterMinutes = tryAfterMinutes;
   }

   /**
    * @return the effectiveTime
    */
   public Date getEffectiveTime() {
      return effectiveTime;
   }

   /**
    * @param effectiveTime the effectiveTime to set
    */
   public void setEffectiveTime(Date effectiveTime) {
      this.effectiveTime = effectiveTime;
   }
}
