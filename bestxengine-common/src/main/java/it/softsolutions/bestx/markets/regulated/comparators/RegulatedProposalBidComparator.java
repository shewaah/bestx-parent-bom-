/**
 * 
 */
package it.softsolutions.bestx.markets.regulated.comparators;

import it.softsolutions.bestx.connections.regulated.RegulatedProposalInputLazyBean;

import java.util.Comparator;

/**
 * @author Stefano
 *
 */
public class RegulatedProposalBidComparator implements Comparator<RegulatedProposalInputLazyBean> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(RegulatedProposalInputLazyBean o1, RegulatedProposalInputLazyBean o2) {
		return o2.getPrice().compareTo(o1.getPrice());
	}
}
