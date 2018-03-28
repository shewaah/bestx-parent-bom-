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
package it.softsolutions.bestx.connections.mts;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: paolo.midali 
 * Creation date: 13-nov-2012 
 * 
 **/

public class MTSMessageFields
{

    /** The Constant XT2_MSG_SUBJECT_SEPARATOR. */
    public static final String XT2_MSG_SUBJECT_SEPARATOR = "/";

    /** The Constant VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME. */
    public static final String VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME = "BVS";

    /** The Constant VALUE_MTSPRIME_SUBSCRIPTION_MARKET_NAME. */
    public static final String VALUE_MTSPRIME_SUBSCRIPTION_MARKET_NAME = "CRD";

    /** The Constant VALUE_BONDVISION_REAL_MARKET_NAME. */
    public static final String VALUE_BONDVISION_REAL_MARKET_NAME = "BVS";

    /** The Constant VALUE_MTSPRIME_REAL_MARKET_NAME. */
    public static final String VALUE_MTSPRIME_REAL_MARKET_NAME = "CRD";

    /** The Constant BANK_SWITCH_REQ. */
    public static final String BANK_SWITCH_REQ = "XT2CBankSwitchReq";

    /** The Constant BANK_SWITCH_RESP. */
    public static final String BANK_SWITCH_RESP = "XT2CBankSwitchResp";

    /** The Constant RFQ_REQ. */
    public static final String RFQ_REQ = "XT2SendNewRFQRfq";

    /** The Constant RFQ_RESP. */
    public static final String RFQ_RESP = "XT2SendNewRFQResp";

    /** The Constant BANK_SWITCH_NOTIFY. */
    public static final String BANK_SWITCH_NOTIFY = "XT2CBankSwitchNotify";

    /** The Constant CONTRIBUTION_SWITCH_REQ. */
    public static final String CONTRIBUTION_SWITCH_REQ = "XT2CContributionSwitchReq";

    /** The Constant CONTRIBUTION_SWITCH_RESP. */
    public static final String CONTRIBUTION_SWITCH_RESP = "XT2CContributionSwitchResp";

    /** The Constant RFQ_REPLY. */
    public static final String RFQ_REPLY = "XT2RfqReply";

    /** The Constant SUBJECT_MARKET_STATUS. */
    public static final String SUBJECT_MARKET_STATUS = "MARKET_STATUS";

    /** The Constant SUBJECT_USER_STATUS. */
    public static final String SUBJECT_USER_STATUS = "USER_STATUS";

    /** The Constant SUBJECT_BANK_STATUS. */
    public static final String SUBJECT_BANK_STATUS = "BANK_STATUS";

    /** The Constant SUBJECT_INSTRUMENT. */
    public static final String SUBJECT_INSTRUMENT = "INSTRUMENT";

    /** The Constant SUBJECT_RFCQ_BOOK. */
    public static final String SUBJECT_RFCQ_BOOK = "RFCQ_BOOK";

    /** The Constant SUBJECT_RFCQ_STATE. */
    public static final String SUBJECT_RFCQ_STATE = "RFCQ";

    /** The Constant SUBJECT_MEMBER_ENABLED. */
    public static final String SUBJECT_MEMBER_ENABLED = "SELL_SIDE_MEMBERS_ENABLED";

    /** The Constant SUBJECT_TRADING_RELATION. */
    public static final String SUBJECT_TRADING_RELATION = "RFCQ_TRADING_RELATION";

    /** The Constant SUBJECT_TRADING_RELATION_EXCEPTION. */
    public static final String SUBJECT_TRADING_RELATION_EXCEPTION = "RFCQ_TRADING_RELATION_EXCEPTION";

    /** The Constant SUBJECT_ORDER_REQUEST. */
    public static final String SUBJECT_ORDER_REQUEST = "XT2COrderReq";

    /** The Constant SUBJECT_ORDER_ACCEPT_QUOTE. */
    public static final String SUBJECT_ORDER_ACCEPT_QUOTE = "XT2CIncomingOrderAcceptanceReq";

    /** The Constant SUBJECT_PRICE_REQUEST. */
    public static final String SUBJECT_PRICE_REQUEST = "XT2CQueryPricesReq";

    /** The Constant SUBJECT_PRICE_RESPONSE. */
    public static final String SUBJECT_PRICE_RESPONSE = "XT2CQueryPricesNotify";

