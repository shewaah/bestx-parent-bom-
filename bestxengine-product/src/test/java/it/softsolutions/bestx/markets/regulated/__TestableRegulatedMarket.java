/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.markets.regulated;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedProposalInputLazyBean;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class __TestableRegulatedMarket extends RegulatedMarket
{

	__TestableRegulatedMarket(){
		setMarketSecurityStatusService(new __TestableMarketSecurityStatusServer());
	}

	public ClassifiedProposal testGetConsolidatedProposal(ProposalSide side, 
			OrderSide orderSide,
			List<RegulatedProposalInputLazyBean> list, 
			Money orderPriceLimit, 
			BigDecimal quantity,
			String instrumentCurrency) throws BestXException {

		//return null;
		Instrument instrument = new Instrument();
		instrument.setCurrency(instrumentCurrency);
		return super.getConsolidatedProposal(side, orderSide, list, orderPriceLimit, quantity, instrument);
	}



	@Override
	public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker,
			Date minArrivalDate)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInstrumentQuotedOnMarket(Instrument instrument)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onOrderReject(String regSessionId, String reason)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onOrderTechnicalReject(String regSessionId, String reason)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onOrderCancelled(String regSessionId, String reason)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getConnectionName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onExecutionReport(String regSessionId, ExecutionReportState executionReportState,
			RegulatedFillInputBean regulatedFillInputBean)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void checkPriceConnection() throws  MarketNotAvailableException
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected String getNewPriceSessionId() throws BestXException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Venue getVirtualVenue()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected MarketBuySideConnection getMarket()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MarketCode getMarketCode()
	{
		return MarketCode.TLX;
	}

	@Override
	public boolean isBuySideConnectionAvailable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPriceConnectionAvailable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMagnetMarket()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cleanBook()
	{
		SimpleMarketProposalAggregator.getInstance().clearBooks();
	}

    @Override
    public void ackProposal(Operation listener, Proposal proposal) throws BestXException {
        // TODO Auto-generated method stub
        
    }

	@Override
    public int getActiveTimersNum() {
	    // TODO Auto-generated method stub
	    return 0;
    }
}
