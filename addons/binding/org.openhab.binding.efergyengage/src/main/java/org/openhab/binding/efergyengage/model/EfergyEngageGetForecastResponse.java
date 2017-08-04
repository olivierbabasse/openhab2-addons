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
 * the monthly money spending estimation response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetForecastResponse {

    EfergyEngageEstimate month_tariff;

    public EfergyEngageEstimate getMonth_tariff() {
        return month_tariff;
    }
}
