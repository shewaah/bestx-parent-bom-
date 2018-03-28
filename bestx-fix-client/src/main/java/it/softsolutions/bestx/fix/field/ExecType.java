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
public enum ExecType {
    Unknown(Character.MIN_VALUE),
    New(quickfix.field.ExecType.NEW),
    PartialFill(quickfix.field.ExecType.PARTIAL_FILL),
    Fill(quickfix.field.ExecType.FILL),
    DoneForDay(quickfix.field.ExecType.DONE_FOR_DAY),
    Canceled(quickfix.field.ExecType.CANCELED),
    Replace(quickfix.field.ExecType.REPLACE),
    PendingCancel(quickfix.field.ExecType.PENDING_CANCEL),
    Stopped(quickfix.field.ExecType.STOPPED),
    Rejected(quickfix.field.ExecType.REJECTED),
    Suspended(quickfix.field.ExecType.SUSPENDED),
    PendingNew(quickfix.field.ExecType.PENDING_NEW),
    Calculated(quickfix.field.ExecType.CALCULATED),
    Expired(quickfix.field.ExecType.EXPIRED),
    Restated(quickfix.field.ExecType.RESTATED),
    PendingReplace(quickfix.field.ExecType.PENDING_REPLACE),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static ExecType getInstanceForFIXValue(char inValue) {
        ExecType execType = mFIXValueTable.get(inValue);
        return execType == null
                ? Unknown
                : execType;
    }

    private ExecType(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, ExecType> mFIXValueTable;
    static {
        Map<Character, ExecType> table = new HashMap<Character, ExecType>();
        for(ExecType execType: values()) {
            table.put(execType.getFIXValue(), execType);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
