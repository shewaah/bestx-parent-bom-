package it.softsolutions.bestx;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;

public class SimpleProposalClassifier implements ProposalClassifier {
   
	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, OrderSide orderSide, BigDecimal qty,
            Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
        ClassifiedProposal classifiedProposal = new ClassifiedProposal();
        classifiedProposal.setValues(proposal);
        classifiedProposal.setCommissionType(CommissionType.AMOUNT);
        String currency = classifiedProposal.getPrice().getStringCurrency();
        classifiedProposal.setCommission(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setCustomerAdditionalExpenses(new Money(currency, BigDecimal.ZERO));
        classifiedProposal.setPriceTelQuel(classifiedProposal.getPrice());
        classifiedProposal.setProposalState(Proposal.ProposalState.VALID);
        if (orderSide == Rfq.OrderSide.BUY && proposal.getSide() == Proposal.ProposalSide.ASK || orderSide == Rfq.OrderSide.SELL && proposal.getSide() == Proposal.ProposalSide.BID) {
            if (proposal.getQty().compareTo(qty) < 0) {
                classifiedProposal.setProposalState(Proposal.ProposalState.REJECTED);
                classifiedProposal.setReason("Available quantity not sufficient");
            } else {
                classifiedProposal.setQty(qty);
            }
        }
        return classifiedProposal;
    }
	
	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order,
            List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassifiedProposal getClassifiedProposal(
			ClassifiedProposal proposal, Order order,
			List<Attempt> previousAttempts, Set<Venue> venues) {
		throw new UnsupportedOperationException();
	}
}
