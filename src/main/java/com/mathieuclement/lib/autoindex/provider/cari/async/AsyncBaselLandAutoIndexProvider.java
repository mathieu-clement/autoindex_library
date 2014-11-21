package com.mathieuclement.lib.autoindex.provider.cari.async;

import org.apache.http.HttpHost;

public class AsyncBaselLandAutoIndexProvider extends AsyncGermanCariAutoIndexProvider {

    @Override
    protected String getCariOnlineFullUrl() {
        return "http://www.mfk-haltersuche.bl.ch/cari-online/";
    }

    private HttpHost httpHost = new HttpHost("www.mfk-haltersuche.bl.ch");
    //private HttpHost httpHost = new HttpHost("193.247.117.81");


    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "www.mfk-haltersuche.bl.ch";
    }
}
