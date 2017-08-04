/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal;

import static org.openhab.binding.efergyengage.EfergyEngageBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.openhab.binding.efergyengage.handler.EfergyEngageHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

/**
 * The {@link EfergyEngageHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageHandlerFactory extends BaseThingHandlerFactory {
    
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(EFERGY_ENGAGE);
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(EFERGY_ENGAGE)) {
            return new EfergyEngageHandler(thing);
        }

        return null;
    }
}

