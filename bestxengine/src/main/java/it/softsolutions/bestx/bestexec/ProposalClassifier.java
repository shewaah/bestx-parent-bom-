package it.softsolutions.bestx.bestexec;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**
 * This interface represents objects for classification of proposals. Proposals are evaluated based on available information, and given a
 * state. Optionally a reason is given. *
 * 
 * @author lsgro
 * 
 */
public interface ProposalClassifier {

    /**
     * This method performs proposal classification, based on all available information.
     * 
     * @param proposal
     *            The proposal to be classified
     * @param order
     *            the Order for which the Book has been collected
     * @param previousAttempts
     *            The list of previous attempts to close the deal
     * @param venues
     *            The set of all the Venues available to the party which sent the order
     * @return A {@link ClassifiedProposal} object, with proposalState set to ProposalState.VALID, ProposalState.DROPPED or
     *         ProposalState.REJECTED, and optionally the reason set to a meaningful explanation.
     */
    ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book);

    /**
     * This method performs proposal classification, based on all available information.
     * 
     * @param proposal
     *            The proposal to be classified
     * @param orderSide
     *            The side of the Order for which the Book has been collected
     * @param qty
     *            The requested quantity
     * @param futSettDate
     *            The requested future settlement date
     * @param previousAttempts
     *            The list of previous attempts to close the deal
     * @param venues
     *            The set of all the Venues available to the party which sent the order
     * @return A {@link ClassifiedProposal} object, with proposalState set to ProposalState.VALID, ProposalState.DROPPED or
     *         ProposalState.REJECTED, and optionally the reason set to a meaningful explanation.
     */
    ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, OrderSide orderSide, BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues);

    /**
     * This method performs proposal classification, based on all available information.
     * 
     * @param proposal
     *            The proposal to be classified
     * @param order
     *            the Order for which the Book has been collected
     * @param previousAttempts
     *            The list of previous attempts to close the deal
     * @param venues
     *            The set of all the Venues available to the party which sent the order
     * @return A {@link ClassifiedProposal} object, with proposalState set to ProposalState.VALID, ProposalState.DROPPED or
     *         ProposalState.REJECTED, and optionally the reason set to a meaningful explanation.
     */
    ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues);
}
