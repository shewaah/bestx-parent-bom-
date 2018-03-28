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
package it.softsolutions.bestx.model;

import it.softsolutions.bestx.services.ordervalidation.OrderValidator;

import java.math.BigDecimal;
import java.util.List;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Customer {
    
    private String fixId;
    private String name;
    private String bbgName;
    private Boolean enabled;
    private BigDecimal maxOrderSize;
    private CustomerFilterRow filterRow;
    private List<OrderValidator> customerValidationRules;
    private Policy policy;
    private String sinfoCode;
    private boolean autoUnexecEnabled = false;
    private CustomerAttributesIFC customerAttributes;

    public void setFixId(String fixId) {
        this.fixId = fixId;
    }

    public String getFixId() {
        return fixId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setCustomerValidationRules(List<OrderValidator> customerValidationRules) {
        this.customerValidationRules = customerValidationRules;
    }

    public List<OrderValidator> getCustomerValidationRules() {
        return customerValidationRules;
    }

    /**
     * @return the maxOrderSize
     */
    public BigDecimal getMaxOrderSize() {
        return maxOrderSize;
    }

    /**
     * @param maxOrderSize
     *            the maxOrderSize to set
     */
    public void setMaxOrderSize(BigDecimal maxOrderSize) {
        this.maxOrderSize = maxOrderSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Customer)) {
            return false;
        }
        return ((Customer) o).getFixId().equals(this.getFixId());
    }

    @Override
    public int hashCode() {
        return getFixId().hashCode();
    }

    @Override
    public String toString() {
        return getFixId();
    }

    /**
     * @return the filterRow
     */
    public CustomerFilterRow getFilterRow() {
        return filterRow;
    }

    /**
     * @param filterRow
     *            the filterRow to set
     */
    public void setFilterRow(CustomerFilterRow filterRow) {
        this.filterRow = filterRow;
    }

    /**
     * @return the bbgName
     */
    public String getBbgName() {
        return bbgName;
    }

    /**
     * @param bbgName
     *            the bbgName to set
     */
    public void setBbgName(String bbgName) {
        this.bbgName = bbgName;
    }

    /**
     * @return the policy
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * @param policy
     *            the policy to set
     */
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public boolean isAutoUnexecEnabled() {
        return autoUnexecEnabled;
    }

    public void setAutoUnexecEnabled(boolean autoUnexecEnabled) {
        this.autoUnexecEnabled = autoUnexecEnabled;
    }

    public String getSinfoCode() {
        return sinfoCode;
    }

    public void setSinfoCode(String sinfoCode) {
        this.sinfoCode = sinfoCode;
    }

    public CustomerAttributesIFC getCustomerAttributes() {
        return this.customerAttributes;
    }

    public void setCustomerAttributes(CustomerAttributesIFC customerAttributes) {
        this.customerAttributes = customerAttributes;
    }

}
