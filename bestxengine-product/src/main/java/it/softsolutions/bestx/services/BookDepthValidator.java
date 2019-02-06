/*
 * Copyright 1997-2015 SoftSolutions! srl 
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

import java.util.List;

import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;

/**
 *
 * Purpose: this class or classes extending this class may be called in the FSM to validate Book depth depending on custom configuration   
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 17/ago/2015
 * 
 **/

public class BookDepthValidator {

	protected int minimumRequiredBookDepth;

	public BookDepthValidator (int minimumRequiredDepth) {
		this.minimumRequiredBookDepth = minimumRequiredDepth;
	}


	public boolean isBookDepthValid(Attempt currentAttempt, Order order) {
		if (currentAttempt.getSortedBook() == null)
			return false;

		List<ClassifiedProposal> proposals = currentAttempt.getSortedBook().getValidSideProposals(order.getSide());
		return proposals.size() >= minimumRequiredBookDepth;
	}

	public int getMinimumRequiredBookDepth() {
		return this.minimumRequiredBookDepth;
	}

}