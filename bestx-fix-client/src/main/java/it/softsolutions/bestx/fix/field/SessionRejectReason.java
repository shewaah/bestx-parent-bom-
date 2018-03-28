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
 * Creation date: Jun 13, 2012 
 * 
 **/
public enum SessionRejectReason {
    Unknown(Integer.MIN_VALUE),
    InvalidTagNumber(quickfix.field.SessionRejectReason.INVALID_TAG_NUMBER),
    RequiredTagMissing(quickfix.field.SessionRejectReason.REQUIRED_TAG_MISSING),
    TagNotDefinedForThisMessageType(quickfix.field.SessionRejectReason.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE),
    UndefinedTag(quickfix.field.SessionRejectReason.UNDEFINED_TAG),
    TagSpecifiedWithoutAValue(quickfix.field.SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE),
    ValueIsIncorrect(quickfix.field.SessionRejectReason.VALUE_IS_INCORRECT),
    IncorrectDataFormatForValue(quickfix.field.SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE),
    DecryptionProblem(quickfix.field.SessionRejectReason.DECRYPTION_PROBLEM),
    SignatureProblem(quickfix.field.SessionRejectReason.SIGNATURE_PROBLEM),
    CompIDProblem(quickfix.field.SessionRejectReason.COMPID_PROBLEM),
    SendingTimeAccuracyProblem(quickfix.field.SessionRejectReason.SENDINGTIME_ACCURACY_PROBLEM),
    InvalidMsgType(quickfix.field.SessionRejectReason.INVALID_MSGTYPE),
    ;
    
    public int getFIXValue() {
        return mFIXValue;
    }

    public static SessionRejectReason getInstanceForFIXValue(int inValue) {
        SessionRejectReason sessionRejectReason = mFIXValueTable.get(inValue);
        return sessionRejectReason == null
                ? Unknown
                : sessionRejectReason;
    }

    private SessionRejectReason(int inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final int mFIXValue;
    private static final Map<Integer, SessionRejectReason> mFIXValueTable;
    static {
        Map<Integer, SessionRejectReason> table = new HashMap<Integer, SessionRejectReason>();
        for(SessionRejectReason sessionRejectReason: values()) {
            table.put(sessionRejectReason.getFIXValue(), sessionRejectReason);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
