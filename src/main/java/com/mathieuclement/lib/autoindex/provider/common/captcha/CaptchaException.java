package com.mathieuclement.lib.autoindex.provider.common.captcha;

/**
 * Exception related to the captcha. (Network or solved incorrectly)
 */
public class CaptchaException extends Exception {
    public CaptchaException() {
    }

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaptchaException(Throwable cause) {
        super(cause);
    }
}
