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

package it.softsolutions.bestx.connections.amosgateway;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.fixgateway.FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Portfolio;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.services.MICCodeService;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 19-ott-2012 
 * 
 * AMOS <--> OMS2
 * 
 **/
public class OMS2FixExecutionReportOutputLazyBean extends FixExecutionReportOutputLazyBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OMS2FixExecutionReportOutputLazyBean.class);
    private static final String AMOS_DEFAULT_CUSTOMER_CODE = "0800";

    protected String idBestx;
    protected final SimpleDateFormat amosDateTimeFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");

    /**
     * Instantiates a new amos fix execution report output lazy bean.
     *
     * @param sessionId the session id
     * @param order the order
     * @param orderId the order id
     * @param executionReport the execution report
     * @param errorCode the error code
     * @param rejectReason the reject reason
     * @param micCodeService the mic code service
     */
    public OMS2FixExecutionReportOutputLazyBean(String sessionId, Order order, String clOrdID, String orderId, ExecutionReport executionReport, int errorCode, String rejectReason, MICCodeService micCodeService) {
        super(sessionId, order, AmosUtility.getInstance().unwrapString(orderId), executionReport, errorCode, rejectReason, micCodeService);
        
        // [DR20121029] Workaround: set custom Akros field BTDSCommissionIndicator as NULL
        //super.btdsCommissionIndicator = null;
        
        //Used for not executed order
        if (rejectReasonDescription == null || rejectReasonDescription.isEmpty())	// overwrite only if not already set
        {
            rejectReasonDescription = "Cancelled";
        }
        futSettDate = order.getFutSettDate();

        execType = (executionReport != null && executionReport.getState() != null) ? executionReport.getState().getValue() : "0";
        //      if (ExecutionReportState.CANCELLED.equals(executionReport.getState()))
        //        execType = "4";
        //      else
        //         execType = "8";
        state = ExecutionReportState.FILLED;
        currency = order.getCurrency();        
        symbol = order.getInstrument().getIsin();
        
		if (executionReport != null && executionReport.getActualQty() != null && executionReport.getActualQty().compareTo(BigDecimal.ZERO) != 0) {
			actualQty = executionReport.getActualQty();
		} else {
			actualQty = order.getQty();
		}
		
        cumQty = order.getQty();
        settlementType = null;
        buildMessage();
        /* 20091110 AMC Claudia Sormani ha chiesto di inviare le commissioni anche su Amos 
         * 20091126 AMC CANCELLATA: le due righe successive sono state ripristinate */
        if(order.getCustomer().getFixId().equalsIgnoreCase(AMOS_DEFAULT_CUSTOMER_CODE)){
            msg.deleteValue(FixMessageFields.FIX_Commission);
            msg.deleteValue(FixMessageFields.FIX_CommType);
        }
        amosDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (transactTime != null)
        {
            msg.setValue(FixMessageFields.FIX_TransactTime, amosDateTimeFormatter.format(transactTime));
        }

        // [DR20121018] BXSUP-1607 - Gestione anomala degli ordini di Banca Leonardo gestiti dal Magnete: ordini parzialmente eseguiti e quindi revocati dal cliente.
        // -------------------------------------------------
        // - clOrdID = 122912F000055
        // - origClOrdID = 122912D000276
        // - orderID = 122912D000276     (marketOrderID)
        // -------------------------------------------------
        if (clOrdID != null) {
            // clOrdID = 122912F000055
            msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(clOrdID));

            // origClOrdID = 122912D000276
            msg.setValue(FixMessageFields.FIX_OrigClOrdID, AmosUtility.getInstance().unwrapString(orderId));

            // orderID = 122912D000276 > (marketOrderID oppure orderID)
            String marketOrderID = executionReport.getMarketOrderID() != null ? executionReport.getMarketOrderID() : AmosUtility.getInstance().unwrapString(orderId);
            msg.setValue(FixMessageFields.FIX_OrderID, marketOrderID);
        }
    }

    /**
     * Called to build the calculated execution report, the one that will be sent after the partial fills.
     * Also called by the partial fills bean creator {@link AmosFixPartFillExecutionReportOutputLazyBean}
     *
     * @param sessionId the session id
     * @param quote the quote
     * @param order the order
     * @param orderId the order id
     * @param attempt the attempt
     * @param executionReport the execution report
     * @param micCodeService the mic code service
     * @param marketMaker the market maker
     */
    public OMS2FixExecutionReportOutputLazyBean(String sessionId, Quote quote, Order order,
                    String orderId, Attempt attempt, ExecutionReport executionReport, MICCodeService micCodeService,
                    MarketMaker marketMaker) {
        super(sessionId, quote, order, orderId, attempt, executionReport, micCodeService);

        // [DR20120614] Workaround: remove Akros field BTDSCommissionIndicator
        msg.deleteValue(FixMessageFields.FIX_BTDSCommissionIndicator);
        
        // 20100712 AMC Chiesto da Sormani di non mandare i Tick ad Amos, ma il valore configurato (in Tick) diviso per 100
        if(commission != null && CommissionType.TICKER.getValue().equalsIgnoreCase(commissionType))
        {
            commission = commission.divide(ONE_HUNDRED);
            msg.setValue(FixMessageFields.FIX_Commission,commission.doubleValue());
        }

        if (executionReport.getAveragePrice() != null)
        {
            msg.setValue(FixMessageFields.FIX_AvgPx, executionReport.getAveragePrice().doubleValue());
        }
        if (orderId != null)
        {
            msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(orderId));
        }
        if (executionReport.getMarketOrderID() != null)
        {
            msg.setValue(FixMessageFields.FIX_OrderID, executionReport.getMarketOrderID());
        }
        else
        {
            msg.setValue(FixMessageFields.FIX_OrderID, "0");
        }
        if (executionReport.getActualQty() != null){
            msg.setValue(FixMessageFields.FIX_CumQty, executionReport.getActualQty().doubleValue());
            msg.setValue(FixMessageFields.FIX_LastShares, executionReport.getActualQty().doubleValue());
        }
        if (executionReport.getLastPx() != null)
        {
            msg.setValue(FixMessageFields.FIX_LastPx, executionReport.getLastPx().doubleValue());
        }
        if (order.getInstrument().getDescription() != null)
        {
            msg.setValue(FixMessageFields.FIX_Symbol, order.getInstrument().getDescription());
        }
        if (executionReport.getText() != null) {
            msg.setValue(FixMessageFields.FIX_Text, executionReport.getText());
        } else {
            msg.setValue(FixMessageFields.FIX_Text, "Eseguito");
        }
        msg.setValue(FixMessageFields.FIX_TimeInForce, "0");

        amosDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (transactTime != null)
        {
            msg.setValue(FixMessageFields.FIX_TransactTime, amosDateTimeFormatter.format(transactTime));
        }
        msg.setValue(FixMessageFields.FIX_ExecType, "0");

        Portfolio portfolio = null;
        if (order.getInstrument().getInstrumentAttributes() != null &&        
                        order.getInstrument().getInstrumentAttributes().getPortfolio() != null)
        {
            portfolio = order.getInstrument().getInstrumentAttributes().getPortfolio(); 
        }

        if (executionReport.getExecBroker() != null)
        {
            msg.setValue(FixMessageFields.FIX_ExecBroker, executionReport.getExecBroker());
        }

        //20110309 Ruggero : ticket 6473, no rateo ad Amos se data settlement ordine != data settlement del titolo
        boolean settlDateEquals = false;
        Date ordSettDate = order.getFutSettDate();
        Date instrBBGSettDate = order.getInstrument().getBBSettlementDate();
        if (ordSettDate != null &&
                        instrBBGSettDate != null &&
                        DateUtils.isSameDay(ordSettDate, instrBBGSettDate))
        {
            LOGGER.debug("Order settlement date {} equals the instrument BBG settlement date {}, send the accrued amount if available.", ordSettDate, instrBBGSettDate);
            settlDateEquals = true;
        }
        else
        {
            LOGGER.debug("Order settlement date {} differs from the instrument BBG settlement date {} Do not send the accrued amount.", (ordSettDate != null ? ordSettDate : "<null date>"), (instrBBGSettDate != null ? instrBBGSettDate : "<null date>") );
        }
        if(accruedInterest != null && settlDateEquals)
        {
            msg.setValue(FixMessageFields.FIX_AccruedAmt, accruedInterest.doubleValue());
        }
        else
        {
            msg.deleteValue(FixMessageFields.FIX_AccruedAmt);
        }
        // [DR20121029] FIX_IDBestx viene rimappato dall'OMS2 con il tag 6000, non supportato da CS ma specifico per Akros
