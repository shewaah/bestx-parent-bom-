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
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public class Instrument implements Cloneable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Instrument.class);

    public static enum QuotingStatus {
        NEG, // in negoziazione
        CAN, // non negoziato
        PVOL // asta volatilita'
    }

    private String bbTicker;
    private String category;
    private String subCategory;
    private String securityType;
    private Country country;
    private BigDecimal coupon;
    private String currency;
    private String description;
    private BigDecimal incSize;
    private String isin;
    private String epic;
    private Date issueDate;
    private String issuerName;
    private Date maturityDate;
    private Date defaultSettlementDate;
    private Integer stdSettlementDays;
    private BigDecimal minSize;
    private String moodyRating;
    private String spRating;
    private String tidm;
    private Date updateDate;
    private Trader defaultTrader;
    private boolean inInventory;
    private String issuerIndustry;
    private String industrySubSector;
    private String RTFITicker;
    private UEMemberIssuer uEMemberIssuer;
    private String offeringType;
    private InstrumentAttributes instrumentAttributes;
    private String calendarCode;
    private Long dayCount;
    private String dayCountCode;
    private BigDecimal faceValue;
    private Date firstCouponDate;
    private BigDecimal frequency;
    private boolean endOfTheMonth;
    private Date BBSettlementDate;
    private String assetType;
    private Date interestAccrualDate;
    private String exDivCalendar;
    private Integer exDivDays;
    private BigDecimal rateo;
    private BigDecimal referenceRPI;
    private BigDecimal currentReferenceRPI;
    private String calculationTypeDesc;
    private String referenceRPIMonthConvention;
    private String bondType;
    
    public static final String INSTR_ASSET_TYPE_GOVIES = "Sovereign";
    public static final String INSTR_SECURITY_TYPE_EURO_ZONE = "EURO-ZONE";
    public static final String INDUSTRY_SUB_SECTOR_SOVEREIGN = "Sovereign";
    
    
    public static class InstrumentAttributes {
        private String isin;
        @SuppressWarnings("unused")
        private String instrument;
        private Boolean internal;
        private Boolean withProspect;
        private Boolean withoutProspect;
        private Boolean outlaw;
        private Boolean retailCustomerDisabled;
        private Portfolio portfolio;

        /**
         * @return the internal
         */
        public Boolean isInternal() {
            return internal;
        }

        /**
         * @param internal
         *            the internal to set
         */
        public void setInternal(Boolean internal) {
            this.internal = internal;
        }

        /**
         * @return the withProspect
         */
        public Boolean isWithProspect() {
            return (withProspect != null) && withProspect;
        }

        /**
         * @param withProspect
         *            the withProspect to set
         */
        public void setWithProspect(Boolean withProspect) {
            this.withProspect = withProspect;
        }

        /**
         * @return the withoutProspect
         */
        public Boolean isWithoutProspect() {
            return withoutProspect;
        }

        /**
         * @param withoutProspect
         *            the withoutProspect to set
         */
        public void setWithoutProspect(Boolean withoutProspect) {
            this.withoutProspect = withoutProspect;
        }

        /**
         * @return the outlaw
         */
        public Boolean isOutlaw() {
            return outlaw;
        }

        /**
         * @param outlaw
         *            the outlaw to set
         */
        public void setOutlaw(Boolean outlaw) {
            this.outlaw = outlaw;
        }

        /**
         * @return the retailCustomerDisabled
         */
        public Boolean isRetailCustomerDisabled() {
            return retailCustomerDisabled;
        }

        /**
         * @param retailCustomerDisabled
         *            the retailCustomerDisabled to set
         */
        public void setRetailCustomerDisabled(Boolean retailCustomerDisabled) {
            this.retailCustomerDisabled = retailCustomerDisabled;
        }

        /**
         * @return the portfolio
         */
        public Portfolio getPortfolio() {
            return portfolio;
        }

        /**
         * @param portfolio
         *            the portfolio to set
         */
        public void setPortfolio(Portfolio portfolio) {
            this.portfolio = portfolio;
        }

        /**
         * @return the isin
         */
        public String getIsin() {
            return isin;
        }

        /**
         * @param isin
         *            the isin to set
         */
        public void setIsin(String isin) {
            this.isin = isin;
        }
    }

    @Override
    public Instrument clone() throws CloneNotSupportedException {
        Instrument bean = null;
        try {
            bean = (Instrument) super.clone();
        } catch (CloneNotSupportedException e) {
            assert (false) : "Fatal error: clone not supported";
        }
        return bean;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        
        if (!(o instanceof Instrument)) {
            return false;
        }
        
        return getIsin().equals(((Instrument) o).getIsin());
    }

    @Override
    public int hashCode() {
        return getIsin().hashCode();
    }

    public void setBbTicker(String bbTicker) {
        this.bbTicker = bbTicker;
    }

    public String getBbTicker() {
        return bbTicker;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Country getCountry() {
        return country;
    }

    public void setCoupon(BigDecimal coupon) {
        this.coupon = coupon;
    }

    public BigDecimal getCoupon() {
        return coupon;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        if (description != null && !description.isEmpty()) {
            return description;
        } else {
            return isin;
        }
    }

    public void setIncSize(BigDecimal incSize) {
        this.incSize = incSize;
    }

    public BigDecimal getIncSize() {
        return incSize;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getIsin() {
        return isin;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setMaturityDate(Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }

    public void setMinSize(BigDecimal minSize) {
        this.minSize = minSize;
    }

    public BigDecimal getMinSize() {
        if (minSize == null) {
            LOGGER.warn("Instrument  {} minimun trading quantity not available, returning 0.", getIsin());
            minSize = BigDecimal.ZERO;
        }

        return minSize;
    }

    public void setMoodyRating(String moodyRating) {
        this.moodyRating = moodyRating;
    }

    public String getMoodyRating() {
        return moodyRating;
    }

    public void setSpRating(String spRating) {
        this.spRating = spRating;
    }

    public String getSpRating() {
        return spRating;
    }

    public void setTidm(String tidm) {
        this.tidm = tidm;
    }

    public String getTidm() {
        return tidm;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    public String getSecurityType() {
        return securityType;
    }

    public void setDefaultSettlementDate(Date defaultSettlementDate) {
        this.defaultSettlementDate = defaultSettlementDate;
    }

    public Date getDefaultSettlementDate() {
        return defaultSettlementDate;
    }

    public void setStdSettlementDays(Integer stdSettlementDays) {
        this.stdSettlementDays = stdSettlementDays;
    }

    public Integer getStdSettlementDays() {
        return stdSettlementDays;
    }

    public void setInInventory(boolean inInventory) {
        this.inInventory = inInventory;
    }

    public boolean isInInventory() {
        return inInventory;
    }

    public void setDefaultTrader(Trader defaultTrader) {
        this.defaultTrader = defaultTrader;
    }

    public Trader getDefaultTrader() {
        return defaultTrader;
    }

    /**
     * @return the issuerIndustry
     */
    public String getIssuerIndustry() {
        return issuerIndustry;
    }

    /**
     * @param issuerIndustry
     *            the issuerIndustry to set
     */
    public void setIssuerIndustry(String issuerIndustry) {
        this.issuerIndustry = issuerIndustry;
    }

    /**
     * @param industrySubSector
     *            the industrySubSector to set
     */
    public void setIndustrySubSector(String industrySubSector) {
        this.industrySubSector = industrySubSector;
    }

    /**
     * @return the industrySubSector
     */
    public String getIndustrySubSector() {
        return industrySubSector;
    }

    /**
     * @return the rTFITicker
     */
    public String getRTFITicker() {
        return RTFITicker;
    }

    /**
     * @param ticker
     *            the rTFITicker to set
     */
    public void setRTFITicker(String ticker) {
        RTFITicker = ticker;
    }

    /**
     * @return the instrumentAttributes
     */
    public InstrumentAttributes getInstrumentAttributes() {
        if (instrumentAttributes == null) {
            InstrumentAttributes a = new InstrumentAttributes();
            a.setIsin(this.isin);
            a.setInternal(false);
            a.setWithoutProspect(false);
            a.setWithProspect(false);
            a.setOutlaw(false);
            Portfolio p = new Portfolio();
            p.setId(0);
            a.setPortfolio(p);
            a.setRetailCustomerDisabled(false);
            this.setInstrumentAttributes(a);
            return a;
        }
        return instrumentAttributes;
    }

    /**
     * @param instrumentAttributes
     *            the instrumentAttributes to set
     */
    public void setInstrumentAttributes(InstrumentAttributes instrumentAttributes) {
        this.instrumentAttributes = instrumentAttributes;
    }

    /**
     * @return the uEMemberIssuer
     */
    public UEMemberIssuer getUEMemberIssuer() {
        return uEMemberIssuer;
    }

    /**
     * @param memberIssuer
     *            the uEMemberIssuer to set
     */
    public void setUEMemberIssuer(UEMemberIssuer memberIssuer) {
        uEMemberIssuer = memberIssuer;
    }

    @Override
    public String toString() {
        return isin;
    }

    /**
     * @return the offeringType
     */
    public String getOfferingType() {
        return offeringType;
    }

    /**
     * @param offeringType
     *            the offeringType to set
     */
    public void setOfferingType(String offeringType) {
        this.offeringType = offeringType;
    }

    public String getEpic() {
        return epic;
    }

    public void setEpic(String epic) {
        this.epic = epic;
    }

    public String getCalendarCode() {
        return calendarCode;
    }

    public void setCalendarCode(String calendarCode) {
        this.calendarCode = calendarCode;
    }

    public Long getDayCount() {
        return dayCount;
    }

    public void setDayCount(Long dayCount) {
        this.dayCount = dayCount;
    }

    public String getDayCountCode() {
        return dayCountCode;
    }

    public void setDayCountCode(String dayCountCode) {
        this.dayCountCode = dayCountCode;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }

    public Date getFirstCouponDate() {
        return firstCouponDate;
    }

    public void setFirstCouponDate(Date firstCouponDate) {
        this.firstCouponDate = firstCouponDate;
    }

    public BigDecimal getFrequency() {
        return frequency;
    }

    public void setFrequency(BigDecimal frequency) {
        this.frequency = frequency;
    }

    public boolean isEndOfTheMonth() {
        return endOfTheMonth;
    }

    public void setEndOfTheMonth(boolean endOfTheMonth) {
        this.endOfTheMonth = endOfTheMonth;
    }

    public Date getBBSettlementDate() {
        return BBSettlementDate;
    }

    public void setBBSettlementDate(Date settlementDate) {
        BBSettlementDate = settlementDate;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Date getInterestAccrualDate() {
        return this.interestAccrualDate;
    }

    public void setInterestAccrualDate(Date interestAccrualDate) {
        this.interestAccrualDate = interestAccrualDate;
    }

    /**
     * @return the exDivCalendar
     */
    public String getExDivCalendar() {
        return this.exDivCalendar;
    }

    /**
     * @param exDivCalendar
     *            the exDivCalendar to set
     */
    public void setExDivCalendar(String exDivCalendar) {
        this.exDivCalendar = exDivCalendar;
    }

    public BigDecimal getRateo() {
        return rateo;
    }

    public void setRateo(BigDecimal rateo) {
        this.rateo = rateo;
    }

    /**
     * @return the exDivDays
     */
    public Integer getExDivDays() {
        return this.exDivDays;
    }

    /**
     * @param exDivDays
     *            the exDivDays to set
     */
    public void setExDivDays(Integer exDivDays) {
        this.exDivDays = exDivDays;
    }

    /**
     * @return the referenceRPI
     */
    public BigDecimal getReferenceRPI() {
        return this.referenceRPI;
    }

    /**
     * @param referenceRPI
     *            the referenceRPI to set
     */
    public void setReferenceRPI(BigDecimal referenceRPI) {
        this.referenceRPI = referenceRPI;
    }

    /**
     * @return the calculationTypeDesc
     */
    public String getCalculationTypeDesc() {
        return this.calculationTypeDesc;
    }

    /**
     * @param calculationTypeDesc
     *            the calculationTypeDesc to set
     */
    public void setCalculationTypeDesc(String calculationTypeDesc) {
        this.calculationTypeDesc = calculationTypeDesc;
    }

    /**
     * @return the currentReferenceRPI
     */
    public BigDecimal getCurrentReferenceRPI() {
        return this.currentReferenceRPI;
    }

    /**
     * @param currentReferenceRPI
     *            the currentReferenceRPI to set
     */
    public void setCurrentReferenceRPI(BigDecimal currentReferenceRPI) {
        this.currentReferenceRPI = currentReferenceRPI;
    }

    /**
     * @return the referenceRPIMonthConvention
     */
    public String getReferenceRPIMonthConvention() {
        return this.referenceRPIMonthConvention;
    }

    /**
     * @param referenceRPIMonthConvention
     *            the referenceRPIMonthConvention to set
     */
    public void setReferenceRPIMonthConvention(String referenceRPIMonthConvention) {
        this.referenceRPIMonthConvention = referenceRPIMonthConvention;
    }

    public String getBondType() {
        return bondType;
    }

    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    public boolean isIndustrySubSectorSovereign() {
    	return INDUSTRY_SUB_SECTOR_SOVEREIGN.equalsIgnoreCase(getIndustrySubSector());
    }
    
    public boolean isAGovie() {
//        return INSTR_ASSET_TYPE_GOVIES.equals(getAssetType());
    	return INDUSTRY_SUB_SECTOR_SOVEREIGN.equalsIgnoreCase(getIndustrySubSector());
    }
    
    public boolean isAnEuroZone() {
        return INSTR_SECURITY_TYPE_EURO_ZONE.equals(getSecurityType());
    }
}