    /** The Constant SUBJECT_FILL. */
    public static final String SUBJECT_FILL = "FILL"; //<mkt>/FILL/IT0000012345_2/date_uniqueSeqNo

    /** The Constant SUBJECT_ORDER. */
    public static final String SUBJECT_ORDER = "ORDER";

    /** The Constant LABEL_CONTRIB_ENABLED. */
    public static final String LABEL_CONTRIB_ENABLED = "ContribEnabled";

    /** The Constant LABEL_PROPOSAL_ENABLED. */
    public static final String LABEL_PROPOSAL_ENABLED = "ProposalEnabled";

    /** The Constant LABEL_ORDER_ENABLED. */
    public static final String LABEL_ORDER_ENABLED = "OrderEnabled";

    /** The Constant LABEL_FORCE_CONRIBUTION. */
    public static final String LABEL_FORCE_CONRIBUTION = "ForceContribution";

    /** The Constant LABEL_MARKET_PROPOSAL_STATUS. */
    public static final String LABEL_MARKET_PROPOSAL_STATUS = "MarketProposalStatus";

    /** The Constant LABEL_MARKET. */
    public static final String LABEL_MARKET = "Market";

    /** The Constant LABEL_TEXT. */
    public static final String LABEL_TEXT = "Text";

    /** The Constant LABEL_ISIN. */
    public static final String LABEL_ISIN = "Isin";

    /** The Constant LABEL_RFCQ_MIN_QTY. */
    public static final String LABEL_RFCQ_MIN_QTY = "MinRfcqRfqQty"; 

    /** The Constant LABEL_RFCQ_QTY_TICK. */
    public static final String LABEL_RFCQ_QTY_TICK = "RfcqQtyTick"; 

    /** The Constant LABEL_SETT_DATE. */
    public static final String LABEL_SETT_DATE = "SettlDate";

    /** The Constant LABEL_MARKET_AFFILIATION. */
    public static final String LABEL_MARKET_AFFILIATION = "MarketAffiliation";

    /** The Constant LABEL_MARKET_AFFILIATIONSTR. */
    public static final String LABEL_MARKET_AFFILIATIONSTR = "MarketAffiliationStr";

    /** The Constant LABEL_QUOTE_INDICATOR. */
    public static final String LABEL_QUOTE_INDICATOR = "QuoteIndicator";

    /** The Constant LABEL_QUOTE_INDICATORSTR. */
    public static final String LABEL_QUOTE_INDICATORSTR = "QuoteIndicatorStr";

    /** The Constant LABEL_RFCQ_LOT_SIZE. */
    public static final String LABEL_RFCQ_LOT_SIZE = "RfcqLotSize";

    /** The Constant LABEL_BOND_TYPE. */
    public static final String LABEL_BOND_TYPE = "BondType";

    /** The Constant LABEL_MEMBER_CODE. */
    public static final String LABEL_MEMBER_CODE = "MemberCode";

    /** The Constant LABEL_MEMBER_ID. */
    public static final String LABEL_MEMBER_ID = "MemberId";

    /** The Constant LABEL_SELL_SIDE_MEMBER_CODE. */
    public static final String LABEL_SELL_SIDE_MEMBER_CODE = "SellSideMemberCode";

    /** The Constant LABEL_BUY_SIDE_MEMBER_CODE. */
    public static final String LABEL_BUY_SIDE_MEMBER_CODE = "BuySideMemberCode";

    /** The Constant LABEL_EVENT. */
    public static final String LABEL_EVENT = "Event";

    /** The Constant LABEL_STATUS. */
    public static final String LABEL_STATUS = "Status";

    /** The Constant LABEL_SELL_SIDE_SUBSTATUS. */
    public static final String LABEL_SELL_SIDE_SUBSTATUS = "SellSideSubStatus";

    /** The Constant LABEL_BUY_SIDE_SUBSTATUS. */
    public static final String LABEL_BUY_SIDE_SUBSTATUS = "BuySideSubStatus";

    /** The Constant LABEL_INSTRUMENT_NEGOTIABLE. */
    public static final String LABEL_INSTRUMENT_NEGOTIABLE = "InstrumentNegotiable";  

    /** The Constant LABEL_MARKET_STATUS. */
    public static final String LABEL_MARKET_STATUS = "MktStatus";

    /** The Constant LABEL_USER_STATUS. */
    public static final String LABEL_USER_STATUS = "Status";

