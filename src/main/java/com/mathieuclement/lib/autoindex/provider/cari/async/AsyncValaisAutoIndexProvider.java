package com.mathieuclement.lib.autoindex.provider.cari.async;

import org.apache.http.HttpHost;

public class AsyncValaisAutoIndexProvider extends AsyncCariAutoIndexProvider {

    @Override
    protected String getCariOnlineFullUrl() {
        return "http://www.vs.ch/cari-online/";
    }

    private HttpHost httpHost = new HttpHost("www.vs.ch");
    //private HttpHost httpHost = new HttpHost("193.247.117.81");


    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "www.vs.ch";
    }
}
