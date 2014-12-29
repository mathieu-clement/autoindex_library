package com.mathieuclement.lib.autoindex.provider.utils;

import com.mathieuclement.lib.autoindex.provider.cari.sync.CariAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @author Mathieu Clément
 * @since 29.12.2013
 */
public class WebServiceBasedCaptchaHandler implements CaptchaHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("autoindex.WebServiceBasedCaptchaHandler");

    public String handleCaptchaImage(int requestId, String captchaImageUrl, HttpClient httpClient, HttpHost httpHost,
                                     HttpContext httpContext,
                                     String httpHostHeaderValue,
                                     CaptchaAutoIndexProvider captchaAutoIndexProvider) throws CaptchaException {
        String s = null;
        File captchaImageFile = null;
        FileOutputStream fos = null;
        try {
            LOGGER.debug("Downloading image...");
            BasicHttpRequest httpRequest = new BasicHttpRequest("GET", captchaImageUrl, HttpVersion.HTTP_1_1);
            httpRequest.setHeader("host", httpHostHeaderValue);
            HttpResponse httpResponse = httpClient.execute(httpHost, httpRequest, httpContext);
            captchaImageFile = File.createTempFile("captcha", ".jpg");
            fos = new FileOutputStream(captchaImageFile);
            httpResponse.getEntity().writeTo(fos);
            fos.close();
            //httpResponse.getEntity().getContent().close();

            try {
                s = solveCaptcha(captchaImageFile, httpClient, captchaAutoIndexProvider).replace("\n", "");
            } catch (Exception e) {
                LOGGER.warn("Failed to decode captcha");
                throw e;
            }

        } catch (Exception e) {
            if (!captchaAutoIndexProvider.isCancelled(requestId)) {
                captchaAutoIndexProvider.cancel(requestId);
                LOGGER.error("Failed to download or open captcha image");
                throw new CaptchaException(e);
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (captchaImageFile != null) {
                if (!captchaImageFile.delete()) {
                    System.err.println("Could not delete " + captchaImageFile.getAbsolutePath());
                }
            }
        }
        return s;
    }

    private String solveCaptcha(File file, HttpClient httpClient, AutoIndexProvider autoIndexProvider)
            throws IOException {
        String system = (autoIndexProvider instanceof CariAutoIndexProvider) ? "cari" : "viacar";

        HttpPost httpPost = new HttpPost("http://mathieuclement.com:13245/" + system);
        // requires HTTPMime library
        FileBody bin = new FileBody(file);
        StringBody comment = null;
        try {
            comment = new StringBody("Image",
                    "image/" + (autoIndexProvider instanceof CariAutoIndexProvider ? "jpeg" : "png"),
                    Charset.defaultCharset());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("file", bin)
                .addPart("comment", comment)
                .build();
        httpPost.setEntity(reqEntity);

        HttpResponse response = httpClient.execute(httpPost);
        LOGGER.debug("----------------------------------------");
        LOGGER.debug(response.getStatusLine().toString());
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Could not send captcha " +
                    "(server error, code " + response.getStatusLine().getStatusCode() + ")");
        }
        HttpEntity resEntity = response.getEntity();
        return EntityUtils.toString(resEntity);
    }

    @Override
    public void onCaptchaFailed() {
    }

    @Override
    public void onCaptchaSuccessful() {
    }
}
