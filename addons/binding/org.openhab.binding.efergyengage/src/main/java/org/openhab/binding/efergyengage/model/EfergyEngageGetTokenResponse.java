package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageGetTokenResponse} represents the model of
 * the login process response message.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetTokenResponse {
    String status;
    String token;
    String desc;

    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public String getDesc() {
        return desc;
    }
}
