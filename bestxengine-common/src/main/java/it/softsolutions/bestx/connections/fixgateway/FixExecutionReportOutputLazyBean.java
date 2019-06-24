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
package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.MICCodeService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * 
 * Purpose: this class is a specialized bean used to send messages to customer fix adapter
 * 
 * Project Name : bestxengine-common First created by: stefano.pontillo Creation date: 15/mag/2012
 * 
 **/
public class FixExecutionReportOutputLazyBean extends FixOutputLazyBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixExecutionReportOutputLazyBean.class);

    protected static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    protected static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);
    private static final int PRICE_DECIMAL_SCALE = 20;
    protected String orderId;
    private String customerOrderId;
    protected BigDecimal orderQty;
    protected BigDecimal actualQty;
    protected BigDecimal leavesQty;
    protected BigDecimal cumQty; // FIX 4.1 - It should not be mandatory in FIX 4.2
    protected String executionReportId;
    protected ExecutionReport.ExecutionReportState state;
    private String rejectReason;
    protected String rejectReasonDescription;
    protected Integer settlementType;
    protected String strSettlementType;
    protected Date futSettDate;
    private String securityId;
    protected String symbol;
    private String idSource;
    private String securityExchange;
    protected OrderSide side;
    protected String currency;
    protected BigDecimal lastPx;
    protected BigDecimal avgPx; // FIX 4.1 - It should not be mandatory in FIX 4.2
    protected BigDecimal price;
    protected BigDecimal commission;
    protected String commissionType;
    protected Integer accruedDays;
    protected BigDecimal accruedInterest;
    protected String lastMkt;
    protected String execTransType;
    protected String execType;
    private String timeInForce;
    private String orderType;
    protected String tipoConto;
    protected String execBroker;
    protected Date transactTime;
    private int errorCode;
    private String executionDestination;
    protected String strFutSettDate; // FIXME togliere appena fix gateway accettera' la data come un long
    protected String internalizationIndicator = null;
    private Integer bestExecutionVenueFlag;
    private Integer priceType;
    
    // BESTX-385: SP send the Factor (228) field
    private BigDecimal factor;
    

    protected XT2Msg msg;
    protected Integer btdsCommissionIndicator;
