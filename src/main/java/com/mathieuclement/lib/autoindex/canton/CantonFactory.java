/*
 * Filename: CantonFactory.java
 *
 * Copyright 1987-2012 by Informatique-MTF, SA, Route du Bleuet 1, CH-1762
 * Givisiez/Fribourg, Switzerland All rights reserved.
 *
 *========================================================================
 */
package com.mathieuclement.lib.autoindex.canton;

import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * Factory to create cantons.
 */
public class CantonFactory {
    public static Canton create(String abbreviation, AutoIndexProvider autoIndexProvider) {
        return new Canton(abbreviation, autoIndexProvider != null, autoIndexProvider);
    }

    public static List<Canton> createAllSwitzerlandCantons() {
        List<Canton> cantons = new LinkedList<Canton>();

        /**
         * http://www.halterauskunft.ch/halterauskuenfte/auskunftschweiz/index.html (see "Hover" images descriptions)
         * http://www.viacar-ag.ch/index.php?TPL=10072
         * http://www.abraxas.ch/fileadmin/customer/Dokumente/Produkte/Abx_CARI_Prod_fr.pdf
         * http://www.suche.ch/info/autoindex
         */

        // Not available
        // * NE: Cannot be accessed publicly (needs "Guichet unique" authentication?).
        // * SZ: No online autoindex (although I could find a form people can fill to hide their data)
        // * JU: Postal 

        /**
         * eAutoindex:
         * * AI
         * * AR
         * * GL
         * * GR
         * * SG
         * * SO
         * * TG
         *
         * Registration needed, otherwise CHF 1.- / SMS
         */

        /**
         *
         */

        // Not available (not implemented yet)
        /*cantons.add (CantonFactory.create ("ag", null));
        cantons.add (CantonFactory.create ("ai", null));
        cantons.add (CantonFactory.create ("ar", null));
        cantons.add (CantonFactory.create ("be", null));
        cantons.add (CantonFactory.create ("bl", null));
        cantons.add (CantonFactory.create ("bs", null));
        cantons.add (CantonFactory.create ("fr", null));
        cantons.add (CantonFactory.create ("ge", null));
        cantons.add (CantonFactory.create ("gl", null));
        cantons.add (CantonFactory.create ("gr", null));
        cantons.add (CantonFactory.create ("ju", null));
        cantons.add (CantonFactory.create ("lu", null));
        cantons.add (CantonFactory.create ("nw", null));
        cantons.add (CantonFactory.create ("ow", null));
        cantons.add (CantonFactory.create ("sg", null));
        cantons.add (CantonFactory.create ("sh", null));
        cantons.add (CantonFactory.create ("so", null));
        cantons.add (CantonFactory.create ("sz", null));
        cantons.add (CantonFactory.create ("tg", null));
        cantons.add (CantonFactory.create ("ti", null));
        cantons.add (CantonFactory.create ("ur", null));
        cantons.add (CantonFactory.create ("vd", null));
        cantons.add (CantonFactory.create ("vs", null));
        cantons.add (CantonFactory.create ("zg", null));
        cantons.add (CantonFactory.create ("zh", null));
        // Liechtenstein (FL) is also supported, though it's not a Canton but a country of its own.
        cantons.add (CantonFactory.create ("fl", null));*/


        return cantons;
    }
}