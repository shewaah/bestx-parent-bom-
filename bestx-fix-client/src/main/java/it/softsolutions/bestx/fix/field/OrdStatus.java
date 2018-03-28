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
 * Creation date: Jun 11, 2012 
 * 
 **/
public enum OrdStatus {
    Unknown(Character.MIN_VALUE),
    New(quickfix.field.OrdStatus.NEW),
    PartiallyFilled(quickfix.field.OrdStatus.PARTIALLY_FILLED),
    Filled(quickfix.field.OrdStatus.FILLED),
    DoneForDay(quickfix.field.OrdStatus.DONE_FOR_DAY),
    Canceled(quickfix.field.OrdStatus.CANCELED),
    PendingCancel(quickfix.field.OrdStatus.PENDING_CANCEL),
    Stopped(quickfix.field.OrdStatus.STOPPED),
    Rejected(quickfix.field.OrdStatus.REJECTED),
    Suspended(quickfix.field.OrdStatus.SUSPENDED),
    PendingNew(quickfix.field.OrdStatus.PENDING_NEW),
    Calculated(quickfix.field.OrdStatus.CALCULATED),
    Expired(quickfix.field.OrdStatus.EXPIRED),
    AcceptedForBidding(quickfix.field.OrdStatus.ACCEPTED_FOR_BIDDING),
    PendingReplace(quickfix.field.OrdStatus.PENDING_REPLACE),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static OrdStatus getInstanceForFIXValue(char inValue) {
        OrdStatus ordStatus = mFIXValueTable.get(inValue);
        return ordStatus == null
                ? Unknown
                : ordStatus;
    }

    private OrdStatus(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, OrdStatus> mFIXValueTable;
    static {
        Map<Character, OrdStatus> table = new HashMap<Character, OrdStatus>();
        for(OrdStatus ordStatus: values()) {
            table.put(ordStatus.getFIXValue(), ordStatus);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