    /** The Constant LABEL_BANK_STATUS. */
    public static final String LABEL_BANK_STATUS = "LogonStatus";

    /** The Constant LABEL_MARKET_PHASE. */
    public static final String LABEL_MARKET_PHASE = "SectionPhase";

    /** The Constant LABEL_CONN_STATUS. */
    public static final String LABEL_CONN_STATUS = "XT2ConnStatus";

    /** The Constant LABEL_ENABLED. */
    public static final String LABEL_ENABLED = "Enabled";

    /** The Constant LABEL_ERROR_CODE. */
    public static final String LABEL_ERROR_CODE = "ErrCode";

    /** The Constant LABEL_ERROR_MESSAGE. */
    public static final String LABEL_ERROR_MESSAGE = "ErrMsg";

    /** The Constant LABEL_CD. */
    public static final String LABEL_CD = "CD";

    /** The Constant LABEL_BOOK_QUOTE_ID. */
    public static final String LABEL_BOOK_QUOTE_ID = "TradingLevel[0].QuoteId";

    /** The Constant LABEL_QUOTE_ID. */
    public static final String LABEL_QUOTE_ID = "QuoteId";

    /** The Constant LABEL_RFCQ_ID. */
    public static final String LABEL_RFCQ_ID = "RfqSeqNo";

    /** The Constant LABEL_RFCQ_SELL_SIDE_MEMBER_CODE. */
    public static final String LABEL_RFCQ_SELL_SIDE_MEMBER_CODE = "TradingLevel[0].MemberCode";

    /** The Constant LABEL_RFCQ_PRICE. */
    public static final String LABEL_RFCQ_PRICE = "TradingLevel[0].TradingLeg[0].Price";

    /** The Constant LABEL_RFCQ_YIELD. */
    public static final String LABEL_RFCQ_YIELD = "TradingLevel[0].TradingLeg[0].Yield";

    /** The Constant LABEL_PRICE. */
    public static final String LABEL_PRICE = "Price";

    /** The Constant LABEL_CONTRACTNO. */
    public static final String LABEL_CONTRACTNO = "ContractNo";

    /** The Constant LABEL_ORDERNUM. */
    public static final String LABEL_ORDERNUM = "OrderNum";

    /** The Constant LABEL_COUNTERPART_MEMBER. */
    public static final String LABEL_COUNTERPART_MEMBER = "CounterpartMember";

    /** The Constant LABEL_ACCRUED_VALUE. */
    public static final String LABEL_ACCRUED_VALUE = "AccruedValue";

    /** The Constant LABEL_CONTRACT_TIME. */
    public static final String LABEL_CONTRACT_TIME = "MktTime";

    /** The Constant LABEL_SIDE. */
    public static final String LABEL_SIDE = "Side";

    /** The Constant LABEL_QUANTITY. */
    public static final String LABEL_QUANTITY = "Quantity";

    /** The Constant LABEL_ACTION. */
    public static final String LABEL_ACTION = "Action";

    /** The Constant LABEL_OFFER_PRICE. */
    public static final String LABEL_OFFER_PRICE = "OfferPrice";

    /** The Constant LABEL_OFFER_YIELD. */
    public static final String LABEL_OFFER_YIELD = "OfferYield";

    /** The Constant LABEL_BOOK_UPDATE_TIME. */
    public static final String LABEL_BOOK_UPDATE_TIME = "BookUpdateTime";

    /** The Constant LABEL_UPDATE_TIME. */
    public static final String LABEL_UPDATE_TIME = "UpdateTime";

    /** The Constant LABEL_UPDATE_TIME_NSEC. */
    public static final String LABEL_UPDATE_TIME_NSEC = "UpdateTime_nSec";

    /** The Constant LABEL_BOOK_UPDATE_TIME_NSEC. */
    public static final String LABEL_BOOK_UPDATE_TIME_NSEC = "BookUpdateTime_nSec";

    /** The Constant LABEL_RFCQ_MSG_ID. */
    public static final String LABEL_RFCQ_MSG_ID = "RfcqMsgId";

    /** The label price item count. */
    public static String LABEL_PRICE_ITEM_COUNT = "NoMDEntries.ItemCount";

    /** The label time. */
    public static String LABEL_TIME = "Time";

    /** The label date. */
    public static String LABEL_DATE = "Date";

    /** The label row columns count. */
    public static String LABEL_ROW_COLUMNS_COUNT = "Columns.ItemCount";

