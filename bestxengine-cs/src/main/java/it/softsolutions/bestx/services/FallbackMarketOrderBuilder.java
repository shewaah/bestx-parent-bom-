/*
* Copyright 1997-2021 SoftSolutions! srl 
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

package it.softsolutions.bestx.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.bestexec.MarketOrderBuilderListener;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.proposalclassifiers.BaseMarketMakerClassifier;
import it.softsolutions.bestx.services.rest.CSMarketOrderBuilder;

/**
 *
 * Purpose: this class is mainly for choose wich builder use following these
 * rules: 
 * 1) the heartbeat flows and system status is up. Request are answered
 * in time with no ERROR. BestX:FI-A uses the response to define the execution
 * attempt. If there is a WARNING, BestX:FI-A logs the warning. 
 * 2) the hearbeat
 * flows and the system status is up. Request is timed out. BestX:FI-A goes to the
 * fallback algo to define the execution attempt wheter it is a LF or algo
 * order. Other orders (orders that are executed afterwards) are not affected.
 * 3) the heartbeat flows and system status is up. Request are answered in time
 * with ERROR. BestX rejects back each order when the service GetRoutingProposal
 * is responding with 1 or more Errors. Each new order continues to call the
 * service GetRoutingProposal. An error response from GetRoutingProposal does
 * not say anything about the availability of the services, that’s what he
 * heartbeat does. 
 * 4) the hearbeat flows and the system status is down. BeBestX:FI-A * goes to fallback 
 * 5) the heartbeat is timed out. BeBestX:FI-Aoes to fallback
 *
 * Project Name : bestxengine-cs First created by: stefano.pontillo Creation
 * date: 27 lug 2021
 * 
 **/
public class FallbackMarketOrderBuilder extends MarketOrderBuilder {
	   private static final Logger LOGGER = LoggerFactory.getLogger(FallbackMarketOrderBuilder.class);

	private MarketOrderBuilder defaultMarketOrderBuilder;

	private CSMarketOrderBuilder csAlgoMarketOrderBuilder;

	public FallbackMarketOrderBuilder() {
		super();
	}
	private class FallbackMarketOrderBuilderListener implements MarketOrderBuilderListener {
		private Operation operation;

		public FallbackMarketOrderBuilderListener(Operation operation) {
			this.operation = operation;
		}

		@Override
		public void onMarketOrderBuilt(MarketOrderBuilder source, MarketOrder marketOrder) {
			this.operation.onMarketOrderBuilt(FallbackMarketOrderBuilder.this, marketOrder);
		}

		@Override
		public void onMarketOrderTimeout(MarketOrderBuilder source) {
			try {
			FallbackMarketOrderBuilder.this.defaultMarketOrderBuilder.buildMarketOrder(operation, operation);
			} catch (Exception e) {
				LOGGER.error("Exception in FallbackMarketOrderBuilder.this.defaultMarketOrderBuilder.buildMarketOrder", e);
			}
		}

		@Override
		public void onMarketOrderException(MarketOrderBuilder source, Exception ex) {
			this.operation.onMarketOrderException(FallbackMarketOrderBuilder.this, ex);
		}

		@Override
		public void onMarketOrderErrors(MarketOrderBuilder source, List<String> errors) {
			this.operation.onMarketOrderErrors(FallbackMarketOrderBuilder.this, errors);
		}

	}

	@Override
	public void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) {
		// Update algo service status in attempt
		operation.getLastAttempt().updateServiceStatus(csAlgoMarketOrderBuilder.getServiceName(),
				!csAlgoMarketOrderBuilder.getServiceStatus(), csAlgoMarketOrderBuilder.getDownReason());

		if (csAlgoMarketOrderBuilder.getServiceStatus() && !BondTypesService.isUST(operation.getOrder().getInstrument())) {
			csAlgoMarketOrderBuilder.buildMarketOrder(operation, new FallbackMarketOrderBuilderListener(operation));
		} else {
			try {
				defaultMarketOrderBuilder.buildMarketOrder(operation, operation);
			} catch (Exception e) {
				LOGGER.error("Exception in csAlgoMarketOrderBuilder.buildMarketOrder", e);
			}
		}
	}

	public MarketOrderBuilder getDefaultMarketOrderBuilder() {
		return defaultMarketOrderBuilder;
	}

	public void setDefaultMarketOrderBuilder(MarketOrderBuilder defaultMarketOrderBuilder) {
		this.defaultMarketOrderBuilder = defaultMarketOrderBuilder;
	}

	public CSMarketOrderBuilder getCsAlgoMarketOrderBuilder() {
		return csAlgoMarketOrderBuilder;
	}

	public void setCsAlgoMarketOrderBuilder(CSMarketOrderBuilder csAlgoMarketOrderBuilder) {
		this.csAlgoMarketOrderBuilder = csAlgoMarketOrderBuilder;
	}

}
