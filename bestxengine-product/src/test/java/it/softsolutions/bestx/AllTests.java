package it.softsolutions.bestx;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import it.softsolutions.bestx.connections.bondvision.BondVisionRFQOutputLazyBeanTest;

@RunWith(Suite.class)
@SuiteClasses({ 
	BondVisionRFQOutputLazyBeanTest.class/*,
	RTFIBSProposalInputLazyBeanTest.class,
	BondVisionMarketTest.class,
	RegulatedMarket_ZeroPriceTest.class, 
	ApplicationMonitorTest.class,
	BloombergProposalInputLazyBeanTest.class,
	SimpleMarketProposalAggregatorTest.class*/
})

public class AllTests {

}
