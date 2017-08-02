package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the montly money spending estimation based on consumption this month.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageEstimate {
    float estimate;
    float previousSum;

    public float getEstimate() {
        return estimate;
    }

    public float getPreviousSum() {
        return previousSum;
    }
}
