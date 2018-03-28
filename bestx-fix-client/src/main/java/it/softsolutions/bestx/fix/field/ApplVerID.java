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
public enum ApplVerID {
	Unknown("NONE"),
	FIX27(quickfix.field.ApplVerID.FIX27),
	FIX30(quickfix.field.ApplVerID.FIX30),
	FIX40(quickfix.field.ApplVerID.FIX40),
	FIX41(quickfix.field.ApplVerID.FIX41),
	FIX42(quickfix.field.ApplVerID.FIX42),
	FIX43(quickfix.field.ApplVerID.FIX43),
	FIX44(quickfix.field.ApplVerID.FIX44),
	FIX50(quickfix.field.ApplVerID.FIX50),
	FIX50SP1(quickfix.field.ApplVerID.FIX50SP1),
	FIX50SP2(quickfix.field.ApplVerID.FIX50SP2),
	;
    
    public String getFIXValue() {
        return mFIXValue;
    }

    public static ApplVerID getInstanceForFIXValue(String inFIXValue) {
        if(inFIXValue == null) {
            return Unknown;
        }
        ApplVerID applVerID = mFIXValueMap.get(inFIXValue);
        return applVerID == null
                ? Unknown
                : applVerID;
    }

    private ApplVerID(String inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final String mFIXValue;
    private static final Map<String, ApplVerID> mFIXValueMap;
    static {
        Map<String, ApplVerID> table = new HashMap<String, ApplVerID>();
        for(ApplVerID applVerID: values()) {
            table.put(applVerID.getFIXValue(),applVerID);
        }
        mFIXValueMap = Collections.unmodifiableMap(table);
    }
    
}