//    protected Integer btdsCommissionIndicator;
    // 20110801 - Ruggero ticket 7610 : currently used only for a TLX execution to store the market pdu. TAS wants this value in the tag 37.
    protected String marketOrderId = null;
    private String ticketOwner;
    
    //BESTX-348: SP-20180905 
    /**
     * Constructor of the execution report bean for new orders
     * 
     * @param sessionId
     *            FIX Session ID
     * @param order
     *            Bean of the customer order
     * @param orderId
     *            ID of the customer order
     */
    public FixExecutionReportOutputLazyBean(String sessionId, Order order, String orderId) {
        super(sessionId);
        this.orderId = orderId;
        price = order.getLimit() != null ? order.getLimit().getAmount() : BigDecimal.ZERO;
        customerOrderId = order.getCustomerOrderId();
        
        
        if (order.getInstrument() != null) {
            symbol = order.getInstrument().getEpic() != null ? order.getInstrument().getEpic() : order.getInstrument().getIsin();
            securityId = order.getInstrument().getIsin();
        }
        else {
            symbol = order.getInstrumentCode(); 
            securityId = symbol;
        }

        idSource = "4";
        securityExchange = order.getSecExchange();
        executionDestination = order.getExecutionDestination();
        orderQty = order.getQty();
        leavesQty = BigDecimal.ZERO;
        side = order.getSide();
        transactTime = DateService.newLocalDate();  //FIXIT get the MarketExecutionReport one if there is one
        orderType = order.getType().getFixCode();
        futSettDate = order.getFutSettDate();
        strFutSettDate = futSettDate != null ? futSettDate.toString() : null;
        settlementType = 6;
        execTransType = "0";
        timeInForce = order.getTimeInForce() != null ? Integer.toString(order.getTimeInForce().getFixCode()) : null;
        actualQty = BigDecimal.ZERO;
        currency = order.getCurrency();
        bestExecutionVenueFlag = order.getBestExecutionVenueFlag();
        priceType = order.getPriceType();
        ticketOwner = order.getTicketOwner();

        buildMessage();
    }

    /**
     * Constructor of the execution report bean for rejected order
     * 
     * @param sessionId
     *            FIX Session ID
     * @param order
     *            Bean of the customer order
     * @param orderId
     *            ID of the customer order
     * @param executionReport
     *            Bean of the execution report
     * @param errorCode
     *            id of the error code
     * @param rejectReason
     *            Description of the rejection
     * @param micCodeService
     *            Service used to generate the MIC code
     */
    public FixExecutionReportOutputLazyBean(String sessionId, Order order, String orderId, ExecutionReport executionReport, int errorCode, String rejectReason, MICCodeService micCodeService) {
        this(sessionId, order, orderId);
        this.errorCode = errorCode;
        if(executionReport == null) {
        	LOGGER.error("executionReport variable is null");
        	return;
        }
    	if (executionReport.getExecutionReportId()!=null) {
    		executionReportId = executionReport.getExecutionReportId();
    	} else {
    		executionReportId = executionReport.getSequenceId();
    	}
        actualQty = executionReport.getActualQty();
        cumQty = actualQty;
        state = executionReport.getState();
        execType = state == ExecutionReport.ExecutionReportState.FILLED ? ExecutionReport.ExecutionReportState.FILLED.getValue() : ExecutionReport.ExecutionReportState.REJECTED.getValue();
        this.rejectReason = rejectReason;
        rejectReasonDescription = rejectReason;
        if(executionReport.getLastMkt() == null || executionReport.getLastMkt().length() <= 0) 
        	lastMkt = micCodeService.getMICCode(executionReport.getMarket(), order.getInstrument());
        else lastMkt = executionReport.getLastMkt();
        price = executionReport.getPrice() != null ? executionReport.getPrice().getAmount() : BigDecimal.ZERO;
        lastPx = price;
        avgPx = price;
        commission = BigDecimal.ZERO;
        commissionType = CommissionType.AMOUNT.getValue();
        ticketOwner = order.getTicketOwner();
        if (executionReport.getFactor() != null && executionReport.getFactor().compareTo(BigDecimal.ZERO) != 0) {
           factor = executionReport.getFactor();
        }
        
        /*
         * Save the transactTime date in order to write it later in the audit DB for the Akros' BackOffice If it is a "resend" of the exec.
         * rep. we keep the previously saved date and use it as transactTime (in case of double execution there will be two identical
         * reports, it will be a clear signal of double execution)
         */
        if (executionReport != null) {
            if (executionReport.getTransactTime() != null) {
                transactTime = executionReport.getTransactTime();
                LOGGER.debug("transactTime set to {} from execReport value", transactTime);
            } else if (executionReport.getSendingTime() != null) {
                transactTime = executionReport.getSendingTime();
                LOGGER.debug("transactTime set to {} from exec Report sendingTime since execReport transactTime is not set", transactTime);
            } else {
                executionReport.setSendingTime(transactTime);
                LOGGER.debug("execReport sendingTime set to {} since execReport sendingTime is not set", transactTime);
            }
        }
        /*
         * substituted by the rows above if (executionReport != null) if (executionReport.getSendingTime() != null) transactTime =
         * executionReport.getSendingTime(); else executionReport.setSendingTime(transactTime);
         */
        LOGGER.info("transactTime is {}", transactTime);
        buildMessage();
    }

    /**
     * Constructor of the execution report bean for executed orders
     * 
     * @param sessionId
     *            FIX Session ID
     * @param quote
     *            Bean quote with market data
     * @param order
     *            Bean of the customer order
     * @param orderId
     *            ID of the customer order
     * @param attempt
     *            Attemp data
     * @param executionReport
     *            Bean of the execution report
     * @param micCodeService
     *            Service used to generate the MIC code
     */
    public FixExecutionReportOutputLazyBean(String sessionId, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, MICCodeService micCodeService) {
        this(sessionId, order, orderId);
        if(executionReport == null) {
        	LOGGER.error("executionReport variable is null");
        	return;
        }
        rejectReasonDescription = executionReport.getText();
        if (executionReport.getExecutionReportId()!=null) {
        	executionReportId = executionReport.getExecutionReportId();
        } else {
        	executionReportId = executionReport.getSequenceId();
        }
        actualQty = executionReport.getActualQty();
        cumQty = actualQty;
        state = executionReport.getState();
        currency = executionReport.getPrice().getStringCurrency();
        lastPx = executionReport.getLastPx();  //BESTX-426 AMC 20190612
        price = order.getLimit() == null ? null : order.getLimit().getAmount();  //BESTX-426 AMC 20190612
        avgPx = lastPx;
        if (executionReport.getFactor() != null && executionReport.getFactor().compareTo(BigDecimal.ZERO) != 0) {
           factor = executionReport.getFactor();
        }

        commission = (executionReport.getCommission() != null) ? executionReport.getCommission() : BigDecimal.ZERO;
        commissionType = (executionReport.getCommissionType() != null) ? executionReport.getCommissionType().getValue() : CommissionType.AMOUNT.getValue();

        // [DR20120614] BTDSCommissionIndicator NON DEVE essere valorizzato qui, siamo in bestx-common e BTDSCommissionIndicator e'
        // specifico di Akros
//        btdsCommissionIndicator = 1;
//
//        if (!order.isAddCommissionToCustomerPrice()) {
//            btdsCommissionIndicator = 0;
//        } else {
//            if (executionReport.getCommissionType() == null) {
//                commission = null;
//                commissionType = null;
//                btdsCommissionIndicator = 0;
//            }
//        }

        if (quote != null && quote.getAccruedInterest() != null) {
            accruedInterest = quote.getAccruedInterest().getAmount();
            accruedDays = quote.getAccruedDays();
        } else if (executionReport != null && executionReport.getAccruedInterestAmount() != null) {
            accruedInterest = executionReport.getAccruedInterestAmount().getAmount();
        } else {
            accruedInterest = null;
            accruedDays = 0;
        }
        
        if (executionReport != null && executionReport.getLastMkt() != null) {
           lastMkt = executionReport.getLastMkt();
        } else if (quote != null && quote.getAskProposal().getMarket() != null) {
           lastMkt = micCodeService.getMICCode(quote.getAskProposal().getMarket(), order.getInstrument());
        } else {
           lastMkt = null;
        }
        
        execType = state.getValue();
        priceType = order.getPriceType();

        /*
         * 2009-09-22 Ruggero Wait to check the behaviour of all the markets. Do they send us the transact time? (Problem pointed out by
         * Claudia Sormani on HI-MTF) if (executionReport.getTransactTime() != null) transactTime = executionReport.getTransactTime();
         */

        /*
         * Save the transactTime date in order to write it later in the audit DB for the Akros' BackOffice If it is a "resend" of the exec.
         * rep. we keep the previously saved date and use it as transactTime (in case of double execution there will be two identical
         * reports, it will be a clear signal of double execution)
         */
        // AMC 20100906 corrected for ticket #5645
        // RR 20101220 logging added
        if (executionReport != null) {
            LOGGER.debug("Getting the transactTime from the execution report");
            if (executionReport.getTransactTime() != null) {
                transactTime = executionReport.getTransactTime();
                LOGGER.debug("TransactTime found in the exec report TransactTime : {}", transactTime);
            } else {
                if (executionReport.getSendingTime() != null) {
                    transactTime = executionReport.getSendingTime();
                    LOGGER.debug("TransactTime found in the exec report SendingTime : {}" + transactTime);
                } else {
                    executionReport.setSendingTime(transactTime);
                    LOGGER.debug("TransactTime not found in the exec report, using a custome one : {}" + transactTime);
                }
            }
        }
        
        ticketOwner = order.getTicketOwner();
        buildMessage();
    }

    /**
     * Constructor of the execution report bean for executed orders without market data
     * 
     * @param sessionId
     *            FIX Session ID
     * @param order
     *            Bean of the customer order
     * @param orderId
     *            ID of the customer order
     * @param attempt
     *            Attemp data
     * @param executionReport
     *            Bean of the execution report
     * @param micCodeService
     *            Service used to generate the MIC code
     */
    public FixExecutionReportOutputLazyBean(String sessionId, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, MICCodeService micCodeService) {
        this(sessionId, null, order, orderId, attempt, executionReport, micCodeService);

        // [DR20120614] Workaround: set custom Akros field BTDSCommissionIndicator as NULL
//        btdsCommissionIndicator = null;
        rejectReasonDescription = executionReport.getText();
        futSettDate = order.getFutSettDate();
        accruedInterest = executionReport.getAccruedInterestAmount() != null ? executionReport.getAccruedInterestAmount().getAmount() : null;
        tipoConto = executionReport.getTipoConto();
        execBroker = executionReport.getExecBroker();
        if (executionReport.getExecutionReportId()!=null) {
        	executionReportId = executionReport.getExecutionReportId();
        } else {
        	executionReportId = order.getFixOrderId();
        }
        execType = state.getValue(); // execType = state == ExecutionReport.ExecutionReportState.FILLED ? "2" : "8";
        currency = order.getCurrency();
        ticketOwner = order.getTicketOwner();
        if (executionReport.getFactor() != null && executionReport.getFactor().compareTo(BigDecimal.ZERO) != 0) {
           factor = executionReport.getFactor();
        }
        
        buildMessage();
    }

    protected void buildMessage() {
        msg = super.buildMsg();
        msg.setName(FixMessageTypes.TRADE.toString());
        msg.setValue(FixMessageFields.FIX_ErrorCode, errorCode);
        if (idSource != null) {
            msg.setValue(FixMessageFields.FIX_IDSource, idSource);
        }

        if (orderId != null) {
            /*
             * 20110801 - Ruggero ticket 7610 : the variable marketOrderId is different from null only if the execution market is TLX, TAS
             * need the market pdu. Check class AkrosFixExecutionReportOutputLazyBean
             */
            if (marketOrderId != null) {
                msg.setValue(FixMessageFields.FIX_OrderID, marketOrderId);
            } else {
                msg.setValue(FixMessageFields.FIX_OrderID, orderId);
            }

            msg.setValue(FixMessageFields.FIX_ClOrdID, orderId);
        }
        if (executionReportId != null) {
            msg.setValue(FixMessageFields.FIX_ExecID, executionReportId);
        }
        if (execTransType != null) {
            msg.setValue(FixMessageFields.FIX_ExecTransType, execTransType);
        }
        if (execType != null) {
            msg.setValue(FixMessageFields.FIX_ExecType, execType);
        }
        if (execType != null) {
            msg.setValue(FixMessageFields.FIX_OrdStatus, execType);
        }
        if (rejectReason != null) {
            msg.setValue(FixMessageFields.FIX_OrdRejReason, rejectReason);
        }
        if (rejectReasonDescription != null) {
            msg.setValue(FixMessageFields.FIX_Text, rejectReasonDescription);
        }
        if (settlementType != null) {
            msg.setValue(FixMessageFields.FIX_SettlmntTyp, settlementType);
        } else if (strSettlementType != null) {
           msg.setValue(FixMessageFields.FIX_SettlmntTyp, strSettlementType);
        }
        if (futSettDate != null) {
            try {
                msg.setValue(FixMessageFields.FIX_FutSettDate, DateService.formatAsLong(DateService.dateISO, futSettDate));
            } catch (Exception e) {
                LOGGER.error("Error while converting Future Settlement Date: {}", DateService.format(DateService.dateISO, futSettDate), e);
            }
        } else if (strFutSettDate != null) {
            try {
                msg.setValue(FixMessageFields.FIX_FutSettDate, strFutSettDate);
            } catch (Exception e) {
                LOGGER.error("Error while converting Future Settlement Date: {}", strFutSettDate, e);
            }
        }
        if (symbol != null) {
            msg.setValue(FixMessageFields.FIX_Symbol, symbol);
        }
        if (securityId != null) {
            msg.setValue(FixMessageFields.FIX_SecurityID, securityId);
        }
        if (securityExchange != null) {
            msg.setValue(FixMessageFields.FIX_SecurityExchange, securityExchange);
        }
        if (executionDestination != null) {
            msg.setValue(FixMessageFields.FIX_ExDestination, executionDestination);
        }
        if (side != null) {
            msg.setValue(FixMessageFields.FIX_Side, side.getFixCode());
        }
        if (orderQty != null) {
            msg.setValue(FixMessageFields.FIX_OrderQty, orderQty.doubleValue());
        }
        if (orderType != null) {
            msg.setValue(FixMessageFields.FIX_OrdType, orderType);
        }
        if (currency != null) {
            msg.setValue(FixMessageFields.FIX_Currency, currency);
        }
        if (lastPx != null) {
            msg.setValue(FixMessageFields.FIX_LastPx, lastPx.doubleValue());
        }
        if (price != null) {
            msg.setValue(FixMessageFields.FIX_Price, price.doubleValue());
        }
        if (avgPx != null) {
            msg.setValue(FixMessageFields.FIX_AvgPx, avgPx.doubleValue());
        }
        if (commission != null) {
            msg.setValue(FixMessageFields.FIX_Commission, commission.doubleValue());
        }
        if (commissionType != null) {
            msg.setValue(FixMessageFields.FIX_CommType, commissionType);
        }
        if (timeInForce != null) {
            msg.setValue(FixMessageFields.FIX_TimeInForce, timeInForce);
        }
        if (actualQty != null) {
            msg.setValue(FixMessageFields.FIX_LastShares, actualQty.doubleValue());
        }
        if (leavesQty != null) {
            msg.setValue(FixMessageFields.FIX_LeavesQty, leavesQty.doubleValue());
        }
        if (cumQty != null) {
            msg.setValue(FixMessageFields.FIX_CumQty, cumQty.doubleValue());
        }
        if (lastMkt != null) {
            msg.setValue(FixMessageFields.FIX_QuoteMarket, lastMkt);
        }
        if (transactTime != null) {
            msg.setValue(FixMessageFields.FIX_TransactTime, DateService.format(DateService.dateTimeISO, transactTime));
        }
        if (accruedDays != null) {
            msg.setValue(FixMessageFields.FIX_QuoteAccruedDays, accruedDays);
        }
        if (accruedInterest != null) {
            msg.setValue(FixMessageFields.FIX_QuoteAccruedInterestAmount, accruedInterest.setScale(PRICE_DECIMAL_SCALE).doubleValue());
        }
        if (tipoConto != null) {
            msg.setValue(FixMessageFields.FIX_TipoConto, tipoConto);
        }
        if (execBroker != null) {
            msg.setValue(FixMessageFields.FIX_ExecBroker, execBroker);
        }
        if (internalizationIndicator != null) {
            msg.setValue(FixMessageFields.FIX_InternalizationIndicator, internalizationIndicator);
        }
//        if (btdsCommissionIndicator != null) {
//            msg.setValue(FixMessageFields.FIX_BTDSCommissionIndicator, btdsCommissionIndicator);
//        }
        if (priceType != null) {
            msg.setValue(FixMessageFields.FIX_PriceType, priceType);
        }
        if (ticketOwner != null) {
            msg.setValue(FixMessageFields.FIX_TicketOwner, ticketOwner);
        }
        //BESTX-348: SP-20180905 
        if (accruedDays != null) {
           msg.setValue(FixMessageFields.FIX_NumDaysInterest, accruedDays);
        }
        // BESTX-385: SP-20190116
        if (factor != null) {
           msg.setValue(FixMessageFields.FIX_Factor, factor.doubleValue());
       }
    }

    @Override
    public XT2Msg getMsg() {
        return msg;
    }
}
