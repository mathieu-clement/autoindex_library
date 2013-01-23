package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NoiseRemover {
    private final static int backgroundRgb = new Color(200, 200, 200).getRGB(); // background color: dark grey
    private final static int newBackgroundRgb = new Color(255, 255, 255).getRGB(); // background color: dark grey
    private final static int noiseLinesRgb = new Color(128, 128, 128).getRGB(); // noise lines color: light grey
    private final static int textRgb = new Color(255, 255, 255).getRGB(); // text color: white
    private final static int whiteRgb = new Color(255, 255, 255).getRGB(); // text color: white
    private final static int blackRgb = new Color(0, 0, 0).getRGB(); // text color: white
    private final static int redRgb = new Color(255, 0, 0).getRGB();
    private static final int noiseRgbLowThreshold = -10197916;
    private static final int noiseRgbHighThreshold = -1000;

    /**
     * Remove (background) noise from captcha image and return un-noised image.
     *
     * @param image Captcha image
     * @return un-noised image from captcha image file
     * @throws IOException
     */
    public static BufferedImage removeNoise(BufferedImage image) throws IOException {
        // Create a new noise-free image, same size as noised image
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        // Remove noise lines
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // Original color
                int originalRgb = image.getRGB(x, y);

                // Color that will be drawn on noise-free image
                // It's the same unless it's a noise line pixel color, in which case we write it as the background color.
                int newRgb = 0;
                if (originalRgb > noiseRgbLowThreshold && originalRgb < noiseRgbHighThreshold) // these strange values are colors next to noise color (RGB 100 and RGB 199)
                    newRgb = newBackgroundRgb;
                else if (originalRgb > noiseRgbHighThreshold)
                    newRgb = redRgb;
                else if (originalRgb < noiseRgbLowThreshold)
                    newRgb = whiteRgb;


                // Write that to noise-free image
                newImage.setRGB(x, y, newRgb);
            }
        }

        return newImage;
    }

    public static void main(String[] args) throws IOException {
        final BufferedImage newImg = removeNoise(ImageIO.read(new File(args[0])));

        if (args.length == 0) {
            System.err.println("Usage: ... source target [format] (jpg by default)");
            System.exit(1);
        } else if (args.length > 1) {
            System.out.print("Processing '" + args[0] + "'... ");
            /* Save to new file */
            ImageIO.write(newImg, args.length > 2 ? args[2] : "jpg", new File(args[1]));
            System.out.println("OK");
        } else {

            /* Show on screen */
            JFrame frame = new JFrame();
            frame.getContentPane().add(new JPanel() {
                @Override
                public void paint(Graphics g) {
                    g.drawImage(newImg, 0, 0, null);
                }
            });
            frame.setSize(newImg.getWidth(), newImg.getHeight() * 2);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);

        }

    }
}
