package it.softsolutions.bestx.mq;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**  
*
* Purpose: mq message listener interface.  
*
* Project Name : mq-service 
* First created by: ruggero.rizzo 
* Creation date: 24/gen/2013 
* 
**/
public interface MQMessageListener extends MessageListener {
    
    /**
     * Messages notification
     * 
     * @param message
     *            : the message received
     */
    @Override
    void onMessage(Message message);

    /**
     * Acknowledgement messages notification
     * 
     * @param message
     *            : the ack message received
     * @throws JMSException
     *             if something does not work
     */
    void acknowledge(Message message) throws JMSException;
}
