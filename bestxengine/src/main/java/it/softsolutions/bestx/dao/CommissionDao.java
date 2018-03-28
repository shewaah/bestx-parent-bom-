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
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.model.CommissionRow;
import it.softsolutions.bestx.model.Customer;

import java.util.Collection;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface CommissionDao {

    /**
     * Get Portfolio Commissions by Customer
     * 
     * @param customer
     * @return Collection of <DbPortfolioCommissionRow>
     */
    Collection<DbPortfolioCommissionRow> getPortfolioCommissions(Customer customer);

    /**
     * Get Ticker Coomission by Customer
     * 
     * @param customer
     * @return Collection of <DbTickerCommissionRow>
     */
    Collection<DbTickerCommissionRow> getTickerCommissions(Customer customer);

    public class DbPortfolioCommissionRow {
        CommissionRow commissionRow;
        int portfolioId;
        boolean MonteTitoliIsin;

        /**
         * @return the commissionRow
         */
        public CommissionRow getCommissionRow() {
            return commissionRow;
        }

        /**
         * @param commissionRow
         *            the commissionRow to set
         */
        public void setCommissionRow(CommissionRow commissionRow) {
            this.commissionRow = commissionRow;
        }

        /**
         * @return the portfolioId
         */
        public int getPortfolioId() {
            return portfolioId;
        }

        /**
         * @param portfolioId
         *            the portfolioId to set
         */
        public void setPortfolioId(int portfolioId) {
            this.portfolioId = portfolioId;
        }

        /**
         * @return the monteTitoliIsin
         */
        public boolean isMonteTitoliIsin() {
            return MonteTitoliIsin;
        }

        /**
         * @param monteTitoliIsin
         *            the monteTitoliIsin to set
         */
        public void setMonteTitoliIsin(boolean monteTitoliIsin) {
            MonteTitoliIsin = monteTitoliIsin;
        }
    }

    public class DbTickerCommissionRow {
        CommissionRow commissionRow;
        String ticker;

        /**
         * @return the commissionRow
         */
        public CommissionRow getCommissionRow() {
            return commissionRow;
        }

        /**
         * @param commissionRow
         *            the commissionRow to set
         */
        public void setCommissionRow(CommissionRow commissionRow) {
            this.commissionRow = commissionRow;
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
}
