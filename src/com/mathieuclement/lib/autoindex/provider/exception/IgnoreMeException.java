package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

public class IgnoreMeException extends PlateRequestException {
    public IgnoreMeException(String message, Plate plate) {
        super(message, plate);
    }
}
