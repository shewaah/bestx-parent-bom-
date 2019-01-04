package it.softsolutions.bestx.pricediscovery;

import it.softsolutions.bestx.mq.messages.MQMessage;

public class PriceDiscoveryResponse extends MQMessage {

   private final String message;
   
   
   public PriceDiscoveryResponse(String messageContent){
      message = messageContent;
   }
   
   @Override
   public String toTextMessage() {
      return message;
   }

}
