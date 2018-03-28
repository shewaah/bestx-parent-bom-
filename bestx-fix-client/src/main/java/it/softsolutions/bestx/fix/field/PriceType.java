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
public enum PriceType {

    Unknown(Integer.MIN_VALUE),
    Percentage(quickfix.field.PriceType.PERCENTAGE),
    PerShare(quickfix.field.PriceType.PER_UNIT),
    FixedAmount(quickfix.field.PriceType.FIXED_AMOUNT),
    ;
    
    public int getFIXValue() {
        return mFIXValue;
    }

    public static PriceType getInstanceForFIXValue(int inValue) {
        PriceType sessionRejectReason = mFIXValueTable.get(inValue);
        return sessionRejectReason == null
                ? Unknown
                : sessionRejectReason;
    }

    private PriceType(int inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final int mFIXValue;
    private static final Map<Integer, PriceType> mFIXValueTable;
    static {
        Map<Integer, PriceType> table = new HashMap<Integer, PriceType>();
        for(PriceType priceType: values()) {
            table.put(priceType.getFIXValue(), priceType);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
