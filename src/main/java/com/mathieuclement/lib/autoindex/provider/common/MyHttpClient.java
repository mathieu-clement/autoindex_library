package com.mathieuclement.lib.autoindex.provider.common;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import java.io.InputStream;
import java.security.KeyStore;

/**
 * A custom HTTP client that knows about some SSL certificates.
 */
public class MyHttpClient extends DefaultHttpClient {

    private final InputStream storeInputStream;

    public MyHttpClient(InputStream storeInputStream) {
        this.storeInputStream = storeInputStream;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
        if (storeInputStream != null) {
            registry.register(new Scheme("https", newSslSocketFactory(storeInputStream), 443));
        } else {
            registry.register(new Scheme("https", SSLSocketFactory
                    .getSocketFactory(), 443));
        }
        return new SingleClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSslSocketFactory(InputStream storeInputStream) {
        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            try {
                trusted.load(storeInputStream, "keystorepass12345autoindexblabla".toCharArray());
            } finally {
                storeInputStream.close();
            }
            return new SSLSocketFactory(trusted);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}