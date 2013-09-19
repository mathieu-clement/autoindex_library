package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

public abstract class PlateRequestException extends Exception {
    private Plate plate;

    public PlateRequestException(String message, Plate plate) {
        super(message);
        this.plate = plate;
    }

    public PlateRequestException(String message, Throwable cause, Plate plate) {
        super(message, cause);
        this.plate = plate;
    }

    public PlateRequestException(String s) {
        super(s);
    }

    public Plate getPlate() {
        return plate;
    }
}
