package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import org.apache.http.HttpHost;

public class ValaisAutoIndexProvider extends CariAutoIndexProvider {
    protected ValaisAutoIndexProvider(CaptchaHandler captchaHandler) {
        super(captchaHandler);
    }

    @Override
    protected String getCariOnlineFullUrl() {
        return "http://www.vs.ch/cari-online/";
    }

    private HttpHost httpHost = new HttpHost("www.vs.ch");

    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "www.vs.ch";
    }
}
