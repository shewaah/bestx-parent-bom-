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
package it.softsolutions.bestx.finders.db;

import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.dao.HolidaysDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.HolidayFinder;
import it.softsolutions.bestx.model.Holiday;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * 
 * Purpose: finder cimport java.util.List; lass to manage search of Holidays
 * 
 * Project Name : bestxengine-common First created by: stefano.pontillo Creation date: 24/mag/2012
 * 
 **/
public class SetHolidayFinder implements HolidayFinder {

	private HolidaysDao holidayDao;
	private Ehcache cache;

	public void init() throws BestXException {
		if (holidayDao == null) {
			throw new ObjectNotInitializedException("Instrument DAO not set");
		}

		cache = CacheManager.getInstance().getCache("bestx-" + Holiday.class.getName());
		cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
	}

	/**
	 * Set the Holiday DAO
	 * 
	 * @param holidayDao
	 *            The DAO to set
	 */
	public void setHolidayDao(HolidaysDao holidayDao) {
		this.holidayDao = holidayDao;
	}

	@Override
	public boolean isAnHoliday(String currency, String countryCode, Date date) {
		// return holidayDao.isAnHoliday(currency, countryCode, date);

		String key = currency + '#' + countryCode + '#' + date;

		Element element = cache != null ? cache.get(key) : null;
		Boolean result = element != null ? (Boolean) element.getObjectValue() : null;

		if (result == null) {
			result = holidayDao.isAnHoliday(currency, countryCode, date);
			cache.put(new Element(key, result));
		}

		return result;
	}

	@Override
	public boolean isAnHoliday(String currency, Date date) {
		// try in cache
		String key = currency + '#' + date;

		Element element = cache != null ? cache.get(key) : null;
		Boolean result = element != null ? (Boolean) element.getObjectValue() : null;

		// there is no data in cache, get from DB
		if (result == null) {
			result = holidayDao.isAnHoliday(currency, date);
			cache.put(new Element(key, result));
		}

		return result;
	}

	@SuppressWarnings("unchecked")
    @Override
	public List<Holiday> getFilteredHolidays(String currency, String countryCode) {
		// return holidayDao.getFilteredHolidays(currency, countryCode);

		String key = currency + '#' + countryCode;

		Element element = cache != null ? cache.get(key) : null;
		List<Holiday> result = element != null ? (List<Holiday>) element.getObjectValue() : null;

		if (result == null) {
			result = holidayDao.getFilteredHolidays(currency, countryCode);

			if (result != null) {
				cache.put(new Element(key, result));
			}
		}

		return result;
	}
}
