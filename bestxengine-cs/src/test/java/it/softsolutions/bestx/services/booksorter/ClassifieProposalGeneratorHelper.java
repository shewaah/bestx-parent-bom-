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

package it.softsolutions.bestx.services.booksorter;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.Date;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class ClassifieProposalGeneratorHelper {

    public static ClassifiedProposal getProposal(BigDecimal price, ProposalSide side, Market market)
    {
        return getProposal(price, side, ProposalType.TRADEABLE, market);
    }

    public static ClassifiedProposal getProposal(BigDecimal price, ProposalSide side, ProposalType type, Market market)
    {
        Date timestamp = new Date();
        return getProposal(price, side, type, timestamp, market);
    }

    public static ClassifiedProposal getProposal(BigDecimal price, ProposalSide side, ProposalType type, Date timestamp, Market market)
    {
        Venue venue = getNewVenue("MM1", 1);
        return getProposal(price, side, type, timestamp, venue, market);
    }
    
    public static ClassifiedProposal getProposal(BigDecimal price, ProposalSide side, ProposalType type, Date timestamp, Venue venue, Market market)
    {
        ClassifiedProposal proposal = new ClassifiedProposal();
        proposal.setQty(new BigDecimal(10000));
        proposal.setPrice(new Money("EUR", price));
        proposal.setSide(side);
        proposal.setMarket(market);
        proposal.setProposalState(ProposalState.NEW);
        proposal.setType(type);
        proposal.setTimestamp(timestamp);
        proposal.setVenue(venue);

        MarketMarketMaker mmm = new MarketMarketMaker();
        mmm.setMarket(market);
        mmm.setMarketMaker(null);
        mmm.setMarketSpecificCode("mktcode");
        proposal.setMarketMarketMaker(mmm);

        return proposal;
    }

    
    /**
     * Sets a timestamp with a defined offset (hours ago)
     * 1 second is added, otherwise tests can fail if test time is very close to limit.
     *
     * @param proposal the proposal
     * @param hoursAgo the hours ago
     */
/*    public static void setProposalTimestampWithHoursOffset(ClassifiedProposal proposal, int hoursAgo)
    {
        Date hoursAgoDate = new Date();
        long time = hoursAgoDate.getTime();
        hoursAgoDate.setTime(hoursAgoDate.getTime()- hoursAgo * 60 * 60 *1000 + (1*1000));	
        time = hoursAgoDate.getTime();

        proposal.setTimestamp(hoursAgoDate);
    }
*/
    /**
     * Returns a date with daysOffset offset.
     *
     * @param daysOffset the days offset
     * @return the date with days offset
     */
/*    public static Date getDateWithDaysOffset(int daysOffset)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, daysOffset);

        return cal.getTime();
    }
*/
    /**
     * Gets the venue.
     *
     * @param MMCode the mM code
     * @return the venue
     */
    public static Venue getNewVenue(String MMCode, int mmRank)
    {
        Venue venue = new Venue();
        venue.setVenueType(VenueType.MARKET_MAKER);
        MarketMaker mm1 = new MarketMaker();
        mm1.setCode(MMCode);
        mm1.setMarketMarketMakers(null);
        mm1.setRank(mmRank);
        venue.setMarketMaker(mm1);

        return venue;
    }

    /**
     * Gets the order.
     *
     * @param qty the qty
     * @param side the side
     * @return the order
     */
/*    public static Order getOrder(BigDecimal qty, OrderSide side)
    {
        Order order = new Order();
        order.setQty(qty);
        order.setSide(side);

        Instrument instrument = new Instrument();
        instrument.setIsin("IT0000000001");
        order.setInstrument(instrument);

        Customer customer = new Customer();
        customer.setName("customer");
        customer.setCustomerAttributes(new CustomerAttributes());
        order.setCustomer(customer);

        return order;
    }

    /**
     * Verify proposal state.
     *
     * @param classifier the classifier
     * @param proposal the proposal
     * @param order the order
     * @param book the book
     * @param venues the venues
     * @param expectedState the expected state
     * @param expectedRejectMessage the expected reject message
     */
//    public static void verifyProposalState(ProposalClassifier classifier, ClassifiedProposal proposal, Order order, ClassifiedBook book, Set<Venue> venues, ProposalState expectedState, String expectedRejectMessage)
//    {
//        try {
//            ProposalState originalState = proposal.getProposalState();
//
//            classifier.getClassifiedProposal(proposal, order, null /*previousAttempts*/, venues, book /*book*/);
//
//            if ( (expectedState == ProposalState.REJECTED) || (expectedState == ProposalState.DROPPED) ) {
//                assertTrue("Invalid output state: " + proposal.getProposalState().name(), proposal.getProposalState() == expectedState);
//                assertTrue("Invalid discard message [" + proposal.getReason() + "], expected [" + expectedRejectMessage + "]", proposal.getReason().startsWith(expectedRejectMessage));
//            }
//            else {
//                assertTrue("Invalid output state: " + proposal.getProposalState().name(), proposal.getProposalState() == expectedState);
//            }
//        }
//        catch (Throwable th) {
//            fail("unexpected exception: " + th.getMessage() + " - " + th.getClass().getSimpleName());
//        }		
//    }
}
