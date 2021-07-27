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

package it.softsolutions.bestx.services.rest.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GetRoutingProposalResponseData {

	public static enum Venue {
		BLOOMBERG("BLOOMBERG"), MARKETAXESS ("MARKETAXESS"), TW ("TW");
		private String venue;
		private Venue(String venue) {
			this.venue = venue;
		}
		public String toString() {
			return this.venue;
		}
	};

	private BigDecimal targetPrice;
	private BigDecimal limitMonitorPrice;
	private List<String> includeDealers = new ArrayList<>();
	private List<String> excludeDealers = new ArrayList<>();
	private Venue targetVenue;
	private List<ExceptionMessage> exceptions;

	public BigDecimal getTargetPrice() {
		return targetPrice;
	}

	public void setTargetPrice(BigDecimal targetPrice) {
		this.targetPrice = targetPrice;
	}

	public BigDecimal getLimitMonitorPrice() {
		return limitMonitorPrice;
	}

	public void setLimitMonitorPrice(BigDecimal limitMonitorPrice) {
		this.limitMonitorPrice = limitMonitorPrice;
	}

	public List<String> getIncludeDealers() {
		return includeDealers;
	}

	public void setIncludeDealers(List<String> includeDealers) {
		this.includeDealers = includeDealers;
	}

	public List<String> getExcludeDealers() {
		return excludeDealers;
	}

	public void setExcludeDealers(List<String> excludeDealers) {
		this.excludeDealers = excludeDealers;
	}

	public Venue getTargetVenue() {
		return targetVenue;
	}

	public void setTargetVenue(Venue targetVenue) {
		this.targetVenue = targetVenue;
	}

	public List<ExceptionMessage> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<ExceptionMessage> exceptions) {
		this.exceptions = exceptions;
	}

	@Override
	public String toString() {
		return "GetRoutingProposalResponseData [targetPrice=" + targetPrice + ", limitMonitorPrice=" + limitMonitorPrice
				+ ", includeDealers=" + includeDealers + ", excludeDealers=" + excludeDealers + ", targetVenue="
				+ targetVenue + ", exceptions=" + exceptions + "]";
	}

}
