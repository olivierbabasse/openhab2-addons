/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jablotron;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link JablotronBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronBindingConstants {

    private static final String BINDING_ID = "jablotron";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_OASIS = new ThingTypeUID(BINDING_ID, "oasis");


    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

    // Constants
    public static final String JABLOTRON_URL = "https://www.jablonet.net/";
    public static final String SERVICE_URL = "app/oasis?service=";
    public static final String AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36";

}
