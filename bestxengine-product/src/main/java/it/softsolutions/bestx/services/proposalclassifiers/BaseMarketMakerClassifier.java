/*
* Copyright 1997-2019 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.proposalclassifiers;

import it.softsolutions.bestx.model.ClassifiedProposal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: this class is mainly for centralizing the composite price market maker verification 
*
* Project Name : bestxengine-product 
* First created by: gabriele.ghidoni 
* Creation date: 17 lug 2019 
* 
**/
public class BaseMarketMakerClassifier {
   private static final Logger LOGGER = LoggerFactory.getLogger(BaseMarketMakerClassifier.class);

   private String marketMakerCompositeCodes;

   private Set<String> marketMakerCompositeCodesSet = new HashSet<>();

   public String getMarketMakerCompositeCodes() {
      return marketMakerCompositeCodes;
   }

   public void setMarketMakerCompositeCodes(String marketMakersCompositeCodes) {
      this.marketMakerCompositeCodes = marketMakersCompositeCodes;
      this.marketMakerCompositeCodesSet = new HashSet<>();
      if (StringUtils.isNotBlank(this.marketMakerCompositeCodes)) {
         this.marketMakerCompositeCodesSet.addAll(Arrays.asList(this.marketMakerCompositeCodes.split(",")));
      }
   }

   /**
    * This is used to tell if the marketMakerCode identifies a composite price market maker
    * 
    * @param marketMakerCode
    * @return true if the marketMakerCode is a composite price market maker
    */
   public boolean isCompositePriceMarketMaker(String marketMakerCode) {
      return marketMakerCompositeCodesSet.contains(marketMakerCode);
   }

   protected boolean isCompositePriceMarketMaker(ClassifiedProposal proposal) {
      if (proposal != null && proposal.getMarketMarketMaker() != null && proposal.getMarketMarketMaker().getMarketMaker() != null) {
         return isCompositePriceMarketMaker(proposal.getMarketMarketMaker().getMarketMaker().getCode());
      }
      else {
         LOGGER.error("Unable to get market maker code for composite price check. proposal: " + proposal, new Exception());
         return false;
      }
   }

}
