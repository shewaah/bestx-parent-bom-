/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.services.logutils;

import it.softsolutions.bestx.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationStatisticsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStatisticsHelper.class);

    public static String dumpEventStats(LogEvent event) {
        return dumpRawEventStats(event.getEventTopicName(), event.getUID(), event.getParentEvent(), event.getTimestamp(), event.getType(), event.getTime());
    }

    public static String dumpRawEventStats(String topic, String eventUID, String parentEvent, long timestamp, String type, long time) {
        StringBuilder sb = new StringBuilder();
        sb.append("{topic: '").append(topic);
        sb.append("', eventUID:'").append(eventUID);
        sb.append("', parentEvent:'").append(parentEvent);
        sb.append("', timestamp: ").append(timestamp);
        sb.append(", type:'").append(type);
        sb.append("', time:").append(time);
        sb.append(" }");
        return sb.toString();
    }

    public synchronized static void logStringAndUpdateOrderIds(Order order, String topic, String eventType) {
        String fixOrderId = order.getFixOrderId();
        String parentLogId = order.getLogId();
        order.generateNextLogId();
        String logId = order.getLogId();

        LogEvent logEv = new LogEvent(logId, parentLogId, topic + '.' + fixOrderId, eventType);
        LOGGER.info("event.new {}", dumpEventStats(logEv));
    }

    public synchronized static void logRealtimeSystemInfo(String message) {
        LOGGER.info("system.info.realtime {}", message);
    }

    public synchronized static void logPlatformInfo(String message) {
        LOGGER.info("system.info.platform {}", message);
    }

}
