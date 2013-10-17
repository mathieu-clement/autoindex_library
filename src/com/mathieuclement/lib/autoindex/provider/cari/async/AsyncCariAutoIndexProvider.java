package com.mathieuclement.lib.autoindex.provider.cari.async;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateOwnerDataException;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.MyHttpClient;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.exception.*;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AsyncCariAutoIndexProvider extends AsyncAutoIndexProvider {

    private Map<PlateType, Integer> plateTypeMapping = new LinkedHashMap<PlateType, Integer>();
    private String lookupOwnerPageName = "rechDet";
    private HttpClient httpClient;
    private HttpContext httpContext;
    private HttpRequest dummyPageViewRequest;
    private BasicHttpEntityEnclosingRequest plateOwnerSearchRequest;
    private HttpResponse plateOwnerResponse;
    private PlateOwner plateOwner;
    private PlateOwner plateOwner1;
    private String htmlPage;
    private CaptchaException captchaException = new CaptchaException("Invalid captcha code");

    public AsyncCariAutoIndexProvider() {
        super();
        initPlateTypeMapping();
    }

    @Override
    public boolean isCaptchaUppercaseOnly() {
        return false;
    }

    private Set<Header> getHttpHeaders() {
        Set<Header> headers = new LinkedHashSet<Header>();

//        headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
//        headers.add(new BasicHeader("Accept-Charset", "utf-8"));
//        headers.add(new BasicHeader("Accept-Language", "fr"));
//        headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate,sdch"));
//        headers.add(new BasicHeader("Connection", "keep-alive"));
//        headers.add(new BasicHeader("User-Agent", "Swiss-AutoIndex/0.1"));
//        headers.add(new BasicHeader("Referer", getCariOnlineFullUrl() + lookupOwnerPageName)); // spelling error on purpose as in the RFC
//        headers.add(new BasicHeader("Origin", getCariHttpHost().getSchemeName() + "://" + getCariHttpHostname()));
//        headers.add(getHostHeader());

        return headers;
    }

    private Header getHostHeader() {
        return new BasicHeader("Host", getCariHttpHostname());
    }



    private void initPlateTypeMapping() {
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

    /**
     * Return the search page URL, e.g. "https://appls2.fr.ch/cari/"
     *
     * @return the search page URL
     */
    protected abstract String getCariOnlineFullUrl();

    protected void makeRequestBeforeCaptchaEntered(Plate plate) throws ProviderException {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        if (httpClient == null) {
            /*
            Workaround for SSL certificates problems
            as found on http://stackoverflow.com/a/4837230/753136

            You have to add CA file with this command:
            CLASSPATH=~/Downloads/bcprov-ext-jdk15on-1.46.jar keytool -importcert -v
            -file /home/mathieu/Documents/appls.fr.der -alias ca -keystore "mySrvTruststore.bks"
            -provider org.bouncycastle.jce.provider.BouncyCastleProvider
            -providerpath "bcprov-jdk16-145.jar" -storetype BKS -storepass testtest

            The bks file is then moved to the res/raw/ folder as trust_store.bks.
             */

            try {
                Security.addProvider(new BouncyCastleProvider());
                File file = new File(MyHttpClient.class.getResource("trust_store.bks").getFile());
                if(!file.exists()) {
                    throw new RuntimeException("BKS file not found!");
                }
                //httpClient = new DecompressingHttpClient(new MyHttpClient(new FileInputStream(file))); // gzip
                // support
                httpClient = new MyHttpClient(new FileInputStream(file));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        makeRequestBeforeCaptchaEntered(plate, httpClient);
    }

    protected void makeRequestBeforeCaptchaEntered(Plate plate, HttpClient httpClient) throws ProviderException {

        // HTTP handling
        if (httpContext == null) {
            httpContext = new BasicHttpContext();
        }
        //httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2965);
        // use our own cookie store


        // Load page a first time and get that session cookie!
        if (dummyPageViewRequest == null) {
            dummyPageViewRequest = new BasicHttpRequest("GET", getCariOnlineFullUrl() + lookupOwnerPageName, HttpVersion.HTTP_1_1);
            CookieStore cookieStore = new BasicCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            //dummyPageViewRequest.setHeader(getHostHeader());
            for (Header header : getHttpHeaders()) {
                dummyPageViewRequest.addHeader(header);
            }
            try {
                HttpResponse dummyResponse = httpClient.execute(getCariHttpHost(), dummyPageViewRequest, httpContext);
                StatusLine statusLine = dummyResponse.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    firePlateRequestException(plate, new ProviderException("Bad status when doing the dummy page view request to get a session: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase(), plate));
                    return;
                }
                dummyResponse.getEntity().getContent().close();
            } catch (IOException e) {
                firePlateRequestException(plate, new ProviderException("Could not do the dummy page view request to get a session.", e, plate));
                return;
            }
        }

        fireCaptchaCodeRequested(plate, generateCaptchaImageUrl(), httpClient, getCariHttpHost(), httpContext, getCariHttpHostname(), this);
    }

    @Override
    protected void doRequestAfterCaptchaEntered(String captchaCode, Plate plate, HttpClient httpClient, HttpContext httpContext) {
        // TODO Doesn't have Cari a TimeOut for the captcha or something like this? Connection can be closed after some time.
        // We have to get that time from the server. Then in the GUI, we show a count down, so the user can see how much time is left to enter the code.
        // After the time is over, up to a maximal number of times, the code is generated again and refreshed on the screen.

        // TODO Set referer
        // TODO Set User-Agent header to the most used browser (probably the last available version of Internet Explorer)
        if (plateOwnerSearchRequest == null) {
            plateOwnerSearchRequest = new BasicHttpEntityEnclosingRequest("POST", getCariOnlineFullUrl() + lookupOwnerPageName, HttpVersion.HTTP_1_1);
            for (Header header : getHttpHeaders()) {
                plateOwnerSearchRequest.addHeader(header);
            }
        }

        List<NameValuePair> postParams = new LinkedList<NameValuePair>();
        postParams.add(new BasicNameValuePair("no", String.valueOf(plate.getNumber())));

        if (!plateTypeMapping.containsKey(plate.getType())) {
            firePlateRequestException(plate, new UnsupportedPlateException("Plate type " + plate.getType() + " is not supported by the Cari provider yet.", plate));
            return;
        }
        postParams.add(new BasicNameValuePair("cat", String.valueOf(plateTypeMapping.get(plate.getType()))));

        // Set sous-catégorie to "Normale" (auto / moto / agricultural / industrial)
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
        postParams.add(new BasicNameValuePair("sousCat", String.valueOf(sousCat)));

        postParams.add(new BasicNameValuePair("captchaVal", captchaCode));

        // hidden parameters
        postParams.add(new BasicNameValuePair("action", "query"));
        postParams.add(new BasicNameValuePair("pageContext", "login"));
        postParams.add(new BasicNameValuePair("valider", "Continuer"));
        postParams.add(new BasicNameValuePair("effacer", "Effacer"));

        try {
            plateOwnerSearchRequest.setEntity(new UrlEncodedFormEntity(postParams));
        } catch (UnsupportedEncodingException e) {
            firePlateRequestException(plate, new ProviderException("Unsupported encoding for plate owner request parameters", e, plate));
            return;
        }

        try {
            plateOwnerResponse = httpClient.execute(getCariHttpHost(), plateOwnerSearchRequest, httpContext);
            if (plateOwnerResponse.getStatusLine().getStatusCode() != 200) {
                firePlateRequestException(plate, new ProviderException("Got status " + plateOwnerResponse.getStatusLine().getStatusCode()
                        + " from server when executing request to get plate owner of plate " + plate, plate));
                return;
            }

            // Extract the plate owner from the HTML response
            plateOwner = htmlToPlateOwner(plateOwnerResponse, plate);
            fireCaptchaCodeAccepted(plate);

            // Close connection and release resources
            // Disabled as a workaround for "java.lang.IllegalStateException: Connection manager has been shut down"
            //httpClient.getConnectionManager().shutdown();

            firePlateOwnerFound(plate, plateOwner);
        } catch (IOException e) {
            firePlateRequestException(plate, new ProviderException("Could not perform plate owner request on plate " + plate, e, plate));
        } catch (PlateOwnerDataException e) {
            firePlateRequestException(plate, new ProviderException("Found a result for " + plate + " but there was a problem parsing that result.", e, plate));
        } catch (CaptchaException e) {
            firePlateRequestException(plate, new ProviderException("Problem with Captcha", e, plate));
        } catch (PlateOwnerNotFoundException e) {
            firePlateRequestException(plate, e);
        } catch (ProviderException e) {
            firePlateRequestException(plate, e);
        } catch (PlateOwnerHiddenException e) {
            firePlateRequestException(plate, e);
        } catch (IgnoreMeException e) {
            e.printStackTrace();
        }

        //firePlateRequestException(plate, new ProviderException("Problem with Captcha", new CaptchaException("Bad captcha"), plate));
    }

    protected abstract HttpHost getCariHttpHost();

    protected abstract String getCariHttpHostname();

    private final Logger logger = Logger.getLogger("CariAutoIndexProvider");

    private static final Pattern plateOwnerPattern = Pattern.compile("<td class='libelle'>(.+)\\s*</td>\\s+<td( nowrap)?>\\s*(.+)\\s*</td>");

    protected PlateOwner htmlToPlateOwner(HttpResponse response, Plate plate) throws IOException, PlateOwnerDataException,
            CaptchaException, ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, IgnoreMeException {
        htmlPage = ResponseUtils.toString(response);

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
            throw captchaException;
        }
        plateOwner1 = new PlateOwner();

        // In Fribourg, currently the message "Aucun détenteur trouvé!" is shown both when the owner wants to hide its data and the number is not allocated,
        // but in Valais, the pages are different. It prints "Ce numéro de plaque est hors tabelle" when nobody owns the number.
        if (htmlPage.contains("Aucun détenteur trouvé!") || htmlPage.contains("Ce numéro de plaque est hors tabelle") || htmlPage.contains("Plaque disponible")) {
            throw new PlateOwnerNotFoundException("Plate owner not found or hidden", plate);
        }

        // See http://www.vs.ch/navig/navig.asp?MenuID=25069&RefMenuID=0&RefServiceID=0
        if (htmlPage.contains("motivation") || htmlPage.contains("parent.parent.location.href=\"http://www.ocn.ch/ocn/fr/pub/ocn_online/autoindex/protection_des_donnees.htm\";")) {
            throw new PlateOwnerHiddenException("Plate owner doesn't want to publish his data.", plate);
        }

        // TODO Handle "Plaque réservée"
        if (htmlPage.contains("Plaque réservée")) {
            throw new PlateOwnerHiddenException("Reserved plate", plate);
        }

        // TODO I noticed in Valais, you can get the message "Plaque disponible". Maybe we can do something with that message.

        Matcher matcher = plateOwnerPattern.matcher(htmlPage);

        while (matcher.find()) {
            if (matcher.group(0).contains("checkField") || matcher.group(0).contains("Captcha Code generation error")) {
                throw new IgnoreMeException("Something went bad because we were presented the form page again!", plate);
            }

            String dataName = matcher.group(1);
            String dataValue = matcher.group(3);
            dataValue = ResponseUtils.removeUselessSpaces(dataValue); // Clean data
            if (dataName != null && dataValue != null) {
                if (dataName.equals("Nom")) {
                    plateOwner1.setName(unescapeHtml(dataValue));
                } else if (dataName.equals("Adresse")) {
                    plateOwner1.setAddress(unescapeHtml(dataValue));
                } else if (dataName.equals("Complément")) {
                    plateOwner1.setAddressComplement(unescapeHtml(dataValue));
                } else if (dataName.equals("Localité")) {
                    // Separate Zip code from town name
                    String[] split = unescapeHtml(dataValue).split(" ");
                    try {
                        plateOwner1.setZip(Integer.parseInt(split[0]));
                    } catch (NumberFormatException nfe) {
                        throw new PlateOwnerDataException("Invalid ZIP code '" + split[0] + "'.", plateOwner);
                    }
                    plateOwner1.setTown(unescapeHtml(dataValue).substring(split[0].length() + 1));
                }
            }
        }

        // Check plate owner data
        plateOwner1.check();

        return plateOwner1;
    }

    protected String unescapeHtml(String escapedHtml) {
        return StringEscapeUtils.unescapeHtml4(escapedHtml);
    }

    public String generateCaptchaImageUrl() {
        return getCariOnlineFullUrl() + "drawCaptcha?rnd=" + Math.random();
    }

    private static Set<PlateType> supportedPlateTypes = new LinkedHashSet<PlateType>();

    static {
        supportedPlateTypes.add(PlateType.AUTOMOBILE);
        supportedPlateTypes.add(PlateType.AUTOMOBILE_BROWN);
        supportedPlateTypes.add(PlateType.AUTOMOBILE_TEMPORARY);
        supportedPlateTypes.add(PlateType.AUTOMOBILE_REPAIR_SHOP);

        supportedPlateTypes.add(PlateType.MOTORCYCLE);
        supportedPlateTypes.add(PlateType.MOTORCYCLE_YELLOW);
        supportedPlateTypes.add(PlateType.MOTORCYCLE_BROWN);
        supportedPlateTypes.add(PlateType.MOTORCYCLE_TEMPORARY);
        supportedPlateTypes.add(PlateType.MOTORCYCLE_REPAIR_SHOP);

        supportedPlateTypes.add(PlateType.MOPED);
        supportedPlateTypes.add(PlateType.AGRICULTURAL);
        supportedPlateTypes.add(PlateType.INDUSTRIAL);
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return supportedPlateTypes.contains(plateType);
    }
}
