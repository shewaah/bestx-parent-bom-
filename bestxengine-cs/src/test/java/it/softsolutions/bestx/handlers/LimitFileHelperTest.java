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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**  

 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 18/set/2013 
 * 
 **/
public class LimitFileHelperTest {
    private static final String lfPrefix = "LF:";
    private static final String lfnpPrefix = "LFNP:";
    private static final int commentLen = 20;
    
    //private static OperationRegistry opRegistry;
    
    static ClassPathXmlApplicationContext context;
    
    @BeforeClass
    public static void setUp() throws Exception {
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        
//        se carico il context la classe viene già inizializzata, non mi serve impostarla manualmente
//        LimitFileHelper.getInstance().setLimitFileCommentPrefix(lfPrefix);
//        LimitFileHelper.getInstance().setLimitFileNoPriceCommentPrefix(lfnpPrefix);
//        LimitFileHelper.getInstance().setCommentsMaxLen(commentLen);
    }
    
    /*
     * empty book: no internal proposal must be found
     */
    @Test
    public void testCommentGeneratorLimitCases() {
        org.junit.Assert.assertEquals(lfPrefix, LimitFileHelper.getInstance().getCommentWithPrefix(null, false));
        
        org.junit.Assert.assertEquals(lfPrefix, LimitFileHelper.getInstance().getCommentWithPrefix("", false));

        String comment = " ";
        org.junit.Assert.assertEquals(lfPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(comment, false));
    }

    @Test
    public void testCommentGeneratorNormalCases() {
        // overwrite max length of comment
        LimitFileHelper.getInstance().setCommentsMaxLen(commentLen);
        
        // normal comment
        String comment = "abc";
        org.junit.Assert.assertEquals(lfPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(comment, false));
        org.junit.Assert.assertEquals(lfnpPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(comment, true));        

        // long comment (26 chars)
        comment = "abcdefghijklmnopqrstuvwxyz";
        // expected: "LF: abcdefghijklmnopqrst" (20 chars prefix NOT included)
        org.junit.Assert.assertEquals((lfPrefix + " " + comment).substring(0, commentLen + lfPrefix.length() + 1), LimitFileHelper.getInstance().getCommentWithPrefix(comment, false));
        org.junit.Assert.assertEquals((lfnpPrefix + " " + comment).substring(0, commentLen+ lfnpPrefix.length() + 1), LimitFileHelper.getInstance().getCommentWithPrefix(comment, true));        
        
        // remove prefix if already existing
        // 1) expect lfPrefix, comment already has lfPrefix
        comment = "abc";
        String commentWithPrefix = lfPrefix + " " + comment;
        org.junit.Assert.assertEquals(lfPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(commentWithPrefix, false));

        // 2) expect lfPrefix, comment already has lfnpPrefix
        commentWithPrefix = lfnpPrefix + " " + comment;
        org.junit.Assert.assertEquals(lfPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(commentWithPrefix, false));

        // 3) expect lfnpPrefix, comment already has lfnpPrefix
        commentWithPrefix = lfnpPrefix + " " + comment;
        org.junit.Assert.assertEquals(lfnpPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(commentWithPrefix, true));

        // 4) expect lfnpPrefix, comment already has lfPrefix
        commentWithPrefix = lfPrefix + " " + comment;
        org.junit.Assert.assertEquals(lfnpPrefix + " " + comment, LimitFileHelper.getInstance().getCommentWithPrefix(commentWithPrefix, true));
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
    }    
}
