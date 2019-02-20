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
package it.softsolutions.bestx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class ConfigurableBookClassifier implements BookClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableBookClassifier.class);

    private List<ProposalClassifier> proposalClassifierList;
    private List<ProposalClassifier> bookClassifierList;

    public void setProposalClassifierList(List<ProposalClassifier> proposalClassifierList) {
        this.proposalClassifierList = proposalClassifierList;
    }

    public void setBookClassifierList(List<ProposalClassifier> bookClassifierList) {
        this.bookClassifierList = bookClassifierList;
    }

    /**
     * Apply all proposal classifiers in sequence on every proposal in the book. For every proposal, the classification ends with the first
     * classificator that discards the proposal, i.e. that sets the proposal state to ProposalState.DROPPED or ProposalState.REJECTED. If no
     * classification discards the proposal, all the classifications are repeated.
     * 
     * @see BookClassifier
     */
    public ClassifiedBook getClassifiedBook(Book book, Order order, List<Attempt> previousAttempts, Set<Venue> venues) {
        LOGGER.debug("Getting classified Book");
        this.checkPreRequisites();
        ClassifiedBook classifiedBook = new ClassifiedBook();
        ArrayList<ClassifiedProposal> classifiedAskProposals = new ArrayList<ClassifiedProposal>();
        ArrayList<ClassifiedProposal> classifiedBidProposals = new ArrayList<ClassifiedProposal>();

        // first apply proposal classifiers
        for (Proposal proposal : book.getAskProposals()) {
            classifiedAskProposals.add(this.applyProposalClassifiers(proposal, order, previousAttempts, venues, null, true));
        }
        for (Proposal proposal : book.getBidProposals()) {
            classifiedBidProposals.add(this.applyProposalClassifiers(proposal, order, previousAttempts, venues, null, true));
        }

        // now apply book classifiers
        classifiedBook.setInstrument(book.getInstrument());
        classifiedBook.setAskProposals(classifiedAskProposals);
        classifiedBook.setBidProposals(classifiedBidProposals);
        return this.applyBookClassifiers(classifiedBook, order, previousAttempts, venues);
    }

    /*
     * Actually never used, but must be implemented because the interface imposes it
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.bestexec.BookClassifier#getClassifiedBook(it.softsolutions.bestx.model.Book,
     * it.softsolutions.bestx.model.Rfq.OrderSide, java.math.BigDecimal, java.util.Date, java.util.List, java.util.Set)
     */
    public ClassifiedBook getClassifiedBook(Book book, Rfq.OrderSide orderSide, BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
        checkPreRequisites();
        ClassifiedBook classifiedBook = new ClassifiedBook();
        ArrayList<ClassifiedProposal> classifiedAskProposals = new ArrayList<ClassifiedProposal>();
        ArrayList<ClassifiedProposal> classifiedBidProposals = new ArrayList<ClassifiedProposal>();

        for (Proposal proposal : book.getAskProposals()) {
            classifiedAskProposals.add(this.applyProposalClassifiers(proposal, orderSide, qty, futSettDate, previousAttempts, venues));
        }

        for (Proposal proposal : book.getBidProposals()) {
            classifiedBidProposals.add(this.applyProposalClassifiers(proposal, orderSide, qty, futSettDate, previousAttempts, venues));
        }
        classifiedBook.setInstrument(book.getInstrument());
        classifiedBook.setAskProposals(classifiedAskProposals);
        classifiedBook.setBidProposals(classifiedBidProposals);
        return classifiedBook;
    }

    private ClassifiedProposal applyProposalClassifiers(Proposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book, boolean useProposalClassifiers) {
        ClassifiedProposal classifiedProposal = new ClassifiedProposal();
        classifiedProposal.setValues((ClassifiedProposal) proposal);
        classifiedProposal.setCommissionType(CommissionType.AMOUNT);
        String currency = classifiedProposal.getPrice().getStringCurrency();
        classifiedProposal.setCommission(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setCustomerAdditionalExpenses(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setPriceTelQuel(classifiedProposal.getPrice());

        if (classifiedProposal.getProposalState() != null && classifiedProposal.getProposalState() == Proposal.ProposalState.NEW) {
            classifiedProposal.setProposalState(Proposal.ProposalState.VALID);
        }
        
        List<ProposalClassifier> classifiersList = null;
        if (useProposalClassifiers) {
            classifiersList = this.proposalClassifierList;
        } else {
            classifiersList = this.bookClassifierList;
        }

        LOGGER.debug("Validating proposal {}", classifiedProposal);
        if (classifiedProposal.getProposalState() == Proposal.ProposalState.DROPPED || classifiedProposal.getProposalState() == Proposal.ProposalState.REJECTED) {
            // System.out.println("*** No need to evaluate proposal for book");
            ; // no need to evaluate it, as it has already been discarded
        } else {
            for (ProposalClassifier classifier : classifiersList) {
                classifiedProposal = classifier.getClassifiedProposal(classifiedProposal, order, previousAttempts, venues, book);
                if (classifiedProposal.getProposalState() == Proposal.ProposalState.DROPPED || 
                    classifiedProposal.getProposalState() == Proposal.ProposalState.REJECTED) {
                    break;
                }
            }
        }
        LOGGER.debug("Proposal for order {} after {} validation: {}", (order != null ? order.getFixOrderId() : order), (useProposalClassifiers ? "Proposal" : "Book"), classifiedProposal);
        return classifiedProposal;
    }

    /*
     * Requested due to the implementation of the method
     * 
     * getClassifiedBook(Book book, Rfq.OrderSide orderSide, BigDecimal qty, Date futSettDate,
     * 
     * but because that method is never called then also this one will never be used.
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.bestexec.BookClassifier#getClassifiedBook(it.softsolutions.bestx.model.Book,
     * it.softsolutions.bestx.model.Rfq.OrderSide, java.math.BigDecimal, java.util.Date, java.util.List, java.util.Set)
     */
    private ClassifiedProposal applyProposalClassifiers(Proposal proposal, OrderSide orderside, BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
        ClassifiedProposal classifiedProposal = new ClassifiedProposal();
        classifiedProposal.setValues((ClassifiedProposal) proposal);
        classifiedProposal.setCommissionType(CommissionType.AMOUNT);
        String currency = classifiedProposal.getPrice().getStringCurrency();
        classifiedProposal.setCommission(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setCustomerAdditionalExpenses(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setPriceTelQuel(classifiedProposal.getPrice());
        classifiedProposal.setProposalState(Proposal.ProposalState.VALID);
        LOGGER.debug("Validating proposal {}", classifiedProposal.toString());
        
        for (ProposalClassifier classifier : this.proposalClassifierList) {
            classifiedProposal = classifier.getClassifiedProposal(classifiedProposal, orderside, qty, futSettDate, previousAttempts, venues);
            if (classifiedProposal.getProposalState() == Proposal.ProposalState.DROPPED 
                    || classifiedProposal.getProposalState() == Proposal.ProposalState.REJECTED
                    || classifiedProposal.getType() == Proposal.ProposalType.COUNTER) {
                break;
            }
        }
        return classifiedProposal;
    }

    private ClassifiedBook applyBookClassifiers(ClassifiedBook book, Order order, List<Attempt> previousAttempts, Set<Venue> venues) {
        ClassifiedBook classifiedBook = new ClassifiedBook();
        ArrayList<ClassifiedProposal> classifiedAskProposals = new ArrayList<ClassifiedProposal>();
        ArrayList<ClassifiedProposal> classifiedBidProposals = new ArrayList<ClassifiedProposal>();
        
        for (Proposal proposal : book.getAskProposals()) {
            classifiedAskProposals.add(this.applyProposalClassifiers(proposal, order, previousAttempts, venues, book, false));
        }
        
        for (Proposal proposal : book.getBidProposals()) {
            classifiedBidProposals.add(this.applyProposalClassifiers(proposal, order, previousAttempts, venues, book, false));
        }
        
        classifiedBook.setInstrument(book.getInstrument());
        classifiedBook.setAskProposals(classifiedAskProposals);
        classifiedBook.setBidProposals(classifiedBidProposals);

        String bidPrefix = "";
        String askPrefix = "";
        if (order != null && order.getSide() == OrderSide.BUY) {
            askPrefix = "[INT-TRACE]";
        } else {
            bidPrefix = "[INT-TRACE]";
        }
        for (ClassifiedProposal proposal : classifiedAskProposals) {
            LOGGER.debug("{} Proposal for order {} after validation: {}", askPrefix, (order != null ? order.getFixOrderId() : order), (proposal != null ? proposal.toStringShort() : proposal));
        }
        for (ClassifiedProposal proposal : classifiedBidProposals) {
            LOGGER.debug("{} Proposal for order {} after validation: {}", bidPrefix, (order != null ? order.getFixOrderId() : order), (proposal != null ? proposal.toStringShort() : proposal));
        }

        LOGGER.debug("Book classified, returning it");
        return classifiedBook;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (this.proposalClassifierList == null) {
            throw new ObjectNotInitializedException("Proposal classifier list not set");
        }
        if (this.bookClassifierList == null) {
            throw new ObjectNotInitializedException("Book classifier list not set");
        }
    }
}
