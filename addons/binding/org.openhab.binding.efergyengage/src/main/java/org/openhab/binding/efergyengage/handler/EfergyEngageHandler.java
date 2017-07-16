/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.efergyengage.internal.EfergyEngageException;
import org.openhab.binding.efergyengage.internal.EfergyEngageMeasurement;
import org.openhab.binding.efergyengage.internal.config.EfergyEngageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.efergyengage.EfergyEngageBindingConstants.*;

/**
 * The {@link EfergyEngageHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(EfergyEngageHandler.class);

    public EfergyEngageHandler(Thing thing) {
        super(thing);
    }

    private String token = null;
    private int utcOffset;

    //Gson parser
    private final JsonParser parser = new JsonParser();

    /**
     * Our configuration
     */
    protected EfergyEngageConfig thingConfig;

    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.

        logger.debug("Efergy Engage configuration");
        String thingUid = getThing().getUID().toString();
        thingConfig = getConfigAs(EfergyEngageConfig.class);
        thingConfig.setThingUid(thingUid);
        int refresh = thingConfig.getRefresh();
        utcOffset = thingConfig.getUtcOffset();

        login();
        initPolling(refresh);
    }

    @Override
    public void dispose() {
        stopPolling();
    }

    private void login() {
        String url = null;

        try {
            String email = thingConfig.getEmail();
            String password = thingConfig.getPassword();
            String device = thingConfig.getDevice();


            url = EFERGY_URL + "/mobile/get_token?device=" + device + "&username=" + email
                    + "&password=" + password;

            URL tokenUrl = new URL(url);
            URLConnection connection = tokenUrl.openConnection();

            String line = readResponse(connection);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            String status = jobject.get("status").getAsString();

            if (status.equals("ok")) {
                token = jobject.get("token").getAsString();
                logger.debug("Efergy token: {}", token);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("Efergy login response: {}", line);
                throw new EfergyEngageException(jobject.get("desc").getAsString());
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (EfergyEngageException e) {
            logger.error("Bad login response: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Can not access device as username and/or password are invalid");
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage token: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    private EfergyEngageMeasurement readInstant() {
        String url = null;
        int instant = -1;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getInstant?token=" + token;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();

            String line = readResponse(connection);

            //read value
            JsonObject jobject = parser.parse(line).getAsJsonObject();
            if (jobject != null && jobject.get("reading") != null) {
                instant = jobject.get("reading").getAsInt();
                logger.debug("Efergy reading: {}", instant);
                measurement.setValue(instant);
                measurement.setMilis(jobject.get("last_reading_time").getAsLong());
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            return null;
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            return null;
        }

        return measurement;
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                        execute();
                } catch (Exception e) {
                    logger.debug("Exception during poll : {}", e);
                }
            }
        }, 0, refresh, TimeUnit.SECONDS);

    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    /**
     * The polling future executes this every iteration
     */
    protected void execute() {

        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            login();
        }

        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            return;
        }

        EfergyEngageMeasurement instant = null;
        EfergyEngageMeasurement dayTotal = null;
        EfergyEngageMeasurement weekTotal = null;
        EfergyEngageMeasurement monthTotal = null;
        EfergyEngageMeasurement yearTotal = null;
        State state;


        for (Channel channel : getThing().getChannels()) {
            switch (channel.getUID().getId()) {
                case CHANNEL_INSTANT:
                    if (instant == null)
                        instant = readInstant();
                    state = new DecimalType(instant.getValue());
                    updateState(channel.getUID(), state);
                    break;
                case CHANNEL_LAST_MEASUREMENT:
                    if (instant == null)
                        instant = readInstant();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new java.util.Date(instant.getMilis()));
                    updateState(channel.getUID(), new DateTimeType(cal));
                    break;
                case CHANNEL_DAYTOTAL:
                    if (dayTotal == null)
                        dayTotal = readEnergy("day");
                    state = new StringType((dayTotal.getValue() + " " + dayTotal.getUnit()));
                    updateState(channel.getUID(), state);
                    break;
                case CHANNEL_WEEKTOTAL:
                    if (weekTotal == null)
                        weekTotal = readEnergy("week");
                    state = new StringType((weekTotal.getValue() + " " + weekTotal.getUnit()));
                    updateState(channel.getUID(), state);
                    break;
                case CHANNEL_MONTHTOTAL:
                    if (monthTotal == null)
                        monthTotal = readEnergy("month");
                    state = new StringType((monthTotal.getValue() + " " + monthTotal.getUnit()));
                    updateState(channel.getUID(), state);
                    break;
                case CHANNEL_YEARTOTAL:
                    if (yearTotal == null)
                        yearTotal = readEnergy("year");
                    state = new StringType((yearTotal.getValue() + " " + yearTotal.getUnit()));
                    updateState(channel.getUID(), state);
                    break;
            }
        }
    }

    private EfergyEngageMeasurement readEnergy(String period) {
        String url = null;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getEnergy?token=" + token + "&period=" + period + "&offset=" + utcOffset;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();

            String line = readResponse(connection);

            //read value
            JsonObject jobject = parser.parse(line).getAsJsonObject();
            if (jobject != null && jobject.get("sum") != null && jobject.get("units") != null) {
                String energy = jobject.get("sum").getAsString();
                String units = jobject.get("units").getAsString();

                logger.debug("Efergy reading for {} period: {} {}", period, energy, units);
                measurement.setValue(Float.valueOf(energy.trim()).floatValue());
                measurement.setUnit(units);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
        return measurement;
    }

    private String readResponse(URLConnection connection) throws IOException {
        //read response
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line + "\n");
        }
        return body.toString();
    }

}
