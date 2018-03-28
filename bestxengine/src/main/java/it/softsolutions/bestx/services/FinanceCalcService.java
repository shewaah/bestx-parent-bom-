package it.softsolutions.bestx.services;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Instrument;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Declares types and methods to query a service providing financial calculations
 * @author lsgro
 *
 */
public interface FinanceCalcService extends Service {
    /**
     * Return type for accrued interest calculation
     * @author lsgro
     *
     */
    public static class AccruedInterest {
        /**
         * The amount of the accrued interest
         */
        public BigDecimal accruedInterestAmount;
        /**
         * The inflation ratio for inflation linked bonds
         */
        public BigDecimal inflationLinkedRatio;
        /**
         * The number of days accrued
         */
        public int accruedDays;
        /**
         * next coupon date         
         */
        public Date nextCouponDate;
        /**
         * Error code for this call. A value != 0 means that the calculation got errors
         */
        public int errorCode;
        /**
         * Error message for this call. It can be null if no error occurred
         */
        public String errorMessage;
    }
    /**
     * Method to calculate the accrued interest for an instrument
     * @param instrument The instrument for which the calculation is performed
     * @param settlementDate The date of interest accrual
     * @return A {@link AccruedInterest} object
     * @throws BestXException In case an error occurs during calculation
     */
    AccruedInterest calculateAccruedInterest(Instrument instrument, Date settlementDate) throws BestXException;
}