    /** The label proposal market maker. */
    public static String LABEL_PROPOSAL_MARKET_MAKER = "CodeMM";

    /** The Constant LABEL_PRICE_STATUS. */
    public static final String LABEL_PRICE_STATUS = "PriceStatus";

    /** The Constant LABEL_PRICE_RESP_QUOTE_ID. */
    public static final String LABEL_PRICE_RESP_QUOTE_ID = "QuoteID";

    /** The Constant LABEL_PRICE_RESP_QUANTITY. */
    public static final String LABEL_PRICE_RESP_QUANTITY = "Qty";

    /** The Constant BVS_PRICE_DISC_REQ_RESULT_MSG. */
    public static final String BVS_PRICE_DISC_REQ_RESULT_MSG = "XT2CQueryPricesResp";

    /** The Constant VALUE_PRICE_STATUS_ACTIVE. */
    public static final String VALUE_PRICE_STATUS_ACTIVE = "A";

    /** The Constant VALUE_PRICE_STATUS_INDICATIVE. */
    public static final String VALUE_PRICE_STATUS_INDICATIVE = "I";

    /** The Constant VALUE_CONN_STATUS_ON. */
    public static final int VALUE_CONN_STATUS_ON = 1;

    /** The Constant VALUE_MARKET_STATUS_ON. */
    public static final int VALUE_MARKET_STATUS_ON = 0;

    /** The Constant VALUE_PHASE_STATUS_OPEN. */
    public static final int VALUE_PHASE_STATUS_OPEN = 4;

    /** The Constant VALUE_USER_STATUS_ON. */
    public static final int VALUE_USER_STATUS_ON = 1;

    /** The Constant VALUE_BANK_STATUS_ON. */
    public static final int VALUE_BANK_STATUS_ON = 1;

    /** The Constant VALUE_RFQ_STATUS_PENDING. */
    public static final int VALUE_RFQ_STATUS_PENDING = 0;

    /** The Constant VALUE_RFQ_STATUS_ACCEPTED. */
    public static final int VALUE_RFQ_STATUS_ACCEPTED = 1;

    /** The Constant VALUE_RFQ_STATUS_EXPIRED. */
    public static final int VALUE_RFQ_STATUS_EXPIRED = 2;

    /** The Constant VALUE_RFQ_STATUS_CLOSED_BY_CLIENT. */
    public static final int VALUE_RFQ_STATUS_CLOSED_BY_CLIENT = 3;

    /** The Constant VALUE_RFQ_STATUS_REJECTED_BY_ALL. */
    public static final int VALUE_RFQ_STATUS_REJECTED_BY_ALL = 4;

    /** The Constant VALUE_RFQ_STATUS_REJECTED_BY_SYSTEM. */
    public static final int VALUE_RFQ_STATUS_REJECTED_BY_SYSTEM = 5;

    /** The Constant VALUE_RFQ_STATUS_CLOSED_BY_GOV. */
    public static final int VALUE_RFQ_STATUS_CLOSED_BY_GOV = 6;

    /** The Constant VALUE_RFQ_STATUS_CLOSED_BY_SYSTEM. */
    public static final int VALUE_RFQ_STATUS_CLOSED_BY_SYSTEM = 7;

    /** The Constant VALUE_RFQ_STATUS_DELETED_BY_GOV. */
    public static final int VALUE_RFQ_STATUS_DELETED_BY_GOV = 8;

    /** The Constant VALUE_SIDE_ASK. */
    public static final int VALUE_SIDE_ASK = 2;

    /** The Constant VALUE_SIDE_BID. */
    public static final int VALUE_SIDE_BID = 1;

    /** The Constant VALUE_ACTION_ACCEPT. */
    public static final int VALUE_ACTION_ACCEPT = 2;

    /** The Constant VALUE_ACTION_CLOSE. */
    public static final int VALUE_ACTION_CLOSE = 3;

    /** The Constant VALUE_EXECUTION_REPORT_STATUS_ACTIVE. */
    public static final int VALUE_EXECUTION_REPORT_STATUS_ACTIVE = 0;

    /** The Constant VALUE_EXECUTION_REPORT_STATUS_CANCELED. */
    public static final int VALUE_EXECUTION_REPORT_STATUS_CANCELED = 1;

    /** The Constant VALUE_EXECUTION_REPORT_STATUS_RESTORED. */
    public static final int VALUE_EXECUTION_REPORT_STATUS_RESTORED = 2;

}
