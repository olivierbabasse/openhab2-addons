package org.openhab.binding.jablotron.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class JablotronWidgetsResponse {
    private int status;

    @SerializedName("cnt-widgets")
    private int cntWidgets;

    @SerializedName("widget")
    private ArrayList<JablotronWidget> widgets;

    public int getStatus() {
        return status;
    }

    public int getCntWidgets() {
        return cntWidgets;
    }

    public ArrayList<JablotronWidget> getWidgets() {
        return widgets;
    }

    public boolean isOKStatus() {
        return status == 200;
    }
}
