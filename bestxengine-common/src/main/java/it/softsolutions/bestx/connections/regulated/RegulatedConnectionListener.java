package it.softsolutions.bestx.connections.regulated;

import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Proposal.ProposalSide;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.naming.OperationNotSupportedException;

public interface RegulatedConnectionListener {
    public static enum StatusField {MARKET_STATUS, MEMBER_STATUS, USER_STATUS}
    void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField);
    void onExecutionReport(String regSessionId, ExecutionReportState executionReportState, RegulatedFillInputBean regulatedFillInputBean);
    void onOrdeReceived(String regSessionId, String orderId);
    void onOrderReject(String regSessionId, String reason);
    void onOrderTechnicalReject(String regSessionId, String reason);
    void onInstrumentPrices(String rtfiSessionId, List<RegulatedProposalInputLazyBean> rtfibsProposal);
    void onNullPrices(String regulatedSessionId, String reason, ProposalSide side) throws OperationNotSupportedException;
    void onSecurityStatus(String isin, String subMarket, String statusCode);
    void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier);
    void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier, String bondType);
    void onOrderCancelled(String regSessionId, String reason);
    void onCancelRequestReject(String regSessionId, String reason);
    void onFasCancelFillAndBook(String regSessionId, String reason);
    void onFasCancelFillNoBook(String regSessionId, String reason);
    void onFasCancelNoFill(String regSessionId, String reason);
    void onConnectionError();
	void onFasCancelNoFill(String regSessionId, String string,
			ExecutionReportState cancelled,
			RegulatedFillInputBean regulatedFillInputBean);
	void onMarketPriceReceived(String regSessionId, BigDecimal marketPrice, RegulatedFillInputBean regFillInputBean);
}
