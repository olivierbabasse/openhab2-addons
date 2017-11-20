/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal.config;

/**
 * The {@link EfergyEngageConfig} is responsible for representing the
 * Efergy Engage Hub configuration.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageConfig {

    private String email;
    private String password;
    private String device;
    private int utcOffset;
    private int refresh;

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(int utcOffset) {
        this.utcOffset = utcOffset;
    }

    private String thingUid;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }


    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getThingUid() {
        return thingUid;
    }

    public void setThingUid(String thingUid) {
        this.thingUid = thingUid;
    }
}
