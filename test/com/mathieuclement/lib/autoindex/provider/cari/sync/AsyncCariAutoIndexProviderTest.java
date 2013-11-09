package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.cari.async.AsyncFribourgAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.CaptchaListener;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.PlateRequestListener;
import com.mathieuclement.lib.autoindex.provider.exception.PlateRequestException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AsyncCariAutoIndexProviderTest {
//    private AsyncCariAutoIndexProvider fribourgAutoIndexProvider;
//    private Canton cantonFribourg;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testWithDialog() throws Exception {
        Canton cantonFribourg = new Canton("FR", true, new AsyncFribourgAutoIndexProvider());
        openDiag(cantonFribourg);
    }

    public void openDiag(final Canton canton) throws Exception {

        // Request on another thread
        // Main thread will be AWT-Thread or similar

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final JDialog requestDialog = new JDialog();
        Container mainPanel = requestDialog.getContentPane();

        /*String[] cantonItems = {"FR", "VS"};
        final JComboBox cantonComboBox = new JComboBox(cantonItems);
        mainPanel.add(cantonComboBox);
        */

        final JTextField plateNumberTextField = new JTextField();
        plateNumberTextField.setHorizontalAlignment(SwingConstants.CENTER);
        plateNumberTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Canton selectedCanton = canton;

                final AsyncAutoIndexProvider asyncAutoIndexProvider = selectedCanton.getAsyncAutoIndexProvider();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int plateNumber = Integer.parseInt(plateNumberTextField.getText());
                        plateNumberTextField.setText("Processing " + plateNumberTextField.getText() + "... Please wait.");
                        plateNumberTextField.setEnabled(false);
                        try {
                            asyncAutoIndexProvider.requestPlateOwner(new Plate(plateNumber, PlateType.AUTOMOBILE, selectedCanton));
                        } catch (ProviderException e1) {
                            e1.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        mainPanel.add(plateNumberTextField);

        canton.getAsyncAutoIndexProvider().addListener(new CaptchaListener() {
            private void generateCaptchaImage(JLabel imageLabel, AsyncAutoIndexProvider provider, HttpClient httpClient, HttpContext httpContext) throws IOException {
                System.out.println("Downloading image...");
                imageLabel.setText("Refreshing captcha...");
                HttpResponse httpResponse = httpClient.execute(new HttpGet(provider.generateCaptchaImageUrl()), httpContext);
                File captchaImageFile = File.createTempFile("cari-captcha", ".jpg");
                FileOutputStream fos = new FileOutputStream(captchaImageFile);
                httpResponse.getEntity().writeTo(fos);
                fos.close();
                httpResponse.getEntity().getContent().close();

                imageLabel.setIcon(new ImageIcon(captchaImageFile.getAbsolutePath()));
            }

            @Override
            public void onCaptchaCodeRequested(Plate plate, String captchaImageUrl, final HttpClient httpClient, HttpHost httpHost, final HttpContext httpContext, String httpHostHeaderValue, AsyncAutoIndexProvider provider) throws ProviderException {
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
                                    generateCaptchaImage(imageLabel, canton.getAsyncAutoIndexProvider(), httpClient, httpContext);
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
                            if (inputField.getText() != null && !"".equals(inputField.getText())) {
                                captchaCode[0] = inputField.getText();
                                dialog.dispose();
                            }
                        }
                    };
                    inputField.addActionListener(actionListener);
                    contentPane.add(inputField, BorderLayout.CENTER);

                    JButton continueButton = new JButton("Continue");
                    contentPane.add(continueButton, BorderLayout.SOUTH);
                    continueButton.addActionListener(actionListener);

                    dialog.pack();
                    dialog.setModal(true);
                    dialog.setLocationRelativeTo(requestDialog);
                    dialog.setVisible(true);

                    System.out.println("User entered '" + captchaCode[0] + "' as Captcha code.");

                    canton.getAsyncAutoIndexProvider().pushCaptchaCode(captchaCode[0], plate, httpClient, httpContext);

                } catch (IOException e) {
                    System.err.println("Failed to download or open captcha image");
                    e.printStackTrace();
                }
            }

            @Override
            public void onCaptchaCodeAccepted(Plate plate) {
                System.out.println("Captcha code accepted.");
            }
        });

        canton.getAsyncAutoIndexProvider().addListener(new PlateRequestListener() {
            @Override
            public void onPlateOwnerFound(Plate plate, PlateOwner plateOwner) {
                plateNumberTextField.setEnabled(true);
                plateNumberTextField.setText("");
                JOptionPane.showMessageDialog(requestDialog, "Owner found: " + plateOwner);
            }

            @Override
            public void onPlateRequestException(Plate plate, PlateRequestException exception) {
                exception.printStackTrace();
                plateNumberTextField.setEnabled(true);
                plateNumberTextField.setText("");
                JOptionPane.showMessageDialog(requestDialog, "Exception: " + exception.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            }
        });

        requestDialog.setTitle("Enter '" + canton.getAbbreviation() + "' number and press 'Enter'");
        requestDialog.setPreferredSize(new Dimension(350, 50));
        requestDialog.pack();
        requestDialog.setLocationRelativeTo(null);
        requestDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        requestDialog.setVisible(true);
    }

    @After
    public void tearDown() throws Exception {

    }
}
