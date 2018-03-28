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
public class UEMemberIssuer {
    private String ticker;
    private String issuerIndustry;

    /**
     * @return the issuerIndustry. Can be null if not found as a member of UE
     */
    public String getIssuerIndustry() {
        return issuerIndustry;
    }

    /**
     * @param issuerIndustry
     *            the issuerIndustry to set
     */
    public void setIssuerIndustry(String issuerIndustry) {
        this.issuerIndustry = issuerIndustry;
    }

    /**
     * @return the ticker
     */
    public String getTicker() {
        return ticker;
    }

    /**
     * @param ticker
     *            the ticker to set
     */
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

}
