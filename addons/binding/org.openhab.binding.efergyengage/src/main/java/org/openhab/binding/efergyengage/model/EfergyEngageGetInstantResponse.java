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
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the response of getting instant power consumption.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetInstantResponse {
    int reading;
    long last_reading_time;

    public int getReading() {
        return reading;
    }

    public long getLastReadingTime() {
        return last_reading_time;
    }
}
