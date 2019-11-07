package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.services.DateService;

public class CSPOBexExecutionReport extends ExecutionReport {

   private static final Logger LOGGER = LoggerFactory.getLogger(CSPOBexExecutionReport.class);
   
	private Integer execAttmptNo;
	private Date execAttmptTime;
	private String multiDealerID;
	private Integer noDealers;
	private List<CSDealerGroup> dealerGroups = new ArrayList<CSDealerGroup>();
	private int pobExMaxSize = 5;
	
	public CSPOBexExecutionReport(ExecutionReport executionReport, int pobExMaxSize) {
		copyFields(executionReport, this);
		this.pobExMaxSize = pobExMaxSize;
	}

	public CSPOBexExecutionReport(int pobExMaxSize) {
		this.pobExMaxSize = pobExMaxSize;
	}

	public void addCSDealerGroup(CSDealerGroup dealerGroup) {
		dealerGroups.add(dealerGroup);
		noDealers = dealerGroups.size();
	}

	public List<CSDealerGroup> getDealerGroups() {
		return dealerGroups;
	}

	public Integer getExecAttmptNo() {
		return execAttmptNo;
	}

	public void setExecAttmptNo(Integer execAttmptNo) {
		this.execAttmptNo = execAttmptNo;
	}

	public Date getExecAttmptTime() {
		return execAttmptTime;
	}

	public void setExecAttmptTime(Date execAttmptTime) {
		this.execAttmptTime = execAttmptTime;
	}

	public String getMultiDealerID() {
		return multiDealerID;
	}

	public void setMultiDealerID(String multiDealerID) {
		this.multiDealerID = multiDealerID;
	}

	public Integer getNoDealers() {
		return noDealers;
	}

	public static class CSDealerGroup {
		private String dealerID;
		private BigDecimal dealerQuotePrice;
		private BigDecimal dealerQuoteOrdQty;
		private Date dealerQuoteTime;
		private DealerQuoteStatus dealerQuoteStatus;
		private String dealerQuoteStatusString;

		public String getDealerID() {
			return dealerID;
		}
		public void setDealerID(String dealerID) {
			this.dealerID = dealerID;
		}
		public BigDecimal getDealerQuotePrice() {
			return dealerQuotePrice;
		}
		public void setDealerQuotePrice(BigDecimal dealerQuotePrice) {
			this.dealerQuotePrice = dealerQuotePrice;
		}
		public BigDecimal getDealerQuoteOrdQty() {
			return dealerQuoteOrdQty;
		}
		public void setDealerQuoteOrdQty(BigDecimal dealerQuoteOrdQty) {
			this.dealerQuoteOrdQty = dealerQuoteOrdQty;
		}
		public Date getDealerQuoteTime() {
			return dealerQuoteTime;
		}
		public void setDealerQuoteTime(Date dealerQuoteTime) {
			this.dealerQuoteTime = dealerQuoteTime;
		}
		public DealerQuoteStatus getDealerQuoteStatus() {
			return dealerQuoteStatus;
		}
		public void setDealerQuoteStatus(AttemptState attemptState) {
			this.dealerQuoteStatus = DealerQuoteStatus.fromAttemptState(attemptState);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(super.toString());
			builder.append(", dealerID=");
			builder.append(dealerID);
			builder.append(", dealerQuotePrice=");
			builder.append(dealerQuotePrice);
			builder.append(", dealerQuoteOrdQty=");
			builder.append(dealerQuoteOrdQty);
			builder.append(", dealerQuoteTime=");
			builder.append(DateService.formatAsUTC("yyyyMMdd-hh:mm:ss.SSS", dealerQuoteTime));
			builder.append(", dealerQuoteStatus=");
			builder.append(dealerQuoteStatus);
			builder.append(", dealerQuoteStatusString=");
			builder.append(dealerQuoteStatusString);
			return builder.toString();
		}
		public void setDealerQuoteStatusString(String auditQuoteState) {
			this.dealerQuoteStatusString = auditQuoteState;
		}
		public String getDealerQuoteStatusString() {
			return this.dealerQuoteStatusString;
		}
	}

	public enum DealerQuoteStatus {
		Rejected,
		Passed,
		Accepted
		
		;

