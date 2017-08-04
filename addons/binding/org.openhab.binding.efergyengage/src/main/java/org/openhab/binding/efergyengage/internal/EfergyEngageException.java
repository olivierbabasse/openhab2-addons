/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal;

/**
 * The {@link EfergyEngageException} represents the exception during
 * communication with the Efergy Engage cloud platform.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class EfergyEngageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EfergyEngageException(String message) {
        super(message);
    }

    public EfergyEngageException(final Throwable cause) {
        super(cause);
    }

    public EfergyEngageException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
