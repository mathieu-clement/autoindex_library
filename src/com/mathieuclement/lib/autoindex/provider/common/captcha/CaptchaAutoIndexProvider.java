package com.mathieuclement.lib.autoindex.provider.common.captcha;

import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.ProgressListener;

import java.util.LinkedList;
import java.util.List;

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

    private List<ProgressListener> progressListeners = new LinkedList<ProgressListener>();

    public void addListener(ProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    public void fireProgress(int current, int maximum) {
        for (ProgressListener progressListener : progressListeners) {
            progressListener.onProgress(current, maximum);
        }
    }

    public abstract boolean isIndeterminateProgress();
}
