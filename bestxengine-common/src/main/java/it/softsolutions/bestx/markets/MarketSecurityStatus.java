package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.math.BigDecimal;
import java.util.Date;

public class MarketSecurityStatus {
    @SuppressWarnings("unused")
    private long id;
    private MarketCode marketCode;
    private SubMarketCode subMarketCode;
    private String instrument;
    private String statusCode;
    private Date settlementDate;
    private BigDecimal minQty;
    private BigDecimal minIncrement;
    private BigDecimal qtyMultiplier;
    private String bondType;
    private String marketAffiliation;
    private int marketAffiliationEnum;
    private int quoteIndicatorEnum;
    private String quoteIndicatorString;
    private Date updateDate;

    public BigDecimal getQtyMultiplier() {
        return this.qtyMultiplier;
    }

    public void setQtyMultiplier(BigDecimal qtyMultiplier) {
        this.qtyMultiplier = qtyMultiplier;
    }

    public BigDecimal getMinQty() {
        return this.minQty;
    }

    public void setMinQty(BigDecimal minQty) {
        this.minQty = minQty;
    }

    public BigDecimal getMinIncrement() {
        return this.minIncrement;
    }

    public void setMinIncrement(BigDecimal qtyTick) {
        this.minIncrement = qtyTick;
    }

    public SubMarketCode getSubMarketCode() {
        return subMarketCode;
    }

    public void setSubMarketCode(SubMarketCode subMarketCode) {
        this.subMarketCode = subMarketCode;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Date getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(Date settlementDate) {
        this.settlementDate = settlementDate;
    }

    public MarketCode getMarketCode() {
        return marketCode;
    }

    public void setMarketCode(MarketCode marketCode) {
        this.marketCode = marketCode;
    }

    public String getBondType() {
        return this.bondType;
    }

    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    public int getMarketAffiliationEnum() {
        return this.marketAffiliationEnum;
    }

    public void setMarketAffiliationEnum(int marketAffiliationEnum) {
        this.marketAffiliationEnum = marketAffiliationEnum;
    }

    public String getMarketAffiliation() {
        return this.marketAffiliation;
    }

    public void setMarketAffiliation(String marketAffiliation) {
        this.marketAffiliation = marketAffiliation;
    }

    public int getQuoteIndicatorEnum() {
        return this.quoteIndicatorEnum;
    }

    public void setQuoteIndicatorEnum(int quoteIndicatorEnum) {
        this.quoteIndicatorEnum = quoteIndicatorEnum;
    }

    public String getQuoteIndicatorString() {
        return this.quoteIndicatorString;
    }

    public void setQuoteIndicatorString(String quoteIndicatorString) {
        this.quoteIndicatorString = quoteIndicatorString;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }
}
