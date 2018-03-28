/**
 * Copyright (c) 2005 SoftSolutions S.r.l. 
 * All Rights Reserved.
 *
 * THIS SOURCE CODE IS CONFIDENTIAL AND PROPRIETARY 
 * AND MAY NOT BE USED OR DISTRIBUTED WITHOUT THE 
 * WRITTEN PERMISSION OF SOFTSOLUTIONS.
 */
package it.softsolutions.bestx.connections.fixgateway;

/**
 * @author Giancarlo Cadei
 * @version $Revision: 1.3 $
 */
public enum FixMessageTypes {
    LOGIN, 
    XT2CLogonServiceResp,
    LOGOUT,
    LOGOUT_RESP,
    XT2CServiceStartedNotify,
    QUOTE,
    QUOTE_REQUEST_RESP,
    ORDER,
    ORDER_RESP,
    ORDCANCEL,
    ORDCANCEL_RESP,
    TRADE,
    TRADE_RESP,
    ORDCANCELACCEPT,
    ORDCANCELACCEPT_RESP,
    ORDCANCELREJECT,
    ORDCANCELREJECT_RESP,
    QUOTE_REQUEST,
    quoteReqID;


//    public static final String FIX_SUBJKEY_LOGIN = "LOGIN";
//    public static final String FIX_SUBJKEY_LOGIN_RESP = "XT2CLogonServiceResp";
//    public static final String FIX_SUBJKEY_LOGOUT = "LOGOUT";
//    public static final String FIX_SUBJKEY_LOGOUT_RESP = "LOGOUT_RESP";
//    public static final String FIX_SUBJKEY_NOTIFY_START = "XT2CServiceStartedNotify";
//    public static final String FIX_SUBJKEY_QUOTE = "QUOTE";
//    public static final String FIX_SUBJKEY_QUOTE_REJECT = "QUOTE_REQUEST_RESP";
//    public static final String FIX_SUBJKEY_ORDER = "ORDER";
//    public static final String FIX_SUBJKEY_ORDER_RESP = "ORDER_RESP";
//    public static final String FIX_SUBJKEY_ORDCANCEL = "ORDCANCEL";
//    public static final String FIX_SUBJKEY_ORDCANCEL_RESP = "ORDCANCEL_RESP";
//    public static final String FIX_SUBJKEY_TRADE = "TRADE";
//    public static final String FIX_SUBJKEY_TRADE_RESP = "TRADE_RESP";
//    public static final String FIX_SUBJKEY_ORDCANCELACCEPT = "ORDCANCELACCEPT";
//    public static final String FIX_SUBJKEY_ORDCANCELACCEPT_RESP = "ORDCANCELACCEPT_RESP";
//    public static final String FIX_SUBJKEY_ORDCANCELREJECT = "ORDCANCELREJECT";
//    public static final String FIX_SUBJKEY_ORDCANCELREJECT_RESP = "ORDCANCELREJECT_RESP";
//    public static final String FIX_SUBJKEY_RFQ = "QUOTE_REQUEST";
//    public static final String FIX_SUBJKEY_QUOTE_RESP = "quoteReqID";
//    public static final String FIX_MKTKEY = "FIX";
    
}
