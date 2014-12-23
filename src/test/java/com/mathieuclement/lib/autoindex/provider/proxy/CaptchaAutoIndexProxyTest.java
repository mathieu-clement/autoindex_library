package com.mathieuclement.lib.autoindex.provider.proxy;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaptchaAutoIndexProxyTest {

    @Test
    public void testGetPlateOwner() throws Exception {
        CaptchaAutoIndexProxy proxy = new CaptchaAutoIndexProxy(null, "toto@noel.ch", null);
        Plate plate = new Plate(61711, PlateType.AUTOMOBILE,
                new Canton("FR", false, (AutoIndexProvider) null));
        PlateOwner expected = new PlateOwner("Mathieu Clément",
                "Ch. de la Croix-Blanche 31",
                "", 1731, "Ependes");

        PlateOwner actual = proxy.getPlateOwner(plate, 0);
        assertEquals(expected, actual);
    }
}