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
package it.softsolutions.bestx.handlers;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalType;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: paolo.midali 
 * Creation date: 04/mar/2013 
 * 
 **/
public class InternalizationHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(InternalizationHelper.class);

	public enum action_type {
		sendRfq, sendOrder, reject, warning
	};

	public static boolean isQuoteActuallyTradeable(Proposal quote, Order order) {
		ProposalType type = quote.getType();
		// settlement date RFQ e quote uguali
		boolean sameSettlementDate = DateUtils.isSameDay(order.getFutSettDate(), quote.getFutSettDate());
		// quantita' della proposal valida se maggiore o uguale a quella dell'ordine
		boolean validProposalQty = quote.getQty().compareTo(order.getQty()) >= 0;

		return (type == ProposalType.TRADEABLE && sameSettlementDate && validProposalQty);
	}

	public static boolean isQuoteActuallyIndicative(Proposal quote, Order order) {
		ProposalType type = quote.getType();
		// settlement date RFQ e quote uguali
		boolean sameSettlementDate = DateUtils.isSameDay(order.getFutSettDate(), quote.getFutSettDate());
		// quantita' della proposal valida se maggiore o uguale a quella dell'ordine
		// settlement date RFQ e quote uguali oppure relative ad un broker che le accetta non standard (ovvero differenti)
		boolean validSettlementDate = sameSettlementDate || quote.isNonStandardSettlementDateAllowed();
		// [RR20130304] CRSBXIGR-48 gestione di una COUNTER come concordato con Anna, se arriva trattarla come un prezzo INDICATIVE
		return (type == ProposalType.INDICATIVE && validSettlementDate) || (type == ProposalType.TRADEABLE && validSettlementDate) || type == ProposalType.COUNTER;
	}

	public static boolean isInternalQuote(MarketCode mktCode, String quoteReqID) {
		return false;
	}
}
