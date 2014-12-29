package com.mathieuclement.lib.autoindex.provider.viacar;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.utils.WebServiceBasedCaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.viacar.sync.ViacarAutoIndexProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class SyncViacarAutoIndexProviderTest {
    private ViacarAutoIndexProvider provider;
    private Canton canton;

    public static final String CANTON_ABBR = "AG";

    @Before
    public void setUp() throws Exception {
        // Solving captcha locally:
        //return ExecCommand.exec("/home/mathieu/Dropbox/work/decode_captcha/cari/decoder_cari.pl " + file
        //        .getAbsolutePath());

        CaptchaHandler autoCaptchaHandler = new WebServiceBasedCaptchaHandler();

        provider = new ViacarAutoIndexProvider(CANTON_ABBR, autoCaptchaHandler);
        canton = new Canton(CANTON_ABBR, true, provider);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    @SuppressFBWarnings(
            value="DMI_RANDOM_USED_ONLY_ONCE",
            justification="This is just a test.")
    public void testAg() throws Exception {
        PlateOwner expected = new PlateOwner("Müller Verena", "Hofstrasse 49", "", 5406, "Rütihof");
        PlateOwner actual = provider.getPlateOwner(new Plate(32413, PlateType.AUTOMOBILE, canton),
                new Random().nextInt());
        Assert.assertEquals(expected, actual);
    }
}
