/*
 * Filename: CariAutoIndexProvider.java
 *
 * Copyright 1987-2012 by Informatique-MTF, SA, Route du Bleuet 1, CH-1762
 * Givisiez/Fribourg, Switzerland All rights reserved.
 *
 *========================================================================
 */
package com.mathieuclement.lib.autoindex.provider.cari.sync;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateOwnerDataException;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.exception.UnsupportedPlateException;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CariAutoIndexProvider
        extends CaptchaAutoIndexProvider {
    private Map<PlateType, Integer> plateTypeMapping = new LinkedHashMap<PlateType, Integer>();

    private void initPlateTypeMapping() {
        plateTypeMapping.put(PlateType.AUTOMOBILE, 1);
        plateTypeMapping.put(PlateType.MOTORCYCLE, 2);
        //plateTypeMapping.put(PlateType.MOPED, 20);
        // TODO Add "Jaune moto"
        plateTypeMapping.put(PlateType.AGRICULTURAL, 4);
        //plateTypeMapping.put(PlateType.INDUSTRIAL, 5);
        // TODO Add "Brun auto"
        // TODO Add "Brun moto"

        // TODO Add "Bateau"
        plateTypeMapping.put(PlateType.BOAT, 21);
    }

    protected CariAutoIndexProvider(CaptchaHandler captchaHandler) {
        super(captchaHandler);
        initPlateTypeMapping();
    }

    /**
     * Return the search page URL, e.g. "https://appls2.fr.ch/cari/"
     *
     * @return the search page URL
     */
    protected abstract String getCariOnlineFullUrl();

    public final PlateOwner getPlateOwner(Plate plate)
            throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, UnsupportedPlateException {
        // Headers
        Header urlEncodedContentTypeHeader = new BasicHeader("Content-Type", "application/x-www-form-urlencoded");
        Header charsetHeader = new BasicHeader("Accept-Charset", "utf-8");
        Header languageHeader = new BasicHeader("Accept-Language", "fr");
        Header encodingHeader = new BasicHeader("Accept-Encoding", "gzip,deflate,sdch");
        Header connectionHeader = new BasicHeader("Connection", "keep-alive");
        // TODO Cannot keep that. Use our own User-Agent or something related to Apache HttpClient...
        Header userAgentHeader = new BasicHeader("User-Agent", "Swiss-AutoIndex/0.1");
        String lookupOwnerPageName = "rechDet";
        Header referrerHeader = new BasicHeader("Referer", getCariOnlineFullUrl() + lookupOwnerPageName); // spelling error on purpose as in the RFC
        Header originHeader = new BasicHeader("Origin", getCariHttpHost().getSchemeName() + "://" + getCariHttpHostname());
        Header hostHeader = new BasicHeader("Host", getCariHttpHostname());

        // HTTP handling
        HttpContext httpContext = new BasicHttpContext();
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        //httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2965);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        // use our own cookie store
        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        // Load page a first time and get that session cookie!
        HttpRequest dummyPageViewRequest = new BasicHttpRequest("GET", getCariOnlineFullUrl() + lookupOwnerPageName, HttpVersion.HTTP_1_1);
        dummyPageViewRequest.setHeader(hostHeader);
        try {
            HttpResponse dummyResponse = httpClient.execute(getCariHttpHost(), dummyPageViewRequest, httpContext);
            StatusLine statusLine = dummyResponse.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                throw new ProviderException("Bad status when doing the dummy page view request to get a session: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase(), plate);
            }
            dummyResponse.getEntity().getContent().close();
        } catch (IOException e) {
            throw new ProviderException("Could not do the dummy page view request to get a session.", e, plate);
        }

        String captchaImageUrl = generateCaptchaImageUrl();
        String captchaValue = captchaHandler.handleCaptchaImage(captchaImageUrl, httpClient, getCariHttpHost(), httpContext, getCariHttpHostname(), this);

        // TODO Doesn't have Cari a TimeOut for the captcha or something like this? Connection can be closed after some time.
        // We have to get that time from the server. Then in the GUI, we show a count down, so the user can see how much time is left to enter the code.
        // After the time is over, up to a maximal number of times, the code is generated again and refreshed on the screen.

        // TODO Set referer
        // TODO Set User-Agent header to the most used browser (probably the last available version of Internet Explorer)
        BasicHttpEntityEnclosingRequest plateOwnerSearchRequest = new BasicHttpEntityEnclosingRequest("POST", getCariOnlineFullUrl() + lookupOwnerPageName, HttpVersion.HTTP_1_1);
        plateOwnerSearchRequest.addHeader(hostHeader);
        plateOwnerSearchRequest.addHeader(urlEncodedContentTypeHeader);
        plateOwnerSearchRequest.addHeader(charsetHeader);
        plateOwnerSearchRequest.addHeader(languageHeader);
        plateOwnerSearchRequest.addHeader(encodingHeader);
        plateOwnerSearchRequest.addHeader(connectionHeader);
        plateOwnerSearchRequest.addHeader(userAgentHeader);
        plateOwnerSearchRequest.addHeader(referrerHeader);
        plateOwnerSearchRequest.addHeader(originHeader);

        List<NameValuePair> postParams = new LinkedList<NameValuePair>();
        postParams.add(new BasicNameValuePair("no", String.valueOf(plate.getNumber())));

        if (!plateTypeMapping.containsKey(plate.getType())) {
            throw new UnsupportedPlateException("Plate type " + plate.getType() + " is not supported by the Cari provider yet.", plate);
        }
        postParams.add(new BasicNameValuePair("cat", String.valueOf(plateTypeMapping.get(plate.getType()))));

        // Set sous-catégorie to "Normale" (auto / moto)
        int sousCat = 1;
        if (PlateType.BOAT.equals(plate.getType())) {
            sousCat = 11;
        }
        postParams.add(new BasicNameValuePair("sousCat", String.valueOf(sousCat)));

        postParams.add(new BasicNameValuePair("captchaVal", captchaValue));

        // hidden parameters
        postParams.add(new BasicNameValuePair("action", "query"));
        postParams.add(new BasicNameValuePair("pageContext", "login"));
        postParams.add(new BasicNameValuePair("valider", "Continuer"));
        postParams.add(new BasicNameValuePair("effacer", "Effacer"));

        try {
            plateOwnerSearchRequest.setEntity(new UrlEncodedFormEntity(postParams));
        } catch (UnsupportedEncodingException e) {
            throw new ProviderException("Unsupported encoding for plate owner request parameters", e, plate);
        }

        try {
            HttpResponse plateOwnerResponse = httpClient.execute(getCariHttpHost(), plateOwnerSearchRequest, httpContext);
            if (plateOwnerResponse.getStatusLine().getStatusCode() != 200) {
                throw new ProviderException("Got status " + plateOwnerResponse.getStatusLine().getStatusCode()
                        + " from server when executing request to get plate owner of plate " + plate, plate);
            }

            // Extract the plate owner from the HTML response
            PlateOwner plateOwner = htmlToPlateOwner(plateOwnerResponse, plate);

            // Close connection and release resources
            httpClient.getConnectionManager().shutdown();

            return plateOwner;

        } catch (IOException e) {
            throw new ProviderException("Could not perform plate owner request on plate " + plate, e, plate);
        } catch (PlateOwnerDataException e) {
            throw new ProviderException("Found a result for " + plate + " but there was a problem parsing that result.", e, plate);
        } catch (CaptchaException e) {
            throw new ProviderException("Problem with Captcha", e, plate);
        }
    }

    protected abstract HttpHost getCariHttpHost();

    protected abstract String getCariHttpHostname();

    private final Logger logger = Logger.getLogger("CariAutoIndexProvider");

    private static final Pattern plateOwnerPattern = Pattern.compile("<td class='libelle'>(.+)\\s*</td>\\s+<td( nowrap)?>\\s*(.+)\\s*</td>");

    private PlateOwner htmlToPlateOwner(HttpResponse response, Plate plate) throws IOException, PlateOwnerDataException, CaptchaException, ProviderException, PlateOwnerNotFoundException {
        String htmlPage = ResponseUtils.toString(response);

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

        if (htmlPage.contains("Code incorrect")) {
            captchaHandler.onCaptchaFailed();
            throw new CaptchaException("Invalid captcha code");
        } else {
            captchaHandler.onCaptchaSuccessful();
        }


        PlateOwner plateOwner = new PlateOwner();

        // In Fribourg, currently the message "Aucun détenteur trouvé!" is shown both when the owner wants to hide its data and the number is not allocated,
        // but in Valais, the pages are different. It prints "Ce numéro de plaque est hors tabelle" when nobody owns the number.
        if (htmlPage.contains("Aucun détenteur trouvé!") || htmlPage.contains("Ce numéro de plaque est hors tabelle")) {
            throw new PlateOwnerNotFoundException("Plate owner not found or hidden", plate);
        }

        // TODO I noticed in Valais, you can get the message "Plaque disponible". Maybe we can do something with that message.

        Matcher matcher = plateOwnerPattern.matcher(htmlPage);

        while (matcher.find()) {
            if (matcher.group(0).contains("checkField") || matcher.group(0).contains("Captcha Code generation error")) {
                throw new ProviderException("Something went bad because we were presented the form page again!", plate);
            }

            String dataName = matcher.group(1);
            String dataValue = matcher.group(3);
            if (dataName != null && dataValue != null) {
                if (dataName.equals("Nom")) {
                    plateOwner.setName(unescapeHtml(dataValue));
                } else if (dataName.equals("Adresse")) {
                    plateOwner.setAddress(unescapeHtml(dataValue));
                } else if (dataName.equals("Complément")) {
                    plateOwner.setAddressComplement(unescapeHtml(dataValue));
                } else if (dataName.equals("Localité")) {
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

    private String unescapeHtml(String escapedHtml) {
        return StringEscapeUtils.unescapeHtml4(escapedHtml);
    }

    private String generateCaptchaImageUrl() {
        return getCariOnlineFullUrl() + "drawCaptcha?rnd=" + Math.random();
    }

    @Override
    public String regenerateCaptchaImageUrl() {
        return generateCaptchaImageUrl();
    }

    private static Set<PlateType> supportedPlateTypes = new LinkedHashSet<PlateType>();

    static {
        supportedPlateTypes.add(PlateType.AUTOMOBILE);
        supportedPlateTypes.add(PlateType.MOTORCYCLE);
        supportedPlateTypes.add(PlateType.AGRICULTURAL);
        supportedPlateTypes.add(PlateType.BOAT);
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return supportedPlateTypes.contains(plateType);
    }
}