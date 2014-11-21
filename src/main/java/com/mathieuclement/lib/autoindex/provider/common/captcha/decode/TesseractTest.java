package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class TesseractTest {
    public static void main(String[] args) throws FileNotFoundException {

        // Some common errors:
        char[][] ambiguousArray = new char[][]{
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


        // stats
        int totalResults = 0;
        int goodResults = 0;
        int[] badResults = new int[200];

        File humanReadableFile = new File("/home/mathieu/Development/android/mc_autoindex_android/test/red-noise-free-images/humandecoded"); //args[0]);
        Scanner scanner = new Scanner(humanReadableFile);
        int i = 0;
        while (scanner.hasNextLine()) {
            String humanDecodedCaptcha = scanner.nextLine();

            // Machine decoding
            mainFor:
            for (int j = 0; j <= 5; j++) {
                totalResults++;

                // Run tesseract command
                try {
                    String parentFilePath = "/home/mathieu/Development/android/mc_autoindex_android/test/red-noise-free-images/";
                    String cmd = "/usr/bin/tesseract " + i + "_" + (j + 1) + ".png " + i + "_" + (j + 1) + " -psm 10";
                    Process process = Runtime.getRuntime().exec(cmd, null, humanReadableFile.getParentFile());
                    process.waitFor();
                    if (process.exitValue() != 0) {
                        System.err.println(i + "_" + (j + 1) + " : Tesseract exited with error status " + process.exitValue() + ").");
                        return;
                    }

                    // "cat" content of file i_j.txt
                    File letterTxtFile = new File(parentFilePath + i + "_" + (j + 1) + ".txt");

                    // waitFor does not work... then we'll wait. File gets created every time!
                    while (!letterTxtFile.exists()) ;

                    Scanner letterScanner = new Scanner(letterTxtFile);
                    if (!letterScanner.hasNextLine()) {
                        System.err.println("Could not read " + letterTxtFile.getName());
                    } else {
                        String txtFileLetter = letterScanner.nextLine();
                        try {
                            if (humanDecodedCaptcha.charAt(j) == txtFileLetter.charAt(0)) {
                                // Success
                                //System.out.println("Nice! " + letterTxtFile.getName() + " is correct.");
                                goodResults++;
                            } else {
                                System.err.println(letterTxtFile.getName() + " : " + humanDecodedCaptcha.charAt(j) + " detected as " + txtFileLetter.charAt(0));

                                // Try with characters that could match from the ambiguous table
                                char[] chars = tryAmbiguous(txtFileLetter.charAt(0), ambiguousArray);
                                for (char aChar : chars) {
                                    if (aChar == humanDecodedCaptcha.charAt(j)) {
                                        System.out.println(letterTxtFile.getName() + ": found " + aChar + " with ambiguous table.");
                                        goodResults++;
                                        continue mainFor;
                                    }
                                }

                                badResults[humanDecodedCaptcha.charAt(j)]++;
                            }
                        } catch (StringIndexOutOfBoundsException sibe) {
                            System.err.println("Could not read " + letterTxtFile.getName());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Increment i
            i++;
        }

        System.out.println();
        System.out.println();
        System.out.println("Good results: " + goodResults);
        System.out.println("Total: " + totalResults);
        System.out.println("Accuracy: " + (100 * goodResults / totalResults) + " %");
        System.out.println(); // Empty line

        // Scores for every character badly detected
        System.out.println("Errors on characters:");
        for (int j = 0; j < badResults.length; j++) {
            int charErrorCount = badResults[j];
            if (charErrorCount > 0) {
                System.out.println(((char) j) + " : " + charErrorCount);
            }
        }

    }

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

        return new char[]{}; // return empty array if character not in table
    }
}
