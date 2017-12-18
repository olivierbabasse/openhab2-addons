/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal.model;

/**
 * The {@link EfergyEngageGetInstantResponse} represents the model of
 * the response of total power consumptions.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetEnergyResponse {
    Float sum;
    String units;
    EfergyEngageError error;

    public Float getSum() {
        return sum;
    }

    public String getUnits() {
        return units;
    }

    public EfergyEngageError getError() {
        return error;
    }
}
