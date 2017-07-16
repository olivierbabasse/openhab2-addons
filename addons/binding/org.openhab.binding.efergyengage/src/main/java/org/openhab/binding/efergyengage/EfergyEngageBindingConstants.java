/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EfergyEngageBindingConstants} class defines common constants, which are
 * used across the whole binding.
 * 
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageBindingConstants {

    public static final String BINDING_ID = "efergyengage";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID EFERGY_ENGAGE = new ThingTypeUID(BINDING_ID, "hub");

    // List of all Channel ids
    public final static String CHANNEL_INSTANT = "instant";
    public final static String CHANNEL_LAST_MEASUREMENT = "last_measurement";
    public final static String CHANNEL_DAYTOTAL = "daytotal";
    public final static String CHANNEL_WEEKTOTAL = "weektotal";
    public final static String CHANNEL_MONTHTOTAL = "monthtotal";
    public final static String CHANNEL_YEARTOTAL = "yeartotal";

    // other constants
    public final static String EFERGY_URL = "https://engage.efergy.com";


}
