package it.softsolutions.bestx.services.marketsecuritystatus;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.regulated.RegulatedMessageFields;
import it.softsolutions.bestx.dao.MarketSecurityStatusDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.markets.MarketSecurityStatus;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.MarketSecurityStatusService;
import it.softsolutions.bestx.services.instrumentstatus.InstrumentStatusNotifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketSecurityStatusServer implements MarketSecurityStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketSecurityStatusServer.class);
    
    private MarketSecurityStatusDao marketSecurityStatusDao;
    private MarketFinder marketFinder;
    private InstrumentStatusNotifier instrStatusNotifier;

    public void init() {
        // checkPreRequisites
        if (marketSecurityStatusDao == null) {
            throw new ObjectNotInitializedException("Market security status DAO not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("Market finder not set");
        }
    }
    
    public void setMarketSecurityStatusDao(MarketSecurityStatusDao marketSecurityStatusDao) {
        this.marketSecurityStatusDao = marketSecurityStatusDao;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    @Override
    public QuotingStatus getInstrumentQuotingStatus(MarketCode marketCode, String instrument) {

        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        
        if (marketSecurityStatus != null) {
            String statusCode = marketSecurityStatus.getStatusCode();
            
            LOGGER.info("marketSecurityStatus found for instrument = {}, marketCode = {}, status = {}", instrument, marketCode, statusCode);
            
            if (QuotingStatus.NEG.name().equals(statusCode)) {
                return QuotingStatus.NEG;
            } else if (QuotingStatus.CAN.name().equals(statusCode)) {
                return QuotingStatus.CAN;
            } else if (QuotingStatus.PVOL.name().equals(statusCode)) {
                return QuotingStatus.PVOL;
            } else {
                LOGGER.warn("Unexpected statusCode [{}] for instrument = {}, marketCode = {}", statusCode, instrument, marketCode);
                return null;
            }
        } else {
            LOGGER.warn("marketSecurityStatus not found for instrument = {}, marketCode = {}", instrument, marketCode);
            return null;
        }
    }

    @Override
    public Date getMarketInstrumentSettlementDate(MarketCode marketCode, String instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        if (marketSecurityStatus == null) {
            return null;
        } else {
            return marketSecurityStatus.getSettlementDate();
        }
    }

    @Override
    public Market getQuotingMarket(MarketCode marketCode, String instrument) throws BestXException {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        if (marketSecurityStatus == null) {
            return null;
        } else if (!QuotingStatus.NEG.name().equals(marketSecurityStatus.getStatusCode())) {
            return null;
        } else {
            Market market = marketFinder.getMarketByCode(marketCode, marketSecurityStatus.getSubMarketCode());
            return market;
        }
    }

    /*
     * 2009-09-30 Ruggero If this method is called it means that the caller is not interested on the quoting status. He wants the market in
     * which the instrument is quoted and doesn't care if it is negotiable, not negotiable or in volatility call.
     */
    @Override
    public Market getInstrumentMarket(MarketCode marketCode, String instrument) throws BestXException {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        
        Market market = (marketSecurityStatus != null) ? marketFinder.getMarketByCode(marketCode, marketSecurityStatus.getSubMarketCode()) : null;
        return market;
    }

    @Override
    public void setMarketSecurityQuotingStatus(MarketCode marketCode, SubMarketCode subMarketCode, String instrument, QuotingStatus quotingStatus) throws BestXException {
        MarketSecurityStatus marketSecurityStatus = getNewOrExistingMarketSecurityStatus(marketCode, subMarketCode, instrument);
        marketSecurityStatus.setStatusCode(quotingStatus.name());

        /*
         * 2009-09-15 Ruggero The following if clause implements this logic : - when a MOT bond changes its status to PVOL (volatility call)
         * we must notify it to the orders (the operations) on that bond. The operations will update the status description, thus showing to
         * the operator and the customer that there is volatility call ongoing - when a MOT bond changes its status back from PVOL to either
         * one of NEG or CAN we must notify the end of the volatility call and remove the isin from the PVOL isins list. The latter
         * behaviour is realized with the call of the removeOperationFromIsin method; it will be done with EVERY new quoting status
         * different from PVOL. If the isin has never changed its status to PVOL, then inside the method nothing will happen.
         */

        if (RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION.equals(quotingStatus.name())) {
            LOGGER.debug("The instrument {} is in volatility call.", instrument);
            instrStatusNotifier.quotingStatusChanged(instrument, quotingStatus);
        } else {
            LOGGER.debug("The instrument {} changed status, it could be exited from the volatility call.", instrument);
            instrStatusNotifier.quotingStatusChanged(instrument, quotingStatus);
            // NOt needed, we must only delete the operation when will be closed
            // the isin must remain, it will be deleted only when we will remove
            // the last of its operations
            // instrStatusNotifier.removeOperationFromIsin(instrument, null);
        }
        marketSecurityStatusDao.saveOrUpdateMarketSecurityStatus(marketSecurityStatus);
    }

    @Override
    public void setMarketSecuritySettlementDate(MarketCode marketCode, SubMarketCode subMarketCode, String instrument, Date settlementDate) throws BestXException {
        MarketSecurityStatus marketSecurityStatus = getNewOrExistingMarketSecurityStatus(marketCode, subMarketCode, instrument);
        marketSecurityStatus.setSettlementDate(settlementDate);
        marketSecurityStatusDao.saveOrUpdateMarketSecurityStatus(marketSecurityStatus);
    }

    @Override
    public void insertMarketSecurityStatusItem(MarketCode marketCode, SubMarketCode subMarketCode, String instrument) throws BestXException {
        MarketSecurityStatus marketSecurityStatus = getNewOrExistingMarketSecurityStatus(marketCode, subMarketCode, instrument);
        marketSecurityStatus.setStatusCode(QuotingStatus.NEG.toString());
        marketSecurityStatusDao.saveOrUpdateMarketSecurityStatus(marketSecurityStatus);
    }

    private MarketSecurityStatus getNewOrExistingMarketSecurityStatus(MarketCode marketCode, SubMarketCode subMarketCode, String instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, subMarketCode, instrument);
        
        if (marketSecurityStatus == null) {
            marketSecurityStatus = new MarketSecurityStatus();
            marketSecurityStatus.setInstrument(instrument);
            marketSecurityStatus.setMarketCode(marketCode);
            marketSecurityStatus.setSubMarketCode(subMarketCode);
            marketSecurityStatus.setStatusCode(QuotingStatus.CAN.toString());
        }
        
        return marketSecurityStatus;
    }

    /**
     * Save to table MarketSecurityStatus the fields minQty, minIncrement and multimplier. Currently they are managed in market HIMTF
     */
    @Override
    public void setMarketSecurityQuantity(MarketCode marketCode, SubMarketCode subMarketCode, String isin, BigDecimal minQty, BigDecimal minIncrement, BigDecimal multiplier) throws BestXException {
        // AMC Attenzione, questi campi sono salvati solo per HIMTF e BV. Gli altri mercati regolamentati non li gestiscono
        if (marketCode != MarketCode.HIMTF && marketCode != MarketCode.BV && marketCode != MarketCode.MTSPRIME) { 
            return;
        }
        
        MarketSecurityStatus marketSecurityStatus = getNewOrExistingMarketSecurityStatus(marketCode, subMarketCode, isin);
        marketSecurityStatus.setMinQty(minQty);
        marketSecurityStatus.setMinIncrement(minIncrement);
        marketSecurityStatus.setQtyMultiplier(multiplier);
        marketSecurityStatusDao.saveOrUpdateMarketSecurityStatus(marketSecurityStatus);
    }

    @Override
    public BigDecimal[] getQuantityValues(MarketCode marketCode, Instrument instrument) {
        BigDecimal retVal[] = new BigDecimal[3];
        // OKKIO che funziona solo se il titolo e' negoziabile
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument.getIsin());
        if (marketSecurityStatus != null) {
            retVal[QuantityValues.MIN_QTY.getPosition()] = marketSecurityStatus.getMinQty();
            retVal[QuantityValues.MIN_INCREMENT.getPosition()] = marketSecurityStatus.getMinIncrement();
            retVal[QuantityValues.QTY_MULTIPLIER.getPosition()] = marketSecurityStatus.getQtyMultiplier();
        }
        return retVal;
    }

    public InstrumentStatusNotifier getInstrStatusNotifier() {
        return instrStatusNotifier;
    }

    public void setInstrStatusNotifier(InstrumentStatusNotifier instrStatusNotifier) {
        this.instrStatusNotifier = instrStatusNotifier;
    }

    @Override
    public void setMarketSecuritySettlementDateAndBondType(MarketCode marketCode, SubMarketCode subMarketCode, String instrument, Date settlementDate, String bondType, int marketAffiliation,
            String marketAffiliationStr, int quoteIndicator, String quoteIndicatorStr) throws BestXException {
        
        MarketSecurityStatus marketSecurityStatus = getNewOrExistingMarketSecurityStatus(marketCode, subMarketCode, instrument);
        marketSecurityStatus.setSettlementDate(settlementDate);
        // AMC Attenzione, questi campi sono salvati solo per BondVision. Gli altri mercati non li gestiscono
        if (marketCode == MarketCode.BV || marketCode == MarketCode.MTSPRIME) {
            marketSecurityStatus.setBondType(bondType);
            marketSecurityStatus.setQuoteIndicatorEnum(quoteIndicator);
            marketSecurityStatus.setQuoteIndicatorString(quoteIndicatorStr);
            marketSecurityStatus.setMarketAffiliationEnum(marketAffiliation);
            marketSecurityStatus.setMarketAffiliation(marketAffiliationStr);
        }
        
        marketSecurityStatusDao.saveOrUpdateMarketSecurityStatus(marketSecurityStatus);
    }

    @Override
    public String getMarketBondType(MarketCode marketCode, String instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        return (marketSecurityStatus != null) ? marketSecurityStatus.getBondType() : null; 
    }

    @Override
    public int getMarketAffiliation(MarketCode marketCode, String instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        return (marketSecurityStatus != null) ? marketSecurityStatus.getMarketAffiliationEnum() : 0;  
    }

    @Override
    public int getQuotIndicator(MarketCode marketCode, String instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument);
        return (marketSecurityStatus != null) ? marketSecurityStatus.getQuoteIndicatorEnum() : VALUE_CLEAN_PRICE;
    }

    public BigDecimal getMinTradingQty(MarketCode marketCode, Instrument instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument.getIsin());
        return (marketSecurityStatus != null) ? marketSecurityStatus.getMinQty() : null;  
    }

    public BigDecimal getMinIncrement(MarketCode marketCode, Instrument instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument.getIsin());
        return (marketSecurityStatus != null) ? marketSecurityStatus.getMinIncrement() : null;
    }

    public BigDecimal getQtyMultiplier(MarketCode marketCode, Instrument instrument) {
        MarketSecurityStatus marketSecurityStatus = marketSecurityStatusDao.getMarketSecurityStatus(marketCode, null, instrument.getIsin());
        return (marketSecurityStatus != null) ? marketSecurityStatus.getQtyMultiplier() : null;
    }

    public boolean validQuantities(MarketCode marketCode, Instrument instrument, Order order) {

        if (instrument == null) {
            LOGGER.warn("Order {}: checking minimum tradable quantity and minimum increment, the instrument received is null, consider quantities as valid to allow the proposal to be processed.",  order.getFixOrderId());
            return true;
        }

        boolean valid = true;
        BigDecimal instrMinTradableQty = null;
        BigDecimal instrMinIncrement = null;
        String isin = instrument.getIsin();
        try {
            LOGGER.debug("Order {}, checking minimum tradable quantity and minimum increment, against the order qty {}", order.getFixOrderId(), order.getQty());
            instrMinTradableQty = getMinTradingQty(marketCode, instrument);
            if (instrMinTradableQty != null) {
                LOGGER.debug("Instrument {}, MarketSecurityStatus min tradable qty : {}", isin, instrMinTradableQty);
            } else {
                instrMinTradableQty = instrument.getMinSize();
                LOGGER.info("Instrument {}, InstrumentsTable minimum tradable quantity : {}", isin, instrMinTradableQty);
            }
        } catch (Exception e) {
            LOGGER.error("Error while looking for the instrument {} minimun size, cannot find it! Setting the minimum tradable quantity to 1 to allow the proposal to be considered.", isin, e);
            instrMinTradableQty = BigDecimal.ONE;
        }

        try {
            instrMinIncrement = getMinIncrement(marketCode, instrument);
            if (instrMinIncrement != null) {
                LOGGER.debug("Instrument {}, MarketSecurityStatus min increment : {}", isin, instrMinIncrement);
            } else {
                instrMinIncrement = BigDecimal.ONE;
                LOGGER.info("Instrument " + isin + ", min increment : " + instrMinIncrement);
            }
        } catch (Exception e) {
            LOGGER.error("Error while looking for the instrument " + isin + " minimun increment, cannot find it. Setting it to 1.", e);
            instrMinIncrement = BigDecimal.ONE;
        }

        if (order.getQty().compareTo(instrMinTradableQty) < 0) {
            LOGGER.info("Order  " + order.getFixOrderId() + ", quantity lesser than the minimum tradable one for the market (" + instrMinTradableQty.doubleValue() + ").");
            valid = false;
        }

        // [RR20120910] Check also the minimum increment
        BigDecimal orderQty = order.getQty();
        instrMinIncrement = instrMinIncrement.setScale(orderQty.scale(), RoundingMode.HALF_DOWN);

        if (orderQty.remainder(instrMinIncrement).compareTo(BigDecimal.ZERO) != 0) {
            LOGGER.info("Order  " + order.getFixOrderId() + ", quantity not multiple of the minimum increment for the market (" + instrMinIncrement.doubleValue() + ").");
            valid = false;
        }

        LOGGER.info("Order " + order.getFixOrderId() + ", the quantities for sending an order to the market " + marketCode.name() + " are" + (valid ? "" : " not") + " valid.");
        return valid;
    }
}
