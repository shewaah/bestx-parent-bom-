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

package it.softsolutions.bestx.services.ordervalidation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.dao.ExchangeRateDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerFilterRow;
import it.softsolutions.bestx.services.CommissionService;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class CustomerFilterFactoryImpl implements CustomerFilterFactory {
    private ExchangeRateDao exchangeRateDao;
    private BigDecimal retailMaxSize;
    private CommissionService commissionService;
    private BigDecimal internalAuthThreshold;
    private List<String> portfolioList;
    private String portfolioListStr;


    /**
     * Inits the.
     */
    public void init() {
        if(exchangeRateDao == null){
            throw new ObjectNotInitializedException("exchangeRateDao property not set");
        }
        if(retailMaxSize == null){
            throw new ObjectNotInitializedException("retailMaxSize property not set");
        }
        if(portfolioList == null) {
            portfolioList = new ArrayList<String>();
            // get portfolio list from portfolioListrStr
            if (portfolioListStr == null )
            {
                throw new ObjectNotInitializedException("portfolioList nor portfolioListStr properties set");
            }
        }
        if(portfolioListStr.length() > 0) {
            int lastIndex = 0;
            int index = portfolioListStr.indexOf(',', lastIndex);
            while (index != -1) {
                String portfolio = portfolioListStr.substring(lastIndex, index).trim();
                lastIndex = index+1;
                index = portfolioListStr.indexOf(',', lastIndex);
                portfolioList.add(portfolio);
            }
            //ultimo portafoglio
            String portfolio = portfolioListStr.substring(lastIndex).trim();
            portfolioList.add(portfolio);
        }
    }

    private RegulatedMktIsinsLoader regulatedMarketIsinsLoader;

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.ordervalidation.CustomerFilterFactory#createFilterList(it.softsolutions.bestx.model.CustomerFilterRow)
     */
    public List<OrderValidator> createFilterList(CustomerFilterRow filterRow, Customer customer) {
        List<OrderValidator> validatorList = new ArrayList<OrderValidator>();

        CustomerAttributes customerAttributes = (CustomerAttributes) customer.getCustomerAttributes(); 

        Boolean isCustomerInternalOne = (customerAttributes != null && customerAttributes.getInternalCustomer() != null && customerAttributes.getInternalCustomer());

        if(filterRow.isRatingFilter()){
            if (isCustomerInternalOne)
            {
                validatorList.add(new FilterOnPortfolioBasis(portfolioList, new RatingFilter()));
            }
            else
            {
                validatorList.add(new RatingFilter());
            }
        }
        if(filterRow.isManualFilter()){
            validatorList.add(new AlwaysManualFilter());
        }
        if(filterRow.isMaxSizeFilter()){
            validatorList.add(new MaxSizeFilter(exchangeRateDao, internalAuthThreshold));
        }
        if(filterRow.isRetailMaxSizeFilter()){
            validatorList.add(new RetailMaxSizeFilter(retailMaxSize));
        }
        if(filterRow.isaddCommissionToCustomerPriceFlagSetter()){
            validatorList.add(new AddCommissionToCustomerPriceFlagSetter());
        }

        if(filterRow.isNotRegisteredInRegulatedMarketsFilter()){
            validatorList.add(new NotRegisteredInRegulatedMarketsFilter(regulatedMarketIsinsLoader));
        }

        return validatorList;
    }


    /**
     * Sets the exchange rate dao.
     *
     * @param exchangeRateDao the exchangeRateDao to set
     */
    public void setExchangeRateDao(ExchangeRateDao exchangeRateDao) {
        this.exchangeRateDao = exchangeRateDao;
    }


    /**
     * Sets the retail max size.
     *
     * @param retailMaxSize the retailMaxSize to set
     */
    public void setRetailMaxSize(BigDecimal retailMaxSize) {
        this.retailMaxSize = retailMaxSize;
    }


    /**
     * Sets the commission service.
     *
     * @param commissionService the new commission service
     */
    public void setCommissionService(CommissionService commissionService)
    {
        this.commissionService = commissionService;
    }


    /**
     * Gets the internal auth threshold.
     *
     * @return the internal auth threshold
     */
    public BigDecimal getInternalAuthThreshold()
    {
        return this.internalAuthThreshold;
    }


    /**
     * Sets the internal auth threshold.
     *
     * @param internalAuthThreshold the new internal auth threshold
     */
    public void setInternalAuthThreshold(BigDecimal internalAuthThreshold)
    {
        this.internalAuthThreshold = internalAuthThreshold;
    }


    /**
     * Gets the portfolio list.
     *
     * @return the portfolio list
     */
    public List<String> getPortfolioList()
    {
        return this.portfolioList;
    }


    /**
     * Sets the portfolio list.
     *
     * @param portfolioList the new portfolio list
     */
    public void setPortfolioList(List<String> portfolioList)
    {
        this.portfolioList = portfolioList;
    }



    /**
     * Sets the portfolio list str.
     *
     * @param portfolioListStr the new portfolio list str
     */
    public void setPortfolioListStr(String portfolioListStr)
    {
        this.portfolioListStr = portfolioListStr;
    }


    /**
     * Gets the regulated market isins loader.
     *
     * @return the regulated market isins loader
     */
    public RegulatedMktIsinsLoader getRegulatedMarketIsinsLoader()
    {
        return this.regulatedMarketIsinsLoader;
    }


    /**
     * Sets the regulated market isins loader.
     *
     * @param regulatedMarketIsinsLoader the new regulated market isins loader
     */
    public void setRegulatedMarketIsinsLoader(RegulatedMktIsinsLoader regulatedMarketIsinsLoader)
    {
        this.regulatedMarketIsinsLoader = regulatedMarketIsinsLoader;
    }
}
