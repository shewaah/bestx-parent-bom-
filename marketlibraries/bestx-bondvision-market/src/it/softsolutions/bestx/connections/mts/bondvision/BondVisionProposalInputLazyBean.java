/**
 * 
 */
package it.softsolutions.bestx.connections.mts.bondvision;

import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_DATE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ISIN;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE_RESP_QUANTITY;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE_RESP_QUOTE_ID;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PROPOSAL_MARKET_MAKER;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_TIME;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_PRICE_STATUS_ACTIVE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_PRICE_STATUS_INDICATIVE;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * @author Stefano
 *
 */
public class BondVisionProposalInputLazyBean extends XT2InputLazyBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BondVisionProposalInputLazyBean.class);
//	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
//	private SimpleDateFormat tf = new SimpleDateFormat("HHmmssSSS");
//	private DecimalFormat dnf = new DecimalFormat("00000000");
//	private DecimalFormat tnf = new DecimalFormat("000000000");
	private int row;
	private int column;
	private ProposalSide side;
	private String sidePrefix;


	public BondVisionProposalInputLazyBean(XT2Msg msg, int row, int column, ProposalSide side) {
		this.msg = msg;
		this.row = row;
		this.column = column;
		this.side = side;
		this.sidePrefix = (side == ProposalSide.BID ? "Bid." : "Ask.");
	}


	public BigDecimal getPrice() {
		
		try {
			BigDecimal price = new BigDecimal(msg.getDouble(sidePrefix + LABEL_PRICE + "." + row + "." + column));
			price = price.setScale(5, BigDecimal.ROUND_HALF_UP);
			return price;
		}
		catch (Exception e) {
            LOGGER.warn("Error while extracting price amount from Price notification [{}] : {}", msg, e.getMessage());
			return null;
		}
	}

	public BigDecimal getQty() {
		try {
			BigDecimal result = new BigDecimal(msg.getDouble(sidePrefix + LABEL_PRICE_RESP_QUANTITY + "." + row + "." + column));
			result = result.setScale(5, BigDecimal.ROUND_HALF_UP);
			return result;
		}
		catch (Exception e) {
            LOGGER.warn("Error while extracting quantity from Price notification [{}] : {}", msg, e.getMessage());
			return null;
		}
	}

	public ProposalSide getSide() {
		return side;
	}

	public Date getTimestamp() {
		String timeStr = msg.getString(sidePrefix + LABEL_TIME + "." + row + "." + column);
		String dateStr = msg.getString(sidePrefix + LABEL_DATE + "." + row + "." + column);
		if (timeStr != null) {
			Date timestamp = null;
			if (timeStr.lastIndexOf(':') >= 8){
			   timeStr = (timeStr.replaceAll(":", ""));
			} else {
			   timeStr = (timeStr.replaceAll(":", "")) + "000";
			}
			long time;
			try {
				time = Long.parseLong(timeStr);
			} catch(NumberFormatException nfe) {
				time = 0;
			}
			if (time == 0) {
				timestamp = DateService.newLocalDate();
			} else {
				try {
					java.util.Date d = DateService.parse(DateService.dateISO, dateStr);
					java.util.Date t = DateService.parse(DateService.timeISO, time);
					timestamp = new Date(d.getTime() + t.getTime());
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
	
   public String getIsin() {
      String isin = null;
      try {
         isin = msg.getString(LABEL_ISIN);
      }
      catch (Exception e) {
          LOGGER.warn("Error while extracting ISIN from Price notification [{}] : {}", msg, e.getMessage());
      }
      return isin;
   }
   
   public ProposalType getType() {
      String strType = null;
      ProposalType type = null;
      try {
          strType = msg.getString(sidePrefix + LABEL_PRICE_STATUS + "." + row + "." + column);
          if (VALUE_PRICE_STATUS_ACTIVE.equalsIgnoreCase(strType))
              type = ProposalType.TRADEABLE;
          else if (VALUE_PRICE_STATUS_INDICATIVE.equalsIgnoreCase(strType))
              type = ProposalType.INDICATIVE;
          return type;
      } catch (Exception e) {
          LOGGER.warn("Error while extracting type from Price notification [{}] : {}", msg, e.getMessage());
          return null;
      }
  }
   
   public String getBondVisionMarketMaker() {
      return msg.getString(sidePrefix + LABEL_PROPOSAL_MARKET_MAKER + "." + row + "." + column);
  }
   
   public String getSenderQuoteId() {
      return msg.getString(sidePrefix + LABEL_PRICE_RESP_QUOTE_ID + "." + row + "." + column);
  }
   
   @Override
   public String toString()
   {
      return msg.toString();
   }
}
