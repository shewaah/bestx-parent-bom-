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

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-protocol-api 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class IB4JOperatorConsoleMessage {

    /** The Constant PUB_SUBJ_OP_STATUS_CHG. */
    public static final String PUB_SUBJ_OP_STATUS_CHG = "/BESTX/OPERATIONS/STATUS";


    // RR Session ID
    /** The Constant RR_SESSION_ID. */
    public static final String RR_SESSION_ID = "BESTX_RR_SESSION_ID";

    // RR request type
    /** The Constant REQ_TYPE. */
    public static final String REQ_TYPE = "BESTX_RR_REQ_MSG_TYPE";
    // RR request type values
    /** The Constant REQ_TYPE_COMMAND. */
    public static final String REQ_TYPE_COMMAND = "BESTX_RR_REQ_VAL_COMMAND"; // command

    /** The Constant REQ_TYPE_QUERY. */
    public static final String REQ_TYPE_QUERY = "BESTX_RR_REQ_VAL_QUERY"; // query
    
    public static final String REQ_TYPE_PRICE_DISCOVERY = "BESTX_RR_REQ_VAL_PRICE_DISCOVERY"; // price discovery
    
    //public static final String REQ_TYPE_CANCEL_OPERATIONS = "BESTX_RR_REQ_VAL_CANCEL_OPERATIONS"; // price discovery

    // RR request command type label
    /** The Constant TYPE_COMMAND. */
    public static final String TYPE_COMMAND = "BESTX_RR_REQ_MSG_COMMAND"; 
    // RR request command type values
    // operation commands
    /** The Constant CMD_RETRY. */
    public static final String CMD_RETRY = "BESTX_RR_REQ_VAL_RETRY";

    /** The Constant CMD_TERMINATE. */
    public static final String CMD_TERMINATE = "BESTX_RR_REQ_VAL_TERMINATE";

    /** The Constant CMD_ABORT. */
    public static final String CMD_ABORT = "BESTX_RR_REQ_VAL_ABORT";

    /** The Constant CMD_SUSPEND. */
    public static final String CMD_SUSPEND = "BESTX_RR_REQ_VAL_SUSPEND";

    /** The Constant CMD_RESTORE. */
    public static final String CMD_RESTORE = "BESTX_RR_REQ_VAL_RESTORE";

    /** The Constant CMD_REINITIATE. */
    public static final String CMD_REINITIATE = "BESTX_RR_REQ_VAL_REINITIATE";

    /** The Constant CMD_FORCE_STATE. */
    public static final String CMD_FORCE_STATE = "BESTX_RR_REQ_VAL_FORCE_STATE";

    /** The Constant CMD_CHANGE_MKT_ENABLED. */
    public static final String CMD_CHANGE_MKT_ENABLED = "BESTX_RR_REQ_VAL_CHANGE_MKT_ENABLED";

    /** The Constant CMD_NOT_EXECUTED. */
    public static final String CMD_NOT_EXECUTED = "NOT_EXECUTED";

    /** The Constant CMD_MANUAL_MANAGE. */
    public static final String CMD_MANUAL_MANAGE = "MANUAL_MANAGE";

    /** The Constant CMD_REVOCATION_WAIT. */
    public static final String CMD_REVOCATION_WAIT = "REVOCATION_WAIT";

    /** The Constant CMD_ORDER_RETRY. */
    public static final String CMD_ORDER_RETRY = "ORDER_RETRY";

    /** The Constant CMD_RESEND_TO_TRADER. */
    public static final String CMD_RESEND_TO_TRADER = "RESEND_TO_TRADER";

    /** The Constant CMD_ACCEPT_ORDER. */
    public static final String CMD_ACCEPT_ORDER = "ACCEPT_ORDER";

    /** The Constant CMD_ACCEPT_SETT_DATE. */
    public static final String CMD_ACCEPT_SETT_DATE = "ACCEPT_SETT_DATE";

    /** The Constant CMD_ACCEPT_REVOCATION. */
    public static final String CMD_ACCEPT_REVOCATION = "ACCEPT_REVOCATION";

    /** The Constant CMD_REJECT_REVOCATION. */
    public static final String CMD_REJECT_REVOCATION = "REJECT_REVOCATION";

    /** The Constant CMD_EXECUTE_ORDER. */
    public static final String CMD_EXECUTE_ORDER = "EXECUTE_ORDER";

    /** The Constant CMD_FORCE_EXECUTED. */
    public static final String CMD_FORCE_EXECUTED = "FORCE_EXECUTED";

    /** The Constant CMD_FORCE_NOT_EXECUTED. */
    public static final String CMD_FORCE_NOT_EXECUTED = "FORCE_NOT_EXECUTED";

    /** The Constant CMD_FORCE_RECEIVED. */
    public static final String CMD_FORCE_RECEIVED = "FORCE_RECEIVED";

    /** The Constant CMD_FORCE_MKT_WAIT. */
    public static final String CMD_FORCE_MKT_WAIT = "FORCE_MKT_WAIT";

    /** The Constant CMD_MANUAL_EXECUTED. */
    public static final String CMD_MANUAL_EXECUTED = "MANUAL_EXECUTED";

    /** The Constant CMD_MANUAL_NOT_EXECUTED. */
    public static final String CMD_MANUAL_NOT_EXECUTED = "MANUAL_NOT_EXECUTED";

    /** The Constant CMD_FORCE_NO_MARKET. */
    public static final String CMD_FORCE_NO_MARKET = "FORCE_NO_MARKET";

    /** The Constant CMD_DISABLE_MARKET. */
    public static final String CMD_DISABLE_MARKET = "DISABLE_MARKET";

    /** The Constant CMD_ACCEPT_COUNTER. */
    public static final String CMD_ACCEPT_COUNTER = "ACCEPT_COUNTER";

    /** The Constant CMD_ACTIVATE_CURANDO. */
    public static final String CMD_ACTIVATE_CURANDO = "ACTIVATE_CURANDO";

    /** The Constant CMD_AUTO_MANUAL_ORDER. */
    public static final String CMD_AUTO_MANUAL_ORDER = "AUTO_MANUAL_ORDER";

    /** The Constant CMD_EXECUTE_CURANDO. */
    public static final String CMD_EXECUTE_CURANDO = "EXECUTE_CURANDO";

    /** The Constant CMD_STOP_TLX_EXECUTION. */
    public static final String CMD_STOP_TLX_EXECUTION = "STOP_TLX_EXEC";

    /** The Constant CMD_SEND_MARKET_CANCEL. */
    public static final String CMD_SEND_MARKET_CANCEL = "SEND_MARKET_CANCEL";

    /** The Constant CMD_RESEND_EXECUTIONREPORT. */
    public static final String CMD_RESEND_EXECUTIONREPORT = "COMMAND_RESEND_EXECUTIONREPORT";

    /** The Constant CMD_SEND_DDE. */
    public static final String CMD_SEND_DDE = "SEND_DDE_DATA";

    /** The Constant CMD_MERGE. */
    public static final String CMD_MERGE = "MERGE_ORDER"; //Stefano 28/01/2008 - Comando di incrocio degli ordini

    /** The Constant CMD_RICALCOLA_SBILANCIO. */
    public static final String CMD_RICALCOLA_SBILANCIO = "RICALCOLA_SBILANCIO";

    /** The Constant CMD_MOVE_NOT_EXECUTABLE. */
    public static final String CMD_MOVE_NOT_EXECUTABLE = "MOVE_NOT_EXECUTABLE";

    /** The Constant CMD_SEND_DES. */
    public static final String CMD_SEND_DES = "SEND_DES_DATA";

    /** The Constant CMD_UNRECONCILED_TRADE_MATCHED. */
    public static final String CMD_UNRECONCILED_TRADE_MATCHED = "UNRECONCILED_TRADE_MATCHED";

    // system state
    /** The Constant CMD_SYSTEM_ACTIVITY_ON. */
    public static final String CMD_SYSTEM_ACTIVITY_ON = "BESTX_RR_REQ_VAL_SYSTEM_ON";

    /** The Constant CMD_SYSTEM_ACTIVITY_OFF. */
    public static final String CMD_SYSTEM_ACTIVITY_OFF = "BESTX_RR_REQ_VAL_SYSTEM_OFF";

    //Market enabled
    /** The Constant CMD_MKT_ENABLE. */
    public static final String CMD_MKT_ENABLE = "BESTX_RR_REQ_VAL_MKT_ON";

    /** The Constant CMD_MKT_DISABLE. */
    public static final String CMD_MKT_DISABLE = "BESTX_RR_REQ_VAL_MKT_OFF";

    /** Command to send an order to Limit File No Price status */
    public static final String CMD_SEND_TO_LFNP = "SEND_TO_LFNP";
    
    public static final String CMD_PRICE_DISCOVERY = "PRICE_DISCOVERY";
    
    public static final String CMD_TAKE_OWNERSHIP = "TAKE_OWNERSHIP";
    
    // RR request command type label
    /** The Constant TYPE_QUERY. */
    public static final String TYPE_QUERY = "BESTX_RR_REQ_MSG_QUERY"; 
    // RR request query type values
    /** The Constant QUERY_GET_CHANNEL_STATUS. */
    public static final String QUERY_GET_CHANNEL_STATUS = "BESTX_RR_REQ_VAL_CHANNEL_STATUS";

    /** The Constant QUERY_GET_SYSTEM_ACTIVITY_STATUS. */
    public static final String QUERY_GET_SYSTEM_ACTIVITY_STATUS = "BESTX_RR_REQ_VAL_SYSTEM_STATUS";

    // RR request type
    /** The Constant RESP_TYPE. */
    public static final String RESP_TYPE = "BESTX_RR_RESP_MSG_TYPE";
    // RR request type values
    /** The Constant RESP_TYPE_COMMAND. */
    public static final String RESP_TYPE_COMMAND = "BESTX_RR_RESP_VAL_COMMAND"; // command

    /** The Constant RESP_TYPE_QUERY. */
    public static final String RESP_TYPE_QUERY = "BESTX_RR_RESP_VAL_QUERY"; // query

    // RR reply status 
    /** The Constant TYPE_STATUS. */
    public static final String TYPE_STATUS = "BESTX_RR_REP_MSG_STATUS";
    // RR reply status values
    /** The Constant STATUS_OK. */
    public static final String STATUS_OK = "BESTX_RR_REP_VAL_STATUS_OK";

    /** The Constant STATUS_ILLEGAL_ARGUMENTS. */
    public static final String STATUS_ILLEGAL_ARGUMENTS = "BESTX_RR_REP_VAL_STATUS_ILLEGAL_ARGUMENTS";

    /** The Constant STATUS_INTERNAL_ERROR. */
    public static final String STATUS_INTERNAL_ERROR = "BESTX_RR_REP_VAL_STATUS_INTERNAL_ERROR";
    // RR reply status field labels
    /** The Constant FLD_STATUS_MESSAGE. */
    public static final String FLD_STATUS_MESSAGE = "BESTX_RR_REP_FLD_STATUS_MSG";

    // Notify subject
    /** The Constant NOTIFY_OP_STATUS_CHG. */
    public static final String NOTIFY_OP_STATUS_CHG = "/BESTX/OPERATION/CHANGESTATE/";

    /** The Constant NOTIFY_OP_INSERT. */
    public static final String NOTIFY_OP_INSERT = "/BESTX/OPERATION/";

    //Notify fields
    /** The Constant FLD_MARKET_NAME. */
    public static final String FLD_MARKET_NAME = "MARKET_NAME";

    /** The Constant FLD_ORDER_NUM. */
    public static final String FLD_ORDER_NUM = "ORDER_NUM";

    /** The Constant FLD_ISIN. */
    public static final String FLD_ISIN = "ISIN";

    /** The Constant FLD_INSTRUMENT_DESCRIPTION. */
    public static final String FLD_INSTRUMENT_DESCRIPTION = "INSTRUMENT_DESCRIPTION";

    /** The Constant FLD_REVOKED. */
    public static final String FLD_REVOKED = "REVOKED";

    /** The Constant FLD_CUSTOMER_CODE. */
    public static final String FLD_CUSTOMER_CODE= "CUSTOMER_CODE";

    /** The Constant FLD_SIDE. */
    public static final String FLD_SIDE="SIDE";

    /** The Constant FLD_ORDER_PRICE. */
    public static final String FLD_ORDER_PRICE= "ORDER_PRICE";

    /** The Constant FLD_QTY. */
    public static final String FLD_QTY="QTY";
    
    /** The Constant FLD_CURRENCY. */
    public static final String FLD_CURRENCY = "CURRENCY";

    /** The Constant FLD_SETTLEMENT_DATE. */
    public static final String FLD_SETTLEMENT_DATE = "SETTLEMENT_DATE";

    /** The Constant FLD_RECEIVE_TIME. */
    public static final String FLD_RECEIVE_TIME = "RECEIVE_TIME";

    /** The Constant FLD_TRADER. */
    public static final String FLD_TRADER = "TRADER";

    /** The Constant FLD_MANAGEMENT. */
    public static final String FLD_MANAGEMENT = "MANAGEMENT";
    
    /** The Constant FLD_REQUESTOR. */
    public static final String FLD_REQUESTOR = "REQUESTOR";

    // Pub sub subject
    /** The Constant PUB_SUBJ_OP_DUMP. */
    public static final String PUB_SUBJ_OP_DUMP = "/BESTX/OPERATIONS/DUMP";
    
    public static final String PUB_SUBJ_PRICE_DISCOVERY = "/BESTX/PRICEDISCOVERY";
    
    // command request / state publish field labels
    /** The Constant FLD_RQF_ID. */
    public static final String FLD_RQF_ID = "BESTX_RR_REQ_FLD_RFQ_ID";

    /** The Constant FLD_ORDER_ID. */
    public static final String FLD_ORDER_ID = "BESTX_RR_REQ_FLD_ORDER_ID";

    /** The Constant FLD_STATE_NAME. */
    public static final String FLD_STATE_NAME = "BESTX_RR_REQ_FLD_STATE_NAME";

    /** The Constant FLD_OLD_STATE_NAME. */
    public static final String FLD_OLD_STATE_NAME = "BESTX_RR_REQ_FLD_OLD_STATE_NAME";

    /** The Constant FLD_COMMENT. */
    public static final String FLD_COMMENT = "BESTX_RR_REQ_FLD_COMMENT";

    // query response field labels
    /** The Constant FLD_CMF_STATUS. */
    public static final String FLD_CMF_STATUS = "BESTX_RR_RESP_FLD_CMF_STATUS"; // int

    /** The Constant FLD_FIX_STATUS. */
    public static final String FLD_FIX_STATUS = "BESTX_RR_RESP_FLD_FIX_STATUS"; // int

    /** The Constant FLD_PENG_STATUS. */
    public static final String FLD_PENG_STATUS = "BESTX_RR_RESP_FLD_PENG_STATUS"; // int

    /** The Constant FLD_SYSTEM_STATUS. */
    public static final String FLD_SYSTEM_STATUS = "BESTX_RR_RESP_FLD_SYSTEM_STATUS"; // int
    // Akros Specific
    /** The Constant FLD_PRICE_SERVICE_STATUS. */
    public static final String FLD_PRICE_SERVICE_STATUS = "BESTX_RR_RESP_FLD_PRICE_SERVICE_STATUS"; // int

    /** The Constant FLD_MKT_PRICE_CONNECTION_STATUS. */
    public static final String FLD_MKT_PRICE_CONNECTION_STATUS = "BESTX_RR_RESP_FLD_MKT_PRICE_CONNECTION_STATUS_"; // int

    /** The Constant FLD_MKT_ORDER_CONNECTION_STATUS. */
    public static final String FLD_MKT_ORDER_CONNECTION_STATUS = "BESTX_RR_RESP_FLD_MKT_ORDER_CONNECTION_STATUS_"; // int
    
    public static final String FLD_FIX_CHANNEL_NAME = "BESTX_RR_RESP_FLD_FIX_CHANNEL_NAME";
    public static final String FLD_FIX_CHANNEL_STATUS = "BESTX_RR_RESP_FLD_FIX_CHANNEL_STATUS";

    /** The Constant FLD_MKT_PRICE_CONNECTION_ENABLED. */
    public static final String FLD_MKT_PRICE_CONNECTION_ENABLED = "FLD_MKT_PRICE_CONNECTION_ENABLED_"; // int

    /** The Constant FLD_MKT_ORDER_CONNECTION_ENABLED. */
    public static final String FLD_MKT_ORDER_CONNECTION_ENABLED = "FLD_MKT_ORDER_CONNECTION_ENABLED_"; // int

    /** The Constant FLD_MKT_DISABLE_COMMENT. */
    public static final String FLD_MKT_DISABLE_COMMENT = "FLD_MKT_DISABLE_COMMENT"; // int

    /** The Constant FLD_MKT_NAME. */
    public static final String FLD_MKT_NAME = "FLD_MKT_NAME"; // string

    /** The Constant FLD_PRICE. */
    public static final String FLD_PRICE = "FLD_PRICE"; // string

    /** The Constant FLD_QUANTITY. */
    public static final String FLD_QUANTITY = "FLD_QTY"; // string

    /** The Constant FLD_MARKETMAKER_CODE. */
    public static final String FLD_MARKETMAKER_CODE = "FLD_MM_CODE"; // string

    /** The Constant FLD_REF_PRICE. */
    public static final String FLD_REF_PRICE = "FLD_REF_PRICE"; // string

    /** The Constant FLD_MATCH_PRICE. */
    public static final String FLD_MATCH_PRICE = "FLD_MATCH_PRICE"; // string

    //reconciled trades fields
    /** The Constant FLD_EXECUTION_PRICE. */
    public static final String FLD_EXECUTION_PRICE = "FLD_EXECUTION_PRICE";

    /** The Constant FLD_EXECUTION_MARKET_MAKER. */
    public static final String FLD_EXECUTION_MARKET_MAKER = "FLD_EXECUTION_MARKET_MAKER";

    /** The Constant FLD_EXECUTION_TICKET_NUMBER. */
    public static final String FLD_EXECUTION_TICKET_NUMBER = "FLD_EXECUTION_TICKET_NUMBER";
    
    /** This field indicates the user that is assigned as owner via take ownership command. */
    public static final String FLD_OWNERSHIP_USER = "FLD_OWNERSHIP_USER";
    
    /** Used to specify connection type status for price connection */
    public static final String FLD_PRICE_CHANNEL = "FLD_PRICE_CHANNEL";
    
    /** Used to specify connection type status for order connection */
    public static final String FLD_ORDER_CHANNEL = "FLD_ORDER_CHANNEL";
    
    /** Used to specify connection type status for order connection */
    public static final String FLD_CANCEL_OPERATIONS = "FLD_CANCEL_OPERATIONS";
    
}