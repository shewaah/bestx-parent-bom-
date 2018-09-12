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

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by:  
* Creation date: 2-nov-2012 
* 
**/
public class FixMessageFields {
    public final static String FIX_TradingCapacity = "TradingCapacity";
    public final static String FIX_DecisionMaker = "DecisionMaker";
    public final static String FIX_ShortSellIndicator = "ShortSellIndicator";
    public final static String FIX_MiFIDTVRestricted = "MiFIDTVRestricted";
    public final static String FIX_SessionID = "SessionId";
    public final static String FIX_ErrorCode = "ErrorCode";
    public final static String FIX_ErrorMsg = "ErrorMsg";
    // EXECUTION REPORT (TRADE)
    public final static String FIX_OrderID = "OrderID";
    public final static String FIX_ExecID = "ExecID";
    public final static String FIX_ExecTransType = "ExecTransType";
    public final static String FIX_ExecType = "ExecType";
    public static final String FIX_OrdStatus = "OrdStatus";
    public static final String FIX_OrdRejReason = "OrdRejReason";
    public static final String FIX_LastShares = "LastShares";
    public static final String FIX_LeavesQty = "LeavesQty";
    public static final String FIX_CumQty = "CumQty";
    public static final String FIX_LastPx = "LastPx";
    public static final String FIX_AvgPx = "AvgPx";
    public static final String FIX_Commission = "Commission";
    public static final String FIX_CommType = "CommType";
    public static final String FIX_TipoConto = "TipoConto";
    public static final String FIX_ExecBroker = "ExecBroker";
    public static final String FIX_IDBestx = "IDBestx";
    public static final String FIX_AccruedAmt = "AccruedAmt";
    public static final String FIX_Counterpart= "Counterpart";
    public static final String FIX_InternalizationIndicator= "InternalizationIndicator";
    public static final String FIX_BTDSCommissionIndicator= "BTDSCommissionIndicator";
    // QUOTE
    public final static String FIX_BidPriceAmount = "BidPx";
    public final static String FIX_AskPriceAmount = "OfferPx";
    public final static String FIX_BidPriceQty = "BidSize";
    public final static String FIX_AskPriceQty = "OfferSize";
    public final static String FIX_QuoteValidityTime = "ValidUntilTime";
    public final static String FIX_QuoteAccruedDays = "NumDaysInterest";  		// RICHIESTO DA AN per FIX GW
    public final static String FIX_QuoteAccruedInterestAmount = "AccruedAmt";   // RICHIESTO DA AN per FIX GW
    public final static String FIX_QuoteMarket = "LastMkt";
    public static final String FIX_QuoteAckStatus = "QuoteAckStatus";
    public static final String FIX_QuoteRejectReason = "QuoteRejectReason";
    // ORDER
    /**Introduced by TDS */
    public final static String FIX_QuoteRequestID = "QuoteReqID";
    public final static String FIX_QuoteID = "QuoteID";
    public final static String FIX_TransactTime = "TransactTime";
    /**char 11 man */
    public final static String FIX_ClOrdID = "ClOrdID";
    /**char 18 opt not used now*/
    public final static String FIX_ExecInst = "ExecInst";
	/**char 21 man*/
    public final static String FIX_HandlInst = "HandlInst";
 	/**char 55 man*/
    public final static String FIX_Symbol = "Symbol";
 	/** char 48 opt*/
    public final static String FIX_SecurityID = "SecurityID";
 	/**char 22 opt fized to 4*/
    public final static String FIX_IDSource = "IDSource";
    /** char 100 opt Code of the execution destination - MIC Code in Execution Report */
    public final static String FIX_ExDestination = "ExDestination";
    /** char 207 L for RFQ Toronto Dominion sometimes O in TAS nor used for orders */
    public final static String FIX_SecurityExchange = "SecurityExchange";
	/**int 38 opt*/
    public final static String FIX_OrderQty = "OrderQty";
 	/**char 40 man 1=Market, 2=Limit*/
    public final static String FIX_OrdType = "OrdType";
 	/**float 44 opt solo se ordtype=2*/
    public final static String FIX_Price = "Price";
 	/**char 15 opt*/
    public final static String FIX_Currency = "Currency";
 	/**char 54 man 1=Buy 2=Sell*/
    public final static String FIX_Side = "Side";
 	/**char 59 opt solo 0 per ora*/
    public final static String FIX_TimeInForce = "TimeInForce";
 	/**char 126 opt non usato per ora*/
    public final static String FIX_ExpireTime = "ExpireTime";
 	/**char 58 opt controparte (a volte)*/
    public final static String FIX_Text = "Text";
 	/**char 109 opt codice cliente (per conto di)*/
    public final static String FIX_ClientID = "ClientID";
 	/**char 107 opt*/
    public final static String FIX_SecurityDesc = "SecurityDesc";
 	/**char 1 opt, codice CED banca mittente*/
    public final static String FIX_Account = "Account";
 	/**char 63 opt data valuta fisso a 6*/
    public final static String FIX_SettlmntTyp = "SettlmntTyp";
 	/**date 64 opt yyyymmdd*/
    public final static String FIX_FutSettDate = "FutSettDate";
    // [DR20120629] CSBESTXPOC-158 - Wrong value for field PriceType on FIX 4.2  
    public final static String FIX_PriceType = "PriceType";
    /**Custom TAG String*/
    public final static String FIX_CustomerType = "CustomerType";  
    /**Custom TAG String*/
    public final static String FIX_CustomerOrderId = "CustomerOrderId";
    /**Custom TAG String*/
    public final static String FIX_CustomerOrderSource = "OrderSource";
    /**Custom TAG String*/
    public static final String FIX_TicketOwner = "TicketOwner";
    //BESTX-348: SP-20180905 added numDaysInterest field
    public static final String FIX_NumDaysInterest = "NumDaysInterest";
    
    
    // REVOKE
    public final static String FIX_OrigClOrdID = "OrigClOrdID";
    public final static String FIX_OrderRevokeAccept = "ORDCANCELACCEPT";
    public final static String FIX_OrderRevokeReject = "ORDCANCELREJECT";
    public final static String FIX_OrderRevokeResponse = "ORDCANCEL_RESP";
    
    //TMO
    public final static String FIX_CustOrderHandlingInstr = "CustOrderHandlingInstr";
    public static final String FIX_EffectiveTime = "EffectiveTime";
    public final static String FIX_TryAfterMinutes = "TryAfterMinutes";
}
