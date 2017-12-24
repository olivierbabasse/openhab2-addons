/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.handler;

import com.google.gson.Gson;
import org.eclipse.smarthome.core.cache.ExpiringCacheMap;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.efergyengage.internal.EfergyEngageException;
import org.openhab.binding.efergyengage.internal.config.EfergyEngageConfig;
import org.openhab.binding.efergyengage.internal.model.*;
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

    private Gson gson = new Gson();

    //caches
    private ExpiringCacheMap<String, EfergyEngageMeasurement> cache;
    private ExpiringCacheMap<String, EfergyEngageEstimate> cacheEstimate;

    /**
     * Our configuration
     */
    private EfergyEngageConfig thingConfig;

    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command.equals(RefreshType.REFRESH) && token != null) {
            updateChannel(channelUID);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Efergy Engage configuration");
        String thingUid = getThing().getUID().toString();
        thingConfig = getConfigAs(EfergyEngageConfig.class);
        thingConfig.setThingUid(thingUid);
        int refresh = thingConfig.getRefresh();
        utcOffset = thingConfig.getUtcOffset();
        cache = new ExpiringCacheMap<>(CACHE_EXPIRY);
        cacheEstimate = new ExpiringCacheMap<>(CACHE_EXPIRY);

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
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            String line = readResponse(connection);

            EfergyEngageGetTokenResponse response = gson.fromJson(line, EfergyEngageGetTokenResponse.class);
            logger.debug("Efergy login response: {}", line);

            if (response.getStatus().equals("ok")) {
                token = response.getToken();
                logger.debug("Efergy token: {}", token);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.error("Efergy login response: {}", line);
                throw new EfergyEngageException(response.getDesc());
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (EfergyEngageException e) {
            logger.error("Bad login response", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Can not access device as username and/or password are invalid");
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage token", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    private EfergyEngageMeasurement readInstant() {
        String url = null;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getInstant?token=" + token;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            String line = readResponse(connection);

            //read value
            EfergyEngageGetInstantResponse response = gson.fromJson(line, EfergyEngageGetInstantResponse.class);
            if (response.getError() == null) {
                measurement.setValue(response.getReading());
                measurement.setMilis(response.getLastReadingTime());
            } else {
                logger.error("{} - {}", response.getError().getDesc(), response.getError().getMore());
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }

        return measurement;
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                execute();
            } catch (Exception e) {
                logger.debug("Exception during poll", e);
            }
        }, 10, refresh, TimeUnit.SECONDS);

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
    private void execute() {
        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            login();
        }

        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            logger.warn("The thing is still not online!");
            return;
        }

        try {
            for (Channel channel : getThing().getChannels()) {
                updateChannel(channel.getUID());
            }
        } catch (Exception ex) {
            logger.error("Error during updating channels", ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    private void updateChannel(ChannelUID uid) {
        EfergyEngageMeasurement value;
        EfergyEngageEstimate est;
        State state;
        switch (uid.getId()) {
            case CHANNEL_INSTANT:
                value = readInstantCached();
                state = new DecimalType(value.getValue());
                updateState(uid, state);
                break;
            case CHANNEL_ESTIMATE:
                est = readForecastCached();
                if (est == null) {
                    logger.warn("A null forecast received!");
                    return;
                }
                state = new DecimalType(est.getEstimate());
                updateState(uid, state);
                break;
            case CHANNEL_COST:
                est = readForecastCached();
                if (est == null) {
                    logger.warn("A null forecast received!");
                    return;
                }
                state = new DecimalType(est.getPreviousSum());
                updateState(uid, state);
                break;
            case CHANNEL_LAST_MEASUREMENT:
                value = readInstantCached();
                if (value.getMilis() > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new java.util.Date(value.getMilis()));
                    updateState(uid, new DateTimeType(cal));
                }
                break;
            case CHANNEL_DAYTOTAL:
                value = readEnergy(DAY);
                state = new StringType((value.getValue() + " " + value.getUnit()));
                updateState(uid, state);
                break;
            case CHANNEL_WEEKTOTAL:
                value = readEnergy(WEEK);
                state = new StringType((value.getValue() + " " + value.getUnit()));
                updateState(uid, state);
                break;
            case CHANNEL_MONTHTOTAL:
                value = readEnergy(MONTH);
                state = new StringType((value.getValue() + " " + value.getUnit()));
                updateState(uid, state);
                break;
            case CHANNEL_YEARTOTAL:
                value = readEnergy(YEAR);
                state = new StringType((value.getValue() + " " + value.getUnit()));
                updateState(uid, state);
                break;
        }
    }

    private EfergyEngageEstimate readForecastCached() {
        if (cacheEstimate.get(CHANNEL_ESTIMATE) == null) {
            cacheEstimate.put(CHANNEL_ESTIMATE, () -> readForecast());
        }
        return cacheEstimate.get(CHANNEL_ESTIMATE);
    }

    private EfergyEngageMeasurement readInstantCached() {
        if (cache.get(CHANNEL_INSTANT) == null) {
            cache.put(CHANNEL_INSTANT, () -> readInstant());
        }
        return cache.get(CHANNEL_INSTANT);
    }

    private EfergyEngageMeasurement readEnergy(String period) {
        String url = null;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getEnergy?token=" + token + "&period=" + period + "&offset=" + utcOffset;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            String line = readResponse(connection);

            //read value
            EfergyEngageGetEnergyResponse response = gson.fromJson(line, EfergyEngageGetEnergyResponse.class);
            Float energy = Float.valueOf(-1);
            String units = "";
            if (response.getError() == null) {
                energy = response.getSum();
                units = response.getUnits();
                logger.debug("Efergy reading for {} period: {} {}", period, energy, units);
            } else {
                logger.error("{} - {}", response.getError().getDesc(), response.getError().getMore());
            }
            measurement.setValue(energy);
            measurement.setUnit(units);
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
        return measurement;
    }

    private EfergyEngageEstimate readForecast() {
        String url = null;

        try {
            url = EFERGY_URL + "/mobile_proxy/getForecast?token=" + token + "&dataType=cost&period=month&offset=" + utcOffset;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            String line = readResponse(connection);

            //read value
            EfergyEngageGetForecastResponse response = gson.fromJson(line, EfergyEngageGetForecastResponse.class);
            if (response.getError() == null) {
                return response.getMonth_tariff();
            } else {
                logger.error("{} - {}", response.getError().getDesc(), response.getError().getMore());
                return null;
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage forecast", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
        return null;
    }

    private String readResponse(URLConnection connection) throws IOException {
        //read response
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        String msg = body.toString();
        logger.debug("Response: {}", msg);
        return msg;
    }

}
