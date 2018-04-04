/**
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
package it.softsolutions.bestx.connections.tradeweb.mds;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.jsscommon.SSLog;
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.client.TradeStacClient;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni Creation date: 09/gen/2015
 * 
 * 
 */
public class MyMDSConnectionImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(MyMDSConnectionImpl.class);

	private MDSConnectionImpl mdsConnection;
	private static Semaphore semaphore = new Semaphore(0);

	private String[] isinCodes = new String[]  
//			{ "XS0463509959", "ES0211845237", "XS0619547838", "XS0473964509", "XS0388249962", "FR0010814459", "XS0275880267", "DE000A1GNAH1", "XS0604462704", "XS0470632646", "USG03762HG25", "XS0190174358", "XS0172546698", "XS0426513387", "FR0010737882", "XS0364908375", "XS0138717441", "XS0503665290", "XS0255817685", "ES0213211107", "XS0464464964", "XS0274375673", "XS0453820366", "XS0275719135", "XS0186652557", "XS0479945353", "XS0068009637", "XS0446381930", "XS0611398008", "XS0439773002", "XS0522408599", "BE0932180103", "XS0268583993", "XS0478931354", "XS0142073419", "XS0604380724", "PTBRIHOM0001", "XS0275937471", "XS0275939683", "GB00B19ZSN13", "XS0263451972", "XS0123682758", "XS0306772699", "XS0213092652", "XS0261579436", "XS0472205300", "XS0050504306", "XS0289513706", "XS0430699008", "FR0010394478", "XS0271528993", "XS0416397338", "XS0273933902", "XS0398990944", "XS0173603969", "XS0372391945", "XS0449155455", "FR0010941484", "FR0011033851", "FR0011034065", "XS0466300257", "DE000CB83CE3", "XS0465601754", "XS0274270908", "FR0000487217", "XS0583495188", "XS0480903466", "XS0595225318", "XS0170485204", "DE000A1C9VQ4", "XS0457848272", "XS0205790214", "XS0275636438", "DE000A0T7J03", "XS0452868788", "DE000A0T5X07", "XS0423048247", "XS0473787025", "XS0473783891", "XS0499449261", "XS0327443627", "USN3033QAU69", "XS0408958683", "XS0241265445", "XS0466148441", "XS0435879605", "FR0000487258", "USF2893TAB29", "USF2893TAC02", "USF2893TAE67", "FR0010961581", "XS0271757832", "XS0438844093", "USL2967VCZ69", "USL2967VED30", "XS0452187320", "XS0306647792", "BE0934924383", "BE0119012905", "XS0163019143", "XS0442348669", "XS0182242247", "XS0458887030", "XS0181013607", "XS0418729934", "XS0248067661", "XS0270195877", "XS0620022128", "XS0458748851", "XS0458749826", "FR0010678185", "XS0272770396", "XS0385688097", "XS0273570241", "XS0350890470", "XS0361336356", "XS0408304995", "XS0229567440", "XS0229561831", "XS0182703743", "XS0474146288", "XS0350820931", "XS0294624373", "XS0288783979", "XS0270349003", "XS0270347304", "XS0340470490", "XS0615238630", "FR0010815464", "FR0010746008", "XS0241194165", "XS0541620901", "XS0456567055", "XS0271758301", "XS0470370932", "XS0120514335", "XS0356452929", "XS0498768315", "XS0526582761", "XS0272401356", "XS0281875483", "XS0548801207", "XS0275432358", "XS0180407602", "XS0413493957", "XS0472569416", "XS0467864160", "XS0481342896", "XS0593062788", "XS0466670345", "XS0298709188", "XS0269885785", "XS0421003665", "XS0582856687", "XS0269436472", "XS0274112076", "XS0275164084", "XS0411850075", "FR0000482770", "FR0010394437", "XS0434974217", "XS0562783034", "XS0204778145", "XS0286155071", "XS0273493311", "XS0441379095", "XS0269774104", "XS0543369184", "XS0481057189", "XS0466117057", "ES0224244063", "USG5825LAA64", "XS0615801742", "XS0270800815", "XS0366102555", "XS0453299173", "XS0608392550", "XS0569255200", "USU6291AAK52", "XS0472503589", "XS0478043291", "XS0363511873", "XS0090254722", "FR0010410068", "XS0282588952", "XS0269903869", "XS0411735482", "XS0201915385", "XS0269183165", "XS0269181979", "XS0494932741", "XS0234964533", "XS0523335395", "XS0458316550", "XS0616431507", "XS0606218021", "FR0010957282", "XS0432072022", "XS0283808797", "XS0140197582", "XS0431150902", "XS0085529757", "XS0282445336", "XS0304159576", "FR0010815472", "XS0545097742", "XS0271070525", "FR0010914408", "XS0473749959", "XS0201065496", "XS0480133338", "XS0271858606", "XS0429467961", "FR0010369587", "XS0399647675", "XS0412842857", "XS0437306904", "XS0417918298", "XS0034981661", "XS0383187720", "XS0469028319", "XS0125077122", "XS0264823567", "DE000A1E8V89", "FR0010989111", "FR0000487886", "XS0587082834", "XS0526755458", "XS0244171236", "XS0362679176", "XS0415065399", "XS0220566383", "XS0451029127", "XS0206407073", "XS0473964764", "XS0421565150", "XS0287290737", "FR0010745976", "XS0548102960", "XS0548102531", "XS0184373925", "XS0486101024", "XS0465576030", "XS0485616758", "XS0414345974", "USG87621AL52", "XS0248395245", "XS0550634355", "XS0557312922", "XS0590171103", "XS0268693743", "XS0578264870", "XS0410303647", "BE6000480606", "US90466MAB54", "XS0472940617", "XS0468542336", "XS0419346977", "XS0142045474", "XS0401891733", "XS0422688019", "FR0010397927", "FR0010474239", "FR0010535567", "FR0010830042", "XS0479869744", "XS0304458051", "XS0202077953", "XS0279211832", "XS0471076876", "XS0497976133", "XS0438200361", "XS0261559594", "XS0273766732", "XS0275769403", "XS0273815026", "XS0276684700", "XS0568142482", "XS0496999219", "XS0496975110", "XS0466169876", "XS0140144204", "XS0093004736", "XS0439818039", "XS0302054050" };
//			{ "FR0010163543", "FR0010517417", "FR0011619436", "FR0011461037", "FR0118462128", "FR0120746609" };
			
