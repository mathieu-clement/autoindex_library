package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Rotater {

    public static void main(String[] args) {
        try {
            String inputFile = args[0];
            //String inputFile = "/home/mathieu/Development/android/mc_autoindex_android/test/red-noise-free-images/6_3.png";
            File originalFile = new File(inputFile);
            final BufferedImage image = ImageIO.read(originalFile);

            final BufferedImage rotatedImg = Rotater.rotateBest(image);

            /*JFrame frame = new JFrame();
            frame.getContentPane().add(new JPanel() {
                @Override
                public void paint(Graphics g) {
                    g.drawImage(rotatedImg, 0, 0, null);
                }
            });
            frame.setSize(50, 100);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);*/
            ImageIO.write(rotatedImg, "png", new File(args[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage rotateBest(final BufferedImage image) {
        Graphics2D g = image.createGraphics();

        BufferedImage dstImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        int bestHeight = 0;
        double bestAngle = 0;

        // Try to rotate from -25° to +25° and look for biggest height
        int initialAngle = -30;
        int targetAngle = +30;
        assert targetAngle > initialAngle;

        int widthCenter = image.getWidth() / 2;
        int heightCenter = image.getHeight() / 2;

        for (double angle = initialAngle; angle <= targetAngle; angle++) {
            if (angle != 0) {
                new AffineTransformOp(
                        AffineTransform.getRotateInstance(Math.toRadians(angle), widthCenter, heightCenter),
                        AffineTransformOp.TYPE_BILINEAR).filter(image, dstImage);

                int blackHeight = findMaxPenHeight(dstImage);
                if (blackHeight > bestHeight) {
                    bestHeight = blackHeight;
                    bestAngle = angle;
                }
            }
        }

        // Found the best rotation, then rotate it there
        new AffineTransformOp(
                AffineTransform.getRotateInstance(Math.toRadians(bestAngle), widthCenter, heightCenter),
                AffineTransformOp.TYPE_BILINEAR).filter(image, dstImage);

        image.flush();

        // Write to image
        return dstImage;
    }

    public static int findMaxPenHeight(BufferedImage img) {
        int minY = img.getHeight();
        int maxY = 0;
        int redRgb = new Color(255, 0, 0).getRGB();

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if ((img.getRGB(x, y) == redRgb) &&
                        (img.getRGB(x, y - 1) == redRgb || img.getRGB(x, y - 2) == redRgb || img.getRGB(x, y - 3) == redRgb)) {
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        return maxY - minY;
    }
}
