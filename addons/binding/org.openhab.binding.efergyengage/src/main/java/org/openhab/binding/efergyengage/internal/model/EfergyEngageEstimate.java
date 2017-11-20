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
 * the montly money spending estimation based on consumption this month.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageEstimate {
    float estimate;
    float previousSum;

    public float getEstimate() {
        return estimate;
    }

    public float getPreviousSum() {
        return previousSum;
    }
}
