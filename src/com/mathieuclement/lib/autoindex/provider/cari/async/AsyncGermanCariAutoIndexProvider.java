package com.mathieuclement.lib.autoindex.provider.cari.async;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateOwnerDataException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mathieu Clément
 * @since 19.09.2013
 */
public abstract class AsyncGermanCariAutoIndexProvider extends AsyncCariAutoIndexProvider {
    private String htmlPage;
    private CaptchaException captchaException = new CaptchaException("Invalid captcha code");
    private static final Pattern plateOwnerPattern = Pattern.compile("<td class='libelle'>(.+)\\s*</td>\\s+<td( nowrap)?>\\s*(.+)\\s*</td>");

    protected PlateOwner htmlToPlateOwner(HttpResponse response, Plate plate) throws IOException, PlateOwnerDataException,
            CaptchaException, ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException {
        htmlPage = ResponseUtils.toString(response);
        System.err.println(htmlPage);

        // Check presence of warning (shown on Fribourg webpage)
        /*
        if(htmlPage.contains("iframe_warning")) {
            logger.warning("Found a warning (iframe_warning) on page!");
        }
        */

        // I have seen this once on the Valais webpage
        if (htmlPage.contains("<title>Error</title>")) {
            throw new ProviderException("Got the Error page: " + htmlPage, plate);
        }

        if (htmlPage.contains("Falscher Code")) {
            throw captchaException;
        }
        PlateOwner plateOwner = new PlateOwner();

        // In Fribourg, currently the message "Aucun détenteur trouvé!" is shown both when the owner wants to hide its data and the number is not allocated,
        // but in Valais, the pages are different. It prints "Ce numéro de plaque est hors tabelle" when nobody owns the number.
        if (
                htmlPage.contains("Das von Ihnen gesuchte Kontrollschild ist nicht in Verkehr") ||
                        htmlPage.contains("Kontrollschild nicht verfügbar")) {
            throw new PlateOwnerNotFoundException("Plate owner not found or hidden", plate);
        }

        // See http://www.vs.ch/navig/navig.asp?MenuID=25069&RefMenuID=0&RefServiceID=0
        if (htmlPage.contains("motivation") ||
                htmlPage.contains("parent.parent.location.href=\"http://www.baselland" +
                        ".ch/formulare/mfk_kontrollschild-gesperrt.html\"") ||
                htmlPage.contains("parent.parent.location.href=\"http://www.ocn" +
                        ".ch/ocn/fr/pub/ocn_online/autoindex/protection_des_donnees.htm\";")) {
            throw new PlateOwnerHiddenException("Plate owner doesn't want to publish his data.", plate);
        }

        // TODO Handle "Plaque réservée"
        /*if (htmlPage.contains("Plaque réservée")) {
            throw new PlateOwnerHiddenException("Reserved plate", plate);
        }*/

        // TODO I noticed in Valais, you can get the message "Plaque disponible". Maybe we can do something with that message.

        Matcher matcher = plateOwnerPattern.matcher(htmlPage);

        while (matcher.find()) {
            if (matcher.group(0).contains("checkField") || matcher.group(0).contains("Captcha Code generation error")) {
                throw new ProviderException("Something went bad because we were presented the form page again!", plate);
            }

            String dataName = matcher.group(1);
            String dataValue = matcher.group(3);
            dataValue = ResponseUtils.removeUselessSpaces(dataValue); // Clean data
            if (dataName != null && dataValue != null) {
                if (dataName.equals("Name")) {
                    plateOwner.setName(unescapeHtml(dataValue));
                } else if (dataName.equals("Adresse")) {
                    plateOwner.setAddress(unescapeHtml(dataValue));
                } else if (dataName.equals("Zusatz")) {
                    plateOwner.setAddressComplement(unescapeHtml(dataValue));
                } else if (dataName.equals("Ort")) {
                    // Separate Zip code from town name
                    String[] split = unescapeHtml(dataValue).split(" ");
                    try {
                        plateOwner.setZip(Integer.parseInt(split[0]));
                    } catch (NumberFormatException nfe) {
                        throw new PlateOwnerDataException("Invalid ZIP code '" + split[0] + "'.", plateOwner);
                    }
                    plateOwner.setTown(unescapeHtml(dataValue).substring(split[0].length() + 1));
                }
            }
        }

        // Check plate owner data
        plateOwner.check();

        return plateOwner;
    }
}
