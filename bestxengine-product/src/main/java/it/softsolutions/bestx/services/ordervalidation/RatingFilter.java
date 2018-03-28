/**
 * 
 */
package it.softsolutions.bestx.services.ordervalidation;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

/**
 * @author Stefano
 * 
 */
public class RatingFilter implements OrderValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(RatingFilter.class);
	
	private boolean sAndPCheck = false;
	private final HashSet<String> spSet_ = new HashSet<String> ();
	private final HashSet<String>  moodySet_ = new HashSet<String> ();
	private final HashSet<String>  badSet_ = new HashSet<String> ();

	// @Override
	public OrderResult validateOrder(Operation unused, Order order) {
		OrderResultBean result = new OrderResultBean();
		result.setOrder(order);
		result.setValid(true);
		result.setReason("");		

		// Filter is applied only to BUY orders
		if (order.getSide().equals(OrderSide.BUY)) {
	      result.setValid(false);
	      String spRating = order.getInstrument().getSpRating();
	      String moodyRating = order.getInstrument().getMoodyRating();

	      if (sAndPCheck && spRating == null) {
	         LOGGER.info("Instrument SpRating is null."); //$NON-NLS-1$
	         result.setValid(false);
	      } else if (moodyRating == null) {
	         LOGGER.info("Instrument MoodyRating is null."); //$NON-NLS-1$
	         result.setValid(false);
//	         result.setReason(Messages.getString("RatingFilter.1", order.getInstrument().getMoodyRating()));
	      } else {
				boolean spRated = false;

				if (sAndPCheck) {
					if (!badSet_.contains(spRating.trim()))
					{
						spRated = true;
					}
					if (spRated && spSet_.contains(spRating.trim()))
					{
						result.setValid(true);
					}
					else if (!spRated && moodySet_.contains(moodyRating.trim()))
					{
						result.setValid(true);
					}
				} else {
					if (moodySet_.contains(moodyRating.trim()))
					{
						result.setValid(true);
					}
				}
			} 
		}
		if( !result.isValid() ){
			result.setReason(Messages.getString("RatingFilter.0", 
				(order.getInstrument().getMoodyRating() != null ? order.getInstrument().getMoodyRating() : "ND")));
		}
		return result;
	}
	/**
	 * @param validator
	 */
	public RatingFilter() {

		spSet_.add("AAA+"); // maybe this doesn't exists //$NON-NLS-1$
		spSet_.add("AAA"); //$NON-NLS-1$
		spSet_.add("AAA-"); //$NON-NLS-1$
		spSet_.add("AA+"); //$NON-NLS-1$
		spSet_.add("AA"); //$NON-NLS-1$
		spSet_.add("AA-"); //$NON-NLS-1$
		spSet_.add("A+"); //$NON-NLS-1$
		spSet_.add("A"); //$NON-NLS-1$
		spSet_.add("A-"); //$NON-NLS-1$
		spSet_.add("BBB+"); //$NON-NLS-1$
		spSet_.add("BBB"); //$NON-NLS-1$
		spSet_.add("BBB-"); //$NON-NLS-1$

		moodySet_.add("Aaa"); //$NON-NLS-1$
		moodySet_.add("Aaa1"); //$NON-NLS-1$
		moodySet_.add("Aaa2"); //$NON-NLS-1$
		moodySet_.add("Aaa3"); //$NON-NLS-1$
		moodySet_.add("Aa"); //$NON-NLS-1$
		moodySet_.add("Aa1"); //$NON-NLS-1$
		moodySet_.add("Aa2"); //$NON-NLS-1$
		moodySet_.add("Aa3"); //$NON-NLS-1$
		moodySet_.add("A"); //$NON-NLS-1$
		moodySet_.add("A1"); //$NON-NLS-1$
		moodySet_.add("A2"); //$NON-NLS-1$
		moodySet_.add("A3"); //$NON-NLS-1$
		moodySet_.add("Baa"); //$NON-NLS-1$
		moodySet_.add("Baa1"); //$NON-NLS-1$
		moodySet_.add("Baa2"); //$NON-NLS-1$
		moodySet_.add("Baa3"); //$NON-NLS-1$

		
		//checked for S&P
		badSet_.add(""); // maybe this doesn't exists //$NON-NLS-1$
		badSet_.add("N.A."); //$NON-NLS-1$
		badSet_.add("NA"); //$NON-NLS-1$
		badSet_.add("NR"); //$NON-NLS-1$

	}

	/**
	 * @return the sAndPCheck
	 */
	public boolean isSAndPCheck() {
		return sAndPCheck;
	}

	/**
	 * @param andP_check the sAndPCheck to set
	 */
	public void setSAndPCheck(boolean andPCheck) {
		sAndPCheck = andPCheck;
	}

    public boolean isDbNeeded() {
    	return true;
    }
    
    public boolean isInstrumentDbCheck() {
    	return false;
    }
}
