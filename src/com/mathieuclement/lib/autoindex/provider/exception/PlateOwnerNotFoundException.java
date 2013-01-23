package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

public class PlateOwnerNotFoundException extends PlateRequestException {

    public PlateOwnerNotFoundException(String message, Plate plate) {
        super(message, plate);
    }

    public PlateOwnerNotFoundException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
