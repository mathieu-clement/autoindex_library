package com.mathieuclement.lib.autoindex.provider.common.captcha;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Implement this interface when using an AutoIndexProvider which needs a CAPTCHA.<br/>
 * You will usually download the image and ask the user to type in what he sees.<br/>
 * You can call the
 */
public interface CaptchaHandler {
    String handleCaptchaImage(String captchaImageUrl, HttpClient httpClient, HttpHost httpHost, HttpContext httpContext,
                              String httpHostHeaderValue,
                              CaptchaAutoIndexProvider captchaAutoIndexProvider);

    void onCaptchaFailed();

    void onCaptchaSuccessful();
}
