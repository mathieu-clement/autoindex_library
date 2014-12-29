package com.mathieuclement.lib.autoindex.provider.cari;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.cari.sync.CariAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.cari.sync.FribourgAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.cari.sync.ValaisAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.utils.WebServiceBasedCaptchaHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

/**
 * Test for cari auto index provider.
 */
public class SyncCariAutoIndexProviderTest {
    private CariAutoIndexProvider fribourgAutoIndexProvider;
    private CariAutoIndexProvider valaisAutoIndexProvider;
    private Canton cantonFribourg;
    private Canton cantonValais;

    @Before
    public void setUp() throws Exception {
        // Solving captcha locally:
        //return ExecCommand.exec("/home/mathieu/Dropbox/work/decode_captcha/cari/decoder_cari.pl " + file
        //        .getAbsolutePath());

        CaptchaHandler autoCaptchaHandler = new WebServiceBasedCaptchaHandler();

        fribourgAutoIndexProvider = new FribourgAutoIndexProvider(autoCaptchaHandler);
        valaisAutoIndexProvider = new ValaisAutoIndexProvider(autoCaptchaHandler);

        cantonFribourg = new Canton("FR", true, fribourgAutoIndexProvider);
        cantonValais = new Canton("VS", true, valaisAutoIndexProvider);
    }

    @After
    public void tearDown() throws Exception {

    }

    Random random = new Random();

    @Test
    public void testFribourg() throws Exception {
        /*PlateOwner expectedFr1 = new PlateOwner("Oberson Julien", "Route des Grives 4", "",
        1763, "Granges-Paccot");
        PlateOwner actualFr1 = fribourgAutoIndexProvider.getPlateOwner(
        new Plate(169169, PlateType.AUTOMOBILE, cantonFribourg));
        */

        PlateOwner expectedFr1 = new PlateOwner("Transports Publics Fribourgeois", "Rue Louis-d'Affry 2",
                "Case postale 1536", 1700, "Fribourg");
        PlateOwner actualFr1 = fribourgAutoIndexProvider.getPlateOwner(new Plate(300340, PlateType.AUTOMOBILE,
                cantonFribourg),
                random.nextInt());
        Assert.assertEquals(expectedFr1, actualFr1);
    }

    @Test(expected = PlateOwnerNotFoundException.class)
    public void testFribourgOwnerHidden() throws Exception {
        fribourgAutoIndexProvider.getPlateOwner(new Plate(6789, PlateType.MOTORCYCLE, cantonFribourg),
                random.nextInt());
    }

    @Test
    public void testFribourgMoto() throws Exception {
        // This test is also interesting to see long values
        /*
        PlateOwner expectedFrMoto1 = new PlateOwner("Clement Jean-Marie Assurances et courtage",
        "Route de la Maison-Neuve 17", "", 1753, "Matran");
        PlateOwner actualFrMoto1 = fribourgAutoIndexProvider.getPlateOwner(
        new Plate(2508, PlateType.MOTORCYCLE, cantonFribourg));
        */

        // Assert.assertEquals(expectedFrMoto1, actualFrMoto1);
        // TODO It is very hard to find a number which is used in winter.
        // If the owner gives his plates back for the winter
        // then they disappear from the website and we cannot test.
    }

    @Test
    public void testFribourgAgricole() throws Exception {
        PlateOwner expectedFrAgri1 = new PlateOwner("Roux Jean-Daniel", "Route de Macconnens 40", "",
                1691, "Villarimboud");
        PlateOwner actualFrAgri1 = fribourgAutoIndexProvider.getPlateOwner(
                new Plate(123, PlateType.AGRICULTURAL, cantonFribourg),
                random.nextInt());
        Assert.assertEquals(expectedFrAgri1, actualFrAgri1);
    }

    @Test
    public void testValais() throws Exception {
        PlateOwner expectedVs1 = new PlateOwner("Kluser Beat", "Hotel Lötschberg", "", 3917, "Kippel");
        PlateOwner actualVs1 = valaisAutoIndexProvider.getPlateOwner(
                new Plate(11111, PlateType.AUTOMOBILE, cantonValais),
                random.nextInt());
        Assert.assertEquals(expectedVs1, actualVs1);

        // This test is interesting to see if HTML unescape worked
        PlateOwner expectedVs2 = new PlateOwner("Defayes Eric", "Route de l'Ecosse 7", "", 1907, "Saxon");
        PlateOwner actualVs2 = valaisAutoIndexProvider.getPlateOwner(
                new Plate(22222, PlateType.AUTOMOBILE, cantonValais),
                random.nextInt());
        Assert.assertEquals(expectedVs2, actualVs2);
    }
}
