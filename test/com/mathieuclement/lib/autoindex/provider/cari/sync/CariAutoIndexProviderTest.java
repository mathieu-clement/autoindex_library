package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.ExecCommand;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
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

import java.io.*;

public class CariAutoIndexProviderTest {
    private CariAutoIndexProvider fribourgAutoIndexProvider;
    private CariAutoIndexProvider valaisAutoIndexProvider;
    private Canton cantonFribourg;
    private Canton cantonValais;

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
                    return ExecCommand.exec("/home/mathieu/Dropbox/work/prout/decoder_cari.pl " + file.getAbsolutePath());
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

        fribourgAutoIndexProvider = new FribourgAutoIndexProvider(autoCaptchaHandler);
        valaisAutoIndexProvider = new ValaisAutoIndexProvider(autoCaptchaHandler);

        cantonFribourg = new Canton("FR", true, fribourgAutoIndexProvider);
        cantonValais = new Canton("VS", true, valaisAutoIndexProvider);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testFribourg() throws Exception {
        /*PlateOwner expectedFr1 = new PlateOwner("Oberson Julien", "Route des Grives 4", "", 1763, "Granges-Paccot");
        PlateOwner actualFr1 = fribourgAutoIndexProvider.getPlateOwner(new Plate(169169, PlateType.AUTOMOBILE, cantonFribourg));
        */

        PlateOwner expectedFr1 = new PlateOwner("Transports Publics Fribourgeois", "Rue Louis-d'Affry 2", "Case postale 1536", 1700, "Fribourg");
        PlateOwner actualFr1 = fribourgAutoIndexProvider.getPlateOwner(new Plate(300340, PlateType.AUTOMOBILE, cantonFribourg));
        Assert.assertEquals(expectedFr1, actualFr1);
    }

    @Test(expected = PlateOwnerNotFoundException.class)
    public void testFribourgOwnerHidden() throws Exception {
        fribourgAutoIndexProvider.getPlateOwner(new Plate(6789, PlateType.MOTORCYCLE, cantonFribourg));
    }

    @Test
    public void testFribourgMoto() throws Exception {
        // This test is also interesting to see long values
        /*
        PlateOwner expectedFrMoto1 = new PlateOwner("Clement Jean-Marie Assurances et courtage", "Route de la Maison-Neuve 17", "", 1753, "Matran");
        PlateOwner actualFrMoto1 = fribourgAutoIndexProvider.getPlateOwner(new Plate(2508, PlateType.MOTORCYCLE, cantonFribourg));
        */

        // Assert.assertEquals(expectedFrMoto1, actualFrMoto1);
        // TODO It is very hard to find a number which is used in winter. If the owner gives his plates back for the winter
        // then they disappear from the website and we cannot test.
    }

    @Test
    public void testFribourgAgricole() throws Exception {
        PlateOwner expectedFrAgri1 = new PlateOwner("Roux Jean-Daniel", "Route de Macconnens 40", "", 1691, "Villarimboud");
        PlateOwner actualFrAgri1 = fribourgAutoIndexProvider.getPlateOwner(new Plate(123, PlateType.AGRICULTURAL, cantonFribourg));
        Assert.assertEquals(expectedFrAgri1, actualFrAgri1);
    }

    @Test
    public void testValais() throws Exception {
        PlateOwner expectedVs1 = new PlateOwner("Kluser Beat", "Hotel Lötschberg", "", 3917, "Kippel");
        PlateOwner actualVs1 = valaisAutoIndexProvider.getPlateOwner(new Plate(11111, PlateType.AUTOMOBILE, cantonValais));
        Assert.assertEquals(expectedVs1, actualVs1);

        // This test is interesting to see if HTML unescape worked
        PlateOwner expectedVs2 = new PlateOwner("Defayes Eric", "Route de l'Ecosse 7", "", 1907, "Saxon");
        PlateOwner actualVs2 = valaisAutoIndexProvider.getPlateOwner(new Plate(22222, PlateType.AUTOMOBILE, cantonValais));
        Assert.assertEquals(expectedVs2, actualVs2);
    }
}
