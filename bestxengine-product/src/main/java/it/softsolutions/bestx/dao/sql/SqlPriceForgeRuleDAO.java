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
package it.softsolutions.bestx.dao.sql;

import it.softsolutions.bestx.dao.PriceForgeRuleDAO;
import it.softsolutions.bestx.dao.bean.BondQtyThresholdBean;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBean;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBean.ActionType;
import it.softsolutions.bestx.dao.bean.PriceForgeRuleBeanImpl;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

/**
 * 
 * Purpose : the PriceForgeRuleDAO retrieve from the database rules to apply to the price discovery in order to put a particular Market
 * Maker in best position
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class SqlPriceForgeRuleDAO implements PriceForgeRuleDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlPriceForgeRuleDAO.class);

    private JdbcTemplate jdbcTemplate;

    private static final String selectPriceForgeCurrencyExchange = "SELECT * FROM PriceForgeCurrencyExchange" + " where Valuta = ?";
    private static final String selectRules = "SELECT * FROM PriceForgeRuleTable WHERE ISIN = ?";

    private static final String selectThresholds = "SELECT " + " BondType, " + " case isAlarmActive " + "   when 1 then ThresholdAlarmQty " + "   when 0 then ThresholdQty " + " end Threshold, "
            + " Side, " + " Ticker " + " FROM BondTypeQtyThreshold " + " WHERE BondType = ? " + " AND Side = ?";
    private PriceForgeRuleBean defNoneRule;
    private PriceForgeRuleBean defBestRule;
    private PriceForgeRuleBean checkProposalRule;

    /**
     * Set the jdbcTemplate used to execute queries
     * 
     * @param jdbcTemplate
     *            the JDBCTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }

    /**
     * Return the list of rules for the instrument
     * 
     * @param isin
     *            The isin code of the instrument
     * @return a list of PriceForgeRuleBean retrieved from the database
     */
    protected List getRules(final String isin) {
        LOGGER.info("getting PriceForgeRule for ISIN " + isin);
        checkPreRequisites();

        List rules = jdbcTemplate.query(selectRules, new PreparedStatementSetter() {
            public void setValues(PreparedStatement stmt) throws SQLException {
                stmt.setString(1, isin);
            }
        }, new RowMapper() {
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PriceForgeRuleBeanImpl(rs);
            }
        });

        return rules;
    }

    /**
     * Generate a PriceForgeRuleBean for the given instrument and order
     * 
     * @param instrument
     *            The instrument to check
     * @param side
     *            The side used to check the quantity odf the order
     * @param order
     *            Order contains data to populate the rule
     * @return a PriceForgeRuleBean contains the behavior of the price discover
     */
    public PriceForgeRuleBean getRule(Instrument instrument, PriceForgeRuleBean.ProposalSide side, Order order) {
        String isin = instrument.getIsin();
        String log = "Order " + order.getFixOrderId() + ". Checking threshold for ISIN " + isin + ". ";
        LOGGER.debug("Order {}, getting PriceForge rules.", order.getFixOrderId());
        List rules = getRules(isin);
        // We must reverse the order side to allow the getBondData method to
        // extract the right threshold. In the database the thresholds have
        // been set from the desk point of view.
        // The proposal side would have been correct, but it has a toString value not
        // compatible with that used in the database.
        OrderSide ordSide = order.getSide();
        if (OrderSide.BUY.equals(ordSide)) {
            ordSide = OrderSide.SELL;
        } else {
            ordSide = OrderSide.BUY;
        }
        BondQtyThresholdBean bondData = getBondData(instrument, order, ordSide);
        BigDecimal spread = null;
        BigDecimal threshold = null;
        if (bondData != null) {
            spread = bondData.getTicker();
            threshold = bondData.getThresholdQty();
        }
        // default NONE rule
        defNoneRule = new PriceForgeRuleBeanImpl();
        defNoneRule.setAction(ActionType.NO_PRICE_DEFAULT);
        defNoneRule.setSide(order.getSide().toString());
        defNoneRule.setIsin(isin);
        // default BEST rule, using the default spread
        defBestRule = new PriceForgeRuleBeanImpl();
        defBestRule.setAction(ActionType.ADD_SPREAD);
        defBestRule.setSpread(spread != null ? spread : PriceForgeRuleBean.DEFAULT_SPREAD);
        defBestRule.setSide(order.getSide().toString());
        defBestRule.setIsin(isin);
        // default CHECK_PROPOSAL rule
        checkProposalRule = new PriceForgeRuleBeanImpl();
        checkProposalRule.setAction(ActionType.CHECK_PROPOSAL);
        checkProposalRule.setSide(order.getSide().toString());
        checkProposalRule.setIsin(isin);
        BigDecimal ordQty = order.getQty();
        ordQty = ordQty.setScale(5, RoundingMode.HALF_DOWN);
        // convert the ordQty in EUR
        String ordCurr = order.getCurrency();
        LOGGER.debug("Order {}, currency {}, quantity {}", order.getFixOrderId(), ordCurr, ordQty);
        ordCurr = ordCurr.trim();
        BigDecimal exchRate = getPriceForgeExchangeRate(ordCurr);
        if (exchRate == null) {
            LOGGER.debug("Order {}, currency {}, exchange rate against EUR not found, set it as 1", order.getFixOrderId(), ordCurr);
            exchRate = BigDecimal.ONE;
        }
        exchRate = exchRate.setScale(5, RoundingMode.HALF_DOWN);
        LOGGER.debug("Order {}, currency {}, exchange rate found against EUR {}", order.getFixOrderId(), ordCurr, exchRate);
        ordQty = ordQty.divide(exchRate, 5, RoundingMode.HALF_DOWN);
        LOGGER.debug("Order {}, quantity in EUR {}", order.getFixOrderId(), ordQty);

        boolean greaterOrEqual = false;
        if (threshold != null) {
            log += "Threshold found : " + threshold.toString() + ".";
            // order qty >= threshold qty
            if (ordQty.compareTo(threshold) >= 0) {
                log += "(1) Quantity " + ordQty.toString() + " greater or equal than the threshold.";
                greaterOrEqual = true;
            }
            if (ordQty.compareTo(threshold) < 0) {
                log += "(1) Quantity " + ordQty.toString() + " lesser than the threshold.";
            }
        }

        PriceForgeRuleBean selectedRule = null;
        LOGGER.debug("Order {}, looping on the rules found for the given isin.", order.getFixOrderId());
        int ruleCounter = 0;
        int rulesFound = 0;
        if (rules != null && !rules.isEmpty()) {
            rulesFound = rules.size() - 1;
        }
        
        // we must always do at least one loop, it is the case of no rules defined for the given isin.
        while (ruleCounter <= rulesFound) {
            if (selectedRule == null) {
                PriceForgeRuleBean ruleBean = null;
                if (rules != null && !rules.isEmpty()) {
                    ruleBean = (PriceForgeRuleBean) rules.get(ruleCounter);
                    LOGGER.debug("Order {}, checking the rule {}", order.getFixOrderId(), ruleBean);
                } else {
                    LOGGER.debug("Order {}, no rules have been found for the given isin, going on with the PriceForge algorithm (will be probably selected a DEFAULT rule or the CHECK PROPOSAL one).",
                            order.getFixOrderId());
                }
                if (greaterOrEqual) {
                    selectedRule = quantityGreaterOrEqualThreshold(instrument, side, order, ruleBean, log, ordQty);
                } else {
                    selectedRule = quantityLesserThanThreshold(instrument, side, order, ruleBean, log, ordQty);
                }
            } else {
                LOGGER.debug("Order {}, rule already selected, this is the wrong side.", order.getFixOrderId());
            }
            ruleCounter++;
        }
        return selectedRule;
    }

    private BigDecimal getPriceForgeExchangeRate(String ordCurr) {
        String[] queryArg = new String[1];
        queryArg[0] = ordCurr;
        BigDecimal exchRate = (BigDecimal) jdbcTemplate.query(selectPriceForgeCurrencyExchange, queryArg, new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return rs.getBigDecimal("ExchangeRate");
                } else {
                    return null;
                }
            }
        });
        return exchRate;
    }

    /**
     * Return the bond data of the givin instrument The threshold is set in the table BondTypeQtyThreshold. We have four fields : BondType,
     * ThresholdQty, Side, Ticker
     * 
     * Fields BondType and Side are the table access key.
     * 
     * @param instrument
     *            The instrument object who retrieve bond data
     * @param order
     *            The order object
     * @param propSide
     *            The proposal side
     * @return
     */
    protected BondQtyThresholdBean getBondData(Instrument instrument, Order order, OrderSide propSide) {
        checkPreRequisites();
        String isin = instrument.getIsin();
        final String bondType = instrument.getBondType();
        final String side = propSide.toString();

        /*
         * 20110427 - Ruggero if the order is a buy one, then we must use the threshold that the buy-side desk put as ask. Vice versa if the
         * order is an ask one, we need the buy threshold.
         * 
         * PriceForgeRuleBean.ProposalSide propSide = order.getSide() == OrderSide.BUY ? PriceForgeRuleBean.ProposalSide.ASK:
         * PriceForgeRuleBean.ProposalSide.BID; final String side = propSide.toString();
         */

        BigDecimal threshold = null;
        LOGGER.info("Order " + order.getFixOrderId() + ". Getting Threshold for ISIN " + isin + "; Bond type " + bondType + ", order side " + side);
        BondQtyThresholdBean bondData = (BondQtyThresholdBean) this.jdbcTemplate.query(selectThresholds, new PreparedStatementSetter() {
            public void setValues(PreparedStatement stmt) throws SQLException {
                // 1st param : bond type
                stmt.setString(1, bondType);
                // 2nd param : side
                stmt.setString(2, side);
            }
        }, new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return new BondQtyThresholdBean(rs);
                } else {
                    return null;
                }
            }
        });

        if (bondData == null) {
            String bondAssetType = instrument.getAssetType();
            if (bondAssetType == null) {
                /*
                 * 20110429 - Ruggero If no asset type could have been found for the instrument we consider it as a corporate. It is enough
                 * to assign to the bondAssetType var e value different from BondQtyThresholdBean.TAB_ASSET_TYPE_GOVIES
                 */
                LOGGER.warn("Order " + order.getFixOrderId() + ". Instrument " + instrument.getIsin()
                        + ", no asset type defined! Decision of the 29 april 2011 : when no asset type is available, consider it as a CORPS.");
                bondAssetType = BondQtyThresholdBean.TAB_ASSET_TYPE_CORPS;
            }
            final String assetType = BondQtyThresholdBean.getTableAssetType(bondAssetType);
            LOGGER.info("Order " + order.getFixOrderId() + ". Getting Threshold for ISIN " + isin + "; Bond asset type " + assetType + ", order side " + side);
            bondData = (BondQtyThresholdBean) this.jdbcTemplate.query(selectThresholds, new PreparedStatementSetter() {
                public void setValues(PreparedStatement stmt) throws SQLException {
                    // 1st param : asset type
                    stmt.setString(1, assetType);
                    // 2nd param : side
                    stmt.setString(2, side);
                }
            }, new ResultSetExtractor() {
                public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                    if (rs.next()) {
                        return new BondQtyThresholdBean(rs);
                    } else {
                        return null;
                    }
                }
            });
            if (bondData != null) {
                LOGGER.info("Order " + order.getFixOrderId() + ". Found bond threshold for ISIN " + isin + " using AssetType " + assetType);
            }

        } else {
            LOGGER.info("Order " + order.getFixOrderId() + ". Found bond threshold for ISIN " + isin + " using BondType " + bondType);
        }
        if (bondData != null) {
            threshold = bondData.getThresholdQty();
        }

        LOGGER.info("Order " + order.getFixOrderId() + ". Threshold for Bond " + isin + ", order side " + side + " : " + (threshold != null ? threshold.toString() : "not found"));
        return bondData;
    }

    private PriceForgeRuleBean quantityGreaterOrEqualThreshold(Instrument instrument, PriceForgeRuleBean.ProposalSide deskSide, Order order, PriceForgeRuleBean rule, String log, BigDecimal ordQty) {
        try {
            LOGGER.debug("QUantity greater or equal than the threshold.");
            log += "Order quantity greater or equal than the threshold. Order side " + order.getSide() + ". Desk side " + deskSide + ". ";
            if (rule != null) {
                log += "Rule side " + rule.getSide() + ".";
                LOGGER.debug("Order {}. Checking rule side against desk side and quantity condition, both must be satistifed to apply the rule.", order.getFixOrderId());

                boolean sideSatisfied = (rule.getSide().compareTo(deskSide) == 0) || (rule.getSide().compareTo(PriceForgeRuleBean.ProposalSide.BOTH) == 0);
                if (!sideSatisfied) {
                    LOGGER.debug("Order {}. Side condition not satisfied!. Stop checking this rule, wrong side.", order.getFixOrderId());
                    return null;
                }

                boolean quantitySatisfied = rule.isQuantityInCondition(ordQty);
                if (!quantitySatisfied) {
                    LOGGER.debug("Order {}. Quantity condition not satisfied!.", order.getFixOrderId());
                } else {
                    LOGGER.debug("Order {}. Side and quantity conditions satisfied.", order.getFixOrderId());
                }

                boolean canApplyRule = sideSatisfied && quantitySatisfied;

                if (rule.isActive()) {
                    log += "Rule active.";
                    ActionType ruleAct = rule.getAction();
                    if (ruleAct != null && ruleAct.equals(ActionType.ADD_SPREAD)) {
                        log += "Rule action is BEST.";
                        // Action = BEST
                        if (canApplyRule) {
                            log += "Can apply the rule, use it.";
                            return rule;
                        } else {
                            log += "Cannot apply the rule, do nothing.";
                            return null;
                        }
                    } else {
                        log += "Rule action is not BEST.";
                        // Action != BEST
                        if (canApplyRule) {
                            log += "Can apply the rule, use it.";
                            return rule;
                        } else {
                            log += "Cannot apply the rule, use the Best Default one.";
                            return defBestRule;
                        }
                    }
                } else {
                    log += "Rule not active, applying the Best Default rule.";
                    return defBestRule;
                }
            } else {
                log += "Rule not available, applying the Best Default rule.";
                return defBestRule;
            }
        } finally {
            LOGGER.info(log);
        }
    }

    private PriceForgeRuleBean quantityLesserThanThreshold(Instrument instrument, PriceForgeRuleBean.ProposalSide deskSide, Order order, PriceForgeRuleBean rule, String log, BigDecimal ordQty) {
        try {
            LOGGER.debug("Quantity lesser than the threshold.");
            log += "Order quantity lesser than the threshold. Order side " + order.getSide() + ". Desk side " + deskSide + ". ";
            if (rule != null) {
                log += "Rule side " + rule.getSide() + ".";
                LOGGER.debug("Order {}. Checking rule side against desk side and quantity condition, both must be satistifed to apply the rule.", order.getFixOrderId());

                boolean sideSatisfied = (rule.getSide().compareTo(deskSide) == 0) || (rule.getSide().compareTo(PriceForgeRuleBean.ProposalSide.BOTH) == 0);
                if (!sideSatisfied) {
                    LOGGER.debug("Order {}. Side condition not satisfied!. Stop checking this rule, wrong side.", order.getFixOrderId());
                    return null;
                }

                boolean quantitySatisfied = rule.isQuantityInCondition(ordQty);
                if (!quantitySatisfied) {
                    LOGGER.debug("Order {}. Quantity condition not satisfied!.", order.getFixOrderId());
                } else {
                    LOGGER.debug("Order {}. Side and quantity conditions satisfied.", order.getFixOrderId());
                }

                boolean canApplyRule = sideSatisfied && quantitySatisfied;

                if (rule.isActive()) {
                    log += "Rule active.";
                    ActionType ruleAct = rule.getAction();
                    if (ruleAct != null && ruleAct.equals(ActionType.ADD_SPREAD)) {
                        log += "Rule action is BEST.";
                        // Action = BEST
                        if (canApplyRule) {
                            log += "Can apply the rule, use it.";
                            return rule;
                        } else {
                            log += "Cannot apply the rule, use the Check Proposal one.";
                            return checkProposalRule;
                        }
                    } else {
                        log += "Rule action is not BEST.";
                        // Action != BEST
                        if (canApplyRule) {
                            log += "Can apply the rule, use it.";
                            return rule;
                        } else {
                            log += "Cannot apply the rule, use the Check Proposal one.";
                            return checkProposalRule;
                        }
                    }
                } else {
                    log += "Rule not active, applying the Check Proposal one.";
                    return checkProposalRule;
                }
            } else {
                log += "Rule not available, applying the Check Proposal rule.";
                return checkProposalRule;
            }
        } finally {
            LOGGER.info(log);
        }
    }

    /**
     * Return a generic rule without price generated by the getRule method
     * 
     * @return a PriceForgeRuleBean contains the behavior of the price discover
     */
    public PriceForgeRuleBean getDefNoneRule() {
        return defNoneRule;
    }

    /**
     * Return a rule wit the best price generated by the getRule method
     * 
     * @return a PriceForgeRuleBean contains the behavior of the price discover
     */
    public PriceForgeRuleBean getDefBestRule() {
        return defBestRule;
    }

    /**
     * Return a generic rule that take no action on the price discovery
     * 
     * @return a PriceForgeRuleBean contains the behavior of the price discover
     */
    public PriceForgeRuleBean getCheckProposalRule() {
        return checkProposalRule;
    }
}
