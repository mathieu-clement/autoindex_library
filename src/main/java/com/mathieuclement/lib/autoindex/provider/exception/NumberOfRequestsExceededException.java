package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

/**
 * @author Mathieu Clément
 * @since 19.09.2013
 */
public class NumberOfRequestsExceededException extends ProviderException {
    public NumberOfRequestsExceededException() {
        super("Number of requests exceeded");
    }

    public NumberOfRequestsExceededException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
