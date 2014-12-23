package com.mathieuclement.lib.autoindex.provider.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

// Android doesn't support URIBuilder yet, so....
public class URIBuilder {
    private String baseUri;
    private final static String ENCODING = "UTF-8";

    private Map<String, String> queryParams = new LinkedHashMap<>();

    public URIBuilder(String baseUri) {
        this.baseUri = baseUri;
    }

    public void addParameter(String name, String value) {
        try {
            name = URLEncoder.encode(name, ENCODING);
            value = URLEncoder.encode(value, ENCODING);
            queryParams.put(name, value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public URI build() {
        StringBuilder sb = new StringBuilder(queryParams.size()*30+baseUri.length());
        sb.append(baseUri);
        sb.append('?');
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue());
            sb.append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        return URI.create(sb.toString());
    }


}