		public static DealerQuoteStatus fromAttemptState(AttemptState attemptState) {
			if(attemptState == null) return DealerQuoteStatus.Accepted;
			switch(attemptState) {
			case REJECTED:
			case EXPIRED:
				return DealerQuoteStatus.Rejected;
			case ACCEPTED_COUNTER:
			case EXECUTED:
				return DealerQuoteStatus.Accepted;
			case PASSED_COUNTER:
				return DealerQuoteStatus.Passed;
			default:
				return DealerQuoteStatus.Accepted;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append(", execAttmptNo=");
		builder.append(execAttmptNo);
		builder.append(", execAttmptTime=");
		builder.append(DateService.formatAsUTC("yyyyMMdd-hh:mm:ss.SSS", execAttmptTime));
		builder.append(", multiDealerID=");
		builder.append(multiDealerID);
		builder.append(", noDealers=");
		builder.append(noDealers);
		for (CSDealerGroup dealer: dealerGroups) {
			builder.append(", dealer=");
			builder.append(dealer.toString());
		}
		return builder.toString();
	}

	public void fillFromOperation(Operation operation) {
		Attempt currentAttempt = operation.getLastAttempt();
		MarketOrder marketOrder = currentAttempt.getMarketOrder();
		MarketExecutionReport marketExecutionReport = null;
		int mktExecReportSize = 0;
		if(currentAttempt.getMarketExecutionReports() != null) {
			mktExecReportSize = currentAttempt.getMarketExecutionReports().size();
			if(mktExecReportSize > 0) {
				marketExecutionReport = currentAttempt.getMarketExecutionReports().get(mktExecReportSize - 1);
			} 
		}
		
		Proposal counteroffer = null;
		if(currentAttempt.getExecutablePrice(0) != null)
			counteroffer = currentAttempt.getExecutablePrice(0).getClassifiedProposal();

		this.setExecAttmptNo(operation.getAttemptNo());
		
		if(marketExecutionReport != null && this.getLastMkt() == null) {
			this.setLastMkt(marketExecutionReport.getLastMkt());
		}

		if(marketOrder!=null) {
			this.setExecAttmptTime(marketOrder.getTransactTime());
			this.setMultiDealerID(marketOrder.getMarket().getMicCode());
		}

		if(marketExecutionReport != null && (marketExecutionReport.getMarket().getMarketCode() == MarketCode.MARKETAXESS
									|| marketExecutionReport.getMarket().getMarketCode() == MarketCode.TW
									|| marketExecutionReport.getMarket().getMarketCode() == MarketCode.BLOOMBERG
									|| marketExecutionReport.getMarket().getMarketCode() == MarketCode.BV
									)) {
			// manage MarketAxess more rich execution report
			// get all quotes from attempt
			int size = Math.min(pobExMaxSize, currentAttempt.getExecutablePrices().size());
			int index = 0;
			if(currentAttempt.getExecutablePrice(0) == null) {
			   index =1;
			   size++;
			}
			for(; index < size; index++) {  //BESTX-314 from 0 to i-1 to catch also executed price
				ExecutablePrice quote = currentAttempt.getExecutablePrice(index);
				if(quote != null) {
					CSDealerGroup dealerGroup = new CSDealerGroup(); 
					if(quote.getMarketMarketMaker() != null && quote.getMarketMarketMaker().getMarketMaker() != null) {
						dealerGroup.setDealerID(quote.getMarketMarketMaker().getMarketMaker().getCode());
					}
					else if (quote.getOriginatorID() != null) {
						dealerGroup.setDealerID(quote.getOriginatorID());
					} else {
						LOGGER.warn("Market maker not defined");
						continue;
					}

					if(quote.getPrice() != null){
						dealerGroup.setDealerQuotePrice(quote.getPrice().getAmount());
						dealerGroup.setDealerQuoteOrdQty(quote.getQty());
						dealerGroup.setDealerQuoteTime(quote.getTimestamp());
					} else {
						dealerGroup.setDealerQuotePrice(new BigDecimal("0"));
						dealerGroup.setDealerQuoteOrdQty(new BigDecimal("0"));
						dealerGroup.setDealerQuoteTime(this.getExecAttmptTime());
					}
					dealerGroup.setDealerQuoteStatus(convertProposalState(quote, marketExecutionReport));
					dealerGroup.setDealerQuoteStatusString(convertAuditState(quote.getAuditQuoteState()));
					this.addCSDealerGroup(dealerGroup);				
				}
			}
		} else {
			CSDealerGroup dealerGroup = new CSDealerGroup();
			dealerGroup.setDealerQuoteStatus(currentAttempt.getAttemptState());
			if(marketExecutionReport != null && marketExecutionReport.getMarketMaker() != null) {  // if there is a different dealer in fill than in market order
				dealerGroup.setDealerID(marketExecutionReport.getMarketMaker().getCode());
			} else if(marketOrder != null && marketOrder.getMarketMarketMaker()!= null) {
				dealerGroup.setDealerID(marketOrder.getMarketMarketMaker().getMarketMaker().getCode());
			} else if(marketExecutionReport != null && marketExecutionReport.getExecBroker() != null)
				dealerGroup.setDealerID(marketExecutionReport.getExecBroker());  //BESTX-424

			if(dealerGroup.getDealerID() != null) {
				if (counteroffer != null && counteroffer.getMarketMarketMaker() != null && counteroffer.getPrice() != null) {  // got a counteroffer
					dealerGroup.setDealerQuotePrice(counteroffer.getPrice().getAmount());
					dealerGroup.setDealerQuoteOrdQty(counteroffer.getQty());
					dealerGroup.setDealerQuoteTime(counteroffer.getTimestamp());  // counteroffer time is in local time
					this.addCSDealerGroup(dealerGroup);
				} else if(marketOrder != null) {
					if(marketExecutionReport != null && marketExecutionReport.getPrice() != null && marketExecutionReport.getPrice().getAmount() != null
							&& BigDecimal.ZERO.compareTo(marketExecutionReport.getPrice().getAmount()) < 0) {
						dealerGroup.setDealerQuotePrice(marketExecutionReport.getPrice().getAmount());
					} else {
						dealerGroup.setDealerQuotePrice(marketOrder.getLimit().getAmount());
					}
					if(marketExecutionReport != null && marketExecutionReport.getActualQty() != null) {
						dealerGroup.setDealerQuoteOrdQty(marketExecutionReport.getActualQty());
					} else {
						dealerGroup.setDealerQuoteOrdQty(marketOrder.getQty());
					}
					dealerGroup.setDealerQuoteTime(DateService.convertUTCToLocal(marketOrder.getTransactTime()));  // market order tiome is in UTC time
					this.addCSDealerGroup(dealerGroup);
				}
			}
		}
	}

	public static String convertAuditState(String auditQuoteState) {
		switch (auditQuoteState) {
		case "Done":
		case "Order Accepted":
		case "Resp Req":
		case "Done-ABC":
		case "Done-ASC":
		case "Done-PhoneSpot":
		case "Done-APC":
		case "Done-Subject to FX":
		case "Done-Amended":
		case "Accepted":
			return "Accepted"; //BESTX-314 CS tracking defect ID 16169
		case "Covered":
			return "Covered";
		case "Tied-For-Cover":
			return "Tied-For-Cover";
		case "Tied for Best":
			return "Tied for Best";
		case "Missed": 
			return "Missed";
		case "EXP-DNQ":
			return "EXP-DNQ";
		case "EXP-Price":
			return "EXP-Price";
		case "Timed Out":
			return "Timed Out";
		case "Passed":
			return "Passed";
		case "Cancelled":
		case "Client CXL":
			return "Cancelled";
		default:
			return "Expired";
		}
	}

	private AttemptState convertProposalState(ExecutablePrice quote, MarketExecutionReport maExecReport) {
		if(quote == null || quote.getProposalState() == null)
			return null;
		switch(quote.getProposalState()) {
		case NEW:
		case VALID:
			if(quote.getMarketMarketMaker() == null && quote.getOriginatorID() != null &&
			quote.getOriginatorID().equals(maExecReport.getExecBroker()))
			return AttemptState.ACCEPTED_COUNTER;
		else if(quote.getMarketMarketMaker() != null && quote.getMarketMarketMaker().getMarketMaker().equals(maExecReport.getMarketMaker())) // it is done
			return AttemptState.ACCEPTED_COUNTER;
		else
			return AttemptState.COUNTER_RECEIVED;  // it has been covered
		case DROPPED:
		case EXPIRED:
		case REJECTED:
			return AttemptState.EXPIRED;
		default:
			return null;
		}
	}
}
