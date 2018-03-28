/*
 * Project Name : ${project_name} 
 * First created by: ${user} 
 * Creation date: ${date} 
 * 
 * Copyright 1997-${year} SoftSolutions! srl 
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
 * 
 */
package it.softsolutions.bestx.services.ranking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.dao.RankingLoaderDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.management.DealerRankingServiceMBean;
import it.softsolutions.bestx.services.DateService;

/**
 * Purpose : default implementation of the service that manages the market makers ranking.
 */
public class DefaultDealerRankingService implements DealerRankingServiceMBean, DealerRankingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDealerRankingService.class);
    private RankingLoaderDao rankingLoaderDao;
    private boolean updateLiveRanking;

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.services.DealerRankingService#loadRankingAndUpdate()
     */
    @Override
    public void loadRankingAndUpdate() throws Exception {
        loadRanking(true);
    }

    /**
     * This method performs the operations on the database needed for the loading of the day's ranking. No arguments if we need to load
     * today's ranking.
     * 
     * @throws Exception
     */
    @Override
    public void loadRanking(boolean updateLiveRanking) throws Exception {

        this.updateLiveRanking = updateLiveRanking;
        String formattedDate = DateService.format(DateService.dateISO, DateService.newLocalDate());
        loadRanking(formattedDate, updateLiveRanking);
    }

    /**
     * Fetch the given date ranking.
     * 
     * @param date
     *            : date requested
     * @return an array of market makers codes.
     */
    public String[] getRanking(String date) throws Exception {
        checkPreRequisites();
        if (!validDate(date)) {
            throw new Exception("Date [" + date + "] not valid, the correct format is : yyyymmdd. Example : 20090229 for the 29 February 2009.");
        }
        String[] returnArray;
        try {
            returnArray = rankingLoaderDao.getRanking(date);
        } catch (Exception e) {
            LOGGER.error("Error while loading the new ranking.", e);
            throw e;
        }
        LOGGER.info("Get ranking for the day [{}] returns {} marketMakers", date, returnArray.length);
        return returnArray;
    }

    /**
     * Check if the date is valid and its format is correct. Date format : yyyymmdd.
     * 
     * @param date
     *            : date to be validated
     * @return true if valid, false otherwise.
     */
    private boolean validDate(String date) {
        boolean validDate = false;
        if (date.length() == 8) {
            Integer day = new Integer(date.substring(6, 8));
            if (day.intValue() > 0 && day.intValue() < 32) {
                Integer month = new Integer(date.substring(4, 6));
                if (month.intValue() > 0 && month.intValue() < 13) {
                    Integer year = new Integer(date.substring(0, 4));
                    if (year.intValue() > 1999) {
                        validDate = true;
                    }
                }
            }
        }
        return validDate;
    }

    /**
     * This method performs the operations on the database needed for the loading of the day's ranking. The argument is the day whose
     * ranking we need. Date format : yyyymmdd.
     * 
     * @param date
     *            : requested date
     * @param updateLiveRanking
     *            : true if we must update the currently used ranking.
     */
    @Override
    public void loadRanking(String date, boolean updateLiveRanking) throws Exception {
        checkPreRequisites();
        LOGGER.info("Ranking for the day : " + date);
        boolean validDate = validDate(date);
        if (!validDate)
            throw new Exception("Date not valid, the correct format is : yyyymmdd. Example : 20090229 for the 29 February 2009.");
        try {
            rankingLoaderDao.loadNewRanking(date, updateLiveRanking);
        } catch (Exception e) {
            LOGGER.error("Error while loading the new ranking.", e);
            throw e;
        }
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (rankingLoaderDao == null) {
            throw new ObjectNotInitializedException("Ranking loader Dao not set");
        }
    }

    /**
     * Get the DAO used to work on the database
     * 
     * @return the RankingLoaderDao instance
     */
    public RankingLoaderDao getRankingLoaderDao() {
        return rankingLoaderDao;
    }

    /**
     * Set the DAO that will be used to work with the database
     * 
     * @param rankingLoaderDao
     *            : the RankingLoaderDao to be used
     */
    public void setRankingLoaderDao(RankingLoaderDao rankingLoaderDao) {
        this.rankingLoaderDao = rankingLoaderDao;
    }

}
