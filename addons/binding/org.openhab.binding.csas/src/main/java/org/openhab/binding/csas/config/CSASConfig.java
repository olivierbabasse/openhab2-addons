package org.openhab.binding.csas.config;

public class CSASConfig {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String webAPIKey;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getWebAPIKey() {
        return webAPIKey;
    }
}
