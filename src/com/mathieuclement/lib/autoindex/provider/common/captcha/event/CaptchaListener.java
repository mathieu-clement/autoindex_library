package com.mathieuclement.lib.autoindex.provider.common.captcha.event;

import com.mathieuclement.lib.autoindex.plate.Plate;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public interface CaptchaListener {
    void onCaptchaCodeRequested(Plate plate, String captchaImageUrl, HttpClient httpClient, HttpHost httpHost, HttpContext httpContext, String httpHostHeaderValue, AsyncAutoIndexProvider provider);

    void onCaptchaCodeAccepted(Plate plate);
}
