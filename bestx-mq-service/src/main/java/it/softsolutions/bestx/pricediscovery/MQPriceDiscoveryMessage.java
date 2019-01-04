package it.softsolutions.bestx.pricediscovery;

import it.softsolutions.bestx.mq.messages.MQMessage;

public class MQPriceDiscoveryMessage extends MQMessage {

   private final String message;
   
   
   public MQPriceDiscoveryMessage(String messageContent){
      message = messageContent;
   }
   
   @Override
   public String toTextMessage() {
      return message;
   }

}
