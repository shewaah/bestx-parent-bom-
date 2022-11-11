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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import it.softsolutions.bestx.dao.RankingLoaderDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;

/**  
*
* Purpose : this DAO loads the market makers ranking from the database. It calculates,
* through SQL queries, a ratio for the effective executions/execution requests for
* every market maker and sort the market makers out working on this calculation.
* The higher the ratio the better the ranking.
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class SqlRankingLoaderDao implements RankingLoaderDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRankingLoaderDao.class);

    private String internalMMcodes = null;
    /*
     * The ranking loading routine must run at the end of the
     * day whom we are calculating the ratios.
     * It receives a parameter, the date, to allow manual loading of rankings.
     * The date accepted must be in the format yyyymmdd.
     */
    private static final String sqlDelete =
            "DELETE FROM RankingHistory" +
                    " WHERE RankingDate = ?";

    /*
     * Here we load the total number of attempts and the total number of
     * executed orders for all the OTC market makers for today.
     * Then we calculate the execution ratio for every mm : executions/attempts.
     * We use a LEFT JOIN because we want every market maker even those on which we
     * have made at least one attempt but no executions.
     */
    private static final String sqlInsert =
            " INSERT INTO RankingHistory " +
                    " SELECT " +
                    " tentativi.BestBankCode, " +
                    " bt.BankName, " +
                    " ISNULL(numeseguiti, 0) as NumEseguiti, " +
                    " NumTentativi, " +   
                    " ISNULL((1.0 * numeseguiti)/numtentativi * 100, 0.0) as ExecutionRatio, " +
                    " ? as RankingDate" +
                    " FROM " +
                    "(" +
                    "   SELECT BestBankCode, count(BestBankCode) as NumTentativi " +
                    "   FROM TabTentativi" +
                    "   WHERE CONVERT(CHAR(8),datacreazione,112) = ?" +
                    "   GROUP BY BestBankCode" +
                    " ) tentativi" +
                    " LEFT JOIN (" +
                    "   SELECT BancaRiferimento, count(BancaRiferimento) as NumEseguiti " +
                    "   FROM TabHistoryOrdini" +
                    "   WHERE Stato = 'StateExecuted'" +
                    "   AND CONVERT(CHAR(8), DataOraRicezione,112) = ?" +
                    "   GROUP BY BancaRiferimento" +
                    " ) eseguiti" +
                    " ON tentativi.BestBankCode = eseguiti.BancaRiferimento" +
                    " JOIN BankTable bt" +
                    " ON tentativi.BestBankCode = bt.BankCode" +
                    " WHERE NumTentativi > 0" +
                    " AND NumTentativi IS NOT NULL" +
                    " AND tentativi.BestBankCode IS NOT NULL" +
                    " AND tentativi.BestBankCode != '' ";

    /*
     * Here we add the special cases in which for that
     * market maker we don't have any attempt or the
     * attempts number is null.
     */
    private static final String sqlInsertSpecialCases=
            " INSERT INTO RankingHistory " +
                    " SELECT " +
                    " tentativi.BestBankCode, " +
                    " bt.BankName, " +
                    " ISNULL(numeseguiti, 0) as NumEseguiti, " +
                    " ISNULL(NumTentativi, 0) as NumTentativi, " +   
                    " 0.0 as ExecutionRatio, " +
                    " ? as RankingDate" +
                    " FROM " +
                    "(" +
                    "   SELECT BestBankCode, count(BestBankCode) as NumTentativi " +
                    "   FROM TabTentativi" +
                    "   WHERE CONVERT(CHAR(8),datacreazione,112) = ?" +
                    "   GROUP BY BestBankCode" +
                    " ) tentativi" +
                    " LEFT JOIN (" +
                    "   SELECT BancaRiferimento, count(BancaRiferimento) as NumEseguiti " +
                    "   FROM TabHistoryOrdini" +
                    "   WHERE Stato = 'StateExecuted'" +
                    "   AND CONVERT(CHAR(8), DataOraRicezione,112) = ?" +
                    "   GROUP BY BancaRiferimento" +
                    " ) eseguiti" +
                    " ON tentativi.BestBankCode = eseguiti.BancaRiferimento" +
                    " JOIN BankTable bt" +
                    " ON tentativi.BestBankCode = bt.BankCode" +
                    " WHERE NumTentativi = 0" +
                    " OR NumTentativi IS NULL" +
                    " AND tentativi.BestBankCode IS NOT NULL" +
                    " AND tentativi.BestBankCode != '' ";

    private static final String updateRanking = 
            "UPDATE BankTable SET Rank = ? WHERE BankCode = ?";

    private static final String existence =
            "SELECT count(*)" +
                    " FROM RankingHistory" +
                    " WHERE NumEseguiti != 0" +
                    " AND   RankingDate = ?";

    /*
     * WIth this query I rebuild the whole ranking including
     * market makers not hit in the given date.
     * I need it because I've to update the ranks in the
     * BankTable and I must do it for EVERY market maker.
     */
    private String selectRanking = null;

    private JdbcTemplate jdbcTemplate;

    /**
     * Constructor, it initializes the select query with the given parameters.
     * @param internalMMcodes : codes for the internal market makers
     */
    public SqlRankingLoaderDao(String internalMMcodes) {
        super();
        setInternalMMcodes(internalMMcodes);
        selectRanking = 
                " SELECT " +
                        "   BestBankCode," +
                        "   BankName," +
                        "   NumEseguiti, " +
                        "   NumTentativi, " +
                        "   ExecutionRatio, " +
                        "   CONVERT(CHAR(8), RankingDate,112) as RankingDateStr, " +
                        "  0 as ForcedRank " +
                        " FROM RankingHistory" +
                        " WHERE RankingDate = ?" +
                        " AND BestBankCode in ( "+
                        this.internalMMcodes+
                        " ) " +
                        "UNION " +
                        " SELECT " +
                        "   bt.BankCode as BestBankCode," +
                        "   bt.BankName," +
                        "   0 as NumEseguiti," +
                        "   0 as NumTentativi," +
                        "   0 as ExecutionRatio," +
                        "   CONVERT(CHAR(8), ?,112) as RankingDateStr, " +
                        "   1 as ForcedRank " +
                        " FROM BankTable bt" +
                        " LEFT JOIN MarketBanks mb on bt.BankCode = mb.BankCode" +
                        " WHERE bt.BankCode in (" +
                        this.internalMMcodes+
                        " ) " +
                        "AND bt.BankCode not in ("+
                        " SELECT " +
                        "   BestBankCode" +
                        " FROM RankingHistory" +
                        " WHERE RankingDate = ?" +
                        " AND BestBankCode in ( "+
                        this.internalMMcodes+
                        " ) )" +

      "UNION " +
      "SELECT " +
      "    BestBankCode, " +
      "    BankName, " +
      "    NumEseguiti, " +
      "    NumTentativi, " +
      "    ExecutionRatio, " +
      "    CONVERT(CHAR(8), RankingDate,112) as RankingDateStr, " +
      "  2 as ForcedRank " +
      " FROM RankingHistory" +
      " WHERE RankingDate = ?" +
      " AND BestBankCode not in ( "+
      this.internalMMcodes+
      " ) " +
      "UNION " +
      " SELECT " +
      "   bt.BankCode as BestBankCode," +
      "   bt.BankName," +
      "   0 as NumEseguiti," +
      "   0 as NumTentativi," +
      "   0 as ExecutionRatio," +
      "   CONVERT(CHAR(8), ?,112) as RankingDateStr, " +
      "  3 as ForcedRank " +
      " FROM BankTable bt" +
      " LEFT JOIN MarketBanks mb on bt.BankCode = mb.BankCode" +
      " WHERE bt.BankCode not in (" +
      this.internalMMcodes+
      " ) " +
      "AND bt.BankCode not in ("+
      "SELECT " +
      "    BestBankCode " +
      " FROM RankingHistory" +
      " WHERE RankingDate = ?" +
      " AND BestBankCode not in ( "+
      this.internalMMcodes+
      " ) )" +
      " GROUP BY bt.BankName, bt.BankCode, bt.Rank" +
      " ORDER BY ForcedRank ASC, ExecutionRatio DESC, NumTentativi DESC";
    }

    /**
     * Set the jdbcTemplate used to execute queries. 
     * @param jdbcTemplate
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
        if (internalMMcodes == null) {
            throw new ObjectNotInitializedException("Internal Market Maker Codes not set");
        }
    }

    /**
     * Load the ranking for the given date and, if requested, updates the current one.
     * Loading the ranking means writing ranking data on the RankingHistoryTable.
     * @param date : ranking date requested
     * @param updateLiveRanking : true to updated the live ranking, false to leave it untouched.
     */
    @Override
	public void loadNewRanking(final String date, boolean updateLiveRanking) throws Exception {
        LOGGER.info("Loading the current day ranking, starting delete/insert routine.");
        checkPreRequisites();

        jdbcTemplate.update(sqlDelete, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                stmt.setString(1, date);
            }
        });
        jdbcTemplate.update(sqlInsert, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                stmt.setString(1, date);
                stmt.setString(2, date);
                stmt.setString(3, date);
            }
        });
        jdbcTemplate.update(sqlInsertSpecialCases, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                stmt.setString(1, date);
                stmt.setString(2, date);
                stmt.setString(3, date);
            }
        });
        if (updateLiveRanking) {

            Integer countRows = (Integer) jdbcTemplate.queryForObject(existence, Integer.class, new Object[] {date});
            LOGGER.info("Found " + countRows + " executions for date " + date);
            if (countRows == null || countRows == 0) {
                throw new Exception("There aren't executed orders for the date " + date);
            } else {
                try {
                    List<RankingRow> rankingRows = this.getRankingList(date);

                    LOGGER.info("Updating BankTable Ranking!");
                    // update the live table ranks
                    Iterator<RankingRow> iterator = rankingRows.iterator();
                    while (iterator.hasNext()) {
                        RankingRow rrTmp = iterator.next();
                        final String bankCode = rrTmp.getBankCode();
                        final int rank = rrTmp.getPosition();
                        LOGGER.info("MM : " + bankCode + " - Rank : " + rank);
                        jdbcTemplate.update(updateRanking, new PreparedStatementSetter() {
                            public void setValues(PreparedStatement stmt) throws SQLException {
                                stmt.setInt(1, rank);
                                stmt.setString(2, bankCode);
                            }
                        });
                    }
                } catch (Exception e) {
                    LOGGER.info("error detected in SQL" + e.getMessage());
                }
            }
        }
        LOGGER.info("Done.");
    }

    /**
     * Set the internal market makers codes.
     * 
     * @param internalMMcodes
     */
    public void setInternalMMcodes(String internalMMcodes) {
        String[] mmSplit = internalMMcodes.split(",");
        this.internalMMcodes = null;
        for (int count = 0; count < mmSplit.length; count++) {
            LOGGER.debug("Internal MM added: {}", mmSplit[count]);
            if (this.internalMMcodes != null) {
                this.internalMMcodes += ", '";
            } else {
                this.internalMMcodes = "'";
            }
            this.internalMMcodes += mmSplit[count] + "'";
        }
    }

    /**
     * Purpose : inner class used to map the ranking row of every
     * market maker.
     */
    private class RankingRow {
        private String bankCode;
        private String bankName;
        private int position;
        private int numTentativi;
        private int eseguiti;
        private BigDecimal executionRatio;

        public String getBankCode() {
            return bankCode;
        }

        public void setBankCode(String bankCode) {
            this.bankCode = bankCode;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getNumTentativi() {
            return this.numTentativi;
        }

        public void setNumTentativi(int numTentativi) {
            this.numTentativi = numTentativi;
        }

        public int getEseguiti() {
            return this.eseguiti;
        }

        public void setEseguiti(int eseguiti) {
            this.eseguiti = eseguiti;
        }

        public BigDecimal getExecutionRatio() {
            return this.executionRatio;
        }

        public void setExecutionRatio(BigDecimal executionRatio) {
            this.executionRatio = executionRatio;
        }

        public String getBankName() {
            return this.bankName;
        }

        public void setBankName(String bankName) {
            this.bankName = bankName;
        }

        @Override
        public String toString() {
            // return executionRatio.setScale(2,RoundingMode.HALF_DOWN).toString() + "% ("+numTentativi+"/"+eseguiti+") - "+ bankName;
            return bankCode + "| " + bankName + "| " + executionRatio.setScale(2, RoundingMode.HALF_DOWN).toString() + "| " + numTentativi + "| " + eseguiti + "| " + position;
        }
    }

    /**
     * Fetch the ranking list for the given date.
     * @param date : the day whose ranking we have been requested.
     * @return the ranking represented as a list of RankingRow.
     */
    public List<RankingRow> getRankingList(final String date) {
        LOGGER.debug("Getting ranking for the date {}. Query {}", date, selectRanking);
        List rankingRows = jdbcTemplate.query(selectRanking, new PreparedStatementSetter() {
            public void setValues(PreparedStatement stmt) throws SQLException {
                stmt.setString(1, date);
                stmt.setString(2, date);
                stmt.setString(3, date);
                stmt.setString(4, date);
                stmt.setString(5, date);
                stmt.setString(6, date);

            }
        }, new RowMapper() {
            // the ranking starts from 0
            private int counter = 0;

            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                RankingRow rankingRow = new RankingRow();
                rankingRow.setBankCode(rs.getString("BestBankCode"));
                rankingRow.setBankName(rs.getString("BankName"));
                int tentativi = rs.getInt("NumTentativi");
                rankingRow.setNumTentativi(tentativi);
                int eseguiti = rs.getInt("NumEseguiti");
                rankingRow.setEseguiti(eseguiti);
                double ratio = rs.getDouble("ExecutionRatio");
                rankingRow.setExecutionRatio(new BigDecimal(ratio));
                String data = rs.getString("RankingDateStr");
                rankingRow.setPosition(counter++);
                return rankingRow;
            }
        });

        return rankingRows;
    }

    /**
     * Fetch the ranking for a given date returning it as an array of strings.
     * @param date : the day whose ranking we have been requested.
     * @return the ranking represented as a list of market makers codes.
     */
    public String[] getRanking(String date) {
        int i = 0;
        List<RankingRow> rankingList = getRankingList(date);
        String[] returnArray = new String[rankingList.size()];
        for (RankingRow r : rankingList) {
            returnArray[i++] = r.toString();
        }
        return returnArray;
    }
}
