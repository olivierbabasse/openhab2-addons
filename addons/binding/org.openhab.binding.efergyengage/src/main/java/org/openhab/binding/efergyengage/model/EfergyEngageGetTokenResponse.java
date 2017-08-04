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
 * The {@link EfergyEngageGetTokenResponse} represents the model of
 * the login process response message.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageGetTokenResponse {
    String status;
    String token;
    String desc;

    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public String getDesc() {
        return desc;
    }
}
