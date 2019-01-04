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
package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Order;


/**  

 *
 * Purpose: this class is mainly for helping with Credit Suisse specific LimitFile orders  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 04/set/2013 
 * 
 **/
public class LimitFileHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LimitFileHelper.class);
    private static LimitFileHelper INSTANCE;
    private String limitFileCommentPrefix;
    private String limitFileNoPriceCommentPrefix;
    private int commentsMaxLen;

    private LimitFileHelper() {
        
    }
    
    public static LimitFileHelper getInstance() {
        if (INSTANCE == null ) {
            INSTANCE = new LimitFileHelper(); 
        }
        
        return INSTANCE;
    }
    
    /**
     * Add the correct limit file prefix, managing the situation in which it already exists
     * @param comment the comment to put a prefix to
     * @param isLimitFileNoPrice true is a limit file no price order, false if it is a limit file order
     * @return the comment with the right prefix
     */
    public String getCommentWithPrefix(String comment, boolean isLimitFileNoPrice) {
        if ( (isLimitFileNoPrice) && ((comment == null) || (comment.isEmpty())) ) {
            return (limitFileNoPriceCommentPrefix != null ? limitFileNoPriceCommentPrefix : "");
        }
        else if ( (!isLimitFileNoPrice) && ((comment == null) || (comment.isEmpty())) ) {
            return (limitFileCommentPrefix != null ? limitFileCommentPrefix : "");
        }

        //if present, remove the prefix
        if (comment.startsWith(limitFileNoPriceCommentPrefix + " ")) {
            comment = comment.substring((limitFileNoPriceCommentPrefix + " ").length());
        }
        if (comment.startsWith(limitFileCommentPrefix + " ")) {
            comment = comment.substring((limitFileCommentPrefix + " ").length());
        }
        
        String completePrefix;
        if (isLimitFileNoPrice) {
            completePrefix = limitFileNoPriceCommentPrefix + " ";
        }
        else {
            completePrefix = limitFileCommentPrefix + " ";
        }
        //the prefix does not count on the comment max length    
        if (comment.length() > commentsMaxLen) {
            comment = comment.substring(0, commentsMaxLen);
        }
        
        comment = completePrefix + comment;        
        return comment;
    }

    /**
     * Remove the configured prefix from the given comment
     * @param comment the comment whose prefix we should remove
     * @param isLimitFileNoPrice true if the order is a Limit File No Price, false otherwise
     * @return the comment stripped of the prefix, or unchanged.
     */
    public String removePrefix(String comment, boolean isLimitFileNoPrice) {
        if (comment == null) {
            return comment;
        }
        if (isLimitFileNoPrice) {
            return comment.replace(limitFileNoPriceCommentPrefix, "");
        } else {
            return comment.replace(limitFileCommentPrefix, "");
        }
    }
 
    /**
     * Remove the configured prefix from the given comment
     * @param comment the comment whose prefix we should remove
     * @return the comment stripped of the prefix, or unchanged.
     */
    public String removePrefix(String comment) {
        if (comment == null) {
            return comment;
        }
        if (comment.startsWith(limitFileNoPriceCommentPrefix)) {
            return comment.replace(limitFileNoPriceCommentPrefix, "").trim();
        } else {
            return comment.replace(limitFileCommentPrefix, "").trim();
        }
    }
    
    /**
     * Add the configured prexif to the given comment
     * @param comment the comment to add the prefix to
     * @param isLimitFileNoPrice true if we are working with a Limit File No Price order, false it is a Limit File order
     * @return the comment with prefix
     */
    public String addPrefix(String comment, boolean isLimitFileNoPrice) {
        if (comment == null) {
            return comment;
        }
        if (isLimitFileNoPrice) {
            return comment = limitFileNoPriceCommentPrefix + removePrefix(comment);
        } else {
            return comment = limitFileCommentPrefix + removePrefix(comment);
        }
    }
    
    
    /**
     * Update the existing order comment (the Text fix field) appending the given one
     * @param newComment the new comment to append
     * @param order the order involved
     * @param isLimitFileNoPrice true if the order is a Limit File No Price, false otherwise
     * @return a comment which could be the concatenation of the new and the existing one
     */
    public String updateComment(String newComment, Order order) {
        String orderComment = order.getText();
        if (newComment == null) {
            return orderComment;
        }
        if (orderComment != null) {
            //replace the existing comment with a new limit file comment, or remove the previous appended part
            if (newComment.startsWith(limitFileNoPriceCommentPrefix) || newComment.startsWith(limitFileCommentPrefix) ){
                orderComment = newComment;
            } else {
                //remove previously appended comment
                orderComment = orderComment.replaceAll(" *-.*", "");
                //append the last comment to the one received from OTEX
                orderComment += " - " + newComment;
            }
            
            if (orderComment.length() > commentsMaxLen) {
                LOGGER.debug("Comment too long [{}], cutting it to {} characters", orderComment, commentsMaxLen);
                orderComment = orderComment.substring(0, commentsMaxLen);
            }
        } else {
            orderComment = newComment;
        }
        return orderComment;
    }
    
    /**
     * Check if the given comment is a limit file one, it means that it should start with one of the configured prefixes
     * @param comment the comment to check
     * @return true if it is a limit file order comment, false otherwise
     */
    public boolean isLimitFileComment(String comment) {
        return comment.startsWith(limitFileCommentPrefix) || comment.startsWith(limitFileNoPriceCommentPrefix);
    }
    
    /**
     * Get the comment from the order Text field or the state comment.
     * We should use the Text field if it has a value, because it has been initialized on order
     * reception and only for limit file orders
     * @param order the order whose Text field might be used
     * @param currentStateComment the comment of the current state
     * @return either the order Text or the state comment
     */
    public String getComment(Order order, String currentStateComment) {
        String comment = null;
        if (order.isLimitFile() && order.getText() != null) {
            comment = order.getText();
        } else {
            comment = currentStateComment;
        }
        return comment;
    }
    
    /**
     * Sets the limit file comment prefix.
     *
     * @param prefix the new limit file comment prefix
     */
    public void setLimitFileCommentPrefix(String limitFileCommentPrefix) {
        this.limitFileCommentPrefix = limitFileCommentPrefix;
    }

    public void setLimitFileNoPriceCommentPrefix(String limitFileNoPriceCommentPrefix) {
        this.limitFileNoPriceCommentPrefix = limitFileNoPriceCommentPrefix;
    }

    public void setCommentsMaxLen(int len) {
        this.commentsMaxLen = len;
    }
    
}
