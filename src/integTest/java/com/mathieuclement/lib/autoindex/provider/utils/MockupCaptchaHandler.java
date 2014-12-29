package com.mathieuclement.lib.autoindex.provider.utils;

import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Created by mathieu on 12/29/14.
 */
public class MockupCaptchaHandler implements CaptchaHandler {
    @Override
    public String handleCaptchaImage(int requestId, String captchaImageUrl, HttpClient httpClient, HttpHost httpHost, HttpContext httpContext, String httpHostHeaderValue, CaptchaAutoIndexProvider captchaAutoIndexProvider) throws CaptchaException {
        return "";
    }

    @Override
    public void onCaptchaFailed() {

    }

    @Override
    public void onCaptchaSuccessful() {

    }
}
