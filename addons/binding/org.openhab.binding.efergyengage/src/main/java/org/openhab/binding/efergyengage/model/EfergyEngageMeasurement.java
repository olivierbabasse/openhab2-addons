/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.model;

/**
 * The {@link EfergyEngageMeasurement} represents the result value of
 * power consumption read from the Efergy Engage cloud platform.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageMeasurement {

    private float value = -1;
    private String unit = "W";
    private long milis = 0;

    public EfergyEngageMeasurement() {
    }


    EfergyEngageMeasurement(float value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public long getMilis() {
        return milis;
    }

    public void setMilis(long milis) {
        this.milis = milis;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String toString() {
        return value + " " + unit;
    }
}
