package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

/**
 * An exception or problem with the AutoIndex provider, usually unexpected, like Time Out, daily limit reached, etc.
 */
public class ProviderException extends PlateRequestException {

    public ProviderException(String message, Plate plate) {
        super(message, plate);
    }

    public ProviderException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
