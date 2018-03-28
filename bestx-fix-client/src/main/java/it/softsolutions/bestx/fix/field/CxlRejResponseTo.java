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
 * Creation date: Jun 12, 2012 
 * 
 **/
public enum CxlRejResponseTo {
    Unknown(Character.MIN_VALUE),
    OrderCancelRequest(quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REQUEST),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static CxlRejResponseTo getInstanceForFIXValue(char inValue) {
        CxlRejResponseTo cxlRejresponseTo = mFIXValueTable.get(inValue);
        return cxlRejresponseTo == null
                ? Unknown
                : cxlRejresponseTo;
    }

    private CxlRejResponseTo(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, CxlRejResponseTo> mFIXValueTable;
    static {
        Map<Character, CxlRejResponseTo> table = new HashMap<Character, CxlRejResponseTo>();
        for(CxlRejResponseTo cxlRejresponseTo: values()) {
            table.put(cxlRejresponseTo.getFIXValue(), cxlRejresponseTo);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
