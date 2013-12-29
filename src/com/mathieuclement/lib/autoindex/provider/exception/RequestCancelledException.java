package com.mathieuclement.lib.autoindex.provider.exception;

import com.mathieuclement.lib.autoindex.plate.Plate;

/**
 * @author Mathieu Clément
 * @since 29.12.2013
 */
public class RequestCancelledException extends PlateRequestException {
    private int requestId;

    public RequestCancelledException(String message, Plate plate, int requestId) {
        super(message, plate);
        this.requestId = requestId;
    }

    public RequestCancelledException(String message, Throwable cause, Plate plate, int requestId) {
        super(message, cause, plate);
        this.requestId = requestId;
    }

    public RequestCancelledException(String s, int requestId) {
        super(s);
        this.requestId = requestId;
    }

    public int getRequestId() {
        return requestId;
    }
}
