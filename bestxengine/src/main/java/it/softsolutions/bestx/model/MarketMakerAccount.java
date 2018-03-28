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

import java.io.Serializable;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class MarketMakerAccount implements Serializable {
    private static final long serialVersionUID = 9075910149198087705L;
    
	private String bankCode;
    private MarketMaker marketMaker;
    private String accountCode;

    public String getBankCode() {
		return bankCode;
	}

	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}

    public MarketMaker getMarketMaker() {
        return marketMaker;
    }

    public void setMarketMaker(MarketMaker marketMaker) {
        this.marketMaker = marketMaker;
    }
    
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MarketMakerAccount)) {
            return false;
        }
        return ((MarketMakerAccount) o).marketMaker.equals(marketMaker) && ((MarketMakerAccount) o).getAccountCode().equals(accountCode);
    }

    @Override
    public int hashCode() {
        return marketMaker.hashCode() + accountCode.hashCode();
    }
}
