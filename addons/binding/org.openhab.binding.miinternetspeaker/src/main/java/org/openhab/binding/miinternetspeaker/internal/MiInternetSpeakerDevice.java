package org.openhab.binding.miinternetspeaker.internal;

/**
 * Created by Ondrej Pecta on 02.05.2017.
 */
public class MiInternetSpeakerDevice {
    private String friendlyName;
    private String manufacturer;
    private String modelDescription;
    private String modelName;

    public MiInternetSpeakerDevice(String friendlyName, String manufacturer, String modelDescription, String modelName) {
        this.friendlyName = friendlyName;
        this.manufacturer = manufacturer;
        this.modelDescription = modelDescription;
        this.modelName = modelName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public String getModelName() {
        return modelName;
    }
}
