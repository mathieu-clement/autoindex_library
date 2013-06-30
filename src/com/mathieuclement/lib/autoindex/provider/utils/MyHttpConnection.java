package com.mathieuclement.lib.autoindex.provider.utils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.params.HttpParams;

/**
 * @author Mathieu Clément
 * @since 29.06.2013
 */
public class MyHttpConnection extends DefaultHttpClientConnection {
    @Override
    protected HttpMessageParser<HttpResponse> createResponseParser(SessionInputBuffer buffer, HttpResponseFactory responseFactory, HttpParams params) {
        return super.createResponseParser(buffer, responseFactory, params);
    }
}
