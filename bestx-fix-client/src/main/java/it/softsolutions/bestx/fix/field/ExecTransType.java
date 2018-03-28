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
public enum ExecTransType {
    Unknown(Character.MIN_VALUE),
    New(quickfix.field.ExecTransType.NEW),
    Cancel(quickfix.field.ExecTransType.CANCEL),
    Correct(quickfix.field.ExecTransType.CORRECT),
    Status(quickfix.field.ExecTransType.STATUS),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static ExecTransType getInstanceForFIXValue(char inValue) {
        ExecTransType execTransType = mFIXValueTable.get(inValue);
        return execTransType == null
                ? Unknown
                : execTransType;
    }

    private ExecTransType(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, ExecTransType> mFIXValueTable;
    static {
        Map<Character, ExecTransType> table = new HashMap<Character, ExecTransType>();
        for(ExecTransType execTransType: values()) {
            table.put(execTransType.getFIXValue(), execTransType);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
