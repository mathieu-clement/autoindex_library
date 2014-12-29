package com.mathieuclement.lib.autoindex.provider.proxy;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.utils.MockupAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.utils.MockupCaptchaHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaptchaAutoIndexProxyTest {

    @Test
    public void testGetPlateOwner() throws Exception {
        CaptchaHandler captchaHandler = new MockupCaptchaHandler();
        CaptchaAutoIndexProvider provider = new MockupAutoIndexProvider(captchaHandler);

        /*
        CaptchaHandler captchaHandler = new WebServiceBasedCaptchaHandler();
        CaptchaAutoIndexProvider fribourgProvider = new FribourgAutoIndexProvider(captchaHandler);
        */

        CaptchaAutoIndexProxy proxy = new CaptchaAutoIndexProxy(
                provider, "mathieu.clement@freebourg.org", captchaHandler);
        Plate plate = new Plate(27442, PlateType.AUTOMOBILE,
                new Canton("FR", false, (AutoIndexProvider) null));
        PlateOwner expected = new PlateOwner("Clement Jean-Marie Assurances et courtage",
                "Route de la Maison-Neuve 17",
                "", 1753, "Matran");

        PlateOwner actual = proxy.getPlateOwner(plate, 0);
        assertEquals(expected, actual);
    }
}