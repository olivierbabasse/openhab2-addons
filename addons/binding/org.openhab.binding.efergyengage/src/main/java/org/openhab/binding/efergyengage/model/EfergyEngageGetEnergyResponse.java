package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the response of total power consumptions.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetEnergyResponse {
    Float sum;
    String units;

    public Float getSum() {
        return sum;
    }

    public String getUnits() {
        return units;
    }
}
