package it.softsolutions.bestx.mq;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQConfigHelper {

   private static final Logger LOGGER = LoggerFactory.getLogger(MQConfigHelper.class);

   private static final String pbeAlgorithm = "PBEWITHSHA1ANDDESEDE";

   private static final String pbePassword = "AyT0P%c$=lPwQ**";

   public static MQConfig getConfigurationFromFile(String configFile, String configPrefix) throws ConfigurationException {
      return getConfigurationFromFile(configFile, configPrefix, null);
   }

   public static MQConfig getConfigurationFromFile(String configFile, String configPrefix, StandardPBEStringEncryptor encryptor) throws ConfigurationException {
      AbstractFileConfiguration configuration = new PropertiesConfiguration();
      configuration.setListDelimiter('|');
      configuration.load(configFile);

      MQConfig serviceCfq = MQConfig.fromConfiguration(configuration.subset(configPrefix));

      if (encryptor == null) {
         encryptor = getEncryptor();
      }

      // Try to decrypt fields with encrypted passwords
      String keyStorePassword = decryptPassword(encryptor, serviceCfq.getSslKeyStorePassword());
      serviceCfq.setSslKeyStorePassword(keyStorePassword);
      String trustStorePassword = decryptPassword(encryptor, serviceCfq.getSslTrustStorePassword());
      serviceCfq.setSslTrustStorePassword(trustStorePassword);

      return serviceCfq;
   }

   private static String decryptPassword(StandardPBEStringEncryptor encryptor, String encryptedPassword) {
      String password = null;
      if (encryptedPassword.startsWith("ENC(")) {
         try {
            encryptedPassword = encryptedPassword.substring(4, encryptedPassword.length() - 1);
            password = encryptor.decrypt(encryptedPassword);
         }
         catch (Exception e) {
            LOGGER.warn("Unable to decrypy password, use the plain value. Reason: {}", e.getMessage());
            password = encryptedPassword;
         }
      }
      else {
         LOGGER.info("Password '*****' not encrypted, use the plain value.", encryptedPassword);
         password = encryptedPassword;
      }
      LOGGER.trace("encryptedPassword = '{}', password = '******'", encryptedPassword);
      return password;
   }

   public static StandardPBEStringEncryptor getEncryptor() {
      StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
      encryptor.setPassword(pbePassword);
      encryptor.setAlgorithm(pbeAlgorithm);
      return encryptor;
   }

}
