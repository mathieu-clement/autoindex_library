package com.mathieuclement.lib.autoindex.provider.viacar;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.ExecCommand;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.viacar.sync.ViacarAutoIndexProvider;
import junit.framework.Assert;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViacarAutoIndexProviderTest {
    private ViacarAutoIndexProvider provider;
    private Canton canton;

    public static final String CANTON_ABBR = "AG";

    @Before
    public void setUp() throws Exception {
        CaptchaHandler autoCaptchaHandler = new CaptchaHandler() {

            public File captchaImageFile;

            private void generateCaptchaImage(CaptchaAutoIndexProvider autoIndexProvider, HttpClient httpClient, HttpContext httpContext) throws IOException {
                System.out.println("Downloading image...");
                HttpResponse httpResponse = httpClient.execute(new HttpGet(autoIndexProvider.regenerateCaptchaImageUrl()), httpContext);
                captchaImageFile = File.createTempFile("cari-captcha", ".jpg");
                FileOutputStream fos = new FileOutputStream(captchaImageFile);
                httpResponse.getEntity().writeTo(fos);
                fos.close();
                httpResponse.getEntity().getContent().close();
            }

            private String solveCaptcha(File file) {
                try {
                    return ExecCommand.exec("/home/mathieu/Dropbox/work/prout/decoder_viacar.pl " + file
                            .getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String handleCaptchaImage(String captchaImageUrl, final HttpClient httpClient, HttpHost httpHost, final HttpContext httpContext, String httpHostHeaderValue, final CaptchaAutoIndexProvider captchaAutoIndexProvider) {
                System.out.println("Captcha image URL: \"" + captchaImageUrl + "\"");

                try {
                    System.out.println("Downloading image...");
                    BasicHttpRequest httpRequest = new BasicHttpRequest("GET", captchaImageUrl, HttpVersion.HTTP_1_1);
                    httpRequest.setHeader("host", httpHostHeaderValue);
                    HttpResponse httpResponse = httpClient.execute(httpHost, httpRequest, httpContext);
                    captchaImageFile = File.createTempFile("cari-captcha", ".jpg");
                    FileOutputStream fos = new FileOutputStream(captchaImageFile);
                    httpResponse.getEntity().writeTo(fos);
                    fos.close();
                    httpResponse.getEntity().getContent().close();

                    return solveCaptcha(captchaImageFile);
                } catch (IOException e) {
                    System.err.println("Failed to download or open captcha image");
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void onCaptchaFailed() {
                System.err.println("Captcha was NOT correct!");
            }

            @Override
            public void onCaptchaSuccessful() {
                System.out.println("Captcha was correct.");
            }
        };

        provider = new ViacarAutoIndexProvider(CANTON_ABBR, autoCaptchaHandler);
        canton = new Canton(CANTON_ABBR, true, provider);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAg() throws Exception {
        PlateOwner expected = new PlateOwner("Müller Verena", "Hofstrasse 49", "", 5406, "Rütihof");
        PlateOwner actual = provider.getPlateOwner(new Plate(32413, PlateType.AUTOMOBILE, canton));
        Assert.assertEquals(expected, actual);
    }
}
