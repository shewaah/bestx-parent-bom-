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
public enum IDSource {
    Unknown("NONE"),
    Cusip(quickfix.field.IDSource.CUSIP),
    IsinNumber(quickfix.field.IDSource.ISIN_NUMBER),
    ;
    
    public String getFIXValue() {
        return mFIXValue;
    }

    public static IDSource getInstanceForFIXValue(String inFIXValue) {
        if(inFIXValue == null) {
            return Unknown;
        }
        IDSource idSource = mFIXValueMap.get(inFIXValue);
        return idSource == null
                ? Unknown
                : idSource;
    }

    private IDSource(String inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final String mFIXValue;
    private static final Map<String, IDSource> mFIXValueMap;
    static {
        Map<String, IDSource> table = new HashMap<String, IDSource>();
        for(IDSource idSource: values()) {
            table.put(idSource.getFIXValue(), idSource);
        }
        mFIXValueMap = Collections.unmodifiableMap(table);
    }
    
}
