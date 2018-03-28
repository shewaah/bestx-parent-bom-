/**
 * Copyright (c) 2005 SoftSolutions S.r.l. 
 * All Rights Reserved.
 *
 * THIS SOURCE CODE IS CONFIDENTIAL AND PROPRIETARY 
 * AND MAY NOT BE USED OR DISTRIBUTED WITHOUT THE 
 * WRITTEN PERMISSION OF SOFTSOLUTIONS.
 */
/*
$Author: spontillo $
$Date: 2008-04-01 08:49:21 $
$Header: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/cmf/CmfConnectionEventFields.java,v 1.2 2008-04-01 08:49:21 spontillo Exp $
$Id: CmfConnectionEventFields.java,v 1.2 2008-04-01 08:49:21 spontillo Exp $
$Name: not supported by cvs2svn $
$Revision: 1.2 $
$Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/cmf/CmfConnectionEventFields.java,v $
$State: Exp $
$Log: not supported by cvs2svn $
Revision 1.1  2008/01/28 15:24:29  lsgro
Prima release

Revision 1.1  2007/12/07 16:17:33  lsgro
Introdotto management applicazione

Revision 1.1  2007/11/28 17:07:09  lsgro
CMF connection basso livello

Revision 1.1  2006/12/22 16:47:14  marcello
*** empty log message ***

Revision 1.1  2005/04/07 09:56:59  gianca
After first release and test od order insertion, filters and controls

*/
package it.softsolutions.bestx.connections.cmf;

/**
 * @author Giancarlo Cadei
 * @version $Revision: 1.2 $
 */
public class CmfConnectionEventFields {

    public static final String CMF_OT_CONNECTION_NOTIFY_PDU_NAME="ConnectionEvent";
    public static final String CMF_OT_STATUS="Status";
    public static final int CMF_OT_STATUS_VAL_UP=1;
    public static final int CMF_OT_STATUS_VAL_DOWN=0;
    
}
