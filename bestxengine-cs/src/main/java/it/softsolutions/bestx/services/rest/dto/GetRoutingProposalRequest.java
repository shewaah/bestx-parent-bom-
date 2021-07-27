/*
* Copyright 1997-2021 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.rest.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class GetRoutingProposalRequest {

	public static enum Side {
		BUY, SELL
	};

	public static enum PriceTypeFIX {
		PERCENTAGE_OF_PAR, UNIT, FIXED_AMOUNT, DISCOUNT_YIELD, PREMIUM, YIELD
	};

	public static enum LegalEntity {
		ZRH, APAC, AMER
	};

	public static enum BidAsk {
		BID, ASK
	};

	public static enum PriceQuality {
		IND, CMP, FRM, AXE, TRD
	};

	public static enum MarketDataSource {
		BLOOMBERG, MARKETAXESS, TW, NEPTUNE, ICE
	};

	public static class ConsolidatedBookElement {
		private BidAsk bidAsk;
		private BigDecimal price;
		private BigDecimal size;
		private Date dateTime;
		private PriceQuality priceQuality;
		private String dealerAtVenue;
		private MarketDataSource dataSource;
		private String marketMakerCode;
		private Optional<String> quoteStatus = Optional.empty();

		public BidAsk getBidAsk() {
			return bidAsk;
		}

		public void setBidAsk(BidAsk bidAsk) {
			this.bidAsk = bidAsk;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public BigDecimal getSize() {
			return size;
		}

		public void setSize(BigDecimal size) {
			this.size = size;
		}

		public Date getDateTime() {
			return dateTime;
		}

		public void setDateTime(Date dateTime) {
			this.dateTime = dateTime;
		}

		public PriceQuality getPriceQuality() {
			return priceQuality;
		}

		public void setPriceQuality(PriceQuality priceQuality) {
			this.priceQuality = priceQuality;
		}

		public String getDealerAtVenue() {
			return dealerAtVenue;
		}

		public void setDealerAtVenue(String dealerAtVenue) {
			this.dealerAtVenue = dealerAtVenue;
		}

		public MarketDataSource getDataSource() {
			return dataSource;
		}

		public void setDataSource(MarketDataSource dataSource) {
			this.dataSource = dataSource;
		}

		public String getMarketMakerCode() {
			return marketMakerCode;
		}

		public void setMarketMakerCode(String marketMakerCode) {
			this.marketMakerCode = marketMakerCode;
		}

		public Optional<String> getQuoteStatus() {
			return quoteStatus;
		}

		public void setQuoteStatus(Optional<String> quoteStatus) {
			this.quoteStatus = quoteStatus;
		}

		@Override
		public String toString() {
			return "ConsolidatedBookElement [bidAsk=" + bidAsk + ", price=" + price + ", size=" + size + ", dateTime="
					+ dateTime + ", priceQuality=" + priceQuality + ", dealerAtVenue=" + dealerAtVenue + ", dataSource="
					+ dataSource + ", marketMakerCode=" + marketMakerCode + ", quoteStatus=" + quoteStatus + "]";
		}

	}

	private String isin;
	private Side side;
	private PriceTypeFIX priceTypeFIX;
	private BigDecimal size;
	private LegalEntity legalEntity;
	private List<ConsolidatedBookElement> consolidatedBook = new ArrayList<>();

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}

	public PriceTypeFIX getPriceTypeFIX() {
		return priceTypeFIX;
	}

	public void setPriceTypeFIX(PriceTypeFIX priceTypeFIX) {
		this.priceTypeFIX = priceTypeFIX;
	}

	public BigDecimal getSize() {
		return size;
	}

	public void setSize(BigDecimal size) {
		this.size = size;
	}

	public LegalEntity getLegalEntity() {
		return legalEntity;
	}

	public void setLegalEntity(LegalEntity legalEntity) {
		this.legalEntity = legalEntity;
	}

	public List<ConsolidatedBookElement> getConsolidatedBook() {
		return consolidatedBook;
	}

	public void setConsolidatedBook(List<ConsolidatedBookElement> consolidatedBook) {
		this.consolidatedBook = consolidatedBook;
	}

	@Override
	public String toString() {
		return "GetRoutingProposalRequest [isin=" + isin + ", side=" + side + ", priceTypeFIX=" + priceTypeFIX
				+ ", size=" + size + ", legalEntity=" + legalEntity + ", consolidatedBook=" + consolidatedBook + "]";
	}

}
