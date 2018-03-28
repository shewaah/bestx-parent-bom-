package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.jsscommon.Money;

import java.util.Date;

public interface MarketBuySideConnection {
    
    void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException;

    void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException;

    void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException;

    void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException;

    void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException;

    void ackProposal(Operation listener, Proposal proposal) throws BestXException;
    
    void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException;

    void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException;

    void revokeOrder(Operation listener, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException;

    void matchOperations(Operation listener, Operation matching, Money ownPrice, Money matchingPrice) throws BestXException;

    void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException;

    MarketCode getMarketCode();

    MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate);
}
