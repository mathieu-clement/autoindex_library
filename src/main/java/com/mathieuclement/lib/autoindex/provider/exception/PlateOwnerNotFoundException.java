package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

/**
 * The plate owner could not be found.
 */
public class PlateOwnerNotFoundException extends PlateRequestException {

    public PlateOwnerNotFoundException(String message, Plate plate) {
        super(message, plate);
    }

    public PlateOwnerNotFoundException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
