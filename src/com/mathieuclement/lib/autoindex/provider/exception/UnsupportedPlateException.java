package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

public class UnsupportedPlateException extends PlateRequestException {

    public UnsupportedPlateException(String message, Plate plate) {
        super(message, plate);
    }

    public UnsupportedPlateException(String message, Throwable cause, Plate plate) {
        super(message, cause, plate);
    }
}
