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
package it.softsolutions.bestx.connections.xt2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 12/ott/2012
 * 
 **/
@Deprecated
public enum XT2DataTypes {
    DateOnly("yyyyMMdd"), 
    TimeOnly("HHmmssSSS"),
    DateTime("yyyyMMddHHmmss"),
    Timestamp("yyyyMMdd-HH:mm:ss"),
;

    private XT2DataTypes(String pattern) {
        this.pattern = pattern;
        this.simpleDateFormat = new SimpleDateFormat(pattern);
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final String pattern;
    private final SimpleDateFormat simpleDateFormat;
    
    public String getPattern() {
        return pattern;
    }

    public static final String[] patterns;
    static {
        List<String> parsePatterns = new ArrayList<String>();
        for (XT2DataTypes dataType : values()) {
            parsePatterns.add(dataType.pattern);
        }
        patterns = parsePatterns.toArray(new String[] {});
    }
    
    public Date parseDate(String source) throws ParseException {
        return simpleDateFormat.parse(source);
    }

    public String format(Date date) {
        return simpleDateFormat.format(date);
    }
}