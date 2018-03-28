
/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * Purpose: this class is mainly for allowing thread safety of variables previously defined as SimpleDataFormat
 *
 * Project Name : bestxengine-common
 * First created by: anna.cochetti
 * Creation date: 10 nov 2015
 * 
 **/

@Deprecated
public class ThreadSafeDateFormat {
		private SimpleDateFormat formatter = null;
		
		public ThreadSafeDateFormat (String formatStr) {
    		formatter = new SimpleDateFormat(formatStr);
		}
		

	    public final String format(Date date) {
	    	synchronized(formatter) {
	    		return formatter.format(date);
	    	}
		}

		public Date parse(String source) throws ParseException {
			return formatter.parse(source);
		}

		final static Date parse(String dateStr, SimpleDateFormat formatter) throws ParseException {
	    	Date res = null;
	    	if ( dateStr != null && formatter != null) {
	    		synchronized(formatter) {
	    			res = formatter.parse(dateStr);
	    		}
	    	}
	    	return res;
	    }
	    final static String format(Date date, SimpleDateFormat formatter) {
	    	String res = null;
	    	if ( date != null && formatter != null) {
	    		synchronized(formatter) {
	    			res = formatter.format(date);
	    		}
	    	}
	    	return res;
	    }

}
