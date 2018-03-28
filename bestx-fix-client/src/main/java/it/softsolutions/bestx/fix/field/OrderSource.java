/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
 * Creation date: 25/gen/2013 
 * 
 **/
public enum OrderSource {
    Unknown(Character.MIN_VALUE),
    AuthorizedDirectMember('1'),
    InstitutionalClientInterconnected('3'),
    PrivateClientInterconnected('7'),
    BranchOfBank('8'),
    OnlineRetailTrading('9'),
    ;
    
    public static final int FIELD = 30004;
    
    public char getFIXValue() {
        return mFIXValue;
    }

    public static OrderSource getInstanceForFIXValue(char inValue) {
        OrderSource orderSource = mFIXValueTable.get(inValue);
        return orderSource == null
                ? Unknown
                : orderSource;
    }

    private OrderSource(char inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final char mFIXValue;
    private static final Map<Character, OrderSource> mFIXValueTable;
    static {
        Map<Character, OrderSource> table = new HashMap<Character, OrderSource>();
        for(OrderSource orderSource: values()) {
            table.put(orderSource.getFIXValue(), orderSource);
        }
        mFIXValueTable = Collections.unmodifiableMap(table);
    }
}
