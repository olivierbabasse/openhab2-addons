package org.openhab.binding.jablotron.model;

public class JablotronLoginResponse {
    private int status;

    public int getStatus() {
        return status;
    }

    public boolean isOKStatus() {
        return status == 200;
    }
}