//        if (executionReport.getId()!= null)
//        {
//            msg.setValue(FixMessageFields.FIX_IDBestx, executionReport.getId().toString());
//        }
        // [DR20121029] FIX_TipoConto viene rimappato dall'OMS2 con il tag 6002, non supportato da CS ma specifico per Akros
//        if (executionReport.getTipoConto() != null)
//        {
//            msg.setValue(FixMessageFields.FIX_TipoConto, executionReport.getTipoConto());
//        }
        if (executionReport.getId() != null)
        {
            msg.setValue(FixMessageFields.FIX_ExecID, executionReport.getId().toString());
        }
        else
        {
            msg.setValue(FixMessageFields.FIX_ExecID, "Not available");
        }

        /* Ruggero - 15/07/2008
         * AMOS non vuole le commissioni, le calcolano loro, non devono proprio esserci
         */
        /* 20091110 AMC Claudia Sormani ha chiesto di inviare le commissioni anche su Amos
         * 20091126 AMC CANCELLATA: le due righe successive sono state ripristinate */
        if(order.getCustomer().getFixId().equalsIgnoreCase(AMOS_DEFAULT_CUSTOMER_CODE)){
            msg.deleteValue(FixMessageFields.FIX_Commission);
            msg.deleteValue(FixMessageFields.FIX_CommType);
        }
    }
}