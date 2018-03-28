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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.dao.CustomerDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.model.Customer;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 22/ago/2012 
* 
**/
public class MapCustomerFinder implements CustomerFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapCustomerFinder.class);

    private CustomerDao customerDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (customerDao == null) {
            throw new ObjectNotInitializedException("Customer DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Customer.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setCustomerDao(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    @Override
    public Customer getCustomerByFixId(String fixId) {
        LOGGER.debug("{}", fixId);
//        Customer customer = customerDao.getCustomerByFixId(fixId);
        
        String key = "FI#" + fixId;
        Element element = cache != null ? cache.get(key) : null;
        Customer customer = element != null ? (Customer) element.getObjectValue() : null;
        
        if (customer == null) {
            customer = customerDao.getCustomerByFixId(fixId);
            
            if (customer != null) {
                cache.put(new Element(key, customer));
            }
        }
        
        if (customer == null) {
            LOGGER.error("Customer not found for fixId = {}", fixId);
        }
        return customer;
    }

    @Override
    public Customer getCustomerBySinfoCodeAndFlag(String sinfoCode, String flag) throws BestXException {
        LOGGER.debug("{}, {}", sinfoCode, flag);
//        Customer customer = customerDao.getCustomerBySinfoCodeAndFlag(sinfoCode, flag);
        
        String key = "SC#" + sinfoCode + '_' + flag;
        Element element = cache != null ? cache.get(key) : null;
        Customer customer = element != null ? (Customer) element.getObjectValue() : null;
        
        if (customer == null) {
            customer = customerDao.getCustomerBySinfoCodeAndFlag(sinfoCode, flag);
            
            if (customer != null) {
                cache.put(new Element(key, customer));
            }
        }
        
        if (customer == null) {
            LOGGER.error("Customer not found for sinfoCode = {} and flag = {}", sinfoCode, flag);
        }

        return customer;
    }

}
