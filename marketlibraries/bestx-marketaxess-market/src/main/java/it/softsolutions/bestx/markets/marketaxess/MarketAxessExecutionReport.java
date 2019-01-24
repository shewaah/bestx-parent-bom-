
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.markets.marketaxess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.DealerID;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.messages.component.Dealers;
import quickfix.FieldNotFound;

/**
 *
 * Purpose: this class is to carry audit data collected from Execution report on MArketAxess
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 01 feb 2017
 * 
 **/

public class MarketAxessExecutionReport extends MarketExecutionReport {

	private List<Dealers.NoDealers> otherDealers = new ArrayList<Dealers.NoDealers>();
	private String notes = "";
	//BESTX-314 CS tracking defect ID 16170
	private BigDecimal lastParPx;
	private Character MKTXTrdRegPublicationReason;
	private Character MKTXTradeReportingInd;
	
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public String setNotes() {
		return this.notes;
	}
	
	public void addDealer(Dealers.NoDealers dealer) {
		otherDealers.add(dealer);
	}
	public List<Dealers.NoDealers> getDealers() {
		return otherDealers;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(super.toString());
		str.append(" notes = ");
		str.append(notes);
		str.append(" dealers = ");
		getDealers().forEach( dealer -> {
			DealerID dealerval = new DealerID();
			try {
				dealer.get(dealerval);
				str.append("[");
				str.append(dealerval.getValue());
				str.append("] ");
			} catch (FieldNotFound e) {
				;
			}});
		str.append(getDealers());
		return str.toString();
	}

	public void setLastParPx(BigDecimal lastParPx) {
		this.lastParPx = lastParPx;
	}

	public BigDecimal getLastParPx() {
		return this.lastParPx;
	}

   
   public Character getMKTXTrdRegPublicationReason() {
      return MKTXTrdRegPublicationReason;
   }

   
   public void setMKTXTrdRegPublicationReason(Character mKTXTrdRegPublicationReason) {
      MKTXTrdRegPublicationReason = mKTXTrdRegPublicationReason;
   }

   
   public Character getMKTXTradeReportingInd() {
      return MKTXTradeReportingInd;
   }

   
   public void setMKTXTradeReportingInd(Character mKTXTradeReportingInd) {
      MKTXTradeReportingInd = mKTXTradeReportingInd;
   }
	
}
