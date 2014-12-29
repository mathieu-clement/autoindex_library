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
import com.mathieuclement.lib.autoindex.provider.exception.IgnoreMeException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.exception.RequestCancelledException;
import com.mathieuclement.lib.autoindex.provider.exception.UnsupportedPlateException;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoIndex for providers using the Cari system from Networkers SA.
 */
public abstract class CariAutoIndexProvider
        extends CaptchaAutoIndexProvider {
    private static Map<PlateType, Integer> plateTypeMapping = new LinkedHashMap<PlateType, Integer>();
    private static final Logger LOGGER = LoggerFactory.getLogger("autoindex.CariAutoIndexProvider");

    private static void initPlateTypeMapping() {
        plateTypeMapping.put(PlateType.AUTOMOBILE, 1);
        plateTypeMapping.put(PlateType.AUTOMOBILE_REPAIR_SHOP, 1);
        plateTypeMapping.put(PlateType.AUTOMOBILE_TEMPORARY, 1);
        plateTypeMapping.put(PlateType.AUTOMOBILE_BROWN, 6);

        plateTypeMapping.put(PlateType.MOTORCYCLE, 2);
        plateTypeMapping.put(PlateType.MOTORCYCLE_REPAIR_SHOP, 2);
        plateTypeMapping.put(PlateType.MOTORCYCLE_YELLOW, 3); // Jaune moto
        plateTypeMapping.put(PlateType.MOTORCYCLE_BROWN, 7); // Brun moto
        plateTypeMapping.put(PlateType.MOTORCYCLE_TEMPORARY, 2);

        plateTypeMapping.put(PlateType.MOPED, 20); // Cyclo

        plateTypeMapping.put(PlateType.AGRICULTURAL, 4);

        plateTypeMapping.put(PlateType.INDUSTRIAL, 5);
    }

    private static final int MAX_CAPTCHA_TRIES = 20;

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

    public final PlateOwner getPlateOwner(Plate plate, int requestId)
            throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException,
            UnsupportedPlateException, CaptchaException, RequestCancelledException {
        return doGetPlateOwner(plate, requestId, 0);
    }

    public final PlateOwner doGetPlateOwner(Plate plate, int requestId, int nbTry)
            throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException,
            UnsupportedPlateException, CaptchaException, RequestCancelledException {
        LOGGER.debug("Try " + nbTry + " for plate " + plate);

        if (mustCancel(requestId)) {
            throw new RequestCancelledException("Request id " + requestId + " was cancelled.", plate, requestId);
        }

        fireProgress(0, PROGRESS_STEPS);

        String lookupOwnerPageName = "rechDet";
        BasicHeader hostHeader = new BasicHeader("Host", getCariHttpHostname());

        // HTTP handling
        HttpContext httpContext = new BasicHttpContext();
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        // use our own cookie store
        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        // Load page a first time and get that session cookie!
        HttpRequest dummyPageViewRequest = new BasicHttpRequest("GET", getCariOnlineFullUrl() + lookupOwnerPageName);
        dummyPageViewRequest.setHeader(hostHeader);
        try {
            HttpResponse dummyResponse = httpClient.execute(getCariHttpHost(), dummyPageViewRequest, httpContext);
            StatusLine statusLine = dummyResponse.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                throw new ProviderException("Bad status when doing the dummy page view request to get a session: " +
                        statusLine.getStatusCode() + " " + statusLine.getReasonPhrase(), plate);
            }
            dummyResponse.getEntity().getContent().close();
        } catch (IOException e) {
            throw new ProviderException("Could not do the dummy page view request to get a session.", e, plate);
        }

        fireProgress(1, PROGRESS_STEPS);

        String captchaImageUrl = generateCaptchaImageUrl();
        String captchaValue = captchaHandler.handleCaptchaImage(requestId, captchaImageUrl, httpClient,
                getCariHttpHost(), httpContext, getCariHttpHostname(), this);

        fireProgress(2, PROGRESS_STEPS);

        BasicHttpEntityEnclosingRequest plateOwnerSearchRequest = new BasicHttpEntityEnclosingRequest(
                "POST", getCariOnlineFullUrl() + lookupOwnerPageName);
        plateOwnerSearchRequest.addHeader(hostHeader);
        addCommonHeaders(plateOwnerSearchRequest);
        // "Referer": spelling error on purpose as in the RFC);
        plateOwnerSearchRequest.addHeader(new BasicHeader("Referer", getCariOnlineFullUrl() + lookupOwnerPageName));
        plateOwnerSearchRequest.addHeader(new BasicHeader("Origin", getCariHttpHost().getSchemeName() + "://" +
                getCariHttpHostname()));

        if (!plateTypeMapping.containsKey(plate.getType())) {
            throw new UnsupportedPlateException("Plate type " + plate.getType() +
                    " is not supported by the Cari provider yet.", plate);
        }

        List<NameValuePair> postParams = makePostParams(plate, captchaValue);

        try {
            plateOwnerSearchRequest.setEntity(new UrlEncodedFormEntity(postParams));
        } catch (UnsupportedEncodingException e) {
            throw new ProviderException("Unsupported encoding for plate owner request parameters", e, plate);
        }

        try {
            HttpResponse plateOwnerResponse = httpClient.execute(getCariHttpHost(), plateOwnerSearchRequest,
                    httpContext);
            if (plateOwnerResponse.getStatusLine().getStatusCode() != 200) {
                throw new ProviderException("Got status " + plateOwnerResponse.getStatusLine().getStatusCode()
                        + " from server when executing request to get plate owner of plate " + plate, plate);
            }


            // Extract the plate owner from the HTML response
            PlateOwner plateOwner = htmlToPlateOwner(plateOwnerResponse, plate);
            fireProgress(PROGRESS_STEPS, PROGRESS_STEPS);

            // Close connection and release resources
            httpClient.getConnectionManager().shutdown();

            return plateOwner;
        } catch (IOException e) {
            throw new ProviderException("Could not perform plate owner request on plate " + plate, e, plate);
        } catch (PlateOwnerDataException e) {
            throw new ProviderException("Found a result for " + plate + " but there was a problem parsing that result.",
                    e, plate);
        } catch (CaptchaException e) {
            if (nbTry > MAX_CAPTCHA_TRIES) {
                throw new ProviderException("Too many tries decoding the captcha image", e, plate);
            }
            return doGetPlateOwner(plate, requestId, nbTry + 1); // Call itself again until nbTry above max
        } catch (IgnoreMeException e) {
            throw new ProviderException("Strange error on plate " + plate + ", went back to welcome page. ", e, plate);
        }
    }

    private List<NameValuePair> makePostParams(Plate plate, String captchaValue) {
        List<NameValuePair> postParams = new LinkedList<NameValuePair>();
        postParams.add(new BasicNameValuePair("no", String.valueOf(plate.getNumber())));
        postParams.add(new BasicNameValuePair("cat", String.valueOf(plateTypeMapping.get(plate.getType()))));
        postParams.add(new BasicNameValuePair("sousCat", String.valueOf(toSousCat(plate))));
        postParams.add(new BasicNameValuePair("captchaVal", captchaValue));
        // hidden parameters
        postParams.add(new BasicNameValuePair("action", "query"));
        postParams.add(new BasicNameValuePair("pageContext", "login"));
        postParams.add(new BasicNameValuePair("valider", "Continuer"));
        postParams.add(new BasicNameValuePair("effacer", "Effacer"));
        return postParams;
    }

    private int toSousCat(Plate plate) {
        int sousCat = 1;
        if (PlateType.AUTOMOBILE_TEMPORARY.equals(plate.getType())) {
            sousCat = 2;
        } else if (PlateType.MOTORCYCLE_TEMPORARY.equals(plate.getType())) {
            sousCat = 2;
        } else if (PlateType.AUTOMOBILE_REPAIR_SHOP.equals(plate.getType())) {
            sousCat = 3;
        } else if (PlateType.MOTORCYCLE_REPAIR_SHOP.equals(plate.getType())) {
            sousCat = 3;
        } else if (PlateType.MOPED.equals(plate.getType())) {
            sousCat = 21;
        }
        return sousCat;
    }

    private void addCommonHeaders(BasicHttpEntityEnclosingRequest plateOwnerSearchRequest) {
        plateOwnerSearchRequest.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        plateOwnerSearchRequest.addHeader(new BasicHeader("Accept-Charset", "utf-8"));
        plateOwnerSearchRequest.addHeader(new BasicHeader("Accept-Language", "fr"));
        plateOwnerSearchRequest.addHeader(new BasicHeader("Accept-Encoding", "gzip,deflate,sdch"));
        plateOwnerSearchRequest.addHeader(new BasicHeader("Connection", "keep-alive"));
        plateOwnerSearchRequest.addHeader(new BasicHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/32.0.1667.0 Safari/537.36"));
    }

    protected abstract HttpHost getCariHttpHost();

    protected abstract String getCariHttpHostname();

    private static final Pattern PLATE_OWNER_PATTERN = Pattern.compile(
            "<td class='libelle'>(.+)\\s*</td>\\s+<td( nowrap)?>\\s*(.+)\\s*</td>");

    private PlateOwner htmlToPlateOwner(HttpResponse response, Plate plate) throws IOException,
            PlateOwnerDataException, CaptchaException, ProviderException, PlateOwnerNotFoundException,
            PlateOwnerHiddenException, IgnoreMeException {
        String htmlPage = ResponseUtils.toString(response);

        // Check presence of warning (shown on Fribourg webpage)
        /*
        if(htmlPage.contains("iframe_warning")) {
            LOGGER.warn("Found a warning (iframe_warning) on page!");
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

        // In Fribourg, currently the message "Aucun détenteur trouvé!" is shown both when the owner wants to hide his
        // data and the number is not allocated,
        // but in Valais, the pages are different. It prints "Ce numéro de plaque est hors tabelle" when nobody owns
        // the number.
        if (htmlPage.contains("Aucun détenteur trouvé!") || htmlPage.contains("Ce numéro de plaque est hors tabelle")) {
            throw new PlateOwnerNotFoundException("Plate owner not found or hidden", plate);
        }
        // See http://www.vs.ch/navig/navig.asp?MenuID=25069&RefMenuID=0&RefServiceID=0
        if (htmlPage.contains("motivation") ||
                // FR
                htmlPage.contains("parent.parent.location.href=" +
                        "\"http://www.ocn.ch/ocn/fr/pub/ocn_online/autoindex/protection_des_donnees.htm\";") ||
                // VS
                htmlPage.contains("MenuID=25069")
                ) {
            throw new PlateOwnerHiddenException("Plate owner doesn't want to publish his data.", plate);
        }

        // TODO Handle "Plaque réservée"
        if (htmlPage.contains("Plaque réservée")) {
            throw new PlateOwnerHiddenException("Reserved plate", plate);
        }

        // TODO I noticed in Valais, you can get the message "Plaque disponible".
        // Maybe we can do something with that message.

        Matcher matcher = PLATE_OWNER_PATTERN.matcher(htmlPage);

        while (matcher.find()) {
            if (matcher.group(0).contains("checkField") || matcher.group(0).contains("Captcha Code generation error")) {
                throw new IgnoreMeException("Something went bad because we were presented the form page again!", plate);
            }

            String dataName = matcher.group(1);
            String dataValue = matcher.group(3);
            dataValue = ResponseUtils.removeUselessSpaces(dataValue); // Clean data
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

    static {
        initPlateTypeMapping();
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        try {
            return plateTypeMapping.get(plateType) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private Set<Integer> cancelledRequests = new HashSet<Integer>();

    private boolean mustCancel(int requestId) {
        return cancelledRequests.contains(requestId);
    }

    @Override
    public void cancel(int requestId) {
        cancelledRequests.add(requestId);
    }

    @Override
    public boolean isCancelled(int requestId) {
        return mustCancel(requestId);
    }

    private static final int PROGRESS_STEPS = 3;

    @Override
    public boolean isIndeterminateProgress() {
        return false;
    }
}