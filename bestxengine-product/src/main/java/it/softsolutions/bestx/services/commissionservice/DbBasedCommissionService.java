package it.softsolutions.bestx.services.commissionservice;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.InstrumentZeroCommissionDao;
import it.softsolutions.bestx.finders.CommissionFinder;
import it.softsolutions.bestx.finders.ExchangeRateFinder;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.CommissionRow;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExchangeRate;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.CommissionService;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbBasedCommissionService implements CommissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbBasedCommissionService.class);

    protected static final String ORDERSIDE_BOTH = "Both";

    protected CommissionFinder commissionFinder;
    protected ExchangeRateFinder exchangeRateFinder;
    private String baseCurrencyString;
    private static BigDecimal hundred = new BigDecimal(100);
    private InstrumentZeroCommissionDao instrumentZeroCommission;
    static protected BigDecimal ten_thousand = new BigDecimal(100 * 100);

    public void init() {
        if (this.commissionFinder == null) {
            LOGGER.error("property commissionFinder not set");
        }
        if (this.exchangeRateFinder == null) {
            LOGGER.error("property exchangeRateFinder not set");
        }
    }

    /*
     * Le commissioni vengono caricate dal finder, possono essere quelle per portfolio o per ticker. L'importo a cui applicarle viene sempre
     * convertito in euro e il valore di conversione viene calcolato come prima operazione. Quando avviene il confronto tra i limiti dello
     * scaglione si confrontano valori in euro.
     */
    @Override
    public Commission getCommission(Customer customer, Instrument instrument, Money amountDue, OrderSide orderSide) throws BestXException {

        BigDecimal amount = amountDue.getAmount();
        ExchangeRate er = exchangeRateFinder.getExchangeRateByCurrency(amountDue.getStringCurrency());

        Commission commission = null;
        // l'amount e' sempre tramutato in euro prima di compararlo alle soglie delle commissioni
        Collection<CommissionRow> commissions = commissionFinder.getCommissions(customer, instrument);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Looking for commissions for customer "
                    + (customer != null ? customer.getFixId() : "null")
                    + " isin "
                    + (instrument != null ? instrument.getIsin() : "null")
                    + " portfolio "
                    + (instrument != null && instrument.getInstrumentAttributes() != null && instrument.getInstrumentAttributes().getPortfolio() != null ? instrument.getInstrumentAttributes().getPortfolio().getId()
                            : "not available"));
        }
        
        for (CommissionRow cr : commissions) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Commission row : " + cr.getMinQty() + " -> " + cr.getMaxQty() + " = "
                        + (cr.getCommission() != null && cr.getCommission().getAmount() != null ? cr.getCommission().getAmount().doubleValue() : "not available"));
            }
            
            if (amount.compareTo(cr.getMinQty().multiply(er.getExchangeRateAmount())) > 0 && (cr.getMaxQty() == null || amount.compareTo(cr.getMaxQty().multiply(er.getExchangeRateAmount())) <= 0)) {

                // TROVATA!!
                LOGGER.debug("	This is the right commission.");

                // l'amount nelle commission e' o la fee minima o il tick percentuale
                commission = new Commission(cr.getCommission().getAmount(), cr.getCommission().getCommissionType());
                commission.setMinimumFeeMaxQty(cr.getMinimumFeeMaxSize());

                // minimum fee always converted in the order currency
                BigDecimal exRate = er.getExchangeRateAmount();
                BigDecimal minimumFeeConverted = cr.getMinimumFee().multiply(exRate);
                LOGGER.debug("Minimum fee found, it is in EUR : {}, converted in {} : {}", cr.getMinimumFee(), amountDue.getStringCurrency(), minimumFeeConverted);
                commission.setMinimumFee(minimumFeeConverted);

                if (commission != null) {
                    LOGGER.debug("Minimum fee found : {}", commission.getMinimumFee());
                }

                // Conversione dell'eventuale fee minima, l'unica ad essere in amount, nella valuta
                // dell'ordine
                if (commission.getCommissionType() == CommissionType.AMOUNT) {
                    commission.setAmount(commission.getAmount().multiply(er.getExchangeRateAmount()));
                }
            }
        }

        /*
         * Check if the instrument is configured for sending zero commission to the customers. We must do it here because we need the
         * correct commission type building the zero commission result. So, if we found something for this order we extract the commission
         * type from it.
         */
        if(instrument != null) {
	        String instrumentOrderSide = instrumentZeroCommission.getZeroCommissionOrderSide(instrument.getIsin());
	        if (instrumentOrderSide != null) {
	            if (LOGGER.isDebugEnabled()) {
	                StringBuilder message = new StringBuilder(512);
	                message.append("Instrument ").append(instrument.getIsin()).append(" is configured for zero commission for side ").append(instrumentOrderSide);
	                LOGGER.debug("{}", message);
	            }
	
	            if (isSameOrderSide(instrumentOrderSide, orderSide)) {
	                return new Commission(BigDecimal.ZERO, (commission != null ? commission.getCommissionType() : Commission.CommissionType.AMOUNT));
	            }
	        }
	    }
        return commission;
    }

    /**
     * Compare zero commission instrument side and order side.
     * 
     * @param instrumentOrderSide
     * @param orderSide
     * @return true if both sides are equal or the zero commission instrument is BOTH, false if both are null or different
     */
    protected boolean isSameOrderSide(String instrumentOrderSide, OrderSide orderSide) {
        if (instrumentOrderSide == null && orderSide == null) {
            return true;
        } else {
            if (instrumentOrderSide != null && orderSide != null) {
                if (instrumentOrderSide.trim().equalsIgnoreCase(ORDERSIDE_BOTH)) {
                    return true;
                }

                if (instrumentOrderSide.trim().equalsIgnoreCase(orderSide.toString())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ruggero Al prezzo vengono aggiunte le commissioni quando queste sono in ticker. Si tratta di aggiungere tick percentuali e quindi
     * posso sommarle/sottrarle direttamente al prezzo. Non si puo' superare, ovviamente, il prezzo limite.
     */
    @Override
    public BigDecimal calculateCommissionedPrice(BigDecimal originalPrice, BigDecimal limitPrice, OrderSide side, Commission commission) throws BestXException {
        BigDecimal commissionedPrice = null;
        if (commission.getCommissionType().compareTo(CommissionType.TICKER) != 0) {
            LOGGER.debug("commissionType != TICKER: " + commission.getCommissionType().getValue());
            throw new BestXException("Cannot calculate Commissioned Price for amount type commission.");
        }
        if (side == OrderSide.BUY) {
            commissionedPrice = originalPrice.add(commission.getAmount().divide(hundred));
            if (limitPrice != null && commissionedPrice.compareTo(limitPrice) > 0) {
                commissionedPrice = limitPrice;
            }
        } else if (side == OrderSide.SELL) {
            commissionedPrice = originalPrice.subtract(commission.getAmount().divide(hundred));
            if (limitPrice != null && commissionedPrice.compareTo(limitPrice) < 0) {
                commissionedPrice = limitPrice;
            }
        }
        return commissionedPrice;
    }

    /**
     * Ruggero Calcolo specifico per fill parziali, utilizziamo un valore di commissioni che ci viene inviato dal chiamante. Al prezzo
     * vengono aggiunte le commissioni quando queste sono in ticker. Si tratta di aggiungere tick percentuali e quindi posso
     * sommarle/sottrarle direttamente al prezzo. Non si puo' superare, ovviamente, il prezzo limite.
     */
    @Override
    public BigDecimal calculatePartialFillsCommissionedPrice(BigDecimal originalPrice, BigDecimal limitPrice, OrderSide side, Commission commission, BigDecimal commissionValue) throws BestXException {
        BigDecimal commissionedPrice = null;
        if (commission.getCommissionType().compareTo(CommissionType.TICKER) != 0) {
            LOGGER.debug("commissionType != TICKER: " + commission.getCommissionType().getValue());
            throw new BestXException("Cannot calculate Commissioned Price for amount type commission.");
        }
        if (side == OrderSide.BUY) {
            commissionedPrice = originalPrice.add(commissionValue);
            if (limitPrice != null && commissionedPrice.compareTo(limitPrice) > 0) {
                commissionedPrice = limitPrice;
            }
        } else if (side == OrderSide.SELL) {
            commissionedPrice = originalPrice.subtract(commissionValue);
            if (limitPrice != null && commissionedPrice.compareTo(limitPrice) < 0) {
                commissionedPrice = limitPrice;
            }
        }
        return commissionedPrice;
    }

    static private MathContext mc = MathContext.DECIMAL32;

    @Override
    public BigDecimal calculateCommissionAmount(Money amountDue, Commission commission) throws BestXException {
        BigDecimal commissionAmount = null;

        // controls for a safe logging
        if (amountDue != null) {
            if (commission != null && commission.getAmount() != null) {
                LOGGER.info("Calculating commission amount: amountDue = {}, commission amount = {}", amountDue.getAmount(), commission.getAmount());
            } else {
                LOGGER.info("Calculating commission amount: amountDue = {}", amountDue.getAmount());
            }

            ExchangeRate er = exchangeRateFinder.getExchangeRateByCurrency(amountDue.getStringCurrency());
            // converte in euro l'ammontare su cui calcolare le commissioni
            commissionAmount = amountDue.getAmount().divide(er.getExchangeRateAmount(), mc);
            // multiply by the ticker
            if (commission != null && commission.getAmount() != null) {
                LOGGER.debug("Multiplying the commission by the ticker {}", commission.getAmount().doubleValue());
                commissionAmount = commissionAmount.multiply(commission.getAmount());
            }
            commissionAmount = commissionAmount.divide(ten_thousand, mc).setScale(2, RoundingMode.HALF_DOWN);
            // riporta le commissioni calcolate nella valuta originale
            commissionAmount = commissionAmount.multiply(er.getExchangeRateAmount(), mc);
            LOGGER.info("Calculating commission amount: amountDue " + amountDue.getAmount() + ", modified commission amount " + commissionAmount.toString());
        } else {
            LOGGER.warn("AmountDue is null! Cannot calculate a commission amount, returning a value of 0.");
            commissionAmount = BigDecimal.ZERO;
        }
        return commissionAmount;
    }

    /**
     * @return the baseCurrency
     */
    public String getBaseCurrencyString() {
        return baseCurrencyString;
    }

    /**
     * @param baseCurrency
     *            the baseCurrency to set
     */
    public void setBaseCurrencyString(String baseCurrencyString) {
        this.baseCurrencyString = baseCurrencyString;
    }

    /**
     * @return the commissionFinder
     */
    public CommissionFinder getCommissionFinder() {
        return commissionFinder;
    }

    /**
     * @param commissionFinder
     *            the commissionFinder to set
     */
    public void setCommissionFinder(CommissionFinder commissionFinder) {
        this.commissionFinder = commissionFinder;
    }

    /**
     * @return the exchangeRateFinder
     */
    public ExchangeRateFinder getExchangeRateFinder() {
        return exchangeRateFinder;
    }

    /**
     * @param exchangeRateFinder
     *            the exchangeRateFinder to set
     */
    public void setExchangeRateFinder(ExchangeRateFinder exchangeRateFinder) {
        this.exchangeRateFinder = exchangeRateFinder;
    }

    /**
     * Initialize commissions on the given execution report. The Commission object contains the amount of the commission, this is usually in
     * a tick (a percentage), save for a minimum fee which is expressed directly in amount.
     * 
     * @throws BestXException
     */
    @Override
    public void initializeExecutionReportCommission(ExecutionReport execReport, ExecutionReport lastExecutionReport, ExecutionReport firstExecutionReport, Commission commission,
            boolean isFirstExecReport, String orderCurr) throws BestXException {

        // Extracting the currency exchange rate
        ExchangeRate er = this.exchangeRateFinder.getExchangeRateByCurrency(orderCurr);
        BigDecimal minFeeMaximumQty = commission.getMinimumFeeMaxQty();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initializing the execution report commissions, working on the exec report : {}", execReport);
            LOGGER.debug("Minimum fee maximum quantity in EUR {}", (minFeeMaximumQty != null ? minFeeMaximumQty : "no value found!"));
            if (minFeeMaximumQty == null) {
                minFeeMaximumQty = BigDecimal.ZERO;
                LOGGER.warn("Cannot find the minimum fee maximum quantity, setting it to zero.");
            }
        }
        minFeeMaximumQty = minFeeMaximumQty.multiply(er.getExchangeRateAmount());
        BigDecimal lastExecCumQty = lastExecutionReport.getActualQty();
        BigDecimal firstExecCumQty = firstExecutionReport.getActualQty();
        BigDecimal minimumFee = commission.getMinimumFee();

        LOGGER.debug("Minimum fee maximum quantity in {} {}", orderCurr, (minFeeMaximumQty != null ? minFeeMaximumQty : "no value found!"));
        LOGGER.debug("Minimum fee {}", (minimumFee != null ? minimumFee : "no value found!"));
        LOGGER.debug("Last execution report cumulative quantity {}", (lastExecCumQty != null ? lastExecCumQty : "no value found!"));
        LOGGER.debug("First execution report cumulative quantity {}", (firstExecCumQty != null ? firstExecCumQty : "no value found!"));

        if (lastExecCumQty != null && lastExecCumQty.compareTo(minFeeMaximumQty) <= 0) {
            LOGGER.debug("The last exec rep cumulative qty is lesser or equal than min fee maximum qty.");
            if (isFirstExecReport) {
                LOGGER.debug("This execution report is the first one, set the commission to the minimum fee {}", commission.getAmount());

                // the commission amount is the minimum fee, no need to convert it in amount
                execReport.setCommission(minimumFee);
                execReport.setAmountCommission(minimumFee);
                execReport.setCommissionType(CommissionType.AMOUNT);
            } else {
                LOGGER.debug("This execution report is not the first one, set to zero the commission, the customer has already received the minimum fee");

                // the execution reports following the first will have zero commissions
                execReport.setCommission(BigDecimal.ZERO);
                execReport.setAmountCommission(BigDecimal.ZERO);
                execReport.setCommissionType(CommissionType.AMOUNT);
            }
        } else {
            /*
             * Cumulative quantity of the last execution report greater than the minimum fee. Check if the first execution report quantity
             * is lesser or equal than the minimum fee max quantity: - if yes, we set the commission amount equals to the minimum fee, then
             * we calculate the real commission value getting multiplying this exec report executed quantity with the commission tick, now
             * we subtract this value from the minimum fee and save the result. This is what we put in excess as commissions in the first
             * execution report. FOr the following execution reports we balance the amount we should put in the report with the excess we
             * have previously calculated. - if no, all the execution reports will have, as commissions, their quantity multiplied by the
             * tick.
             */

            if (lastExecCumQty == null) {
                LOGGER.debug("The last exec rep cumulative qty is null! Going on considering it as greater than the minimum fee qty. This should never happen.");
            }
            LOGGER.debug("The last exec rep cumulative qty is greater than min fee maximum qty. We must now check the first execution report cumulative quantity.");

            if (firstExecCumQty != null) {
                if (firstExecCumQty.compareTo(minFeeMaximumQty) <= 0) {
                    LOGGER.debug("First exec rep cumulative qty is lesser or equal than min fee maximum qty.");

                    // current exec report qty multiplied by the tick (must be divided by one hundred)
                    BigDecimal convertedTick = commission.getAmount().divide(ten_thousand);
                    LOGGER.debug("Tick converted for multiplication : {}", convertedTick);

                    BigDecimal realCommissions = execReport.getActualQty().multiply(convertedTick);
                    LOGGER.debug("Real commission based on the current exec report cumulative qty : {}", realCommissions);

                    // for the first exec report the commission excess is zero
                    BigDecimal commissionExcess = commission.getCommissionExcess();
                    LOGGER.debug("Actual commission excess : {}", commissionExcess);

                    if (isFirstExecReport) {

                        LOGGER.debug("This is the first exec rep, set its commission to the minimum fee ({}), remembering that we will have to discount the excess in the following reports.",
                                commission.getMinimumFee());

                        // the commission amount is the minimum fee, no need to convert it in amount
                        execReport.setCommission(minimumFee);
                        execReport.setAmountCommission(minimumFee);
                        execReport.setCommissionType(CommissionType.AMOUNT);

                        commissionExcess = commission.getMinimumFee().subtract(realCommissions);
                        LOGGER.debug("Updated commission excess (subtracted the real commission {}) : {}", realCommissions, commissionExcess);

                        // store the commission excess
                        commission.setCommissionExcess(commissionExcess);
                    } else {
                        /*
                         * If the real commissions are >= than the excess we subtract the latter from the former, the result is the
                         * commission to be set in the current execution report. Eventually the excess will reach zero.
                         */
                        LOGGER.debug("The exec report is not the first, check the real commission {} against the excess {}", realCommissions, commissionExcess);

                        if (realCommissions.compareTo(commissionExcess) >= 0) {
                            BigDecimal newCommission = realCommissions.subtract(commissionExcess);
                            LOGGER.debug("Real commission greater or equal than the excess, subtracting the latter from the former, new commission = {}", newCommission);

                            execReport.setCommission(newCommission);
                            execReport.setAmountCommission(newCommission);
                            // being the real commissions equals or greater than the excess
                            // we have completely absorbed it, thus we must set it to zero
                            LOGGER.debug("Being the real commissions equals or greater than the excess we have completely absorbed it, thus we set it to zero");

                            commission.setCommissionExcess(BigDecimal.ZERO);
                            execReport.setCommissionType(CommissionType.AMOUNT);
                        } else {
                            /*
                             * The excess is greater than the commission for this execution report, we must not make the customer pays more
                             * and we must subtract the actual commission from the excess, which will eventually reach zero.
                             */
                            LOGGER.debug("Real commission lesser than the excess, we must not make the customer pays more and we must subtract the actual commission from the excess, which will eventually reach zero");

                            execReport.setCommission(BigDecimal.ZERO);
                            execReport.setAmountCommission(BigDecimal.ZERO);
                            execReport.setCommissionType(CommissionType.AMOUNT);
                            // subtract the real commission from the excess and store its new value
                            commissionExcess = commissionExcess.subtract(realCommissions);
                            LOGGER.debug("Subtracted the real commission ({}) from the excess, whose new value is : {}", realCommissions, commissionExcess);
                            commission.setCommissionExcess(commissionExcess);
                        }
                    }
                } else {
                    /*
                     * executed quantity of the first exec rep greater than the minimum fee one, no need to keep track of the excess, there
                     * isn't any.
                     */
                    LOGGER.debug("First exec rep cumulative qty is greater than min fee maximum qty. We calculate the real commission multiplying the cumulative quantity with the tick (which will be previously converted being a percentage).");

                    BigDecimal convertedTick = commission.getAmount().divide(ten_thousand);
                    LOGGER.debug("We converted the tick for multiplication : {}", convertedTick);

                    BigDecimal realCommissions = execReport.getActualQty().multiply(convertedTick);
                    LOGGER.debug("Real commissions calculated with the converted tick : {}", realCommissions);
                    execReport.setCommission(realCommissions);
                    execReport.setAmountCommission(realCommissions);
                    execReport.setCommissionType(CommissionType.AMOUNT);
                }
            } else {
                LOGGER.debug("No first exec rep cumulative qty! Nothing that we can do.");
            }
        }
    }

    public void setInstrumentZeroCommission(InstrumentZeroCommissionDao instrumentZeroCommission) {
        this.instrumentZeroCommission = instrumentZeroCommission;
    }
}
