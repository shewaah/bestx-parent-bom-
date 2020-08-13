/*
* Copyright 1997-2020 SoftSolutions! srl 
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

package it.softsolutions.bestx.datacollector;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.BaseOperatorConsoleAdapter;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.PriceType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : bestxengine-cs First created by: stefano.pontillo Creation
 * date: 15 lug 2020
 * 
 **/
public class DataCollectorKafkaImpl extends BaseOperatorConsoleAdapter implements DataCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectorKafkaImpl.class);

	private static final int CHECK_PERIOD = 30000;
	
	// Spring properties
	private Resource configFilename;
	private String priceTopic;
	private String bookTopic;
	private String pobexTopic;
	private String monitorTopic;
	private String marketMakerCompositeCodes;

	private Set<String> compositeMarketMakers;

	private Executor executor;

	private DataCollectorKafkaKeyStrategy priceKeyStrategy;
	private DataCollectorKafkaKeyStrategy bookKeyStrategy;
	private DataCollectorKafkaKeyStrategy pobexKeyStrategy;

	// Class internal properties
	private Producer<String, String> kafkaProducer;
	private KafkaConnectionChecker kafkaConnectionChecker;
	private boolean active = false;
	private boolean connected = false;
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");

	private int convertPriceTypeToInt(PriceType priceType) {
		switch (priceType) {
		case PRICE:
			return 1;
		case SPREAD:
			return 6;
		case YIELD:
			return 9;
		case UNIT:
			return 2;
		default:
			return 0;
		}
	}

	public void init() throws BestXException {
		this.compositeMarketMakers = new HashSet<>();

		String[] marketMakerCompositeStrings = marketMakerCompositeCodes.split(",");
		for (String marketMakerCompositeString : marketMakerCompositeStrings) {
			this.compositeMarketMakers.add(marketMakerCompositeString.trim());
		}
	}

	@Override
	public void connect() {
		LOGGER.info("Received request to connect to Datalake service");
		if (!this.connected) {
			try {
				this.active = true;
				Properties props = new Properties();
				props.load(configFilename.getInputStream());
				this.kafkaProducer = new KafkaProducer<>(props);
				if (this.monitorTopic != null) {
					this.connected = false;
					this.kafkaConnectionChecker = new KafkaConnectionChecker();
				} else {
					this.connected = true;
				}
				(new Thread(this.kafkaConnectionChecker)).start();
				LOGGER.info("Request to connect to Datalake service completed successfully");
			} catch (Exception e) {
				this.active = false;
				LOGGER.error("Unable to connect to Kafka datalake service", e);
			}
		} else {
			LOGGER.warn("Connection request will be ignored, already connected");
		}
	}

	@Override
	public void disconnect() {
		LOGGER.info("Request received to disconnect from Datalake Service");
		try {
			if (this.kafkaConnectionChecker != null) {
				this.kafkaConnectionChecker.stop();
			}
			if (this.kafkaProducer != null) {
				this.kafkaProducer.close();
			}
			LOGGER.info("Request received to disconnect from Datalake Service completed successfully");
		} finally {
			this.kafkaProducer = null;
			this.connected = false;
			this.active = false;
		}
	}

	@Override
	public boolean isConnected() {
		return this.active && this.connected;
	}

	@Override
	public void sendBookAndPrices(Operation operation) {
		Attempt currentAttempt = operation.getLastAttempt();
		int attemptNo = operation.getAttemptNo();

		if (active) {
			this.executor.execute(() -> {
				JSONObject message = new JSONObject();
				message.put("isin", operation.getOrder().getInstrument().getIsin());
				message.put("ordID", operation.getOrder().getFixOrderId());
				message.put("attempt", attemptNo);

				SortedBook book = currentAttempt.getSortedBook();

				Map<String, ClassifiedProposal> bidProposals = new HashMap<String, ClassifiedProposal>();
				Map<String, ClassifiedProposal> askProposals = new HashMap<String, ClassifiedProposal>();

				for (ClassifiedProposal prop : book.getBidProposals()) {
					bidProposals.put(prop.getMarket().getMarketCode() + "_"
							+ prop.getMarketMarketMaker().getMarketSpecificCode(), prop);
				}
				for (ClassifiedProposal prop : book.getAskProposals()) {
					askProposals.put(prop.getMarket().getMarketCode() + "_"
							+ prop.getMarketMarketMaker().getMarketSpecificCode(), prop);
				}

				JSONArray jsonMap = new JSONArray();
				Set<String> keyset = new HashSet<>();
				keyset.addAll(askProposals.keySet());
				keyset.addAll(bidProposals.keySet());

				for (String askMmm : keyset) {
					ClassifiedProposal askProp = askProposals.get(askMmm);
					ClassifiedProposal bidProp = bidProposals.get(askMmm);

					JSONObject proposal = new JSONObject(); // Proposal element to be sent in the book JSON message
					JSONObject rawProposal = new JSONObject(); // Raw proposal to be sent as an independent price JSON
																// message

					rawProposal.put("isin", operation.getOrder().getInstrument().getIsin());
					rawProposal.put("ordID", operation.getOrder().getFixOrderId());
					rawProposal.put("attempt", operation.getAttemptNo());

					boolean goodPrice = false;
					if (askProp != null) {
						proposal.element("askPrice", askProp.getPrice().getAmount());
						proposal.element("askQty", askProp.getQty());
						rawProposal.element("askPrice", askProp.getPrice().getAmount());
						rawProposal.element("askQty", askProp.getQty());
						if (BigDecimal.ZERO.compareTo(askProp.getPrice().getAmount()) < 0) {
							goodPrice = true;
						}
					}
					if (bidProp != null) {
						proposal.element("bidPrice", bidProp.getPrice().getAmount());
						proposal.element("bidQty", bidProp.getQty());
						rawProposal.element("bidPrice", bidProp.getPrice().getAmount());
						rawProposal.element("bidQty", bidProp.getQty());
						if (BigDecimal.ZERO.compareTo(bidProp.getPrice().getAmount()) < 0) {
							goodPrice = true;
						}
					}
					if (!goodPrice) {
						continue;
					}

					ClassifiedProposal goodProp = null;
					if (askProp != null && bidProp != null) {
						if (operation.getOrder().getSide() == OrderSide.BUY) {
							goodProp = askProp;
						} else {
							goodProp = bidProp;
						}
					} else if (askProp != null) {
						goodProp = askProp;
					} else if (bidProp != null) {
						goodProp = bidProp;
					} else {
						continue;
					}

					String marketMakerCode = goodProp.getMarketMarketMaker().getMarketMaker().getCode();
					boolean isComposite = this.compositeMarketMakers.contains(marketMakerCode);

					proposal.element("PriceType", this.convertPriceTypeToInt(goodProp.getPriceType()));
					proposal.element("PriceQuality", isComposite ? "CMP" : "IND");
					rawProposal.element("PriceType", this.convertPriceTypeToInt(goodProp.getPriceType()));
					rawProposal.element("PriceQuality", isComposite ? "CMP" : "IND");

					proposal.element("timestamp", df.format(goodProp.getTimestamp()));
					rawProposal.element("timestamp", df.format(goodProp.getTimestamp()));

					proposal.element("market", goodProp.getMarket().getMarketCode());
					proposal.element("marketmaker", marketMakerCode);
					rawProposal.element("market", goodProp.getMarket().getMarketCode());
					rawProposal.element("marketmaker", marketMakerCode);

					if (goodProp.getProposalSubState() != null
							&& goodProp.getProposalSubState() != Proposal.ProposalSubState.NONE) {
						proposal.element("state", goodProp.getProposalSubState().toString());
					} else {
						proposal.element("state", goodProp.getProposalState().toString());
					}
					proposal.element("comment", goodProp.getReason());

					jsonMap.add(proposal);

					LOGGER.trace("Sending message to topic {}: {}", priceTopic, rawProposal.toString());
					kafkaProducer.send(
							new ProducerRecord<String, String>(priceTopic,
									this.priceKeyStrategy.calculateKey(operation, attemptNo), rawProposal.toString()),
							(metadata, exception) -> {
								if (exception != null) {
									connected = false;
									LOGGER.warn("Error while trying to send message: " + message.toString(), exception);
								} else {
									connected = true;
								}
							});

				}
				message.element("prices", jsonMap);

				LOGGER.trace("Sending message to topic {}: {}", bookTopic, message.toString());
				kafkaProducer.send(
						new ProducerRecord<String, String>(bookTopic,
								this.bookKeyStrategy.calculateKey(operation, attemptNo), message.toString()),
						(metadata, exception) -> {
							if (exception != null) {
								connected = false;
								LOGGER.warn("Error while trying to send message: " + message.toString(), exception);
							} else {
								connected = true;
							}
						});
			});
		}
	}

	@Override
	public void sendPobex(Operation operation) {
		Attempt currentAttempt = operation.getLastAttempt();
		int attemptNo = operation.getAttemptNo();
		if (active) {
			this.executor.execute(() -> {

				JSONObject message = new JSONObject();
				message.put("isin", operation.getOrder().getInstrument().getIsin());
				message.put("ordID", operation.getOrder().getFixOrderId());
				message.put("attempt", attemptNo);

				message.element("PriceQuality", "FRM");

				JSONArray jsonMap = new JSONArray();

				ExecutablePrice goodPrice = null;

				for (ExecutablePrice ep : currentAttempt.getExecutablePrices()) {
					if (goodPrice == null) {
						goodPrice = ep;
					}

					JSONObject proposal = new JSONObject(); // Proposal element to be sent in the book JSON message

					if (operation.getOrder().getSide() == OrderSide.BUY) {
						proposal.element("askPrice", ep.getPrice().getAmount());
						if (ep.getQty() != null) {
							proposal.element("askQty", ep.getQty());
						}
					} else {
						proposal.element("bidPrice", ep.getPrice().getAmount());
						if (ep.getQty() != null) {
							proposal.element("bidQty", ep.getQty());
						}
					}

					if (ep.getPriceType() != null) {
						proposal.element("PriceType", this.convertPriceTypeToInt(ep.getPriceType()));
					}

					if (ep.getMarketMarketMaker() != null && ep.getMarketMarketMaker().getMarketMaker() != null) {
						proposal.element("marketmaker", ep.getMarketMarketMaker().getMarketMaker().getCode());
					} else {
						proposal.element("marketmaker", ep.getOriginatorID());
					}

					proposal.element("status", ep.getAuditQuoteState());

					jsonMap.add(proposal);

				}
				message.element("prices", jsonMap);

				if (goodPrice != null) {
					message.element("timestamp", df.format(goodPrice.getTimestamp()));
					message.element("market", goodPrice.getMarket().getMarketCode());

					LOGGER.trace("Sending message to topic {}: {}", pobexTopic, message.toString());
					kafkaProducer.send(
							new ProducerRecord<String, String>(pobexTopic,
									this.pobexKeyStrategy.calculateKey(operation, attemptNo), message.toString()),
							(metadata, exception) -> {
								if (exception != null) {
									connected = false;
									LOGGER.warn("Error while trying to send message: " + message.toString(), exception);
								} else {
									connected = true;
								}
							});
				}
			});
		}
	}

	public Resource getConfigFilename() {
		return configFilename;
	}

	public void setConfigFilename(Resource configFilename) {
		this.configFilename = configFilename;
	}

	public String getPriceTopic() {
		return priceTopic;
	}

	public void setPriceTopic(String priceTopic) {
		this.priceTopic = priceTopic;
	}

	public String getBookTopic() {
		return bookTopic;
	}

	public void setBookTopic(String bookTopic) {
		this.bookTopic = bookTopic;
	}

	public String getPobexTopic() {
		return pobexTopic;
	}

	public void setPobexTopic(String pobexTopic) {
		this.pobexTopic = pobexTopic;
	}

	public String getMarketMakerCompositeCodes() {
		return marketMakerCompositeCodes;
	}

	public String getMonitorTopic() {
		return monitorTopic;
	}

	public void setMonitorTopic(String monitorTopic) {
		this.monitorTopic = monitorTopic;
	}

	public void setMarketMakerCompositeCodes(String marketMakerCompositeCodes) {
		this.marketMakerCompositeCodes = marketMakerCompositeCodes;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public DataCollectorKafkaKeyStrategy getPriceKeyStrategy() {
		return priceKeyStrategy;
	}

	public void setPriceKeyStrategy(DataCollectorKafkaKeyStrategy priceKeyStrategy) {
		this.priceKeyStrategy = priceKeyStrategy;
	}

	public DataCollectorKafkaKeyStrategy getBookKeyStrategy() {
		return bookKeyStrategy;
	}

	public void setBookKeyStrategy(DataCollectorKafkaKeyStrategy bookKeyStrategy) {
		this.bookKeyStrategy = bookKeyStrategy;
	}

	public DataCollectorKafkaKeyStrategy getPobexKeyStrategy() {
		return pobexKeyStrategy;
	}

	public void setPobexKeyStrategy(DataCollectorKafkaKeyStrategy pobexKeyStrategy) {
		this.pobexKeyStrategy = pobexKeyStrategy;
	}

	private class KafkaConnectionChecker implements Runnable {
		private boolean keepChecking = true;

		@Override
		public void run() {
			while (keepChecking) {
				try {
					String messageValue = "HEARTBEAT: " + df.format(new Date());
					ProducerRecord<String, String> rec = new ProducerRecord<>(monitorTopic, messageValue);
					LOGGER.info("Sending heartbeat message... {}", messageValue);
					kafkaProducer.send(rec, (metadata, exception) -> {
						if (exception != null) {
							connected = false;
							LOGGER.warn("Error while trying to send heartbeat message: " + messageValue, exception);
						} else {
							connected = true;
							LOGGER.info("Successfully sent heartbeat message! {}", messageValue);
						}
					});
					Thread.sleep(CHECK_PERIOD);
				} catch (Exception e) {
					LOGGER.error("Error while trying to check connection status: ", e);
					if (keepChecking) connected = false;
				}
			}
			connected = false;
		}
		
		public void stop() {
			this.keepChecking = false;
		}
		
	}
	
	
	
}
