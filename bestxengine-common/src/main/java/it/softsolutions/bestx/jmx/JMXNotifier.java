/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2009-06-12 12:51:47 $
 * Header       : $Id: JMXNotifier.java,v 1.1 2009-06-12 12:51:47 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/jmx/JMXNotifier.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.jmx;

import javax.management.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

import it.softsolutions.bestx.services.DateService;

public class JMXNotifier implements NotificationPublisherAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMXNotifier.class);
    
    private long eventSeqNo;
    private NotificationPublisher publisher;

    public JMXNotifier() {
        super();
    }

    public synchronized void notifyEvent(String type, String eventMsg) {
        try {
            Notification n = new Notification(type, this, ++eventSeqNo, DateService.currentTimeMillis(), eventMsg);

            if (publisher != null) {
                publisher.sendNotification(n);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to send JMX notification: {}", e.getMessage(), e);
        }
    }

    @Override
	public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
        this.publisher = notificationPublisher;
    }
}