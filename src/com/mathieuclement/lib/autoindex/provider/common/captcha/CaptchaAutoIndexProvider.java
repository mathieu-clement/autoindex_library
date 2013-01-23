package com.mathieuclement.lib.autoindex.provider.common.captcha;

import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;

/**
 * An AutoIndexProvider which requires captcha.<br/>
 * Add your own listener to listen when captcha is required and ask the user to copy the captcha.<br/>
 * You can ask the provider to get another captcha by calling the {@link #regenerateCaptchaImageUrl()} method.
 */
public abstract class CaptchaAutoIndexProvider implements AutoIndexProvider {
    public CaptchaHandler captchaHandler;

    protected CaptchaAutoIndexProvider(CaptchaHandler captchaHandler) {
        this.captchaHandler = captchaHandler;
    }

    public abstract String regenerateCaptchaImageUrl();
}
