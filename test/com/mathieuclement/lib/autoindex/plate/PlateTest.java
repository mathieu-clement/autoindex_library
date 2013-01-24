package com.mathieuclement.lib.autoindex.plate;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Author: Mathieu Clément
 * Date: 23.01.2013
 */
public class PlateTest {
    @Test
    public void testFormatNumber() throws Exception {
        test("1", 1);
        test("10", 10);
        test("652", 652);
        test("1025", 1025);
        test("9999", 9999);
        test("10 000", 10000);
        test("99 999", 99999);
        test("100 000", 100000);
        test("123 456", 123456);
        test("999 999", 999999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failTooSmallNumber() throws Exception {
        test("number too small...", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failTooBigNumber() throws Exception {
        test("number too big...", 1000000);
    }

    private static void test(String expectedStr, int number) {
        Assert.assertEquals(expectedStr, Plate.formatNumber(number));
    }
}
