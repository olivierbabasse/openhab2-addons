package org.openhab.binding.jablotron.model;

public class JablotronEvent {
    private String datum;
    private long time;
    private String code;
    private String event;

    public String getDatum() {
        return datum;
    }

    public long getTime() {
        return time;
    }

    public String getCode() {
        return code;
    }

    public String getEvent() {
        return event;
    }
}
