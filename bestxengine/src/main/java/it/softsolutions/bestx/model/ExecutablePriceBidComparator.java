package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.Comparator;

import quickfix.field.PriceType;

public class ExecutablePriceBidComparator implements Comparator<ExecutablePrice> {

	@Override
	public int compare(ExecutablePrice o1, ExecutablePrice o2) {
		ClassifiedProposal p1= o1.getClassifiedProposal(), p2 = o2.getClassifiedProposal();
		if(p1.getPriceType() != p2.getPriceType())
			throw new IllegalArgumentException("price type are not the same");
		if(p2.getPrice().getAmount().compareTo(BigDecimal.ZERO) == 0)
			if(p1.getPrice().getAmount().compareTo(BigDecimal.ZERO) == 0)
				return 0;
			else return 1;
		switch(p1.getPriceType()) {
		case PRICE:
		case SPREAD:
			return o2.getClassifiedProposal().getPrice().getAmount().compareTo(o1.getClassifiedProposal().getPrice().getAmount());
		case YIELD:
			return o1.getClassifiedProposal().getPrice().getAmount().compareTo(o2.getClassifiedProposal().getPrice().getAmount());
		default:
			throw new IllegalArgumentException("price type not recognized");
		}
	}
}
