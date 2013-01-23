package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CaptchaCutter {

    public static void main(String[] args) {
        try {
            File completeImageFile = new File(args[0]);
            BufferedImage img = ImageIO.read(completeImageFile);
            BufferedImage[] croppedImages = CaptchaCutter.cropImageInTiles(img, 29, 55, 75, 97, 119);

            for (int i = 0; i < croppedImages.length; i++) {
                ImageIO.write(croppedImages[i], "png", makeCroppedImageFile(completeImageFile, Integer.toString(i)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File makeCroppedImageFile(File completeImageFile, String i) {
        String parent = completeImageFile.getParent() == null ? "" : (completeImageFile.getParent() + File.separatorChar);
        return new File(parent +
                completeImageFile.getName().substring(0, completeImageFile.getName().length() - 4) + '_' + i + ".png");
    }

    private static BufferedImage cropImage(BufferedImage image, int x1, int x2) {
        // New image
        int cropYmin = 20;
        int cropYmax = 43;
        BufferedImage newImage = new BufferedImage(x2 - x1, cropYmax - cropYmin, image.getType());

        int newX;
        int newY;
        for (int y = cropYmin; y < cropYmax; y++) {
            newX = 0;
            newY = y - cropYmin;
            for (int x = x1; x < x2; x++) {
                try {
                    newImage.setRGB(newX++, newY, image.getRGB(x, y));
                } catch (ArrayIndexOutOfBoundsException oobe) {
                    throw new RuntimeException("Could not write pixel value to coordinate (" + (newX - 1) + "," + y + ")", oobe);
                }
            }
        }

        return newImage;
    }

    /**
     * Crop images in tiles based on the given x coordinates.
     *
     * @param image   Image to be cropped in many tiles
     * @param xCoords coordinates of the splitting points (on the X axis)
     * @return the resulting cropped images
     * @throws IOException if any image could not be created
     */
    public static BufferedImage[] cropImageInTiles(BufferedImage image, int... xCoords) throws IOException {
        // Number of images that will be created: xCoords.length + 1
        BufferedImage[] croppedImages = new BufferedImage[xCoords.length + 1];

        // First image is [0 -- xCoords[0]]
        // Second image is [xCoords[0] -- xCoords[1]]
        // Nth image is [xCoords[n-2] -- xCoords[n-1]]
        // Last image is [xCoords[xCoords.length] -- image.getWidth()

        // First image
        croppedImages[0] = cropImage(image, 0, xCoords[0]);

        // Images in the middle

        for (int i = 0; i < xCoords.length - 1; i++) {
            croppedImages[i + 1] = cropImage(image, xCoords[i], xCoords[i + 1]);
        }

        // Last image
        croppedImages[xCoords.length] = cropImage(image, xCoords[xCoords.length - 1], image.getWidth());

        return croppedImages;
    }
}
