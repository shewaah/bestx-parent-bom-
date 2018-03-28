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
package it.softsolutions.bestx.fix;

import java.util.ArrayList;
import java.util.List;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 11, 2012 
 * 
 **/
public enum DataTypes {
    UTCTimestamp("yyyyMMdd-HH:mm:ss.SSS"),
    UTCTimeOnly("HH:mm:ss.SSS"),
    UTCDateOnly("yyyyMMdd"),
    LocalMktDate("yyyyMMdd"),
    TZTimeOnly("HH:mm:ss.SSSZZ"),
    TZTimestamp("yyyyMMdd-HH:mm:ss.SSSZZ"),
    MonthYear("yyyyMM"),
    ;
    
    private DataTypes(String pattern) {
        this.pattern = pattern;
    }
    private final String pattern;
    
    public String getPattern() {
        return pattern;
    }
    
    public static final String[] patterns;
    static {
        List<String> parsePatterns = new ArrayList<String>();
        for (DataTypes dataType : values()) {
            parsePatterns.add(dataType.pattern);
        }
        patterns = parsePatterns.toArray(new String[]{});
    }
    
    /**
     * example use
     * 
     if (expireDate != null) {
            res.set(new ExpireDate(DateFormatUtils.format(expireDate, DataTypes.LocalMktDate.getPattern())));
        }
        
     if (message.isSetExpireDate()) {
            res.expireDate = DateUtils.parseDate(message.getExpireDate().getValue(), DataTypes.patterns);
        }
       
     * 
     */
    
}