/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jablotron.handler;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.JablotronBindingConstants;
import org.openhab.binding.jablotron.config.JablotronConfig;
import org.openhab.binding.jablotron.internal.JablotronResponse;
import org.openhab.binding.jablotron.internal.discovery.JablotronDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * The {@link JablotronBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronBridgeHandler extends BaseThingHandler implements BridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronBridgeHandler.class);

    private String session = "";
    private int stavA = 0;
    private int stavB = 0;
    private int stavABC = 0;
    private int stavPGX = 0;
    private int stavPGY = 0;

    /**
     * Our configuration
     */
    protected JablotronConfig thingConfig;
    private JablotronDiscoveryService discoveryService;


    public JablotronBridgeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void childHandlerInitialized(ThingHandler thingHandler, Thing thing) {
    }

    @Override
    public void childHandlerDisposed(ThingHandler thingHandler, Thing thing) {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_1)) {
            // TODO: handle command
        }
    }

    @Override
    public void initialize() {
        String thingUid = getThing().getUID().toString();
        thingConfig = getConfigAs(JablotronConfig.class);
        thingConfig.setThingUid(thingUid);

        login();
        scheduler.schedule(() -> startDiscovery(), 1, TimeUnit.SECONDS);
    }

    private void login() {
        String url = null;

        try {
            //login
            stavA = 0;
            stavB = 0;
            stavABC = 0;
            stavPGX = 0;
            stavPGY = 0;

            url = JABLOTRON_URL + "ajax/login.php";
            String urlParameters = "login=" + thingConfig.getLogin() + "&heslo=" + thingConfig.getPassword() + "&aStatus=200&loginType=Login";
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();

            synchronized (session) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Referer", JABLOTRON_URL);
                connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
                connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");

                setConnectionDefaults(connection);
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(postData);
                }

                JablotronResponse response = new JablotronResponse(connection);
                if (response.getException() != null) {
                    logger.error("JablotronResponse login exception: {}", response.getException());
                    return;
                }

                if (!response.isOKStatus())
                    return;

                //get cookie
                session = response.getCookie();
                if (!session.equals("") ) {
                    logger.debug("Successfully logged to Jablotron cloud!");
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    logger.error("Cannot log in to Jablotron cloud!");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot login to Jablonet cloud");
                }
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (Exception e) {
            logger.error("Cannot get Jablotron login cookie: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    private String getBrowserTimestamp() {
        return "_=" + System.currentTimeMillis();
    }

    private void setConnectionDefaults(HttpsURLConnection connection) {
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", AGENT);
        connection.setRequestProperty("Accept-Language", "cs-CZ");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setUseCaches(false);
    }

    public void setDiscoveryService(JablotronDiscoveryService jablotronDiscoveryService) {
        this.discoveryService = jablotronDiscoveryService;
    }

    public void startDiscovery() {
        if( !thing.getStatus().equals(ThingStatus.ONLINE)) {
            return;
        }

        try {
            //cloud request
            String url = JABLOTRON_URL + "ajax/widget-new.php?" + getBrowserTimestamp();

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL + "cloud");
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            setConnectionDefaults(connection);

            JablotronResponse response = new JablotronResponse(connection);

            if (response.getException() != null) {
                logger.error("JablotronResponse widget exception: {}", response.getException().toString());
                return;
            }

            if (response.getResponseCode() != 200 || !response.isOKStatus()) {
                return;
            }

            if (response.getWidgetsCount() == 0) {
                logger.error("Cannot found any jablotron device");
                return;
            }

            String serviceId = response.getServiceId(0);

            //service request
            url = response.getServiceUrl(0);
            logger.info("Found Jablotron service: {} id: {}", response.getServiceName(0), serviceId);

            cookieUrl = new URL(url);
            connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL);
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            setConnectionDefaults(connection);

            if (connection.getResponseCode() == 200) {
                logger.debug("Successfully logged to Jablotron cloud!");
                discoveryService.oasisDiscovered("Jablotron OASIS Alarm", serviceId);
            } else {
                logger.error("Cannot initialize Jablotron service: {}", serviceId);
            }
        }
        catch(Exception ex)  {
            logger.error("Cannot discover Jablotron services!", ex);
        }
    }
}
