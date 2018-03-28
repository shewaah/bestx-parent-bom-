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
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-01-15 08:59:28 $
 * Header       : $Id: MapFlyingRFQServer.java,v 1.1 2010-01-15 08:59:28 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/services/flyingrfqservice/MapFlyingRFQServer.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.services.flyingrfqservice;

import it.softsolutions.bestx.services.FlyingRFQService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapFlyingRFQServer implements FlyingRFQService {
    
    private Map<String, String> rfqMap = new ConcurrentHashMap<String, String>();

    public String get(String isin) {
        return rfqMap.get(isin);
    }

    public void put(String isin, String rfqId) {
        rfqMap.put(isin, rfqId);
    }

    public void remove(String isin) {
        rfqMap.remove(isin);
    }
}
