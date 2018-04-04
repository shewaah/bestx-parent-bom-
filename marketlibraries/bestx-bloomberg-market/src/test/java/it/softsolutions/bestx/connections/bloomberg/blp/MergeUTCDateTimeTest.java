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
package it.softsolutions.bestx.connections.bloomberg.blp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: davide.rossoni 
 * Creation date: 17/ott/2012 
 * 
 **/
public class MergeUTCDateTimeTest {

    private Date todayDate = new Date();
    private Date utcTime1970 = DateTimeFormat.forPattern("HH:mm:ss.SSS").parseDateTime("15:23:55.123").toDate();
    private Date localTime = DateTimeFormat.forPattern("HH:mm:ss.SSS").parseDateTime("14:23:55.123").toDate();
    private Date midnightTime = DateTimeFormat.forPattern("HH:mm:ss.SSS").parseDateTime("00:00:00.000").toDate();
    
    private Date yesterdayDate = DateUtils.addDays(new Date(), -1);
    private Date yesterdayUtcTime1970 = DateTimeFormat.forPattern("HH:mm:ss.SSS").parseDateTime("23:58:55.123").toDate();
    private Date yesterdayLocalTime = DateTimeFormat.forPattern("HH:mm:ss.SSS").parseDateTime("00:58:55.123").toDate();
    
    private DateTimeComparator dayOnlyComparator = DateTimeComparator.getDateOnlyInstance();
    private DateTimeComparator timeOnlyComparator = DateTimeComparator.getTimeOnlyInstance();

    @Test
    public void convertUTCToLocalNull() {
        Date res = BLPHelper.convertUTCToLocal(null, null);
        assertTrue(DateUtils.isSameDay(res, new Date()));
    }
    
    @Test
    public void convertUTCToLocalDate() {
        Date res = BLPHelper.convertUTCToLocal(new Date(), null);
        assertEquals(0, dayOnlyComparator.compare(res, todayDate));
        assertEquals(0, timeOnlyComparator.compare(res, midnightTime));
    }
    
//    @Test
//    public void convertUTCToLocalTime() {
//        Date res = BLPHelper.convertUTCToLocal(null, utcTime1970);
//        assertEquals(0, dayOnlyComparator.compare(res, todayDate));
//        assertEquals(0, timeOnlyComparator.compare(res, localTime));
//    }
//    
//    @Test
//    public void convertUTCToLocalDateTime() {
//        Date res = BLPHelper.convertUTCToLocal(todayDate, utcTime1970);
//        assertEquals(0, dayOnlyComparator.compare(res, todayDate));
//        assertEquals(0, timeOnlyComparator.compare(res, localTime));
//    }
//    
//    @Test
//    public void convertUTCToLocalDateYesterday() {
//        Date res = BLPHelper.convertUTCToLocal(yesterdayDate, utcTime1970);
//        assertEquals(0, dayOnlyComparator.compare(res, yesterdayDate));
//        assertEquals(0, timeOnlyComparator.compare(res, localTime));
//    }
//    
//    @Test
//    public void convertUTCToLocalDateYesterday2() {
//        Date res = BLPHelper.convertUTCToLocal(yesterdayDate, yesterdayUtcTime1970);
//        assertEquals(0, dayOnlyComparator.compare(res, todayDate));
//        assertEquals(0, timeOnlyComparator.compare(res, yesterdayLocalTime));
//    }
}
