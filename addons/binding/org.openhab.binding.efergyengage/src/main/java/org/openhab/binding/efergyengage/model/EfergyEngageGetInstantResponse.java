package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the response of getting instant power consumption.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetInstantResponse {
    int reading;
    long last_reading_time;

    public int getReading() {
        return reading;
    }

    public long getLastReadingTime() {
        return last_reading_time;
    }
}
