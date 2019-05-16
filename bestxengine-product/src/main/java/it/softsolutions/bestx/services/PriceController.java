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
package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.proposalclassifiers.DiscardWorstPriceProposalClassifier;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this is a service that performs controls on the prices. On creation it will work with the limit price if existing and check it against the best price.
 * 
 * Singleton: the Enum-way In the second edition of his book "Effective Java" Joshua Bloch claims that "a single-element enum type is the best way to implement a singleton"[1] for any Java that
 * supports enums. The use of an enum is very easy to implement and has no drawbacks regarding serializable objects, which have to be circumvented in the other ways.
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 25/mag/2012
 * 
 **/
public enum PriceController {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceController.class);

    /**
     * Check if the maximum deviation has been enabled (i.e. configured value > 0.0)
     * 
     * @param customer
     *            : the customer whose configuration we are checking
     * @return true if enabled, false otherwise
     * @throws BestXException
     *             when some problem has been detected
     */
    public boolean isMaxDeviationEnabled(Customer customer) throws BestXException {

        CustomerAttributes csCustAttr = (CustomerAttributes) customer.getCustomerAttributes();
        if (csCustAttr == null) {
            throw new BestXException("Customer attributes can't be null");
        }

        BigDecimal maximumDeviation = csCustAttr.getLimitPriceOffMarket();
        if (maximumDeviation == null) {
            throw new BestXException("Customer maximum allowed deviation not configured.");
        }

        if (maximumDeviation.doubleValue() < 0.0) {
            throw new BestXException("Customer maximum allowed deviation can't be negative!");
        }

        // customer allowed deviation set to zero, this check is not requested, return
        // a proposal to send the order in Manual Curando
        if (maximumDeviation.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.info("Customer [{}] maximum allowed deviation set to zero, check disabled.", customer.getFixId());
            return false;
        } else {
            LOGGER.debug("Customer [{}] maximum allowed deviation {}, check enabled.", customer.getFixId(), maximumDeviation);
            return true;
        }
    }

    /**
     * Check if the maximum quote spread has been enabled (i.e. configured value > 0.0)
     * 
     * @param customer
     *            : the customer whose configuration we are checking
     * @return true if enabled, false otherwise
     * @throws BestXException
     *             when some problem has been detected
     */
    public boolean isQuoteSpreadEnabled(Customer customer) throws BestXException {

        CustomerAttributes csCustAttr = (CustomerAttributes) customer.getCustomerAttributes();
        if (csCustAttr == null) {
            throw new BestXException("Customer attributes can't be null");
        }

        BigDecimal maximumQuoteSpread = csCustAttr.getWideQuoteSpread();
        if (maximumQuoteSpread == null) {
            throw new BestXException("Customer maximum allowed quote spread not configured.");
        }

        if (maximumQuoteSpread.doubleValue() < 0.0) {
            throw new BestXException("Customer maximum allowed quote spread can't be negative!");
        }

        // customer allowed quote spread set to zero, this check is not requested, return
        if (maximumQuoteSpread.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.info("Customer [{}] maximum allowed quote spread set to zero, check disabled.", customer.getFixId());
            return false;
        } else {
            return true;
        }
    }

    /**
     * Every customer has a percentage configured to find out if, an off-market limit price, can be still considered for manual care by the trader. The percentage is the deviation from the best
     * proposal price that we can tolerate. Order with limit prices with a deviation from the best one greater than this will be rejected.
     * 
     * A percentage of 0 states that this check has been disabled for that customer.
     * 
     * @param orderPrice
     *            : the order limit price
     * @param worstPriceDiscardedProposals
     *            : list of proposal discarded because the price is worse than the order limit one
     * @param customer
     *            : the order's customer
     * 
     * @return a classified proposal is one is found with a price deviation from the limit within the allowed one. Null otherwise
     * @throws BestXException
     *             when some problem has been detected
     */
    public ClassifiedProposal checkMaxDeviation(BigDecimal orderPrice, List<ClassifiedProposal> worstPriceDiscardedProposals, Customer customer) throws BestXException {
        LOGGER.trace("{}, {}", orderPrice, worstPriceDiscardedProposals);
        if (orderPrice == null || worstPriceDiscardedProposals == null || customer == null) {
            throw new IllegalArgumentException("params can't be null");
        }

        if (orderPrice.doubleValue() < 0.0) {
            throw new BestXException("orderPrice can't be negative");
        }

        if (worstPriceDiscardedProposals.isEmpty()) {
            LOGGER.trace("List of proposals with prices worse than the order limit is empty, cannot perform limit price deviation check.");
            return null;
        }

        CustomerAttributes csCustAttr = (CustomerAttributes) customer.getCustomerAttributes();
        if (csCustAttr == null) {
            throw new BestXException("Customer attributes can't be null");
        }

        BigDecimal maximumDeviation = csCustAttr.getLimitPriceOffMarket();
        if (maximumDeviation == null) {
            throw new BestXException("Customer maximum allowed deviation not configured.");
        }

        if (maximumDeviation.doubleValue() < 0.0) {
            LOGGER.error("Customer maximum allowed deviation can't be negative!");
            return worstPriceDiscardedProposals.get(0);
        }

        // customer allowed deviation set to zero, this check is not requested, return
        // a proposal to send the order in Manual Curando
        if (maximumDeviation.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.trace("Customer maximum allowed deviation set to zero, check disabled.");
            return worstPriceDiscardedProposals.get(0);
        }

        ClassifiedProposal validProposal = null;
        for (ClassifiedProposal proposal : worstPriceDiscardedProposals) {
            Money propPrice = proposal.getPrice();
            BigDecimal propPriceAmount = propPrice.getAmount();
            if (propPriceAmount.doubleValue() <= 0) {
                continue;
            }
            double delta = Math.abs(propPriceAmount.doubleValue() - orderPrice.doubleValue());
            // % deviation of the difference between the proposal and limit prices over the best price
            double deviationFromPropPrice = delta / propPriceAmount.doubleValue() * 100;
            if (deviationFromPropPrice < maximumDeviation.doubleValue()) {
                LOGGER.trace("Proposal price deviation from the order limit ({}%) lesser than the customer [{}] configured maximum allowed ({}%)", deviationFromPropPrice, customer.getName(),
                                maximumDeviation);
                validProposal = proposal;
                //save for GUI order sorting
                validProposal.setBestPriceDeviationFromLimit(deviationFromPropPrice);
                break;
            }
        }

        if (validProposal == null) {
            LOGGER.trace("No proposals found with price deviation from the order limit lesser than the customer [{}] configured maximum allowed ({}%)", customer.getName(), maximumDeviation);
        }
        return validProposal;
    }

    /**
     * Calculate the delta between order limit price and proposal price for the best proposal, working only on the proposals not satisfying the order limit (there is a check)
     * The proposals list MUST BE ordered, it comes from a SortedBook
     * @param orderPrice order limit price
     * @param bookProposals list of proposals
     * @param customer customer
     * @return the delta value calculated for the best proposal (which is the first rejected exceeding the limit price)
     * @throws BestXException
     */
    public Double getBestProposalDelta(BigDecimal orderPrice, List<ClassifiedProposal> bookProposals, Customer customer) throws BestXException {
        LOGGER.debug("{}, {}", orderPrice, bookProposals);
        if (orderPrice == null || bookProposals == null || customer == null) {
            throw new IllegalArgumentException("params can't be null");
        }

        if (orderPrice.doubleValue() < 0.0) {
            LOGGER.error("Cannot calculate delta, order price is negative: {}", orderPrice.doubleValue());
            throw new BestXException("orderPrice can't be negative");
        }

        if (bookProposals.isEmpty()) {
            LOGGER.debug("List of proposals with prices worse than the order limit is empty, cannot calculate delta between order limit price and best price.");
            return null;
        }

        Double bestAndDeltaLimit = null;
        
        for (ClassifiedProposal proposal : bookProposals) {
            if (!DiscardWorstPriceProposalClassifier.REJECT_REASON.equals(proposal.getReason())) {
                continue;
            }
            
            Money propPrice = proposal.getPrice();
            BigDecimal propPriceAmount = propPrice.getAmount();
            if (propPriceAmount.doubleValue() <= 0) {
                continue;
            }
            double delta = Math.abs(propPriceAmount.doubleValue() - orderPrice.doubleValue());
            // % deviation of the difference between the proposal and limit prices over the best price
            double deviationFromPropPrice = delta / propPriceAmount.doubleValue() * 100;
            bestAndDeltaLimit = deviationFromPropPrice;
            LOGGER.debug("Delta found calculated from order price {} and best price {}: {}", propPriceAmount.doubleValue(), orderPrice.doubleValue(), bestAndDeltaLimit);
            break;
        }
        if (bestAndDeltaLimit == null) {
            LOGGER.debug("No proposals suited to the delta calculation, set it to zero.");
//            bestAndDeltaLimit = BigDecimal.ZERO.doubleValue();
            bestAndDeltaLimit = null;
        }
        return bestAndDeltaLimit;
    }
    /**
     * As per customer request BXCRESUI-41 we must automatically reject orders when the best found is on Bloomberg. This functionality can be activated/disactivated through Spring config (this will be
     * checked by the caller)
     * 
     * @return true if the order must be rejected, false otherwise
     */
    public boolean rejectOrderWhenBloombergIsBest(ClassifiedBook book, Order order) {
        boolean reject = false;

        if (book == null) {
            throw new IllegalArgumentException("the book cannot be null");
        }

        if (order == null) {
            throw new IllegalArgumentException("the order cannot be null");
        }

        OrderSide orderSide = order.getSide();
        ClassifiedProposal bestProposal = book.getBestProposalBySide(orderSide);

        if (bestProposal != null) {
            Market propMarket = bestProposal.getMarket();
            if (propMarket != null && MarketCode.BLOOMBERG.equals(propMarket.getMarketCode())) {
                LOGGER.info("Order {}, best proposal is on Bloomberg [{}], automatically reject the order.", order.getFixOrderId(), bestProposal);
                reject = true;
            }
        }
        return reject;
    }

    /**
     * Check if the order limit price deviation from the best is compatible with customer requests
     * 
     * @param sortedBook
     *            : the book to check
     * @param order
     *            : customer order
     * @return true if the deviation between the prices is over the customer limit, false otherwise
     * @throws BestXException
     */
    public boolean isDeviationFromLimitOverTheMax(SortedBook sortedBook, Order order) throws BestXException {
       if (sortedBook == null) {
            LOGGER.trace("{}, sortedBook null, no need to check the deviation from the customer configured maximum limit price", order.getFixOrderId());
            // returning false will make everything work as usual
            return false;
        }

        Money orderLimit = order.getLimit();
        if (orderLimit == null) {
            LOGGER.info("Order {} without a limit price, cannot check the deviation between the best proposal and the limit price.");
            return false;
        }

        boolean overTheMax = false;
        List<ClassifiedProposal> offLimitProposals = new ArrayList<ClassifiedProposal>();
        List<ClassifiedProposal> bookProposals = null;

        bookProposals = sortedBook.getSideProposals(order.getSide());
        // find proposals with price worse than the order limit, we are interested only in these because
        // the others have been rejected for reasons that are still valid, we must ignore them.
        for (ClassifiedProposal bookProposal : bookProposals) {
            if (bookProposal.getReason() != null && bookProposal.getReason().equals(DiscardWorstPriceProposalClassifier.REJECT_REASON)) {
                offLimitProposals.add(bookProposal);
            }
        }

        BigDecimal limitPrice = orderLimit.getAmount();
        Customer customer = order.getCustomer();
        ClassifiedProposal firstValidProp = checkMaxDeviation(limitPrice, offLimitProposals, customer);
        // if no valid proposal has been found, the limit price is too far from the market price,
        // the order must be rejected, send the automatic not execution
        if (firstValidProp == null) {
            overTheMax = true;
        } else {
            LOGGER.debug("Found a valid proposal [{}]. The order limit price deviation from the proposal one is within the percentage configured for the customer or the configured value is zero (check disabled).", firstValidProp);
            // used in the GUI to sort orders
            order.setBestPriceDeviationFromLimit(firstValidProp.getBestPriceDeviationFromLimit());
        }
        return overTheMax;
    }
    
    /**
     * Check if the order limit price deviation from the best is compatible with customer requests
     * 
     * @param bestPriceDeviationFromLimit
     *            : the best proposal price deviation from the limit price
     * @param order
     *            : customer order
     * @return true if the deviation between the prices is over the customer limit, false otherwise
     * @throws BestXException
     */
    public boolean isDeviationFromLimitOverTheMax(Double bestPriceDeviationFromLimit, Order order) throws BestXException {
        //[RR20140926] CRSBXTEM-128: the delta can be null, it happens when a limit file order can be executed but BestX 
        //is configured to not execute such orders
        if (bestPriceDeviationFromLimit == null){
            LOGGER.debug("Deviation is null, no need to check it.");
            return false;
        }
        
        Money orderLimit = order.getLimit();
        if (orderLimit == null) {
            LOGGER.info("Order {} without a limit price, cannot check the deviation between the best proposal and the limit price.");
            return false;
        }

        Customer customer = order.getCustomer();
        
        CustomerAttributes csCustAttr = (CustomerAttributes) customer.getCustomerAttributes();
        if (csCustAttr == null) {
            throw new BestXException("Customer attributes can't be null");
        }

        BigDecimal maximumDeviation = csCustAttr.getLimitPriceOffMarket();
        if (maximumDeviation == null) {
            throw new BestXException("Customer maximum allowed deviation not configured.");
        }

        if (maximumDeviation.doubleValue() < 0.0) {
            LOGGER.error("Customer maximum allowed deviation can't be negative! Cannot perform the check, consider delta within the threshold");
            return false;
        }

        // customer allowed deviation set to zero, this check is not requested,
        if (maximumDeviation.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.trace("Customer maximum allowed deviation set to zero, check disabled. Consider delta within the threshold");
            return false;
        }
        
        boolean overTheMax = true;
        if (bestPriceDeviationFromLimit < maximumDeviation.doubleValue()) {
            LOGGER.trace("Proposal price deviation from the order limit ({}%) lesser than the customer [{}] configured maximum allowed ({}%)", bestPriceDeviationFromLimit, customer.getName(),
                            maximumDeviation);
            overTheMax = false;
        }
        return overTheMax;
    }   
}