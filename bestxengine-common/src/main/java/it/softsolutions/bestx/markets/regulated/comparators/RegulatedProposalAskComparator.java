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
public class RegulatedProposalAskComparator implements Comparator<RegulatedProposalInputLazyBean> {

	public int compare(RegulatedProposalInputLazyBean o1, RegulatedProposalInputLazyBean o2) {
		return o1.getPrice().compareTo(o2.getPrice());
	}
}
