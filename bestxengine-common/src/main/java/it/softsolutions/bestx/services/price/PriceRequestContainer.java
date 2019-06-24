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
package it.softsolutions.bestx.services.price;

import it.softsolutions.bestx.Operation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceRequestContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceRequestContainer.class);
    
    private static PriceRequestContainer instance = null;
    private Map<Integer, List<Operation>> priceRequestQueues = new ConcurrentHashMap<Integer, List<Operation>>();

    private PriceRequestContainer() {
    }

    public synchronized static PriceRequestContainer getInstance() {
        if (instance == null) {
            instance = new PriceRequestContainer();
        }
        return instance;
    }

    public void addPriceRequestOperation(Integer queueId, Operation operation) {
        List<Operation> requestQueue = null;
        if (!priceRequestQueues.containsKey(queueId)) {
            requestQueue = new CopyOnWriteArrayList<Operation>();
            priceRequestQueues.put(queueId, requestQueue);
        } else {
            requestQueue = priceRequestQueues.get(queueId);
        }
        LOGGER.info("New operation added to the price request queue " + queueId + " : " + operation.getOrder().getFixOrderId());
        requestQueue.add(operation);
    }

    public Operation getOldestOperation(Integer queueId) {
        List<Operation> requestQueue = null;
        if (!priceRequestQueues.containsKey(queueId)) {
            LOGGER.warn("Cannot satisfy a request for an operation to process, no price request queue available for the id " + queueId);
            return null;
        } else {
            requestQueue = priceRequestQueues.get(queueId);
            if (requestQueue.isEmpty()) {
                LOGGER.warn("Cannot satisfy a request for an operation to process, empty queue with id " + queueId);
                return null;
            } else {
                return requestQueue.remove(0);
            }
        }
    }

}
