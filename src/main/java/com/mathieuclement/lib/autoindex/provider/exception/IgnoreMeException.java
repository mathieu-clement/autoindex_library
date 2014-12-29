package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

// TODO This class shouldn't even exist.
/**
 * An exception that must be ignored.
 */
public class IgnoreMeException extends PlateRequestException {
    public IgnoreMeException(String message, Plate plate) {
        super(message, plate);
    }
}
