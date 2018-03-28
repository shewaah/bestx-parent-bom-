/*
 * Copyright 1997-2012 SoftSolutions! srl 
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
package it.softsolutions.bestx.connections.mts;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.regulated.CancelRequestOutputBean;
import it.softsolutions.bestx.model.Instrument;

import java.math.BigDecimal;
import java.util.Date;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: anna.cochetti
 * Creation date: 13-nov-2012 
 * 
 **/

public interface MTSConnection extends Connection
{
    void acceptQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException;
    void rejectQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException;
    void sendMemberTradeOnRequest();
    void sendUserTradeOnRequest();


    /**
     * The Enum TradingRelationStatus.
     */
    public static enum TradingRelationStatus {
        PROCESSING("Processing"),
        ACCEPTED("Accepted"),
        REVOKED("Revoked"),
        REFUSED("Refused"),
        UNKNOWN("Unknown");

        String strValue ;

        TradingRelationStatus(String strValue) {
            this.strValue = strValue;
        }

        /**
         * Gets the str value.
         *
         * @return the str value
         */
        public String getStrValue() {
            return strValue;
        }
        static TradingRelationStatus getTradingRelationStatus(int i)
        {
            switch(i)
            {
            case 0:
                return PROCESSING;
            case 1:
                return ACCEPTED;
            case 2:
                return REVOKED;
            case 3:
                return REFUSED;
            default:
                return UNKNOWN;
            }
        }
    }

    /**
     * The Enum TradingRelationEvent.
     */
    public static enum TradingRelationEvent {
        ACCEPT("Accept"),
        REFUSE("Refuse"),
        REVOKE("Revoke"),
        RESET("Reset"),
        UNKNOWN("Unknown");

        String strValue;

        /**
         * Gets the str value.
         *
         * @return the str value
         */
        public String getStrValue() {
            return strValue;
        }

        TradingRelationEvent(String strValue)
        {
            this.strValue = strValue;
        }
        static TradingRelationEvent getTradingEvent(int i)
        {
            switch(i)
            {
            case 0:
                return ACCEPT;
            case 1:
                return REFUSE;
            case 2:
                return REVOKE;
            case 3:
                return RESET;
            default:
                return UNKNOWN;
            }
        }
    }

    /**
     * The Enum TradingRelationExceptionStatus.
     */
    public static enum TradingRelationExceptionStatus {
        ACTIVE("Active"),
        DELETED("Deleted"),
        UNKNOWN("Unknown");

        String strValue ;

        TradingRelationExceptionStatus(String strValue) {
            this.strValue = strValue;
        }

        /**
         * Gets the str value.
         *
         * @return the str value
         */
        public String getStrValue() {
            return strValue;
        }
        static TradingRelationExceptionStatus getTradingRelationExceptionStatus(int i)
        {
            switch(i)
            {
            case 0:
                return ACTIVE;
            case 1:
                return DELETED;
            default:
                return UNKNOWN;
            }
        }
    }

    /**
     * The Enum TradingRelationExceptionEvent.
     */
    public static enum TradingRelationExceptionEvent {
        INSERT("Insert"),
        DELETE("Delete"),
        UNKNOWN("Unknown");

        String strValue ;

        TradingRelationExceptionEvent(String strValue) {
            this.strValue = strValue;
        }

        /**
         * Gets the str value.
         *
         * @return the str value
         */
        public String getStrValue() {
            return strValue;
        }
        static TradingRelationExceptionEvent getTradingRelationExceptionEvent(int i)
        {
            switch(i)
            {
            case 0:
                return INSERT;
            case 1:
                return DELETE;
            default:
                return UNKNOWN;
            }
        }
    }

    class TradingRelation {
        private final TradingRelationStatus status;
        private final TradingRelationStatus sellSideSubStatus;
        private final TradingRelationStatus buySideSubStatus;
        private final TradingRelationEvent event;
        public TradingRelationStatus getStatus()
        {
            return this.status;
        }
        public TradingRelationEvent getEvent()
        {
            return this.event;
        }
        public TradingRelation(TradingRelationStatus status, TradingRelationStatus sellSideSubStatus, TradingRelationStatus buySideSubStatus, TradingRelationEvent event) {
            this.status = status;
            this.sellSideSubStatus = sellSideSubStatus;
            this.buySideSubStatus = buySideSubStatus;
            this.event = event;
        }
        public TradingRelationStatus getSellSideSubStatus()
        {
            return this.sellSideSubStatus;
        }
        public TradingRelationStatus getBuySideSubStatus()
        {
            return this.buySideSubStatus;
        }
        @Override
        public String toString()
        {
            return "TradingRelation: Status = "+
                            this.status +
                            ", SellSideStatus = "+
                            this.sellSideSubStatus +
                            ", BuySideStatus = "+
                            this.buySideSubStatus +
                            ", Event = "+
                            this.event;
        }
    }

    void requestInstrumentPriceSnapshot(String bvSessionId, Instrument instrument, String subMarket, String market)
                    throws BestXException;
    void revokeOrder(CancelRequestOutputBean order) throws BestXException;
    void setMTSConnectionListener(MTSConnectionListener listener);

    /*   class TradingRelationException {
      private final String bondType;
      private final TradingRelationExceptionStatus status;
      private final TradingRelationExceptionEvent event;
      public TradingRelationExceptionStatus getStatus()
      {
         return this.status;
      }
      public TradingRelationExceptionEvent getEvent()
      {
         return this.event;
      }
      public String getBondType()
      {
         return this.bondType;
      }
      public TradingRelationException(String bondType, TradingRelationExceptionStatus status, TradingRelationExceptionEvent event) {
         this.bondType = bondType;
         this.status = status;
         this.event = event;
      }
      @Override
      public String toString()
      {
         return "TradingRelationException: Status = "+
         this.status +
         ", BondType = "+
         this.bondType +
         ", Event = "+
         this.event;
      }
   }*/

}
