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
public enum SettlmntTyp {
    Unknown(Character.MIN_VALUE),
    Cash(quickfix.field.SettlmntTyp.CASH),
    Regular(quickfix.field.SettlmntTyp.REGULAR),
    Future(quickfix.field.SettlmntTyp.FUTURE),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static SettlmntTyp getInstanceForFIXValue(char inValue) {
        SettlmntTyp settlmntTyp = mFIXValueTable.get(inValue);
        return settlmntTyp == null
                ? Unknown
                : settlmntTyp;
    }

    private SettlmntTyp(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, SettlmntTyp> mFIXValueTable;
    static {
        Map<Character, SettlmntTyp> table = new HashMap<Character, SettlmntTyp>();
        for(SettlmntTyp settlmntTyp: values()) {
            table.put(settlmntTyp.getFIXValue(), settlmntTyp);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}