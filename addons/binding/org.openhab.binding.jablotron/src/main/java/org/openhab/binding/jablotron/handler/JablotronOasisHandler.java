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
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.config.OasisConfig;
import org.openhab.binding.jablotron.internal.Utils;
import org.openhab.binding.jablotron.model.JablotronControlResponse;
import org.openhab.binding.jablotron.model.JablotronEvent;
import org.openhab.binding.jablotron.model.JablotronLoginResponse;
import org.openhab.binding.jablotron.model.JablotronStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

/**
 * The {@link JablotronOasisHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronOasisHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronOasisHandler.class);

    private Gson gson = new Gson();

    private OasisConfig thingConfig;
    private String session = "";
    private int stavA = 0;
    private int stavB = 0;
    private int stavABC = 0;
    private int stavPGX = 0;
    private int stavPGY = 0;
    private boolean controlDisabled = true;
    private boolean inService = true;
    private int lastHours = -1;


    public JablotronOasisHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_COMMAND) && command instanceof StringType) {
            scheduler.schedule(() -> {
                sendCommand(command.toString(), thingConfig.getUrl());
            }, 0, TimeUnit.SECONDS);
        }
    }

    @Override
    public void initialize() {
        thingConfig = getConfigAs(OasisConfig.class);
        scheduler.schedule(() -> {
            doInit();
        }, 0, TimeUnit.SECONDS);
    }

    private void doInit() {
        login();
        initializeService();

        scheduler.scheduleWithFixedDelay(() -> {
            updateAlarmStatus();
        }, 1, thingConfig.getRefresh(), TimeUnit.SECONDS);
    }

    private void readAlarmStatusNew(JablotronStatusResponse response) {
        logger.debug("Reading alarm status...");
        controlDisabled = response.isControlDisabled();

        stavA = response.getSekce().get(0).getStav();
        stavB = response.getSekce().get(1).getStav();
        stavABC = response.getSekce().get(2).getStav();

        stavPGX = response.getPgm().get(0).getStav();
        stavPGY = response.getPgm().get(1).getStav();

        logger.debug("Stav A: {}", stavA);
        logger.debug("Stav B: {}", stavB);
        logger.debug("Stav ABC: {}", stavABC);
        logger.debug("Stav PGX: {}", stavPGX);
        logger.debug("Stav PGY: {}", stavPGY);

        for (Channel channel : getThing().getChannels()) {
            State newState = null;
            String type = channel.getUID().getId();

            switch (type) {
                case CHANNEL_STATUS_A:
                    newState = (stavA == 1) ? OnOffType.ON : OnOffType.OFF;
                    break;
                case CHANNEL_STATUS_B:
                    newState = (stavB == 1) ? OnOffType.ON : OnOffType.OFF;
                    break;
                case CHANNEL_STATUS_ABC:
                    newState = (stavABC == 1) ? OnOffType.ON : OnOffType.OFF;
                    break;
                case CHANNEL_STATUS_PGX:
                    newState = (stavPGX == 1) ? OnOffType.ON : OnOffType.OFF;
                    break;
                case CHANNEL_STATUS_PGY:
                    newState = (stavPGY == 1) ? OnOffType.ON : OnOffType.OFF;
                    break;
                case CHANNEL_ALARM:
                    newState = (response.isAlarm()) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                    break;
                case CHANNEL_LAST_EVENT_TIME:
                    Date lastEvent = response.getLastEventTime();
                    if (lastEvent != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(lastEvent);
                        newState = new DateTimeType(cal);
                    }
                    break;
                default:
                    break;
            }

            if (newState != null) {
                //eventPublisher.postUpdate(itemName, newState);
                updateState(channel.getUID(), newState);
            }
        }
    }

    private synchronized JablotronStatusResponse sendGetStatusRequest() {

        String url = JABLOTRON_URL + "app/oasis/ajax/stav.php?" + Utils.getBrowserTimestamp();
        try {
            URL cookieUrl = new URL(url);

            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL + OASIS_SERVICE_URL + thingConfig.getServiceId());
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            setConnectionDefaults(connection);

            String line = Utils.readResponse(connection);
            return gson.fromJson(line, JablotronStatusResponse.class);
        } catch (Exception e) {
            logger.error("sendGetStatusRequest exception", e);
            return null;
        }
    }

    private synchronized boolean updateAlarmStatus() {
        logger.debug("updating alarm status...");

        // relogin every hour
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        if (lastHours >= 0 && lastHours != hours) {
            relogin();
        }
        lastHours = hours;

        JablotronStatusResponse response = sendGetStatusRequest();

        if (response == null || response.getStatus() != 200) {
            session = "";
            controlDisabled = true;
            inService = false;
            login();
            initializeService();
            response = sendGetStatusRequest();
        }
        if (response.isBusyStatus()) {
            logger.warn("OASIS is busy...giving up");
            logout();
            return false;
        }
        if (response.hasEvents()) {
            ArrayList<JablotronEvent> events = response.getEvents();
            for (JablotronEvent event : events) {
                logger.debug("Found event: {} {} {}", event.getDatum(), event.getCode(), event.getEvent());
                updateLastEvent(event.getCode());
            }
        }

        inService = response.inService();

        if (inService) {
            logger.warn("Alarm is in service mode...");
            return false;
        }

        if (response.isOKStatus() && response.hasSectionStatus()) {
            readAlarmStatusNew(response);
        } else {
            logger.error("Cannot get alarm status!");
            session = "";
            return false;
        }
        return true;
    }

    private void relogin() {
        logger.info("Doing relogin");
        logout();
        login();
        initializeService();
    }

    private void updateLastEvent(String code) {
        for (Channel channel : getThing().getChannels()) {
            if (channel.getUID().getId().equals(CHANNEL_LAST_EVENT)) {
                updateState(channel.getUID(), new StringType(code));
            }
        }
    }


    public synchronized void sendCommand(String code, String serviceUrl) {
        int status = 0;
        int result = 0;
        try {
            if (!getThing().getStatus().equals(ThingStatus.ONLINE)) {
                login();
                initializeService();
            }
            if (!updateAlarmStatus()) {
                logger.error("Cannot send user code due to alarm status!");
                return;
            }
            while (controlDisabled) {
                logger.debug("Waiting for control enabling...");
                Thread.sleep(1000);
                updateAlarmStatus();
            }

            JablotronControlResponse response = sendUserCode("", serviceUrl);
            if (response == null) {
                return;
            }

            status = response.getStatus();
            result = response.getVysledek();
            if (status == 200 && result == 4) {
                logger.debug("Sending user code: {}", code);
                response = sendUserCode(code, serviceUrl);
            } else {
                logger.warn("Received unknown status: {}", status);
            }
            //handleJablotronResult(response);
            handleHttpRequestStatus(response.getStatus());
        } catch (Exception e) {
            logger.error("internalReceiveCommand exception", e);
        }
    }

    private void handleHttpRequestStatus(int status) throws InterruptedException {
        switch (status) {
            case 0:
                logout();
                break;
            case 201:
                logout();
                break;
            case 300:
                logger.error("Redirect not supported");
                break;
            case 800:
                login();
                initializeService();
                break;
            case 200:
                scheduler.schedule(() -> {
                    updateAlarmStatus();
                }, 0, TimeUnit.SECONDS);
                scheduler.schedule(() -> {
                    updateAlarmStatus();
                }, 10, TimeUnit.SECONDS);
                break;
            default:
                logger.error("Unknown status code received: {}", status);
        }
    }

    private synchronized JablotronControlResponse sendUserCode(String code, String serviceUrl) {
        String url;

        try {
            url = JABLOTRON_URL + "app/oasis/ajax/ovladani.php";
            String urlParameters = "section=STATE&status=" + ((code.isEmpty()) ? "1" : "") + "&code=" + code;
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            JablotronControlResponse response;

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Referer", serviceUrl);
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            setConnectionDefaults(connection);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }
            String line = Utils.readResponse(connection);
            response = gson.fromJson(line, JablotronControlResponse.class);

            logger.debug("sendUserCode result: {}", response.getVysledek());
            return response;
        } catch (Exception ex) {
            logger.error("sendUserCode exception", ex);
        }
        return null;
    }

    private void logout() {

        String url = JABLOTRON_URL + "logout";
        try {
            URL cookieUrl = new URL(url);

            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL + OASIS_SERVICE_URL + thingConfig.getServiceId());
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            setConnectionDefaults(connection);

            String line = Utils.readResponse(connection);
            logger.debug("logout... {}", line);
            updateStatus(ThingStatus.OFFLINE);
        } catch (Exception e) {
            //Silence
            //logger.error(e.toString());
        } finally {
            controlDisabled = true;
            inService = false;
            session = "";
        }
    }

    private void setConnectionDefaults(HttpsURLConnection connection) {
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", AGENT);
        connection.setRequestProperty("Accept-Language", "cs-CZ");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setUseCaches(false);
    }

    private synchronized void login() {
        String url = null;

        try {
            //login
            stavA = 0;
            stavB = 0;
            stavABC = 0;
            stavPGX = 0;
            stavPGY = 0;

            JablotronBridgeHandler bridge = (JablotronBridgeHandler) this.getBridge().getHandler();
            url = JABLOTRON_URL + "ajax/login.php";
            String urlParameters = "login=" + bridge.bridgeConfig.getLogin() + "&heslo=" + bridge.bridgeConfig.getPassword() + "&aStatus=200&loginType=Login";
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
            if (response == null) {
                logger.error("Login response is not json! {}", line);
                return;
            }

            if (!response.isOKStatus())
                return;

            //get cookie
            session = Utils.getSessionCookie(connection);
            if (!session.equals("")) {
                logger.debug("Successfully logged to Jablonet cloud!");
            } else {
                logger.error("Cannot log in to Jablonet cloud!");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot login to Jablonet cloud");
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (Exception e) {
            logger.error("Cannot get Jablotron login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    private void initializeService() {
        String url = thingConfig.getUrl();
        String serviceId = thingConfig.getServiceId();
        try {
            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", JABLOTRON_URL);
            connection.setRequestProperty("Cookie", session);
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            setConnectionDefaults(connection);

            if (connection.getResponseCode() == 200) {
                logger.info("Jablotron OASIS service: {} successfully initialized", serviceId);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.error("Cannot initialize Jablotron service: {}", serviceId);
                logger.error("Got response code: {}", connection.getResponseCode());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot initialize OASIS service");
            }
        } catch (Exception ex) {
            logger.error("Cannot initialize Jablotron service: {}", serviceId, ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot initialize OASIS service");
        }
    }
}
