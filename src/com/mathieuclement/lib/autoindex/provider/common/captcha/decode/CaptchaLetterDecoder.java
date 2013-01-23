package com.mathieuclement.lib.autoindex.provider.common.captcha.decode;

import java.awt.image.BufferedImage;

/**
 * Decoder of one letter from an image containing one symbol.
 */
public interface CaptchaLetterDecoder {
    /**
     * Opens image and decodes the letter that is drawn.
     *
     * @param image An image containing one symbol
     * @return the symbol / letter drawn in the image
     */
    char[] decodeLetter(BufferedImage image);
}
