package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import org.apache.http.HttpHost;

/**
 * AutoIndex provider for Fribourg.
 */
public class FribourgAutoIndexProvider extends CariAutoIndexProvider {
    public FribourgAutoIndexProvider(CaptchaHandler captchaHandler) {
        super(captchaHandler);
    }

    @Override
    protected String getCariOnlineFullUrl() {
        return "https://appls.fr.ch/cari-online/";
    }

    private HttpHost httpHost = new HttpHost("appls.fr.ch", 443,
            "https"); // Request has to be done with the correct name matching the Server certificate!

    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "appls.fr.ch";
    }
}
