package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import org.apache.http.HttpHost;

@Deprecated
public class FribourgAutoIndexProvider extends CariAutoIndexProvider {

    //private HttpHost httpHost = new HttpHost("156.25.9.252", 443, "https"); // appls2.fr.ch
    private HttpHost httpHost = new HttpHost("appls2.fr.ch", 443, "https"); // Request has to be done with the correct name matching the Server certificate!

    public FribourgAutoIndexProvider(CaptchaHandler captchaHandler) {
        super(captchaHandler);
    }

    @Override
    protected String getCariOnlineFullUrl() {
        return "https://appls2.fr.ch/cari-online/";
    }

    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "appls2.fr.ch";
    }
}
