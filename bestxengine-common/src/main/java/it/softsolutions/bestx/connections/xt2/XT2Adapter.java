package it.softsolutions.bestx.connections.xt2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.jsscommon.SSLog;
import it.softsolutions.xt2.enums.XT2EnableFlag;
import it.softsolutions.xt2.jpapi.XT2ConnectionIFC;
import it.softsolutions.xt2.jpapi.XT2PlatformAPI;
import it.softsolutions.xt2.jpapi.XT2UserSessionIFC;
import it.softsolutions.xt2.jpapi.exceptions.XT2PapiException;
import it.softsolutions.xt2.jpapi.helper.XT2ServiceHelper;
import it.softsolutions.xt2.protocol.XT2CInitPlatformApi;
import it.softsolutions.xt2.protocol.XT2CInitUserSession;

/**
 * POJO Adapter for XT2 connection. Useful for Spring initialization
 * @author lsgro
 *
 */
public class XT2Adapter implements XT2ConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(XT2Adapter.class);
    
    private XT2PlatformAPI jpapi;
    private XT2UserSessionIFC session;
    
    private String username;
    private String password;

    public XT2Adapter(String environment, String username, String password, boolean useLogging) throws XT2PapiException {
        this.username = username;
        this.password = password;
        jpapi = XT2PlatformAPI.getInstance();
        XT2CInitPlatformApi initPlatform = new XT2CInitPlatformApi();
        initPlatform.setInfoBusEnv(environment);
        initPlatform.setUseLogging(useLogging ? XT2EnableFlag.ENABLED : XT2EnableFlag.DISABLED);
        initPlatform.setLogVerbosity(SSLog.LL_INFO);
        LOGGER.debug("Create XT2 session for environment: {}, user: {}", environment, username);
        try {
            jpapi.init(initPlatform);
            session = jpapi.initSession(username, password);
        } catch (XT2PapiException e) {
            throw e;
        } catch (Exception e) {
            throw new XT2PapiException("Exception (" + e.getClass().getName() + ") in XT2 API init: " + e.getMessage());
        }
    }
    
    @Override
    public XT2ConnectionIFC getConnection(String service) throws XT2PapiException {
        XT2ServiceHelper serviceHelper = XT2ServiceHelper.getInstance();
        LOGGER.debug("Create XT2 connection to service: {}", service);
        try {
            XT2CInitUserSession initPdu  = serviceHelper.getInitSessionPdu(service);
            if (initPdu == null) {
                throw new XT2PapiException("Configuration not found for service: " + service);
            }
            initPdu.setUserName(username);
            initPdu.setPassword(password);

            return session.createConnection(initPdu);
        } catch (XT2PapiException e) {
            throw e;
        } catch (Exception e) {
            throw new XT2PapiException("Exception (" + e.getClass().getName() + ") while creating XT2 connection: " + e.getMessage());
        }
    }
}
