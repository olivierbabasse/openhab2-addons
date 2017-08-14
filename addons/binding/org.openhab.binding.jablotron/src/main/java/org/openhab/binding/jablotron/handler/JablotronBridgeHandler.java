/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jablotron.handler;

import com.google.gson.Gson;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.config.JablotronConfig;
import org.openhab.binding.jablotron.internal.Utils;
import org.openhab.binding.jablotron.internal.discovery.JablotronDiscoveryService;
import org.openhab.binding.jablotron.model.JablotronLoginResponse;
import org.openhab.binding.jablotron.model.JablotronWidgetsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

/**
 * The {@link JablotronBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronBridgeHandler extends BaseThingHandler implements BridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronBridgeHandler.class);

    private Gson gson = new Gson();

    private String session = "";

    /**
     * Our configuration
     */
    protected JablotronConfig bridgeConfig;
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

    }

    @Override
    public void initialize() {
        String thingUid = getThing().getUID().toString();
        bridgeConfig = getConfigAs(JablotronConfig.class);
        bridgeConfig.setThingUid(thingUid);

        scheduler.scheduleWithFixedDelay(() -> {
            startDiscovery();
        }, 1, bridgeConfig.getRefresh(), TimeUnit.SECONDS);
    }

    private void login() {
        String url = null;

        try {
            url = JABLOTRON_URL + "ajax/login.php";
            String urlParameters = "login=" + bridgeConfig.getLogin() + "&heslo=" + bridgeConfig.getPassword() + "&aStatus=200&loginType=Login";
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Referer", JABLOTRON_URL);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            setConnectionDefaults(connection);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            String line = Utils.readResponse(connection);
            JablotronLoginResponse response = gson.fromJson(line, JablotronLoginResponse.class);

            if (!response.isOKStatus()) {
                logger.error("Invalid response: {}", line);
                return;
            }

            //get cookie
            session = Utils.getSessionCookie(connection);
            if (!session.equals("")) {
                logger.debug("Successfully logged to Jablotron cloud!");
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.error("Cannot log in to Jablotron cloud!");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot login to Jablonet cloud");
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (Exception e) {
            logger.error("Cannot get Jablotron login cookie: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
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
        login();
        if (!thing.getStatus().equals(ThingStatus.ONLINE)) {
            return;
        }
        discoverServices();
        logout();
    }

    private void discoverServices() {
        try {
            String url = JABLOTRON_URL + "ajax/widget-new.php?" + Utils.getBrowserTimestamp();

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL + "cloud");
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            setConnectionDefaults(connection);

            String line = Utils.readResponse(connection);
            JablotronWidgetsResponse response = gson.fromJson(line, JablotronWidgetsResponse.class);

            if (!response.isOKStatus()) {
                logger.error("Invalid widgets response: {}", line);
                return;
            }

            if (response.getCntWidgets() == 0) {
                logger.error("Cannot found any Jablotron device");
                return;
            }

            for (int i = 0; i < response.getCntWidgets(); i++) {
                String serviceId = String.valueOf(response.getWidgets().get(i).getId());
                url = response.getWidgets().get(i).getUrl();
                logger.debug("Found Jablotron service: {} id: {}", response.getWidgets().get(i).getName(), serviceId);
                if (response.getWidgets().get(i).getTemplateService().equals(THING_TYPE_OASIS.getId())) {
                    discoveryService.oasisDiscovered("Jablotron OASIS Alarm", serviceId, url);
                } else {
                    logger.error("Unsupported device type discovered: {}", response.getWidgets().get(i).getTemplateService());
                }
            }
        } catch (Exception ex) {
            logger.error("Cannot discover Jablotron services!", ex);
        }
    }

    private void logout() {

        String url = JABLOTRON_URL + "logout";
        try {
            URL cookieUrl = new URL(url);

            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL);
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            setConnectionDefaults(connection);

            String line = Utils.readResponse(connection);
            logger.debug("logout... {}", line);
        } catch (Exception e) {
            //Silence
            //logger.error(e.toString());
        } finally {
            session = "";
        }
    }
}
