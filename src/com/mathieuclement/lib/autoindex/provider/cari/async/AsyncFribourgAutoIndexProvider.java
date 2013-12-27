package com.mathieuclement.lib.autoindex.provider.cari.async;

import org.apache.http.HttpHost;

public class AsyncFribourgAutoIndexProvider extends AsyncCariAutoIndexProvider {
    @Override
    protected String getCariOnlineFullUrl() {
        return "https://appls.fr.ch/cari-online/";
    }

    //private HttpHost httpHost = new HttpHost("156.25.9.252", 443, "https"); // appls2.fr.ch
    private HttpHost httpHost = new HttpHost("appls.fr.ch", 443, "https"); // Request has to be done with the correct name matching the Server certificate!

    @Override
    protected HttpHost getCariHttpHost() {
        return httpHost;
    }

    @Override
    protected String getCariHttpHostname() {
        return "appls.fr.ch";
    }
}
