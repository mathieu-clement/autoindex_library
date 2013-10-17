package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

public class CariAutoIndexProviderTest {
    private CariAutoIndexProvider fribourgAutoIndexProvider;
    private CariAutoIndexProvider valaisAutoIndexProvider;
    private Canton cantonFribourg;
    private Canton cantonValais;

    @Before
    public void setUp() throws Exception {
        CaptchaHandler dialogCaptchaHandler = new CaptchaHandler() {

            private void generateCaptchaImage(JLabel imageLabel, CaptchaAutoIndexProvider autoIndexProvider, HttpClient httpClient, HttpContext httpContext) throws IOException {
                System.out.println("Downloading image...");
                imageLabel.setText("Refreshing captcha...");
                HttpResponse httpResponse = httpClient.execute(new HttpGet(autoIndexProvider.regenerateCaptchaImageUrl()), httpContext);
                File captchaImageFile = File.createTempFile("cari-captcha", ".jpg");
                FileOutputStream fos = new FileOutputStream(captchaImageFile);
                httpResponse.getEntity().writeTo(fos);
                fos.close();
                httpResponse.getEntity().getContent().close();

                imageLabel.setIcon(new ImageIcon(captchaImageFile.getAbsolutePath()));
            }

            @Override
            public String handleCaptchaImage(String captchaImageUrl, final HttpClient httpClient, HttpHost httpHost, final HttpContext httpContext, String httpHostHeaderValue, final CaptchaAutoIndexProvider captchaAutoIndexProvider) {
                System.out.println("Captcha image URL: \"" + captchaImageUrl + "\"");

                try {
                    System.out.println("Downloading image...");
                    BasicHttpRequest httpRequest = new BasicHttpRequest("GET", captchaImageUrl, HttpVersion.HTTP_1_1);
                    httpRequest.setHeader("host", httpHostHeaderValue);
                    HttpResponse httpResponse = httpClient.execute(httpHost, httpRequest, httpContext);
                    File captchaImageFile = File.createTempFile("cari-captcha", ".jpg");
                    FileOutputStream fos = new FileOutputStream(captchaImageFile);
                    httpResponse.getEntity().writeTo(fos);
                    fos.close();
                    httpResponse.getEntity().getContent().close();

                    final String[] captchaCode = new String[1];

                    final JDialog dialog = new JDialog();
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    Container contentPane = dialog.getContentPane();
                    contentPane.setLayout(new BorderLayout());

                    final JLabel imageLabel = new JLabel(new ImageIcon(captchaImageFile.getAbsolutePath()));
                    imageLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getClickCount() == 2) {
                                try {
                                    generateCaptchaImage(imageLabel, captchaAutoIndexProvider, httpClient, httpContext);
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }
                    });
                    contentPane.add(imageLabel, BorderLayout.NORTH);

                    final JTextField inputField = new JTextField();
                    ActionListener actionListener = new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            captchaCode[0] = inputField.getText();
                            dialog.dispose();
                        }
                    };
                    inputField.addActionListener(actionListener);
                    contentPane.add(inputField, BorderLayout.CENTER);

                    JButton continueButton = new JButton("Continue");
                    contentPane.add(continueButton, BorderLayout.SOUTH);
                    continueButton.addActionListener(actionListener);

                    dialog.pack();
                    dialog.setModal(true);
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);

                    System.out.println("User entered '" + captchaCode[0] + "' as Captcha code.");

                    return captchaCode[0];

                } catch (IOException e) {
                    System.err.println("Failed to download or open captcha image");
                    e.printStackTrace();
                }

                return readString("Enter Captcha code");
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

        fribourgAutoIndexProvider = new FribourgAutoIndexProvider(dialogCaptchaHandler);
        valaisAutoIndexProvider = new ValaisAutoIndexProvider(dialogCaptchaHandler);

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

    private String readString(String fieldName) {

        //  prompt the user to enter their name
        System.out.print(fieldName + ": ");

        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String userInput = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
            userInput = br.readLine();
        } catch (IOException ioe) {
            System.out.println("IO error trying to read standard input!");
        }

        return userInput;

    }
}
