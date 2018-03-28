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
package it.softsolutions.bestx.fix.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: June 11, 2012
 * 
 **/
public enum MsgType {
	Unknown("NONE"),
	Heartbeat(quickfix.field.MsgType.HEARTBEAT),
	TestRequest(quickfix.field.MsgType.TEST_REQUEST),
	ResendRequest(quickfix.field.MsgType.RESEND_REQUEST),
	Reject(quickfix.field.MsgType.REJECT),
	SequenceReset(quickfix.field.MsgType.SEQUENCE_RESET),
	Logout(quickfix.field.MsgType.LOGOUT),
	IndicationOfInterest(quickfix.field.MsgType.INDICATION_OF_INTEREST),
	Advertisement(quickfix.field.MsgType.ADVERTISEMENT),
	ExecutionReport(quickfix.field.MsgType.EXECUTION_REPORT),
	OrderCancelReject(quickfix.field.MsgType.ORDER_CANCEL_REJECT),
	Logon(quickfix.field.MsgType.LOGON),
	News(quickfix.field.MsgType.NEWS),
	Email(quickfix.field.MsgType.EMAIL),
	OrderSingle(quickfix.field.MsgType.ORDER_SINGLE),
	OrderList(quickfix.field.MsgType.ORDER_LIST),
	OrderCancelRequest(quickfix.field.MsgType.ORDER_CANCEL_REQUEST),
	OrderCancelReplaceRequest(quickfix.field.MsgType.ORDER_CANCEL_REPLACE_REQUEST),
	OrderStatusRequest(quickfix.field.MsgType.ORDER_STATUS_REQUEST),
	AllocationInstruction(quickfix.field.MsgType.ALLOCATION_INSTRUCTION),
	ListCancelRequest(quickfix.field.MsgType.LIST_CANCEL_REQUEST),
	ListExecute(quickfix.field.MsgType.LIST_EXECUTE),
	ListStatusRequest(quickfix.field.MsgType.LIST_STATUS_REQUEST),
	ListStatus(quickfix.field.MsgType.LIST_STATUS),
	AllocationInstructionAck(quickfix.field.MsgType.ALLOCATION_INSTRUCTION_ACK),
	DontKnowTrade(quickfix.field.MsgType.DONT_KNOW_TRADE),
	QuoteRequest(quickfix.field.MsgType.QUOTE_REQUEST),
	Quote(quickfix.field.MsgType.QUOTE),
	SettlementInstructions(quickfix.field.MsgType.SETTLEMENT_INSTRUCTIONS),
	MarketDataRequest(quickfix.field.MsgType.MARKET_DATA_REQUEST),
	MarketDataSnapshotFullRefresh(quickfix.field.MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH),
	MarketDataIncrementalRefresh(quickfix.field.MsgType.MARKET_DATA_INCREMENTAL_REFRESH),
	MarketDataRequestReject(quickfix.field.MsgType.MARKET_DATA_REQUEST_REJECT),
	QuoteCancel(quickfix.field.MsgType.QUOTE_CANCEL),
	QuoteStatusRequest(quickfix.field.MsgType.QUOTE_STATUS_REQUEST),
	MassQuoteAcknowledgement(quickfix.field.MsgType.MASS_QUOTE_ACKNOWLEDGEMENT),
	SecurityDefinitionRequest(quickfix.field.MsgType.SECURITY_DEFINITION_REQUEST),
	SecurityDefinition(quickfix.field.MsgType.SECURITY_DEFINITION),
	SecurityStatusRequest(quickfix.field.MsgType.SECURITY_STATUS_REQUEST),
	SecurityStatus(quickfix.field.MsgType.SECURITY_STATUS),
	TradingSessionStatusRequest(quickfix.field.MsgType.TRADING_SESSION_STATUS_REQUEST),
	TradingSessionStatus(quickfix.field.MsgType.TRADING_SESSION_STATUS),
	MassQuote(quickfix.field.MsgType.MASS_QUOTE),
	BusinessMessageReject(quickfix.field.MsgType.BUSINESS_MESSAGE_REJECT),
	BidRequest(quickfix.field.MsgType.BID_REQUEST),
	BidReposne(quickfix.field.MsgType.BID_RESPONSE),
	ListStrikePrice(quickfix.field.MsgType.LIST_STRIKE_PRICE),
	XmlMessage(quickfix.field.MsgType.XML_MESSAGE),
	RegistrationInstructions(quickfix.field.MsgType.REGISTRATION_INSTRUCTIONS),
	RegistrationInstructionsResponse(quickfix.field.MsgType.REGISTRATION_INSTRUCTIONS_RESPONSE),
	OrderMassCancelRequest(quickfix.field.MsgType.ORDER_MASS_CANCEL_REQUEST),
	OrderMassCancelReport(quickfix.field.MsgType.ORDER_MASS_CANCEL_REPORT),
	NewOrderCross(quickfix.field.MsgType.NEW_ORDER_CROSS),
	CrossOrderCancelReplaceRequest(quickfix.field.MsgType.CROSS_ORDER_CANCEL_REPLACE_REQUEST),
	CrossOrderCancelRequest(quickfix.field.MsgType.CROSS_ORDER_CANCEL_REQUEST),
	SecurityTypeRequest(quickfix.field.MsgType.SECURITY_TYPE_REQUEST),
	SecurityTypes(quickfix.field.MsgType.SECURITY_TYPES),
	SecurityListRequest(quickfix.field.MsgType.SECURITY_LIST_REQUEST),
	SecurityList(quickfix.field.MsgType.SECURITY_LIST),
	DerivativeSecurityListRequest(quickfix.field.MsgType.DERIVATIVE_SECURITY_LIST_REQUEST),
	DerivativeSecurityList(quickfix.field.MsgType.DERIVATIVE_SECURITY_LIST),
	NewOrderMultileg(quickfix.field.MsgType.NEW_ORDER_MULTILEG),
	MultilegOrderCancelReplaceRequest(quickfix.field.MsgType.MULTILEG_ORDER_CANCEL_REPLACE),
	TradeCaptureReportRequest(quickfix.field.MsgType.TRADE_CAPTURE_REPORT_REQUEST),
	TradeCaptureReport(quickfix.field.MsgType.TRADE_CAPTURE_REPORT),
	OrderMassStatusRequest(quickfix.field.MsgType.ORDER_MASS_STATUS_REQUEST),
	QuoteRequestReject(quickfix.field.MsgType.QUOTE_REQUEST_REJECT),
	RFQrequest(quickfix.field.MsgType.RFQ_REQUEST),
	QuoteStatusReport(quickfix.field.MsgType.QUOTE_STATUS_REPORT),
	QuoteResponse(quickfix.field.MsgType.QUOTE_RESPONSE),
	Confirmation(quickfix.field.MsgType.CONFIRMATION),
	PositionMaintenanceRequest(quickfix.field.MsgType.POSITION_MAINTENANCE_REQUEST),
	PositionmaintenanceReport(quickfix.field.MsgType.POSITION_MAINTENANCE_REPORT),
	RequestForPosition(quickfix.field.MsgType.REQUEST_FOR_POSITIONS),
	RequestForPositionsAck(quickfix.field.MsgType.REQUEST_FOR_POSITIONS_ACK),
	PositionReport(quickfix.field.MsgType.POSITION_REPORT),
	TradeCaptureReportRequestAck(quickfix.field.MsgType.TRADE_CAPTURE_REPORT_REQUEST_ACK),
	TradeCaptureReportAck(quickfix.field.MsgType.TRADE_CAPTURE_REPORT_ACK),
	AllocationReport(quickfix.field.MsgType.ALLOCATION_REPORT),
	ConfirmationAck(quickfix.field.MsgType.CONFIRMATION_ACK),
	SettlementInstructionRequest(quickfix.field.MsgType.SETTLEMENT_INSTRUCTION_REQUEST),
	AssignmentReport(quickfix.field.MsgType.ASSIGNMENT_REPORT),
	CollateralRequest(quickfix.field.MsgType.COLLATERAL_REQUEST),
	CollateralAssignment(quickfix.field.MsgType.COLLATERAL_ASSIGNMENT),
	CollateralResponse(quickfix.field.MsgType.COLLATERAL_RESPONSE),
	CollateralReport(quickfix.field.MsgType.COLLATERAL_REPORT),
	CollateralInquiry(quickfix.field.MsgType.COLLATERAL_INQUIRY),
	NetworkStatusRequest(quickfix.field.MsgType.NETWORK_STATUS_REQUEST),
	NetworkStatusResponse(quickfix.field.MsgType.NETWORK_STATUS_RESPONSE),
	UserRequest(quickfix.field.MsgType.USER_REQUEST),
	UserResponse(quickfix.field.MsgType.USER_RESPONSE),
	CollateralInquiryAck(quickfix.field.MsgType.COLLATERAL_INQUIRY_ACK),
	ConfirmationRequest(quickfix.field.MsgType.CONFIRMATION_REQUEST),
	;
    
    public String getFIXValue() {
        return mFIXValue;
    }

    public static MsgType getInstanceForFIXValue(String inFIXValue) {
        if(inFIXValue == null) {
            return Unknown;
        }
        MsgType msgType = mFIXValueMap.get(inFIXValue);
        return msgType == null
                ? Unknown
                : msgType;
    }

    private MsgType(String inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final String mFIXValue;
    private static final Map<String, MsgType> mFIXValueMap;
    static {
        Map<String, MsgType> table = new HashMap<String, MsgType>();
        for(MsgType msgType: values()) {
            table.put(msgType.getFIXValue(),msgType);
        }
        mFIXValueMap = Collections.unmodifiableMap(table);
    }
    
}
