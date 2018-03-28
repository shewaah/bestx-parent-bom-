/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao.bean;

import it.softsolutions.bestx.model.Instrument;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**  
*
* Purpose: Bean that represents fields of the BondTypeQtyThreshold table, used by the Price forge to define the threshold of a bond quantity 
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class BondQtyThresholdBean {

    public static final String TAB_COL_BONDTYPE = "BondType";
    // this is not a real column, we build it in the query depending on the isAlarmActive flag
    public static final String TAB_COL_THRESHOLDQTY = "Threshold";
    public static final String TAB_COL_SIDE = "Side";
    public static final String TAB_COL_TICKER = "Ticker";
    public static final String TAB_ASSET_TYPE_GOVIES = "DEF_GOVIES";
    public static final String TAB_ASSET_TYPE_CORPS = "DEF_CORPS";

    private String bondType;
    private BigDecimal thresholdQty;
    private String side;
    private BigDecimal ticker;

    /**
     * Constructor given a Resultset from a database query
     * 
     * @param rs
     *            The Resultset of a database query on the BondTypeQtyThreshold table
     * @throws SQLException
     *             throws by the Resultset
     */
    public BondQtyThresholdBean(ResultSet rs) throws SQLException {
        setBondType(rs.getString(TAB_COL_BONDTYPE));
        setThresholdQty(rs.getBigDecimal(TAB_COL_THRESHOLDQTY));
        setSide(rs.getString(TAB_COL_SIDE));
        setTicker(rs.getBigDecimal(TAB_COL_TICKER));
    }

    /**
     * Empty constructor
     */
    public BondQtyThresholdBean() {
    }

    /**
     * Return the bond tyoe
     * 
     * @return String represent the bond type
     */
    public String getBondType() {
        return bondType;
    }

    /**
     * Set the bond type
     * 
     * @param bondType
     *            of the bond
     */
    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    /**
     * Return the threshold quantity
     * 
     * @return the threshold quantity
     */
    public BigDecimal getThresholdQty() {
        return thresholdQty;
    }

    /**
     * Set the threshold quntity in the bean
     * 
     * @param thresholdQty
     *            value of the threshold quantity
     */
    public void setThresholdQty(BigDecimal thresholdQty) {
        this.thresholdQty = thresholdQty;
    }

    /**
     * Return the side of the bond
     * 
     * @return value of the side
     */
    public String getSide() {
        return side;
    }

    /**
     * Set the side of the bond
     * 
     * @param side
     *            of the bond
     */
    public void setSide(String side) {
        this.side = side;
    }

    /**
     * Return the ticker of the bond
     * 
     * @return ticker of the bond
     */
    public BigDecimal getTicker() {
        return ticker;
    }

    /**
     * Set the ticker of the bond
     * 
     * @param ticker
     *            of the bond
     */
    public void setTicker(BigDecimal ticker) {
        this.ticker = ticker;
    }

    /**
     * Translates the InstrumentsTable asset type in the one used in the BondTypeQtyThreshold table.
     * 
     * Instrument GOVT asset type stands for the BondTypeQtyThreshold table DEF_GOVIES asset type. Every other Instrument asset type stands
     * for DEF_CORPS.
     * 
     * @param bondAssetType
     *            the bond asset type of the instrument
     * @return DEF_GOVIES if the given asset type is equal INSTR_ASSET_TYPE_GOVIES othwerwise return TAB_ASSET_TYPE_CORPS
     */
    public static String getTableAssetType(String bondAssetType) {
        if (bondAssetType.equals(Instrument.INSTR_ASSET_TYPE_GOVIES)) {
            return TAB_ASSET_TYPE_GOVIES;
        } else {
            return TAB_ASSET_TYPE_CORPS;
        }
    }

}
