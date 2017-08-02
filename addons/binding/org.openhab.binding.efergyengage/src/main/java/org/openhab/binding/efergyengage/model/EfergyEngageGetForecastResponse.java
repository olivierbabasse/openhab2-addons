package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the montly money spending estimation response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetForecastResponse {

    EfergyEngageEstimate month_tariff;

    public EfergyEngageEstimate getMonth_tariff() {
        return month_tariff;
    }
}
