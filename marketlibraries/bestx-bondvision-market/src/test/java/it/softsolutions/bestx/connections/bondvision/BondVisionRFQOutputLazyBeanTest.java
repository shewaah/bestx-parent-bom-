package it.softsolutions.bestx.connections.bondvision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.xt2.protocol.XT2Msg;

public class BondVisionRFQOutputLazyBeanTest {
	private Messages messages;
	@Before
	public void setUp() throws Exception {
		// initializa Messages
		messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");
	}

	@After
	public void tearDown() throws Exception {
	}
	/*
	@Test
	public void testGetMsg() {
		MarketOrder order = new MarketOrder();
		order.setPriceDiscoveryType(PriceDiscoveryType.NATIVE_PRICEDISCOVERY);
		Instrument inst = new Instrument();
		inst.setIsin("IT0000000001");
		order.setInstrument(inst);
		order.setSide(OrderSide.BUY);
		order.setQty(new BigDecimal(5000000));
		MarketMarketMaker mmm = new MarketMarketMaker();
		mmm.setMarketSpecificCode("23340SS0");
		order.setMarketMarketMaker(mmm);
		order.setFutSettDate(new Date());
		
		BondVisionRFQOutputLazyBean rfq = new BondVisionRFQOutputLazyBean(order, new BigDecimal(1000));
		
		XT2Msg rfqMsg = rfq._getMsg();
		try {
			int bvsOrigin = rfqMsg.getInt("BVSOrigin");	// 0=From Scratch, 1=From Single Dealer, 2=From Inventory
			assertEquals("Expected bvsOrigin = 1", bvsOrigin, 1);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}

		
		order.setPriceDiscoveryType(PriceDiscoveryType.BBG_PRICEDISCOVERY);

		rfq = new BondVisionRFQOutputLazyBean(order, new BigDecimal(1000));
		
		rfqMsg = rfq._getMsg();
		try {
			int bvsOrigin = rfqMsg.getInt("BVSOrigin");	// 0=From Scratch, 1=From Single Dealer, 2=From Inventory
			assertEquals("Expected bvsOrigin = 0", bvsOrigin, 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
		
	}
*/
}
