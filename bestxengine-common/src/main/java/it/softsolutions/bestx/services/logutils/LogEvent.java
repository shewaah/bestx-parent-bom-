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

import it.softsolutions.bestx.services.DateService;

public class LogEvent {
    private long time_ = DateService.currentTimeMillis();
    private long timestamp_ = System.nanoTime();
    private String uid_ = null;
    private String parentEventUID_ = null;
    private String topicName_ = null;
    private String type_ = null;

    public LogEvent(String eventUID, String parentEventUID, String topicName, String type) {
        this.parentEventUID_ = parentEventUID;
        this.uid_ = eventUID;
        this.topicName_ = topicName;
        this.type_ = type;
    }

    public long getTime() {
        return time_;
    }

    public long getTimestamp() {
        return timestamp_;
    }

    public String getUID() {
        return uid_;
    }

    public String getParentEvent() {
        return parentEventUID_;
    }

    public void setParentEvent(String value) {
        parentEventUID_ = value;
    }

    public void resetUID() {
        uid_ = null;
    }

    public String getEventTopicName() {
        return topicName_;
    }

    public String getType() {
        return type_;
    }
}
