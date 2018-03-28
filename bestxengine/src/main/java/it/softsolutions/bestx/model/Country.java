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

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Country {
    private String code;
    private String name;
    private Boolean isOcse;
    private Boolean isUe;
    private Boolean valid262Ocse;

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setOcse(Boolean isOcse) {
        this.isOcse = isOcse;
    }

    public Boolean isOcse() {
        return isOcse;
    }

    public void setUe(Boolean isUe) {
        this.isUe = isUe;
    }

    public Boolean isUe() {
        return isUe;
    }

    /**
     * @return the valid262OcseFlag
     */
    public Boolean isValid262Ocse() {
        return valid262Ocse;
    }

    /**
     * @param valid262OcseFlag
     *            the valid262OcseFlag to set
     */
    public void setValid262Ocse(Boolean valid262Ocse) {
        this.valid262Ocse = valid262Ocse;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Country)) {
            return false;
        }
        return ((Country) o).getCode().equals(this.getCode());
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
