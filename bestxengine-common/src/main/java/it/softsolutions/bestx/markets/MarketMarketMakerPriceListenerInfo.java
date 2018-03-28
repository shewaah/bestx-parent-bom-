/**
 * 
 */
package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Proposal.ProposalSide;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Stefano
 *
 */
public class MarketMarketMakerPriceListenerInfo extends GenericPriceListenerInfo {

    public Map<MarketMarketMaker, MarketMarketMakerProposalState> missingProposalStates = new HashMap<MarketMarketMaker, MarketMarketMakerProposalState>();

    public static class MarketMarketMakerProposalState {
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
        
        public boolean nothingArrived() {
            return !askArrived && !bidArrived;
        }
    }
	
	public MarketMarketMakerPriceListenerInfo(){
		super();
	}
	
    public String missingMarketMarketMakerCodes() {
        StringBuilder venueBuffer = new StringBuilder();
        Iterator<MarketMarketMaker> mmmIterator = missingProposalStates.keySet().iterator();
        if (mmmIterator.hasNext()) {
            venueBuffer.append(mmmIterator.next().getMarketSpecificCode());
            while (mmmIterator.hasNext()) {
                venueBuffer.append(',');
                venueBuffer.append(mmmIterator.next().getMarketSpecificCode());
            }
        }
        return venueBuffer.toString();
    }
}
