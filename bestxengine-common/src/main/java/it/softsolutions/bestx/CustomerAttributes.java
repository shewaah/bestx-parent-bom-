/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-10-20 06:52:20 $
 * Header       : $Id: CustomerAttributes.java,v 1.8 2010-10-20 06:52:20 anna.cochetti Exp $
 * Revision     : $Revision: 1.8 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/CustomerAttributes.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx;

import it.softsolutions.bestx.model.CustomerAttributesIFC;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerAttributes implements CustomerAttributesIFC {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAttributes.class);

	public static enum AttributeName {
		addCommissionToCustomerPrice, id, magnetProposalOnInexecution, internalCustomer, onlyEUROrders, amountCommissionWanted, wideQuoteSpread, limitPriceOffMarket;
	}

	private Boolean addCommissionToCustomerPrice;
	private int id;
	private Boolean magnetProposalOnInexecution;
	private Boolean internalCustomer;
	private Boolean onlyEUROrders;
	private Boolean amountCommissionWanted;
	private BigDecimal wideQuoteSpread;
	private BigDecimal limitPriceOffMarket;

	@Override
	public Object getAttribute(String attributeName) {
		Object res = null;

		switch (AttributeName.valueOf(attributeName)) {
		case addCommissionToCustomerPrice:
			res = addCommissionToCustomerPrice;
			break;
		case amountCommissionWanted:
			res = amountCommissionWanted;
			break;
		case id:
			res = id;
			break;
		case internalCustomer:
			res = internalCustomer;
			break;
		case limitPriceOffMarket:
			res = limitPriceOffMarket;
			break;
		case magnetProposalOnInexecution:
			res = magnetProposalOnInexecution;
			break;
		case onlyEUROrders:
			res = onlyEUROrders;
			break;
		case wideQuoteSpread:
			res = wideQuoteSpread;
			break;
		default:
			LOGGER.warn("Attribute {} not found", attributeName);
			break;
		}
		return res;
	}

	@Override
	public void setAttribute(String attributeName, Object attributeValue) {
		switch (AttributeName.valueOf(attributeName)) {
		case addCommissionToCustomerPrice:
			this.addCommissionToCustomerPrice = (Boolean) attributeValue;
			break;
		case amountCommissionWanted:
			this.amountCommissionWanted = (Boolean) attributeValue;
			break;
		case id:
			this.id = (Integer) attributeValue;
			break;
		case internalCustomer:
			this.internalCustomer = (Boolean) attributeValue;
			break;
		case limitPriceOffMarket:
			this.limitPriceOffMarket = (BigDecimal) attributeValue;
			break;
		case magnetProposalOnInexecution:
			this.magnetProposalOnInexecution = (Boolean) attributeValue;
			break;
		case onlyEUROrders:
			this.onlyEUROrders = (Boolean) attributeValue;
			break;
		case wideQuoteSpread:
			this.wideQuoteSpread = (BigDecimal) attributeValue;
			break;
		default:
			LOGGER.warn("Attribute {} not found", attributeName);
			break;
		}
	}

	@Override
	public Boolean getBooleanAttribute(String attributeName) {
		Boolean res = null;

		switch (AttributeName.valueOf(attributeName)) {
		case addCommissionToCustomerPrice:
			res = addCommissionToCustomerPrice;
			break;
		case amountCommissionWanted:
			res = amountCommissionWanted;
			break;
		case magnetProposalOnInexecution:
			res = magnetProposalOnInexecution;
			break;
		case onlyEUROrders:
			res = onlyEUROrders;
			break;
		default:
			LOGGER.warn("Attribute {} is not a boolean", attributeName);
			break;
		}
		return res;
	}

	@Override
	public void setBooleanAttribute(String attributeName, Boolean attributeValue) {
		switch (AttributeName.valueOf(attributeName)) {
		case addCommissionToCustomerPrice:
			this.addCommissionToCustomerPrice = (Boolean) attributeValue;
			break;
		case amountCommissionWanted:
			this.amountCommissionWanted = (Boolean) attributeValue;
			break;
		case internalCustomer:
			this.internalCustomer = (Boolean) attributeValue;
			break;
		case magnetProposalOnInexecution:
			this.magnetProposalOnInexecution = (Boolean) attributeValue;
			break;
		case onlyEUROrders:
			this.onlyEUROrders = (Boolean) attributeValue;
			break;
		default:
			LOGGER.warn("Attribute {} is not a boolean", attributeName);
			break;
		}

	}

	public Boolean getAddCommissionToCustomerPrice() {
    	return addCommissionToCustomerPrice;
    }

	public void setAddCommissionToCustomerPrice(Boolean addCommissionToCustomerPrice) {
    	this.addCommissionToCustomerPrice = addCommissionToCustomerPrice;
    }

	public Boolean getMagnetProposalOnInexecution() {
    	return magnetProposalOnInexecution;
    }

	public void setMagnetProposalOnInexecution(Boolean magnetProposalOnInexecution) {
    	this.magnetProposalOnInexecution = magnetProposalOnInexecution;
    }

	public Boolean getInternalCustomer() {
    	return internalCustomer;
    }

	public void setInternalCustomer(Boolean internalCustomer) {
    	this.internalCustomer = internalCustomer;
    }

	public Boolean getOnlyEUROrders() {
    	return onlyEUROrders;
    }

	public void setOnlyEUROrders(Boolean onlyEUROrders) {
    	this.onlyEUROrders = onlyEUROrders;
    }

	public Boolean getAmountCommissionWanted() {
    	return amountCommissionWanted;
    }

	public void setAmountCommissionWanted(Boolean amountCommissionWanted) {
    	this.amountCommissionWanted = amountCommissionWanted;
    }

	public BigDecimal getWideQuoteSpread() {
    	return wideQuoteSpread;
    }

	public void setWideQuoteSpread(BigDecimal wideQuoteSpread) {
    	this.wideQuoteSpread = wideQuoteSpread;
    }

	public BigDecimal getLimitPriceOffMarket() {
    	return limitPriceOffMarket;
    }

	public void setLimitPriceOffMarket(BigDecimal limitPriceOffMarket) {
    	this.limitPriceOffMarket = limitPriceOffMarket;
    }

	public int getId() {
    	return id;
    }

	public void setId(int id) {
    	this.id = id;
    }

	
	
}