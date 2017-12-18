/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal.model;

public class EfergyEngageError {
    private String desc;
    private String more;
    private Number id;

    public String getDesc() {
        return desc;
    }

    public String getMore() {
        return more;
    }

    public Number getId() {
        return id;
    }
}
