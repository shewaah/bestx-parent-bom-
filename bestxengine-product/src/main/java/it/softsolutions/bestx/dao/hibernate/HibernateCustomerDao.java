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
package it.softsolutions.bestx.dao.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.CustomerDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.CustomerFilterRow;
import it.softsolutions.bestx.services.ordervalidation.CustomerFilterFactory;
import it.softsolutions.bestx.services.ordervalidation.OrderValidator;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateCustomerDao implements CustomerDao {
    private SessionFactory sessionFactory;
    private CustomerFilterFactory customerFilterFactory;

    /**
     * Set the Filter Factory
     * 
     * @param customerFilterFactory
     *            Costomer Filter Factory
     */
    public void setCustomerFilterFactory(CustomerFilterFactory customerFilterFactory) {
        this.customerFilterFactory = customerFilterFactory;
    }

    /**
     * Set the Hibernate SessionFactory
     * 
     * @param sessionFactory
     *            Hibernate Sessionfactory
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void init() throws BestXException {
        if (sessionFactory == null) {
            throw new ObjectNotInitializedException("Session factory not set");
        }
        if (customerFilterFactory == null) {
            throw new ObjectNotInitializedException("customerFilterFactory property not set");
        }
    }

    /**
     * Return a customer object given its id fix
     * 
     * @param id
     *            fix id of the customer to find
     * @return Customer object of the fix id passed
     * @throws DataAccessException
     *             throws if there is a connection problem to the database
     */
    @Override
	public Customer getCustomerByFixId(String clientCode) throws DataAccessException {
        Customer customer = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            customer = (Customer) session.createQuery("from Customer where fixId = :clientCode").setString("clientCode", clientCode).uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        
        if (customer != null && customer.getCustomerValidationRules() == null) {
            CustomerFilterRow filterRow = customer.getFilterRow();
            if (filterRow != null) {
                List<OrderValidator> customerValidationRules = customerFilterFactory.createFilterList(filterRow, customer); 
                customer.setCustomerValidationRules(customerValidationRules);
            }
        }
        return customer;
    }

    @Override
    public Customer getCustomerBySinfoCodeAndFlag(String sinfoCode, String flag) {
        Customer customer = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            if (flag != null && !flag.isEmpty()) {
                customer = (Customer) session.createQuery("from Customer where sinfoCode = :sinfoCode and clientCode like '%' + :flag")
                        .setString("sinfoCode", sinfoCode)
                        .setString("flag", flag)
                        .uniqueResult();
            } else {
                customer = (Customer) session.createQuery("from Customer where sinfoCode = :sinfoCode and clientCode like '%[^a-z, A-Z]'")
                        .setString("sinfoCode", sinfoCode)
                        .uniqueResult();
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
        
        if (customer != null && customer.getCustomerValidationRules() == null) {
            CustomerFilterRow filterRow = customer.getFilterRow();
            if (filterRow != null) {
                List<OrderValidator> customerValidationRules = customerFilterFactory.createFilterList(filterRow, customer); 
                customer.setCustomerValidationRules(customerValidationRules);
            }
        }
        return customer;
    }
}
