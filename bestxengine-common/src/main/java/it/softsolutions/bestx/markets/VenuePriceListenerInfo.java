/**
 * 
 */
package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Venue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Stefano
 *
 */
public class VenuePriceListenerInfo extends GenericPriceListenerInfo {

    public Map<Venue, VenueProposalState> missingProposalStates = new HashMap<Venue, VenueProposalState>();
    
    public static class VenueProposalState {
        private boolean askArrived;
        private boolean bidArrived;
        
        public void addProposalSide(ProposalSide side) {
            switch (side) {
            case ASK:
                askArrived = true;
                break;
            case BID:
                bidArrived = true;
                break;
            default:
                break;
            }
        }
        
        public boolean bothSidesArrived() {
            return askArrived && bidArrived;
        }
        
        public boolean isSideArrived(ProposalSide side){
            switch (side) {
            case ASK:
                return askArrived;
            case BID:
                return bidArrived;
            default:
                return false;
            }
        }
    }	
	
	public VenuePriceListenerInfo(){
		super();
	}
	
	public String missingVenueCodes() {
        StringBuilder venueBuffer = new StringBuilder();
        Iterator<Venue> venueIterator = missingProposalStates.keySet().iterator();
        if (venueIterator.hasNext()) {
            venueBuffer.append(venueIterator.next().getCode());
            while (venueIterator.hasNext()) {
                venueBuffer.append(',');
                venueBuffer.append(venueIterator.next().getCode());
            }
        }
        return venueBuffer.toString();
    }
}
