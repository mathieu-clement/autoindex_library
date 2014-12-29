package com.mathieuclement.lib.autoindex.provider.utils;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Test for response utils.
 */
public class ResponseUtilsTest {
    @Test
    public void testRemoveUselessSpaces() throws Exception {
        String[] strings = {
                "   a  b      c     ", "a b c",
                "a b  c", "a b c",
                "abc", "abc",
                "This is nice!", "This is nice!",
                "This    is nice!", "This is nice!"
        };

        for (int i = 0; i < strings.length; i += 2) {
            Assert.assertEquals(strings[i + 1], ResponseUtils.removeUselessSpaces(strings[i]));
        }
    }
}
