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
package it.softsolutions.bestx.connections.regulated;

import it.softsolutions.bestx.services.DateService;

/** 

 *
 * Purpose: This class defines all field names for regulated markets messages
 *
 * Project Name : bestxengine-common
 * First created by: stefano.pontillo
 * Creation date: 06/giu/2012
 *
 **/
public class RegulatedMessageFields {
    
	public static final String EXECUTION_TYPE = "ExecutionType";
	public static final String LABEL_ISIN = "Isin";
	public static final String LABEL_MARKET = "Market";
	public static final String LABEL_PRICE = "Price";
	public static final String LABEL_SIDE = "Side";
	public static final String ORDER_MODE = "OrderMode";
	public static final String SUBJECT_ORDER_REQUEST = "XT2COrderReq";
	
    public static final String SUBJECT_REG_PRICEREQ_PDU = "XT2CQueryPricesReq";
    public static final String SUBJECT_MARKET_STATUS = "MARKET_STATUS";
    public static final String SUBJECT_USER_STATUS = "USER_STATUS";
    public static final String SUBJECT_REG_ORDER = "XT2CNewOrderSingleReq";
    public static final String SUBJECT_REG_ORDER_RESP = "XT2COrderResp";
    public static final String SUBJECT_PRICE_RESPONSE = "XT2CQueryPricesNotify";
    public static final String NAME_PRICE_RESPONSE = "XT2CQueryPricesResp";
    public static final String SUBJECT_EXECUTION_REPORT = "EXECUTIONREPORT";
    public static final String SUBJECT_SECURITY_DEFINITION = "SECURITY_DEFINITION";
    public static final String SUBJECT_BUSINESS_REJECT = "BusinessMessageReject";
    public static final String SUBJECT_ORDER_REJECT = "OrderReject";
    public static final String SUBJECT_TRADING_SESSION_STATUS = "XT2CTradingSessionStatus";
    public static final String SUBJECT_ORDER_CANCEL_REQUEST = "XT2COrderCancelReq";
    public static final String SUBJECT_ORDER_CANCEL_RESPONSE = "XT2COrderCancelResp";
    public static final String SUBJECT_ORDER_CANCEL_REJECT = "XT2COrderCancelReject";

    public static final String LABEL_REG_SESSION_ID = "CD";
    public static final String LABEL_EXT_SESSION_ID = "EXTCD";
    public static final String LABEL_REG_ISIN = "Isin";
    public static final String LABEL_REG_SIDE = "Side";
    public static final String LABEL_REG_MARKET = "Market";
    public static final String LABEL_REG_USERNAME = "UserName";
    public static final String LABEL_REG_SUBMARKET = "SubMarketName";
    public static final String LABEL_REG_SUBSCRIPTION = "EnableSubscription";
    public static final String LABEL_MARKET_STATUS = "MktStatus";
    public static final String LABEL_USER_STATUS = "Status";
    public static final String LABEL_ERROR_CODE = "ErrCode";
    public static final String LABEL_ERROR_MESSAGE = "ErrMsg";
    public static final String LABEL_ORDER_ID = "OrderID";
    public static final String LABEL_ORDER_NUM = "OrderNum";
    public static final String LABEL_ORDER_ACCOUNT = "Account";
    public static final String LABEL_CLIENT_ID = "ClientID";
    public static final String LABEL_TRADING_SESSION_ID = "NoTradingSessions.TradingSessionID";
    public static final String LABEL_TIMEINFORCE = "TimeInForce";
    public static final String LABEL_SETTLEMENT_DATE = "SettlmntDate";
    public static final String LABEL_TRANSACT_TIME = "TransactTime";
    public static final String LABEL_ORDER_QTY = "OrderQty";
    public static final String LABEL_REG_PRICE = "Price";
    public static final String LABEL_REG_CURRENCY = "Currency";
    public static final String LABEL_REG_TEXT = "Text";
    public static final String LABEL_MAX_FLOOR = "MaxFloor";
    public static final String LABEL_PRICE_ITEM_COUNT = "NoMDEntries.ItemCount";
    public static final String LABEL_QUANTITY = "Qty";
    public static final String LABEL_TIME = "Time";
    public static final String LABEL_DATE = "Date";
    public static final String LABEL_FILL = "FILL";
    public static final String LABEL_ORDER = "ORDER";
    public static final String LABEL_ORDER_STATUS = "OrdStatus";
    public static final String LABEL_SECURITY_STATUS = "XT2CSecurityStatus";
    public static final String LABEL_TRADING_SESSION_STATUS = "TradingSessionID";
    public static final String LABEL_SEGMENT_STATUS = "MDMarketStatusID";
    public static final String LABEL_SESSION_STATUS = "SessionStatus";
    public static final String LABEL_EXPIRE_DATE = "ExpireDate";
    public static final String LABEL_ORIGINAL_CLIENT_ORDERID = "OrigClOrdID";
    public static final String LABEL_SYMBOL = "Symbol";
    public static final String LABEL_LAST_QUANTITY = "LastQty";
    public static final String LABEL_LAST_PRICE = "LastPx";
    public static final String LABEL_CONTRACT_NO = "ContractNo";
    public static final String LABEL_ORDER_TRADER = "OrderTrader";
    public static final String LABEL_TIMESTAMP = "TimeStamp";
    public static final String LABEL_SECURITY_ID_SOURCE = "SecurityIDSource";
    public static final String LABEL_COUNTERPART = "ExecBroker";
    public static final String LABEL_COUNTERPARTMEMBER = "CounterpartMember";
    public static final String LABEL_MIN_QTY = "MinorOrderQty";
    public static final String LABEL_MIN_INCREMENT = "QtyTick"; 
    public static final String LABEL_QTY_MULTIPLIER = "ContractMultiplier";
    public static final String LABEL_REJECT_REASON = "RejectReason";
    public static final String PREFIX_BID = "Bid";
    public static final String PREFIX_ASK = "Ask";
    public static final String LABEL_ORDER_SOURCE = "OrderSource";


