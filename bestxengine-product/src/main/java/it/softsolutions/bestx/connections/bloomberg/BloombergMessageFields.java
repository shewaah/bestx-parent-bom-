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
package it.softsolutions.bestx.connections.bloomberg;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 11/ott/2012 
* 
**/
public class BloombergMessageFields {
	
	public static final String SUBJECT_BBG_PRICE_REQUEST = "BBFeedSubscriptionPdu";
	public static final String BLOOM_MKT_LABEL = "MARKET";
	public static final String BLOOM_MKT_VALUE = "BLOOM";
	public static final String BLOOM_CLASS_LABEL = "CLASS_NAME";
	public static final String BLOOM_CLASS_VALUE = "BOOK";
	public static final String BLOOM_SOURCE_LABEL = "MAIN_SOURCE";
	public static final String BLOOM_SOURCE_VALUE = "F";
	public static final String BLOOM_TYPE_LABEL = "SUB_TYPE";
	public static final String BLOOM_TYPE_VALUE = "ISIN";
	public static final String BLOOM_DES_LABEL = "DESCRIPTION";
	public static final String BLOOM_DES_VALUE = "None";
	public static final String BLOOM_ISIN_LABEL = "ISIN";
	public static final String BLOOM_SNAPSHOT_LABEL = "SNAPSHOT_ONLY";
	public static final int BLOOM_SNAPSHOT_VALUE = 1;
	public static final String BLOOM_INST_CODE_LABEL = "INSTRUMENT_CODE";
	public static final String BLOOM_CODE_SEPARATOR = "@";
	
	public static final String BLOOM_PRICEPDU_SUBJECT = "BOOK";
	
	public static final String BLOOM_TIME = "Time";
	public static final String BLOOM_TIMETS = "XT2TS";
	public static final String BLOOM_DATE = "Date";
	public static final String BLOOM_BID_PRICE = "Bid1P";
	public static final String BLOOM_ASK_PRICE = "Ask1P";
	public static final String BLOOM_BID_QTY = "Bid1Q";
	public static final String BLOOM_ASK_QTY = "Ask1Q";
	public static final String BLOOM_ERRORCODE_LABEL = "ErrorCode";
	public static final String BLOOM_ERRORMSG_LABEL = "ErrorMsg";

	public static final String TRADEFEED_FILL_QUEUE = "/BLOOMBERG_TRADE_FEED/FILL/*";

	public static final String TRADEFEED_FILLPDU_SUBJECT = "FILL";
	public static final String TRADEFEED_SECURITYID_LABEL = "SecurityId";
	public static final String TRADEFEED_TRADER_LABEL = "Trader";
	public static final String TRADEFEED_TICKETNUM_LABEL = "TSN";
	public static final String TRADEFEED_QUANTITY_LABEL = "Quantity";
	public static final String TRADEFEED_NODA_LABEL = "NumberOfDaysAccrued";
	public static final String TRADEFEED_ACCRUEDINT_LABEL = "AccruedInterestRepo";
	public static final String TRADEFEED_ACCRUEDINT_FRACT_LABEL = "AccruedInterestRepoFractInd";
	public static final String TRADEFEED_CURRENCY_LABEL = "Currency";
	public static final String TRADEFEED_SIDE_LABEL = "Side";
	public static final String TRADEFEED_SETTLEMENTDATE_LABEL = "SettlementDate";
	public static final String TRADEFEED_PRICE = "PriceStr";
	public static final String TRADEFEED_MARKET_MAKER_ACCOUNT = "CustomerAccount";
	public static final String TRADEFEED_SIDE_BUY_VALUE = "B";
}
