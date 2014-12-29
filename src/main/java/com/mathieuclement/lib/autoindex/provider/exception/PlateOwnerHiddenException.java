package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

/**
 * The plate owner data is hidden.
 */
public class PlateOwnerHiddenException extends PlateRequestException {

    public PlateOwnerHiddenException(String message, Plate plate) {
        super(message, plate);
    }

    public PlateOwnerHiddenException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
