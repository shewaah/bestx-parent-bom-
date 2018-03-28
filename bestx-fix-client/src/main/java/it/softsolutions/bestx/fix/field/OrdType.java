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
public enum OrdType {
    Unknown(Character.MIN_VALUE),
    Market(quickfix.field.OrdType.MARKET),
    Limit(quickfix.field.OrdType.LIMIT),
    Stop(quickfix.field.OrdType.STOP),
    StopLimit(quickfix.field.OrdType.STOP_LIMIT),
    MarketOnClose(quickfix.field.OrdType.MARKET_ON_CLOSE),
    WithOrWithout(quickfix.field.OrdType.WITH_OR_WITHOUT),
    LimitOrBetter(quickfix.field.OrdType.LIMIT_OR_BETTER),
    LimitWithOrWithout(quickfix.field.OrdType.LIMIT_WITH_OR_WITHOUT),
    OnBasis(quickfix.field.OrdType.ON_BASIS),
    OnClose(quickfix.field.OrdType.ON_CLOSE),
    LimitOnClose(quickfix.field.OrdType.LIMIT_ON_CLOSE),
    ForexMarket(quickfix.field.OrdType.FOREX_MARKET),
    PreviouslyQuoted(quickfix.field.OrdType.PREVIOUSLY_QUOTED),
    PreviouslyIndicated(quickfix.field.OrdType.PREVIOUSLY_INDICATED),
    ForexLimit(quickfix.field.OrdType.FOREX_LIMIT),
    ForexSwap(quickfix.field.OrdType.FOREX_SWAP),
    ForexPreviouslyQuoted(quickfix.field.OrdType.FOREX_PREVIOUSLY_QUOTED),
    Funari(quickfix.field.OrdType.FUNARI),
    MarketIfTouched(quickfix.field.OrdType.MARKET_IF_TOUCHED),
    MarketWithLeftoverAsLimit(quickfix.field.OrdType.MARKET_WITH_LEFTOVER_AS_LIMIT),
    PreviousFundValuationPoint(quickfix.field.OrdType.PREVIOUS_FUND_VALUATION_POINT),
    NextFundValuationPoint(quickfix.field.OrdType.NEXT_FUND_VALUATION_POINT),
    Pegged(quickfix.field.OrdType.PEGGED),
    CounterOrderSelection(quickfix.field.OrdType.COUNTER_ORDER_SELECTION);
   
    public char getFIXValue() {
        return mFIXValue;
    }

    public static OrdType getInstanceForFIXValue(char inValue) {
        OrdType ordType = mFIXValueTable.get(inValue);
        return ordType == null
                ? Unknown
                : ordType;
    }

    private OrdType(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, OrdType> mFIXValueTable;
    static {
        Map<Character, OrdType> table = new HashMap<Character, OrdType>();
        for(OrdType ordType: values()) {
            table.put(ordType.getFIXValue(), ordType);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
