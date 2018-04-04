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
package it.softsolutions.bestx.markets.bloomberg;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.model.Proposal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 12/lug/2013
 * 
 **/
public class OnQuoteSubjectRunnable implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnQuoteSubjectRunnable.class);
	private final BloombergMarket market;
	private final String quoteReqId;
	private final String quoteId;

	public OnQuoteSubjectRunnable(String quoteReqId, String quoteId, BloombergMarket market) {
		this.market = market;
		this.quoteReqId = quoteReqId;
		this.quoteId = quoteId;
	}

	@Override
	public void run() {
		try {
			final Operation operation = market.getOperationRegistry().getExistingOperationById(OperationIdType.TSOX_CLORD_ID, quoteReqId);

			// market.getMarketStatistics().orderResponseReceived(quoteReqId, false);
            LOGGER.info("[MktMsg] OrderID {}, QuoteStatus/Expired received QuoteReqID {}, QuoteID {}", operation.getOrder().getFixOrderId(), quoteReqId, quoteId);

			operation.onMarketProposalStatusChange(market, quoteId, Proposal.ProposalType.INDICATIVE);
		} catch (BestXException e) {
			LOGGER.error("Error while finding Operation on {} with QuoteReqId {}", market.getMarketCode(), quoteReqId, e);
		}

	}
}
