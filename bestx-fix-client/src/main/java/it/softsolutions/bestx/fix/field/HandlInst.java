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
public enum HandlInst {
    Unknown(Character.MIN_VALUE),
    AutomatedExecutionOrderPrivate(quickfix.field.HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE),
    AutomatedExecutionOrderPublic(quickfix.field.HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC),
    ManualOrder(quickfix.field.HandlInst.MANUAL_ORDER),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static HandlInst getInstanceForFIXValue(char inValue) {
        HandlInst handlInst = mFIXValueTable.get(inValue);
        return handlInst == null
                ? Unknown
                : handlInst;
    }

    private HandlInst(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, HandlInst> mFIXValueTable;
    static {
        Map<Character, HandlInst> table = new HashMap<Character, HandlInst>();
        for(HandlInst handlInst: values()) {
            table.put(handlInst.getFIXValue(), handlInst);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