//			{ "XS0416397338" };
			
			{ "XS0463509959", "ES0211845237", "XS0619547838", "XS0473964509", "XS0388249962", "FR0010814459", "XS0275880267", "DE000A1GNAH1", "XS0604462704", "XS0470632646", "USG03762HG25", "XS0190174358", "XS0172546698", "XS0426513387", "FR0010737882", "XS0364908375", "XS0138717441", "XS0503665290", "XS0255817685", "ES0213211107", "XS0464464964", "XS0274375673", "XS0453820366", "XS0275719135", "XS0186652557", "XS0479945353", "XS0068009637", "XS0446381930", "XS0611398008",
			"DE000A0GMHG2", "DE000A1ALVC5", "FR0010853226", "FR0010952770", "XS0169888558", "XS0197646218", "XS0203712939", "XS0205790214", "XS0207065110",
	        "XS0210314299", "XS0210629522", "XS0211034466", "XS0212170939", "XS0212249014", "XS0212401920", "XS0212420987", "XS0213101230", "XS0214318007", "XS0214965534", "XS0215093534",
	        "XS0215153296", "XS0225369403", "XS0451037062", "XS0544720641" };

	public void init() throws Exception {
		Messages messages = new Messages();
		messages.setBundleName("messages");
		messages.setLanguage("it");
		messages.setCountry("IT");

		SSLog.init("SSLog.properties");

		// final Semaphore semaphore = new Semaphore(0);

		// Initialize finders using Spring
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cs-spring.xml");

		InstrumentFinder instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
		MarketMakerFinder marketMakerFinder = (MarketMakerFinder) context.getBean("dbBasedMarketMakerFinder");
		VenueFinder venueFinder = (VenueFinder) context.getBean("dbBasedVenueFinder");
		MarketFinder marketFinder = (MarketFinder) context.getBean("dbBasedMarketFinder");
		Executor executor = (Executor) context.getBean("threadPool.TradewebMarket");

		TradeStacClient tradestacClient = new TradeStacClient();

		TradeStacPreTradeConnectionListener mdsConnectionListener = new TradeStacPreTradeConnectionListener() {

			@Override
			public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
				LOGGER.debug("[{}] {}", connectionName, connectionStatus);
				semaphore.release();
			}

			@Override
			public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
				LOGGER.debug("[{}] {}", connectionName, connectionStatus);
				// semaphore.release();
			}

			@Override
			public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal) {
				LOGGER.debug("{}, {}, {}", instrument, askClassifiedProposal, bidClassifiedProposal);

				if (askClassifiedProposal.getProposalState() != ProposalState.REJECTED) {
					System.out.println(String.format("%s@%s [%s] %s - %s || %s - %s", instrument.getIsin(), bidClassifiedProposal.getMarketMarketMaker(), 
							askClassifiedProposal.getProposalState(), 
							bidClassifiedProposal.getQty(), bidClassifiedProposal.getPrice().getAmount(), 
							askClassifiedProposal.getPrice().getAmount(), askClassifiedProposal.getQty())); 
				} else {
					System.out.println(String.format("%s@%s [%s - %s] %s", instrument.getIsin(), bidClassifiedProposal.getMarketMarketMaker(),
							askClassifiedProposal.getProposalState(), askClassifiedProposal.getProposalSubState(), 
							bidClassifiedProposal.getReason()));
				}
			}

            @Override
            public void onSecurityListCompleted(Set<String> securityList) {
                // TODO Auto-generated method stub
                
            }
		};

		mdsConnection = new MDSConnectionImpl();
		mdsConnection.setFixConfigFileName("fix-tradestac-mds.properties");
		mdsConnection.setTradeStacClientSession(tradestacClient);
		mdsConnection.setInstrumentFinder(instrumentFinder);
		mdsConnection.setMarketMakerFinder(marketMakerFinder);
		mdsConnection.setVenueFinder(venueFinder);
		mdsConnection.setMarketFinder(marketFinder);
		mdsConnection.setExecutor(executor);
		mdsConnection.setTradeStacPreTradeConnectionListener(mdsConnectionListener);
		mdsConnection.init();
		mdsConnection.connect();
		semaphore.acquire();

	}

	public void run() throws BestXException, InterruptedException {

		mdsConnection.requestInstrumentStatus();

		new Thread() {

			@Override
			public void run() {
				try {

					for (String isin : isinCodes) {
						Thread.sleep(3000);
	                    
	//					String isin = "DE000A0GMHG2";
						List<String> marketMakerCodes = new ArrayList<>();
						marketMakerCodes.add("DLRX");
						marketMakerCodes.add("DLRY");
						marketMakerCodes.add("DLRZ");
						marketMakerCodes.add("DLRW");
	//					marketMakerCodes.add("ALL");
	
						Instrument instrument = new Instrument();
						instrument.setIsin(isin);
						instrument.setCurrency("EUR");
						mdsConnection.requestInstrumentPriceSnapshot(instrument, marketMakerCodes);
					}
				} catch (Exception e) {
					LOGGER.error("{}", e.getMessage(), e);
				}
			}
		}.start();

		do {
			TimeUnit.SECONDS.sleep(5000);
		} while (true);
	}

	public static void main(String[] args) {
		MyMDSConnectionImpl myMDSConnectionImpl = new MyMDSConnectionImpl();
		try {
			myMDSConnectionImpl.init();
			myMDSConnectionImpl.run();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("{}", e.getMessage(), e);
		}

	}
}
