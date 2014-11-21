package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class CaptchaDecoder {

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File(args[0]));
        Set<String> strings = decode(image, new CaptchaLetterDecoder() {
            @Override
            public char[] decodeLetter(BufferedImage image) {
                try {
                    File letterImageFile = File.createTempFile("captcha-decoder", "temp-letter-image.png");
                    ImageIO.write(image, "png", letterImageFile);
                    Runtime.getRuntime().exec("/usr/bin/tesseract " + letterImageFile.getAbsolutePath() + " " + letterImageFile.getParent() + "/temp-letter -psm 10");
                    File letterFile = new File(letterImageFile.getParentFile(), "temp-letter.txt");
                    // Wait file is created
                    while (!letterFile.exists()) ;

                    // Read
                    Scanner scanner = new Scanner(letterFile);
                    char[] returnedValue;
                    if (scanner.hasNextLine()) {
                        returnedValue = tryAmbiguous(scanner.nextLine().charAt(0), ambiguousCharArray);
                    } else {
                        returnedValue = null;
                    }
                    scanner.close();
                    letterFile.delete();
                    letterImageFile.delete();
                    return returnedValue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        System.out.println(strings);
    }

    /**
     * Decode captcha from original (noised) image.
     *
     * @param noisedImage Image noisedImage
     * @return possible codes from the captcha
     */
    public static Set<String> decode(BufferedImage noisedImage, CaptchaLetterDecoder singleLetterDecoder) throws IOException {
        BufferedImage unNoisedImage = NoiseRemover.removeNoise(noisedImage);
        BufferedImage[] croppedImages = CaptchaCutter.cropImageInTiles(unNoisedImage, 9, 29, 55, 74, 97, 119, 141);

        char[][] possibleLetters = new char[6][]; // array first index is letter position (0 is first letter), content is possible matching letters

        for (int i = 1; i < croppedImages.length - 1; i++) { // first and last images are ignored
            BufferedImage croppedImage = croppedImages[i];
            possibleLetters[i - 1] = singleLetterDecoder.decodeLetter(croppedImage);
        }


        // Cleaning up... Give hints to Garbage Collector.
        noisedImage.flush();
        unNoisedImage.flush();
        for (BufferedImage croppedImage : croppedImages) {
            croppedImage.flush();
        }

        // Make a Set of all possibilities
        Set<String> possibilities = new LinkedHashSet<String>();
        for (int a = 0; a < possibleLetters[0].length; a++) {
            for (int b = 0; b < possibleLetters[1].length; b++) {
                for (int c = 0; c < possibleLetters[2].length; c++) {
                    for (int d = 0; d < possibleLetters[3].length; d++) {
                        for (int e = 0; e < possibleLetters[4].length; e++) {
                            for (int f = 0; f < possibleLetters[5].length; f++) {
                                possibilities.add("" + possibleLetters[0][a] + possibleLetters[1][b] + possibleLetters[2][c] + possibleLetters[3][d] +
                                        possibleLetters[4][e] + possibleLetters[5][f]);
                            }
                        }
                    }
                }
            }
        }

        return possibilities;
    }

    public static char[][] ambiguousCharArray = new char[][]{
            {'w', 'W'}, // meaning: w is detected as W
            {'W', 'w'},
            {'x', 'X'},
            {'X', 'x'},
            {'e', '9', 'ﬂ'},
            {'9', 'e'},
            {'y', 'Y'},
            {'Y', 'y', 'V', 'v'},
            {'v', 'V'},
            {'V', 'v'},
            {'s', 'S'},
            {'m', 'M', 'W'},
            {'n', 'H', 'h'},
            {'k', 'K'},
            {'K', 'k'},
            {'j', '/'},
            {'/', 'j'},
            {'p', 'P'},
            {'P', 'p'},
            {'q', '9'},
            {'c', 'C', 'D', 'o', 'O'},
            {'C', 'c', 'D', 'O', 'o'},
            {'o', 'O', 'c', 'C'},
            {'O', 'o', 'c', 'C', 'D'},
            {'D', 'O'},
            {'u', 'U'},
            {'U', 'u'},
            {'t', 'f'},
            {'f', 't'}
    };

    private static char[] tryAmbiguous(char inputChar, char[][] ambiguousArray) {
        for (char[] charArr : ambiguousArray) {
            // first element is character that should be detected
            if (charArr[0] == inputChar) {
                char[] returnedArray = new char[charArr.length - 1];
                int returnedArrayIndex = 0;
                for (int i = 1; i < charArr.length; i++) {
                    returnedArray[returnedArrayIndex++] = charArr[i];
                }
                return returnedArray;
            }
        }

        return new char[]{inputChar}; // return inputted character if not in table
    }
}
