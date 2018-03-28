/**
 * 
 */
package it.softsolutions.bestx.connections.regulated;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_QUANTITY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SIDE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBMARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TIME;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.PREFIX_ASK;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.PREFIX_BID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_SIDE_ASK;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_SIDE_BID;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * @author Stefano
 * 
 */
public class RegulatedProposalInputLazyBean extends XT2InputLazyBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedProposalInputLazyBean.class);
    
//    private SimpleDateFormat dateFmt = new SimpleDateFormat(DateService.dateISO);
    private int index;
    private String prefix; // Deve essere passato xcon il punto finale

    public RegulatedProposalInputLazyBean(XT2Msg msg, int index) {
        this(msg, index, "");
    }

    public RegulatedProposalInputLazyBean(XT2Msg msg, int index, String prefix) {
        this.msg = msg;
        this.index = index;
        this.prefix = prefix;
    }

    public BigDecimal getPrice() {
        try {
            BigDecimal price = new BigDecimal(msg.getDouble(prefix + LABEL_REG_PRICE + "." + index));
            price = price.setScale(5, BigDecimal.ROUND_HALF_UP);
            return price;
        } catch (Exception e) {
            LOGGER.warn("Error while extracting price amount from Price notification [{}] : {}", msg, e.getMessage());
            return null;
        }
    }

    public BigDecimal getQty() {
        try {
            BigDecimal result = new BigDecimal(msg.getDouble(prefix + LABEL_QUANTITY + "." + index));
            result = result.setScale(5, BigDecimal.ROUND_HALF_UP);
            return result;
        } catch (Exception e) {
            LOGGER.warn("Error while extracting quantity from Price notification [{}] : {}", msg, e.getMessage());
            return null;
        }
    }

    public String getSubMarketCode() {
        String code = msg.getString(LABEL_REG_SUBMARKET);
        if (code == null) {
            code = msg.getString(LABEL_REG_MARKET);
        }
        if (code == null) {
            code = msg.getString(LABEL_REG_SUBMARKET + "." + index);
        }
        if (code == null) {
            code = msg.getString(LABEL_REG_MARKET + "." + index);
        }
        return code;
    }

    public ProposalSide getSide() {
        int intSide = -1;
        ProposalSide side = null;
        try {
            if (prefix.length() > 0) {
                if (prefix.startsWith(PREFIX_BID)) {
                    side = ProposalSide.BID;
                } else if (prefix.startsWith(PREFIX_ASK)) {
                    side = ProposalSide.ASK;
                } else {
                    LOGGER.warn("Error while extracting quantity from Price notification [{}]", msg);
                    return null;
                }
            } else {
                intSide = msg.getInt(LABEL_REG_SIDE + "." + index);
                if (intSide == VALUE_SIDE_BID) {
                    side = ProposalSide.BID;
                } else if (intSide == VALUE_SIDE_ASK) {
                    side = ProposalSide.ASK;
                } else {
                    LOGGER.warn("Error while extracting side from Price notification [{}]", msg);
                    return null;
                }
            }
            return side;
        } catch (Exception e) {
            LOGGER.warn("Error while extracting side from Price notification [{}] : {}", msg, e.getMessage());
            return null;
        }
    }

    public Date getTimestamp() {
        String timeStr = msg.getString(prefix + LABEL_TIME + "." + index);
        String dateStr = msg.getString(prefix + LABEL_DATE + "." + index);
        if (timeStr != null) {
            Date timestamp = null;
            if (timeStr.lastIndexOf(':') >= 8) {
                timeStr = (timeStr.replaceAll(":", ""));
            } else if (timeStr.lastIndexOf(':') > 0) {
                timeStr = (timeStr.replaceAll(":", "")) + "000";
            }// Per MTS Prime il time non ha ":" ed e' nel formato HHmmssSSS

            long time;
            try {
                time = Long.parseLong(timeStr);
            } catch (NumberFormatException nfe) {
                time = 0;
            }
            if (time == 0) {
                timestamp = DateService.newUTCDate();
            } else {
                try {
                     Date timeStmp = DateService.parse(DateService.dateTimeISO, dateStr + timeStr);
                     timestamp = timeStmp;
                } catch (Exception e) {
                    LOGGER.warn("Error converting timestamp", e);
                    timestamp = DateService.newLocalDate();
                }
            }
            return timestamp;
        } else {
            return DateService.newLocalDate();
        }
    }
}
