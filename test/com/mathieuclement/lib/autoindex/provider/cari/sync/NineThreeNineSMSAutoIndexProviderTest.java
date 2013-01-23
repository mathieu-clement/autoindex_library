package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.sms.Mock939Provider;
import com.mathieuclement.lib.autoindex.provider.common.sms.SMSAutoIndexProvider;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

public class NineThreeNineSMSAutoIndexProviderTest {
    private SMSAutoIndexProvider smsAutoIndexProvider;

    @Before
    public void setUp() throws Exception {
        smsAutoIndexProvider = Mock939Provider.getInstance().getSmsAutoIndexProvider();
    }

    @Test
    public void testGetPlateOwner() throws Exception {
        Assert.assertEquals(new PlateOwner("Maria Kuhn", "Valéestrasse 23", "", 7746, "Pagnoncini"), smsAutoIndexProvider.getPlateOwner(new Plate(11000, PlateType.AUTOMOBILE, Mock939Provider.getInstance().getCantonAppenzellInside())));
    }
}
