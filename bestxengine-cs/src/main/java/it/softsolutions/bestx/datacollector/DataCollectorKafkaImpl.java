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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.PriceType;
import it.softsolutions.bestx.model.SortedBook;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 15 lug 2020 
* 
**/
public class DataCollectorKafkaImpl extends BaseOperatorConsoleAdapter implements DataCollector {

   private static final Logger LOGGER = LoggerFactory.getLogger(DataCollectorKafkaImpl.class);
   
   //Spring properties
   private Resource configFilename;
   private String priceTopic;
   private String bookTopic;
   private String pobexTopic;
   private String marketMakerCompositeCodes;
   
   private Executor executor;
   
   //Class properties
   private Producer<String, String> kafkaProducer;
   private boolean active = false;
   
   
   public void init() throws BestXException {
      connectKafka();
   }
   
   @Override
   public void connect() {
      if (!active) {
         try {
            connectKafka();
         }
         catch (BestXException e) {
            LOGGER.error("Unable to connect to Kafka datalake service", e);
         }
      }
   }
   
   private void connectKafka() throws BestXException {
      Properties props = new Properties();
      try {
         props.load(configFilename.getInputStream());
         kafkaProducer = new KafkaProducer<>(props);
         active = true;
      }
      catch (IOException e) {
         active = false;
         throw new BestXException("Unable to connect to Kafka service", e);
      }
   }

   @Override
   public void disconnect() {
      kafkaProducer.close();
      kafkaProducer = null;
      active = false;
   }

   @Override
   public boolean isConnected() {
      return active;
   }
   

   @Override
   public void sendPrice(Proposal proposal) {
      

   }

   @Override
   public void sendBook(Operation operation) {
      if (active) {
    	  this.executor.execute(() -> {
    	  
         Attempt currentAttempt = operation.getLastAttempt();
         
         JSONObject message = new JSONObject();
         message.put("isin", operation.getOrder().getInstrument().getIsin());
         message.put("ordID", operation.getOrder().getFixOrderId());
         message.put("attempt", operation.getAttemptNo());
         
         SortedBook book = currentAttempt.getSortedBook();
         
         Map<String, ClassifiedProposal> bidProposals = new HashMap<String, ClassifiedProposal>();
         Map<String, ClassifiedProposal> askProposals = new HashMap<String, ClassifiedProposal>();
         
         for (ClassifiedProposal prop : book.getBidProposals()) {
            bidProposals.put(prop.getMarket().getMarketCode() + "_" + prop.getMarketMarketMaker().getMarketSpecificCode(), prop);
         }
         for (ClassifiedProposal prop : book.getAskProposals()) {
            askProposals.put(prop.getMarket().getMarketCode() + "_" + prop.getMarketMarketMaker().getMarketSpecificCode(), prop);
         }
         
         JSONArray jsonMap = new JSONArray();
         Set<String> keyset = new HashSet<>(); 
         keyset.addAll(askProposals.keySet());
         keyset.addAll(bidProposals.keySet());
         
         for (String askMmm : keyset) {
            ClassifiedProposal askProp = askProposals.get(askMmm);
            ClassifiedProposal bidProp = bidProposals.get(askMmm);
            
            JSONObject proposal = new JSONObject();
            ClassifiedProposal goodProp = null;
            boolean goodPrice = false;
            if (askProp != null) {
               proposal.element("askPrice", askProp.getPrice().getAmount());
               proposal.element("askQty", askProp.getQty());
               goodProp = askProp;
               if (BigDecimal.ZERO.compareTo(askProp.getPrice().getAmount()) < 0) {
                  goodPrice = true;
               }
            }
            if (bidProp != null) {
               proposal.element("bidPrice", bidProp.getPrice().getAmount());
               proposal.element("bidQty", bidProp.getQty());
               goodProp = bidProp; 
               if (BigDecimal.ZERO.compareTo(bidProp.getPrice().getAmount()) < 0) {
                  goodPrice = true;
               }
            }
            if (!goodPrice) {
               continue;
            }
            proposal.element("PriceType", goodProp.getPriceType() == PriceType.PRICE ? 1 : 0);
            proposal.element("PriceQuality", marketMakerCompositeCodes.contains(askMmm) ? "CMP" : "IND");
            
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");
            proposal.element("timestamp", df.format(goodProp.getTimestamp()));
            
            proposal.element("market", goodProp.getMarket().getMarketCode());
            proposal.element("marketmaker", goodProp.getMarketMarketMaker().getMarketSpecificCode());
            
            if (goodProp.getProposalSubState() != null && goodProp.getProposalSubState() != Proposal.ProposalSubState.NONE) {
               proposal.element("state", goodProp.getProposalSubState().toString());
            } else {
               proposal.element("state", goodProp.getProposalState().toString());
            }
            proposal.element("comment", goodProp.getReason());
            
            jsonMap.add(proposal);
         }
         message.element("prices", jsonMap);
         
         kafkaProducer.send(new ProducerRecord<String, String>(bookTopic, operation.getOrder().getInstrument().getIsin(), message.toString()));
    	  }
         );
      }
   }

   @Override
   public void sendPobex() {
      

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

   
   public void setMarketMakerCompositeCodes(String marketMakerCompositeCodes) {
      this.marketMakerCompositeCodes = marketMakerCompositeCodes;
   }

	public Executor getExecutor() {
		return executor;
	}
	
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
   
   
}
