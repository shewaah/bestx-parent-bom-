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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 11/dic/2013
 * 
 */
public interface CommissionService {
	Commission getCommission(Customer customer, Instrument instrument, Money amountDue, OrderSide orderSide) throws BestXException;

	BigDecimal calculateCommissionedPrice(BigDecimal originalPrice, BigDecimal limitPrice, OrderSide side, Commission commission) throws BestXException;

	public BigDecimal calculateCommissionAmount(Money amountDue, Commission commission) throws BestXException;

	public void initializeExecutionReportCommission(ExecutionReport execReport, ExecutionReport lastExecutionReport, ExecutionReport firstExecutionReport, Commission commission,
	        boolean isFirstExecReport, String orderCurr) throws BestXException;

	BigDecimal calculatePartialFillsCommissionedPrice(BigDecimal originalPrice, BigDecimal limitPrice, OrderSide side, Commission commission, BigDecimal commissionValue) throws BestXException;
}
