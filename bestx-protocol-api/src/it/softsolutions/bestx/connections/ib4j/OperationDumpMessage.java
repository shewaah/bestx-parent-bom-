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

package it.softsolutions.bestx.connections.ib4j;

import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.ib4j.IBException;
import it.softsolutions.ib4j.clientserver.IBcsMessage;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-protocol-api 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class OperationDumpMessage extends IBcsMessage {

    private static final long serialVersionUID = 5977634591219422937L;
    
    /**
     * Instantiates a new akros operation dump message.
     *
     * @param operation the operation
     * @param operationState the operation state
     * @throws IBException the iB exception
     */
    public OperationDumpMessage(Operation operation, OperationState operationState) throws IBException {
        setValue("0perationState", operationState.getClass().getSimpleName());
        String fixSession = operation.getIdentifier(OperationIdType.FIX_SESSION);
        if (fixSession != null)
        {
            setValue("fixSession", fixSession);
        }
        String btsTsn = operation.getIdentifier(OperationIdType.BTS_TSN);
        if (btsTsn != null)
        {
            setValue("btsTsn", btsTsn);
        }
        Rfq rfq = operation.getRfq();
        String rfqId = operation.getIdentifier(OperationIdType.RFQ_ID);
        if (rfq != null)
        {
            setRfqValues("rfq", rfq, rfqId);
        }
        Order order = operation.getOrder();
        String orderId = operation.getIdentifier(OperationIdType.ORDER_ID);
        if (order != null)
        {
            setOrderValues(order, orderId);
        }
        if (operation.getQuote() != null)
        {
            setQuoteValues(operation.getQuote());
        }
        int attemptNo = 0;
        List<Attempt> attemptList = operation.getAttempts();
        for (Attempt attempt : attemptList) {
            setProposalValues("attempt" + attemptNo++ + "ExecutionProposal", attempt.getExecutionProposal());
            if (attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null)
            {
                setProposalValues("attempt" + attemptNo++ + "CounterOffer", attempt.getExecutablePrice(0).getClassifiedProposal());
            }
            if (attempt.getSortedBook() != null)
            {
                setBookValues("attempt" + attemptNo++, attempt.getSortedBook());
            }
        }                   
        int executionReportNo = 0;
        List<ExecutionReport> executionReportList = operation.getExecutionReports();
        for (ExecutionReport executionReport : executionReportList) {
            setExecutionReportsValues(executionReportNo++, executionReport);
        }
    }

    private void setExecutionReportsValues(int index, ExecutionReport executionReport) throws IBException {
        setValue("executionReport" + index + "UniqueId", executionReport.getSequenceId());
        setValue("executionReport" + index + "State", executionReport.getState().name());
        setValue("executionReport" + index + "MarketId", executionReport.getMarket() != null ? executionReport.getMarket().getMarketId() : null);
        setValue("executionReport" + index + "InstrumentIsin", executionReport.getInstrument() != null ? executionReport.getInstrument().getIsin() : null);
        setValue("executionReport" + index + "Side", executionReport.getSide().name());
        setValue("executionReport" + index + "OrderQty", executionReport.getOrderQty());
        setValue("executionReport" + index + "ActualQty", executionReport.getActualQty());
        setValue("executionReport" + index + "Price", executionReport.getPrice() != null ? executionReport.getPrice().getAmount().toString() : null);
        setValue("executionReport" + index + "Ticket", executionReport.getTicket());
        setValue("executionReport" + index + "TransactTime", executionReport.getTransactTime() != null ? DateFormatUtils.format(executionReport.getTransactTime(), "yyyyMMdd HH:mm:ss.SSS") : null);
    }

    private void setBookValues(String prefix, SortedBook book) throws IBException {
        int askProposalNo = 0, bidProposalNo = 0;
        for (ClassifiedProposal askProposal : book.getAskProposals())
        {
            setProposalValues(prefix + "bookAskProposal" + askProposalNo++, askProposal);
        }
        for (ClassifiedProposal bidProposal : book.getBidProposals())
        {
            setProposalValues(prefix + "bookBidProposal" + bidProposalNo++, bidProposal);
        }
    }

    private void setQuoteValues(Quote quote) throws IBException {
        setValue("quoteInstrumentIsin", quote.getInstrument().getIsin());
        setValue("quoteSettlementDate", quote.getFutSettDate() != null ? DateFormatUtils.format(quote.getFutSettDate(), "yyyyMMdd") : null);
        setValue("quoteExpirationDate", quote.getExpiration() != null ? DateFormatUtils.format(quote.getExpiration(), "yyyyMMdd") : null);
        setValue("quoteAccruedDays", quote.getAccruedDays());
        setValue("quoteAccruedAmt", quote.getAccruedInterest() != null ? quote.getAccruedInterest().getAmount().toString() : null);
        setProposalValues("quoteAskProposal", quote.getAskProposal());
        setProposalValues("quoteBidProposal", quote.getBidProposal());
    }
    private void setRfqValues(String prefix, Rfq rfq, String rfqId) throws IBException {
        setValue(prefix + "UniqueId", rfqId);
        setValue(prefix + "CustomerId", rfq.getCustomer());
        setValue(prefix + "InstrumentIsin", rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : null);
        setValue(prefix + "Qty", rfq.getQty());
        setValue(prefix + "Side", rfq.getSide().name());
        setValue(prefix + "SettlementType", rfq.getSettlementType());
        setValue(prefix + "SettlementDate", rfq.getFutSettDate() != null ? DateFormatUtils.format(rfq.getFutSettDate(), "yyyyMMdd") : null);
        setValue(prefix + "SecExchange", rfq.getSecExchange());
        setValue(prefix + "TransactTime", rfq.getTransactTime() != null ? DateFormatUtils.format(rfq.getTransactTime(), "yyyyMMdd HH:mm:ss.SSS") : null);
    }
    private void setOrderValues(Order order, String orderId) throws IBException {
        setRfqValues("order", order, orderId);
        setValue("orderCustomerOrderId", order.getCustomerOrderId());
        setValue("orderType", order.getType().name());
        setValue("orderLimit", order.getLimit());
    }
    private void setProposalValues(String prefix, Proposal proposal) throws IBException {
        setValue(prefix + "MarketId", proposal.getMarket() != null ? proposal.getMarket().getMarketId() : null);
        setValue(prefix + "VenueCode", proposal.getVenue() != null ? proposal.getVenue().getCode() : null);
        setValue(prefix + "Side", proposal.getSide().name());
        setValue(prefix + "Type", proposal.getType().name());
        setValue(prefix + "Qty", proposal.getQty());
        setValue(prefix + "Price", proposal.getPrice() != null ? proposal.getPrice().getAmount().toString() : null);
        setValue(prefix + "Expiration", proposal.getExpiration() != null ? DateFormatUtils.format(proposal.getExpiration(), "yyyyMMdd") : null);
        setValue(prefix + "FutSettDate", proposal.getFutSettDate() != null ? DateFormatUtils.format(proposal.getFutSettDate(), "yyyyMMdd") : null);
        setValue(prefix + "CustomerAdditionalExpenses", proposal.getCustomerAdditionalExpenses() != null ? proposal.getCustomerAdditionalExpenses().getAmount().toString() : null);
        setValue(prefix + "Timestamp", proposal.getTimestamp() != null ? DateFormatUtils.format(proposal.getTimestamp(), "yyyyMMdd HH:mm:ss.SSS") : null);
    }
}
