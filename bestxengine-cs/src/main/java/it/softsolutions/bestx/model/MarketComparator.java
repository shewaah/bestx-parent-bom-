package it.softsolutions.bestx.model;

import java.util.Comparator;

/**
 * 
 * Purpose: class comparing markets based on the Market Rank
 * 
 * Project Name : bestxengine
 * First created by: anna.cochetti 
 * Creation date: 23/01/2019
 * 
 **/
public class MarketComparator implements Comparator<Market> {

	@Override
	public int compare(Market o1, Market o2) {
		return o1.getRanking().compareTo(o2.getRanking());
	}

}
