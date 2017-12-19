/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
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
    public final static String CHANNEL_ESTIMATE = "estimate";
    public final static String CHANNEL_COST = "cost";
    public final static String CHANNEL_LAST_MEASUREMENT = "last_measurement";
    public final static String CHANNEL_DAYTOTAL = "daytotal";
    public final static String CHANNEL_WEEKTOTAL = "weektotal";
    public final static String CHANNEL_MONTHTOTAL = "monthtotal";
    public final static String CHANNEL_YEARTOTAL = "yeartotal";

    // other constants
    public final static String EFERGY_URL = "https://engage.efergy.com";
    public final static String DAY = "day";
    public final static String WEEK = "week";
    public final static String MONTH = "month";
    public final static String YEAR = "year";
    public final static int CACHE_EXPIRY = 5000;
    public final static int CONNECT_TIMEOUT = 3000;
    public final static int READ_TIMEOUT = 10000;
}
