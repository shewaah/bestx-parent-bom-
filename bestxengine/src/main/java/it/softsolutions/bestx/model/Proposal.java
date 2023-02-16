/*
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public class Proposal implements Cloneable, Comparable<Proposal> {

    /**
     * The Enum ProposalSide.
     */
    public enum ProposalSide {
        BID("0"), ASK("1"), ;

        /**
         * Get the proposal side fix code.
         *
         * @return fix code for the side
         */
        public String getFixCode() {
            return mFIXValue;
        }

        private final String mFIXValue;

        private ProposalSide(String inFIXValue) {
            mFIXValue = inFIXValue;
        }
    }

    /**
     * The Enum ProposalState.
     */
    public enum ProposalState {
        NEW, VALID, EXPIRED, ACCEPTABLE, REJECTED, DROPPED  // order is important: Enum.compareTo returns state with higher priority as higher    
   }

    /**
     * Possible details for the proposal ProposalState
     * This Enum may be used to compare proposals, so maintain a meaningful sorting
     * @author ruggero.rizzo
     *
     */
    public enum ProposalSubState {
        NONE, 
        PRICE_NOT_VALID, 
        QUANTITY_NOT_VALID, 
        ZERO_QUANTITY, 
        REJECTED_BY_MARKET, 
        REJECTED_BY_DEALER, 
        TOO_OLD, 
        NOT_TRADING, 
        PRICE_WORST_THAN_LIMIT,
        OUTSIDE_SPREAD,
        MARKET_TRIED
    }
    
    /**
     * The Enum ProposalType.
     */
    public enum ProposalType { 
    	 // Counter and tradeable shall remain as last in the enumerated
        CLOSED, SET_TO_ZERO, INDICATIVE, SPREAD_ON_BEST, COMPOSITE, IOI, AXE, RESTRICTED_TRADEABLE, TRADEABLE, COUNTER;
    }
    
    public enum PriceType {
        PRICE(1), YIELD(9), SPREAD(6), UNIT(2), DISCOUNT_MARGIN(100), UNKNOWN(0);
        
        private final int mFIXValue;
       
        public int getFixCode() {
           return mFIXValue;
       }
       private PriceType(int fixValue) {
           mFIXValue = fixValue;
       }
       public static PriceType createPriceType(int fixValue) {
          return Arrays.stream(PriceType.values()).filter(a -> a.getFixCode() == fixValue).findFirst().orElse(PriceType.UNKNOWN);
       }
    }
    @SuppressWarnings("unused")
    private Long id;
    private Money customerAdditionalExpenses;
    private Money orderPrice;
    private Date expiration;
    private Date futSettDate;
    private Market market;
    private Money price;
    private BigDecimal qty;
    private Proposal.ProposalSide side;
    private Proposal.ProposalType type;
    private MarketMarketMaker marketMarketMaker;
    private Venue venue;
    private Trader trader;
    private String senderQuoteId;
    private String onBehalfOfCompID;
    private String quoteReqId;

    private boolean nonStandardSettlementDateAllowed;
    private Date timestamp;
    private String timestampstr;
    private Money accruedInterest;
    private Integer accruedDays;
    private BigDecimal executionQtyMultiplier;
    // used only when Proposal type is SPREAD_ON_BEST
    private BigDecimal spread;
    private Money priceTelQuel;
    private BigDecimal originalPrice;
    private BigDecimal yield;
    private PriceType priceType;
    private BigDecimal unit;

    private boolean isInternal = false;

    /**
     * Checks if is the proposal is from an internal trader
     *
     * @return true, if is internal
     */
    public boolean isInternal() {
        return isInternal;
    }

    /**
     * Sets the internal flag, if the proposal is from an internal trader
     *
     * @param isInternal the new internal
     */
    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }
    
    /**
     * Gets the accrued interest.
     *
     * @return the accrued interest
     */
    public Money getAccruedInterest() {
        return accruedInterest;
    }

    /**
     * Sets the accrued interest.
     *
     * @param accruedInterest the new accrued interest
     */
    public void setAccruedInterest(Money accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    /**
     * Gets the accrued days.
     *
     * @return the accrued days
     */
    public Integer getAccruedDays() {
        return accruedDays;
    }

    /**
     * Sets the accrued days.
     *
     * @param accruedDays the new accrued days
     */
    public void setAccruedDays(Integer accruedDays) {
        this.accruedDays = accruedDays;
    }

    /**
     * Instantiates a new proposal.
     */
    public Proposal() {
    }

    /**
     * Sets the values.
     *
     * @param proposal the new values
     */
    public void setValues(Proposal proposal) {
        setCustomerAdditionalExpenses(proposal.getCustomerAdditionalExpenses());
        setOrderPrice(proposal.getOrderPrice());
        setExpiration(proposal.getExpiration());
        setFutSettDate(proposal.getFutSettDate());
        setMarket(proposal.getMarket());
        setPrice(proposal.getPrice());
        setPriceType(proposal.getPriceType());
        setQty(proposal.getQty());
        setSide(proposal.getSide());
        setType(proposal.getType());
        setMarketMarketMaker(proposal.getMarketMarketMaker());
        setVenue(proposal.getVenue());
        setTrader(proposal.getTrader());
        setSenderQuoteId(proposal.getSenderQuoteId());
        setOnBehalfOfCompID(proposal.getOnBehalfOfCompID());
        setNonStandardSettlementDateAllowed(proposal.isNonStandardSettlementDateAllowed());
        setAccruedInterest(proposal.getAccruedInterest());
        setAccruedDays(proposal.getAccruedDays());
        setExecutionQtyMultiplier(proposal.getExecutionQtyMultiplier());
        setTimestamp(proposal.getTimestamp());
        setTimestampstr(proposal.getTimestampstr());
        setSpread(proposal.getSpread());
        setOriginalPrice(proposal.getOriginalPrice());
        setYield(proposal.getYield());
        setUnit(proposal.getUnit());
    }

    @Override
    public Proposal clone() throws CloneNotSupportedException {
        Proposal bean = null;
        try {
            bean = (Proposal) super.clone();
        } catch (CloneNotSupportedException e) {
            // It should never happen
        }
        bean.id = null;
        bean.setValues(this);
        return bean;
    }

    /**
     * Sets the customer additional expenses.
     *
     * @param customerAdditionalExpenses the new customer additional expenses
     */
    public void setCustomerAdditionalExpenses(Money customerAdditionalExpenses) {
        this.customerAdditionalExpenses = customerAdditionalExpenses;
    }

    /**
     * Sets the expiration.
     *
     * @param expiration the new expiration
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Sets the fut sett date.
     *
     * @param futSettDate the new fut sett date
     */
    public void setFutSettDate(Date futSettDate) {
        this.futSettDate = futSettDate;
    }

    /**
     * Sets the market.
     *
     * @param market the new market
     */
    public void setMarket(Market market) {
        this.market = market;
    }

    /**
     * Sets the price.
     *
     * @param price the new price
     */
    public void setPrice(Money price) {
        this.price = price;
    }

    /**
     * Sets the qty.
     *
     * @param qty the new qty
     */
    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    /**
     * Sets the side.
     *
     * @param side the new side
     */
    public void setSide(Proposal.ProposalSide side) {
        this.side = side;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(Proposal.ProposalType type) {
        this.type = type;
    }

    /**
     * Sets the venue.
     *
     * @param venue the new venue
     */
    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    /**
     * Gets the customer additional expenses.
     *
     * @return the customer additional expenses
     */
    public Money getCustomerAdditionalExpenses() {
        return customerAdditionalExpenses;
    }

    /**
     * Gets the expiration.
     *
     * @return the expiration
     */
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Gets the fut sett date.
     *
     * @return the fut sett date
     */
    public Date getFutSettDate() {
        return futSettDate;
    }

    /**
     * Gets the market.
     *
     * @return the market
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Gets the price.
     *
     * @return the price
     */
    public Money getPrice() {
        return price;
    }

    /**
     * Gets the qty.
     *
     * @return the qty
     */
    public BigDecimal getQty() {
        return qty;
    }

    /**
     * Gets the side.
     *
     * @return the side
     */
    public Proposal.ProposalSide getSide() {
        return side;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public Proposal.ProposalType getType() {
        return type;
    }

    /**
     * Gets the venue.
     *
     * @return the venue
     */
    public Venue getVenue() {
        return venue;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the new timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Date getTimestamp() {
        if (timestamp == null) {
        	timestamp = new Date();
        } 
        return timestamp;
    }

    /**
     * Sets the timestampstr.
     *
     * @param timestampstr the new timestampstr
     */
    public void setTimestampstr(String timestampstr) {
        this.timestampstr = timestampstr;
    }

    /**
     * Gets the timestampstr.
     *
     * @return the timestampstr
     */
    public String getTimestampstr() {
    	if(this.timestampstr == null)
    		this.timestampstr = this.getTimestamp().toString();
        return timestampstr;
    }

    /**
     * Sets the trader.
     *
     * @param trader the new trader
     */
    public void setTrader(Trader trader) {
        this.trader = trader;
    }

    /**
     * Gets the trader.
     *
     * @return the trader
     */
    public Trader getTrader() {
        return trader;
    }

    /**
     * Sets the order price.
     *
     * @param orderPrice the new order price
     */
    public void setOrderPrice(Money orderPrice) {
        this.orderPrice = orderPrice;
    }

    /**
     * Gets the order price.
     *
     * @return the order price
     */
    public Money getOrderPrice() {
        return orderPrice;
    }

    /**
     * Checks if is non standard settlement date allowed.
     *
     * @return true, if is non standard settlement date allowed
     */
    public boolean isNonStandardSettlementDateAllowed() {
        return nonStandardSettlementDateAllowed;
    }

    /**
     * Sets the non standard settlement date allowed.
     *
     * @param nonStandardSettlementDateAllowed the new non standard settlement date allowed
     */
    public void setNonStandardSettlementDateAllowed(boolean nonStandardSettlementDateAllowed) {
        this.nonStandardSettlementDateAllowed = nonStandardSettlementDateAllowed;
    }

    /**
     * Gets the market market maker.
     *
     * @return the market market maker
     */
    public MarketMarketMaker getMarketMarketMaker() {
        return marketMarketMaker;
    }

    /**
     * Sets the market market maker.
     *
     * @param marketMarketMaker the new market market maker
     */
    public void setMarketMarketMaker(MarketMarketMaker marketMarketMaker) {
        this.marketMarketMaker = marketMarketMaker;
    }

    /**
     * Gets the sender quote id.
     *
     * @return the sender quote id
     */
    public String getSenderQuoteId() {
        return senderQuoteId;
    }

    /**
     * Sets the sender quote id. (id assigned by the market)
     *
     * @param senderQuoteId the new sender quote id
     */
    public void setSenderQuoteId(String senderQuoteId) {
        this.senderQuoteId = senderQuoteId;
    }

    /**
     * Gets the quote req id. (id of the request sent by Bestx)
     *
     * @return the quoteReqId
     */
    public String getQuoteReqId() {
        return quoteReqId;
    }

    /**
     * Sets the quote req id. (id of the request sent by Bestx)
     *
     * @param quoteReqId the new quoteReqId
     */
    public void setQuoteReqId(String quoteReqId) {
        this.quoteReqId = quoteReqId;
    }
    
    /**
     * Gets the execution qty multiplier.
     *
     * @return the executionQtyMultiplier
     */
    public BigDecimal getExecutionQtyMultiplier() {
        return executionQtyMultiplier;
    }

    /**
     * Sets the execution qty multiplier.
     *
     * @param executionQtyMultiplier the executionQtyMultiplier to set
     */
    public void setExecutionQtyMultiplier(BigDecimal executionQtyMultiplier) {
        this.executionQtyMultiplier = executionQtyMultiplier;
    }

    /**
     * Gets the spread.
     *
     * @return the spread
     */
    public BigDecimal getSpread() {
        return spread;
    }

    /**
     * Sets the spread.
     *
     * @param spread the new spread
     */
    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    /**
     * Sets the price tel quel.
     *
     * @param priceTelQuel the new price tel quel
     */
    public void setPriceTelQuel(Money priceTelQuel) {
        this.priceTelQuel = priceTelQuel;
    }

    /**
     * Gets the price tel quel.
     *
     * @return the price tel quel
     */
    public Money getPriceTelQuel() {
        return priceTelQuel;
    }

    /**
     * Gets the original price.
     *
     * @return the original price
     */
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    /**
     * Sets the original price.
     *
     * @param originalPrice the new original price
     */
    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    /**
     * Gets the yield.
     *
     * @return the yield
     */
    public BigDecimal getYield() {
        return yield;
    }

    /**
     * Sets the yield.
     *
     * @param yield the new yield
     */
    public void setYield(BigDecimal yield) {
        this.yield = yield;
    }
    /**
     * Gets the unit.
     * 
     * @return the unit
     */
    public BigDecimal getUnit() {
    	return unit;
    }
    /**
     * Sets the unit.
     * 
     * @param unit the new unit
     */
    public void setUnit(BigDecimal unit) {
    	this.unit = unit;
    }
    @Override
    public int compareTo(Proposal prop) {
        if (prop == null) {
            return -1;
        } else if (!prop.getMarket().equals(getMarket())) {
            return prop.getMarket().getMarketCode().compareTo(getMarket().getMarketCode());
        } else if (!prop.getMarketMarketMaker().equals(getMarketMarketMaker())) {
            return prop.getMarketMarketMaker().getMarketSpecificCode().compareTo(getMarketMarketMaker().getMarketSpecificCode());
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Proposal [futSettDate=");
        builder.append(futSettDate);
        builder.append(", quoteReqId=");
        builder.append(quoteReqId);
        builder.append(", market=");
        builder.append(market);
        builder.append(", price=");
        builder.append(price != null ? price.getAmount() : null);
        builder.append("/");
        builder.append( (price != null) && (price.getStringCurrency() != null) ? price.getStringCurrency() : null);
        builder.append(", qty=");
        builder.append(qty);
        builder.append(", side=");
        builder.append(side);
        builder.append(", type=");
        builder.append(type);
        builder.append(", marketMarketMaker=");
        builder.append(marketMarketMaker);
        builder.append(", nonStdSettlDateAllowed=");
        builder.append(nonStandardSettlementDateAllowed);
        builder.append(", priceType=");
        builder.append(priceType);
        builder.append("]");
        return builder.toString();
    }

	/**
	 * @return the onBehalfOfCompID
	 */
	public String getOnBehalfOfCompID() {
		return onBehalfOfCompID;
	}

	/**
	 * @param onBehalfOfCompID the onBehalfOfCompID to set
	 */
	public void setOnBehalfOfCompID(String onBehalfOfCompID) {
		this.onBehalfOfCompID = onBehalfOfCompID;
	}

	/**
	 * @return the priceType
	 */
	public PriceType getPriceType() {
		return priceType;
	}

	/**
	 * @param priceType the priceType to set
	 */
	public void setPriceType(PriceType priceType) {
		this.priceType = priceType;
	}
    
    
}