    public static final String LABEL_EX_DESTINATION = "ExDestination";
    public static final String MTF_DESTINATION_VALUE = "MTF";


    public static final String LABEL_MKT_STATUS = "MktStatus";
    public static final String LABEL_USR_STATUS = "Status";
    public static final int VALUE_MKT_STATUS_ON = 3;
    public static final int VALUE_USR_STATUS_ON = 1;

    public static final String VALUE_CLIENT_ID = "Akros";
    public static final String VALUE_TRADING_SESSION_ID = "NEG";
    public static final String VALUE_DEFAULT_ORDER_TEXT = "SABE order";
    public static final int VALUE_MARKET_STATUS_ON = 2;
    public static final int VALUE_TIMEINFORCE_FOK = 4;
    public static final int VALUE_TIMEINFORCE_FAS = 0;
    public static final int VALUE_MAX_FLOOR = 0;
    public static final int VALUE_SIDE_BID = 0;
    public static final int VALUE_SIDE_ASK = 1;
    public static final int VALUE_ORDER_STATUS_NEW = 0;
    public static final int VALUE_ORDER_STATUS_REJECTED = 8;
    public static final int VALUE_ORDER_STATUS_PARTIALFILL_EXECUTED = 1;
    public static final int VALUE_ORDER_STATUS_EXECUTED = 2;
    public static final int VALUE_ORDER_STATUS_CANCELLED = 4;
    public static final String VALUE_TRADING_STATUS_QUOTED = "NEG";
    public static final String VALUE_TRADING_STATUS_NOT_QUOTED = "CAN";
    public static final String VALUE_TRADING_STATUS_VOLATILITY_AUCTION = "PVOL";
    public static final String VALUE_SYMBOL = "N/A";

    //		MTS PRIME ORDER STATUS 
    //    0  CMF_ORDER_STATUS_Processing 
    //    1  CMF_ORDER_STATUS_Accepted
    //    2  CMF_ORDER_STATUS_Refused
    //    3  CMF_ORDER_STATUS_CompFilled
    //    4  CMF_ORDER_STATUS_PartFilled
    //    5  CMF_ORDER_STATUS_ZeroFilled
    //    6  CMF_ORDER_STATUS_ZeroFilledAutoApplication
    //    7  CMF_ORDER_STATUS_ZeroFilledSuspension
    //    8  CMF_ORDER_STATUS_Timeout
    public static final int CRD_ORDER_STATUS_NEW = 2;
    public static final int CRD_ORDER_STATUS_REFUSED = 7;
    public static final int CRD_ORDER_STATUS_COMPLETEFILL = 6;
    public static final int CRD_ORDER_STATUS_PARTIALFILL = 5;
    public static final int CRD_ORDER_STATUS_ZEROFILL = 4;

    public static final int CRD_DEFINED_FILL = 2;

    public static final String CRD_MESSAGE = "Msg";
    public static final String CRD_FILLED_QTY = "Qty";
    public static final String CRD_TIME = "Time";
    public static final String CRD_DATE = "Date";
    public static final String CRD_ORDER_NUM = "OrderNum";
    public static final String CRD_TRADE_COMMENT = "TradeComment";
    public static final int CRD_EVENT_TYPE_ORDER_FAILURE = 1;


    public static final String LABEL_ORDER_TYPE = "OrdType";
    public static final int VALUE_ORDER_TYPE_SUBJECT_LIMIT = 2;
    public static final int VALUE_ORDER_TYPE_SUBJECT_MARKET = 1;

    public static final String FORMAT_SETTLEMENT_DATE = DateService.dateISO;
}
