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
 * Creation date: June 11, 2012
 * 
 **/
public enum Side {
    Unknown(Character.MIN_VALUE),
    Buy(quickfix.field.Side.BUY),
    Sell(quickfix.field.Side.SELL),
    BuyMinus(quickfix.field.Side.BUY_MINUS),
    SellPlus(quickfix.field.Side.SELL_PLUS),
    SellShort(quickfix.field.Side.SELL_SHORT),
    SellShortExempt(quickfix.field.Side.SELL_SHORT_EXEMPT),
    Undisclosed(quickfix.field.Side.UNDISCLOSED),
    Cross(quickfix.field.Side.CROSS),
    CrossShort(quickfix.field.Side.CROSS_SHORT),
    ;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static Side getInstanceForFIXValue(char inValue) {
        Side side = mFIXValueTable.get(inValue);
        return side == null
                ? Unknown
                : side;
    }

    private Side(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, Side> mFIXValueTable;
    static {
        Map<Character, Side> table = new HashMap<Character, Side>();
        for(Side side: values()) {
            table.put(side.getFIXValue(), side);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
