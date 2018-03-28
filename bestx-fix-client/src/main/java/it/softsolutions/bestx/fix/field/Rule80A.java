/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
 * Creation date: 25/gen/2013 
 * 
 **/
public enum Rule80A {
    Unknown(Character.MIN_VALUE),
    AgencySingleOrder(quickfix.field.Rule80A.AGENCY_SINGLE_ORDER),
    ShortExemptTransactionReferToAType(quickfix.field.Rule80A.SHORT_EXEMPT_TRANSACTION_REFER_TO_A_TYPE),
    ProgramOrderNonIndexArbForMemberFirm(quickfix.field.Rule80A.PROGRAM_ORDER_NON_INDEX_ARB_FOR_MEMBER_FIRM),
    ProgramOrderIndexArbForMemberFirm(quickfix.field.Rule80A.PROGRAM_ORDER_INDEX_ARB_FOR_MEMBER_FIRM),
    ShortExemptTransactionForPrincipal(quickfix.field.Rule80A.SHORT_EXEMPT_TRANSACTION_FOR_PRINCIPAL),
    ShortExemptTransactionReferToWType(quickfix.field.Rule80A.SHORT_EXEMPT_TRANSACTION_REFER_TO_W_TYPE),
    ShortExemptTransactionReferToIType(quickfix.field.Rule80A.SHORT_EXEMPT_TRANSACTION_REFER_TO_I_TYPE),
    IndividualInvestor(quickfix.field.Rule80A.INDIVIDUAL_INVESTOR),
    ProgramOrderIndexArbForIndividualCustomer(quickfix.field.Rule80A.PROGRAM_ORDER_INDEX_ARB_FOR_INDIVIDUAL_CUSTOMER),
    ProgramOrderNonIndexArbForIndividualCustomer(quickfix.field.Rule80A.PROGRAM_ORDER_NON_INDEX_ARB_FOR_INDIVIDUAL_CUSTOMER),
    ShortExemptAffiliated(quickfix.field.Rule80A.SHORT_EXEMPT_AFFILIATED),
    ProgramOrderIndexArbForOtherMember(quickfix.field.Rule80A.PROGRAM_ORDER_INDEX_ARB_FOR_OTHER_MEMBER),
    ProgramOrderNonIndexArbForOtherMember(quickfix.field.Rule80A.PROGRAM_ORDER_NON_INDEX_ARB_FOR_OTHER_MEMBER),
    ProprietaryAffiliated(quickfix.field.Rule80A.PROPRIETARY_AFFILIATED),
    Principal(quickfix.field.Rule80A.PRINCIPAL),
    TransactionsNonMember(quickfix.field.Rule80A.TRANSACTIONS_NON_MEMBER),
    SpecialistTrades(quickfix.field.Rule80A.SPECIALIST_TRADES),
    TransactionsUnaffiliatedMember(quickfix.field.Rule80A.TRANSACTIONS_UNAFFILIATED_MEMBER),
    ProgramOrderIndexArbForOtherAgency(quickfix.field.Rule80A.PROGRAM_ORDER_INDEX_ARB_FOR_OTHER_AGENCY),
    AllOtherOrdersAsAgentForOtherMember(quickfix.field.Rule80A.ALL_OTHER_ORDERS_AS_AGENT_FOR_OTHER_MEMBER),
    ShortExemptNotAffiliated(quickfix.field.Rule80A.SHORT_EXEMPT_NOT_AFFILIATED),
    ProgramOrderNonIndexArbForOtherAgency(quickfix.field.Rule80A.PROGRAM_ORDER_NON_INDEX_ARB_FOR_OTHER_AGENCY),
    ShortExemptNonmember(quickfix.field.Rule80A.SHORT_EXEMPT_NONMEMBER),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static Rule80A getInstanceForFIXValue(char inValue) {
        Rule80A rule80A = mFIXValueTable.get(inValue);
        return rule80A == null
                ? Unknown
                : rule80A;
    }

    private Rule80A(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, Rule80A> mFIXValueTable;
    static {
        Map<Character, Rule80A> table = new HashMap<Character, Rule80A>();
        for(Rule80A rule80A: values()) {
            table.put(rule80A.getFIXValue(), rule80A);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
