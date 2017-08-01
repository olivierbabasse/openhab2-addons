package org.openhab.binding.efergyengage.model;

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
