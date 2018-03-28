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
 * Creation date: Jun 14, 2012 
 * 
 **/
public enum CommType {
    Unknown(Character.MIN_VALUE),
    PerUnit(quickfix.field.CommType.PER_UNIT),
    Percentage(quickfix.field.CommType.PERCENTAGE),
    Absolute(quickfix.field.CommType.ABSOLUTE),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static CommType getInstanceForFIXValue(char inValue) {
        CommType commType = mFIXValueTable.get(inValue);
        return commType == null
                ? Unknown
                : commType;
    }

    private CommType(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, CommType> mFIXValueTable;
    static {
        Map<Character, CommType> table = new HashMap<Character, CommType>();
        for(CommType commType: values()) {
            table.put(commType.getFIXValue(), commType);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
