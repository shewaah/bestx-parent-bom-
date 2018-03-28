
/*
 * Copyright 1997-2016 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.services;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 29 nov 2016
 * 
 **/

public class DateService {

	/* formatters*/
    public static String timeISO = "HHmmssSSS";
    public static String dateISO = "yyyyMMdd";
    public static String dateTimeISO = "yyyyMMddHHmmssSSS";
    
    public static String dateFIX = "yyyy-MM-dd";
    public static String timeFIX = "HH:mm:ss.SSS";
    public static String dateTimeFIX = "yyyy-MM-dd HH:mm:ss.SSS";
    public static String dateTimeMixedISOFIX = "yyyyMMdd-HH:mm:ss.SSS";
    
	
	private static DateTimeZone defaultDateTimeZone = DateTimeZone.getDefault();
	
	public static DateTimeZone getDefaultDateTimeZone() {
		return defaultDateTimeZone;
	}

	public static void setDefaultDateTimeZone(DateTimeZone defaultDateTimeZone) {
		DateService.defaultDateTimeZone = defaultDateTimeZone;
	}

	/**
	 * Merges and converts date and time in UTC timezone in a single Date in Default (local) timezone
	 * 
	 * @param date a date in UTC timezone (usually a FIX MDEntryDate)
	 * @param time a time in UTC timezone (usually a FIX MDEntryTime)
	 * @return a Date in Default timezone
	 */
	public static Date convertUTCToLocal(Date date, Date time) {
		Date res = null;

		if (time != null) {
			DateTime utcDate = new DateTime(date, DateTimeZone.UTC);
			DateTime utcTime = new DateTime(time, DateTimeZone.UTC);
			// concatenate date and time and retrieve UTC dateTime
			DateTime utcDateTime = utcDate.toLocalDate().toDateTime(utcTime.toLocalTime());
			// force timeZone to UTC
			utcDateTime = utcDateTime.withZone(DateTimeZone.UTC);

			res = new Date(DateTimeZone.getDefault().convertUTCToLocal(utcDateTime.getMillis()));
		} else {
			// restrict date to localDate in order to avoid 02:00:00 instead of 00:00:00
			res = new LocalDate(date).toDate();
		}
		return res;
	}

	/**
	 * Merges and converts date and time in Default (local) timezone in a single Date to UTC timezone
	 * 
	 * @param date a date in Local timezone (usually a FIX MDEntryDate)
	 * @param time a time in Local timezone (usually a FIX MDEntryTime)
	 * @return a Date in UTC timezone
	 */
	public static Date convertLocalToUTC(Date date, Date time) {
		Date res = null;

		if (time != null) {
			DateTime localDate = new DateTime(date, defaultDateTimeZone);
			DateTime localTime = new DateTime(time, defaultDateTimeZone);
			// concatenate date and time and retrieve local dateTime
			DateTime localDateTime = localDate.toLocalDate().toDateTime(localTime.toLocalTime());

			res = new Date(DateTimeZone.getDefault().convertLocalToUTC(localDateTime.getMillis(), false));
		} else {
			// restrict date to localDate in order to avoid 02:00:00 instead of 00:00:00
			res = new LocalDate(date, DateTimeZone.UTC).toDate();
		}
		return res;
	}

	/** creates a new Date using timezone UTC */
	public static Date newUTCDate() {
		Date nd = new Date();
		DateTime res = new LocalDate(nd, DateTimeZone.UTC).toDateTime(new LocalTime(nd, DateTimeZone.UTC));
		return res.toDate();
	}
	
	/** creates a new Date using Default (local) timezone */
	public static Date newLocalDate() {
		return new Date();
	}
	/** creates a new Date using Default (local) timezone */
	public static Date newLocalDateMidnight() {
		DateTime a = new DateTime();
		Date b = a.toDate();
		b.setTime(b.getTime()-a.getMillisOfDay());
		return b;
		
	}
	
	public static Date convertLocalToUTC(Date date) {
		return new Date(DateTimeZone.getDefault().convertLocalToUTC(date.getTime(), false));
	}
	
	public static Date convertUTCToLocal(Date date) {
		return new Date(DateTimeZone.getDefault().convertUTCToLocal(date.getTime()));
	}
	
	
	public static String formatAsUTC(String pattern, Date date) {
		if (date==null) return "";
		DateTime jodaDate = new DateTime(date, DateTimeZone.UTC);
		return jodaDate.toString(DateTimeFormat.forPattern(pattern));
	}
	
	public static String format(String pattern, Date date) {
		if (date==null) return "";
		DateTime jodaDate = new DateTime(date);
		return jodaDate.toString(DateTimeFormat.forPattern(pattern));
	}
	
	public static Long formatAsLong(String pattern, Date date) throws IllegalArgumentException {
		if (date==null) throw new IllegalArgumentException();
		DateTime jodaDate = new DateTime(date);
		Long ln = null;
		try {
			ln = Long.parseLong(jodaDate.toString(DateTimeFormat.forPattern(pattern)));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
		return ln;
	}
/**
 * parses a local time from the given string str according to the provided pattern
 * @param pattern
 * @param str if null the local system time is returned
 * @return a date
 */
	public static Date parse(String pattern, String str) throws IllegalArgumentException {
		if(str==null) return new Date();
		return DateTimeFormat.forPattern(pattern).parseDateTime(str).toDate();
	}
	
	/**
	 * parses a local time from the given Long ln according to the provided pattern
	 * @param pattern
	 * @param ln if null the local system time is returned
	 * @return a date
	 */
	public static Date parse(String pattern, Long ln) throws IllegalArgumentException {
		if(ln==null) return newLocalDate();
		return DateTimeFormat.forPattern(pattern).parseDateTime(Long.toString(ln)).toDate();
	}

	public static Date composeDate(Date dateOnly, Date timeOnly) {
		DateTime localDate = new DateTime(dateOnly, defaultDateTimeZone);
		DateTime localTime = new DateTime(timeOnly, defaultDateTimeZone);

		DateTime dateTime = localDate.toLocalDate().toDateTime(localTime.toLocalTime());
		Date res = new Date(dateTime.getMillis());
		return res;
	}
	
	public static long currentTimeMillis() {
		return	System.currentTimeMillis();
		}
	}
