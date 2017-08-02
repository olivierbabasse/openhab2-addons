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
