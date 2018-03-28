package it.softsolutions.bestx.services;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Order;

import java.math.BigDecimal;
import java.util.Date;

public interface MarketSecurityStatusService {
    Market getQuotingMarket(MarketCode marketCode, String instrument) throws BestXException;
    Date getMarketInstrumentSettlementDate(MarketCode marketCode, String instrument) throws BestXException;
    Instrument.QuotingStatus getInstrumentQuotingStatus(MarketCode marketCode, String instrument) throws BestXException;
    void insertMarketSecurityStatusItem(MarketCode marketCode, SubMarketCode subMarketCode, String instrument) throws BestXException;
    void setMarketSecurityQuotingStatus(MarketCode marketCode, SubMarketCode subMarketCode, String instrument, Instrument.QuotingStatus quotingStatus) throws BestXException;
    void setMarketSecuritySettlementDate(MarketCode marketCode, SubMarketCode subMarketCode, String instrument, Date settlementDate) throws BestXException;
    // New: Used for BondVision and MTSPrime
    void setMarketSecuritySettlementDateAndBondType(MarketCode marketCode, SubMarketCode subMarketCode,
          String instrument, Date settlementDate, String bondType, int marketAffiliation, String marketAffiliationStr, int quoteIndicator, String quoteIndicatorStr) throws BestXException;
    void setMarketSecurityQuantity(MarketCode marketCode, SubMarketCode subMarketCode, String isin, BigDecimal minQty,
          BigDecimal qtyTick, BigDecimal multiplier) throws BestXException;
   BigDecimal[] getQuantityValues(MarketCode marketCode, Instrument instrument);
   BigDecimal getMinTradingQty(MarketCode marketCode, Instrument instrument);
   BigDecimal getMinIncrement(MarketCode marketCode, Instrument instrument);
   BigDecimal getQtyMultiplier(MarketCode marketCode, Instrument instrument);
   /* 2009-09-30 Ruggero
    * If this method is called it means that the caller is not interested
    * on the quoting status.
    * He wants the market in which the instrument is quoted and doesn't care
    * if it is negotiable, not negotiable or in volatility call.
    */
   public Market getInstrumentMarket(MarketCode marketCode, String instrument) throws BestXException;
   public String getMarketBondType(MarketCode marketCode, String instrument);
   public int getMarketAffiliation(MarketCode marketCode, String instrument);
   //20110816 - Ruggero
   /**
    * Check the minimum trading quantity and the minimum increment for the given instrument and the
    * given market, available in the MarketSecurityStatus table, against the order quantity.
    * @param marketCode
    * @param instrument
    * @param order
    * @return true if quantities are compatible or the instrument is null, false otherwise.
    */
   public boolean validQuantities(MarketCode marketCode, Instrument instrument, Order order);
   public int getQuotIndicator(MarketCode marketCode, String instrument);

   public static final int VALUE_CLEAN_PRICE = 0;
   public static final int VALUE_YIELD = 1;
   public static final int VALUE_DIRTY_PRICE = 2;
   public static final int VALUE_PRICE_32 = 3;


   public static enum QuantityValues { 
      MIN_QTY(0), 
      MIN_INCREMENT(1), 
      QTY_MULTIPLIER(2);
      
   private final int position;
   
   private QuantityValues(int position) {
      this.position = position;
   }
   
   public int getPosition(){
      return this.position;
      };
   }

}
