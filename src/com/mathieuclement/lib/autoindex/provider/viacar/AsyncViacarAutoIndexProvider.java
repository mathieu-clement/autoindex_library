package com.mathieuclement.lib.autoindex.provider.viacar;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.exception.NumberOfRequestsExceededException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mathieu Clément
 * @since 29.06.2013
 */
public class AsyncViacarAutoIndexProvider extends AsyncAutoIndexProvider {

    private static final String THIS_IS_THE_CAPTCHA = "THIS_IS_THE_CAPTCHA";
    private final String resultUri = "https://www.viacar.ch/eindex/Result.aspx?Var=1";
    private String cantonAbbr;
    private static Set<PlateType> supportedPlateTypes = new LinkedHashSet<PlateType>();

    static {
        supportedPlateTypes.add(PlateType.AUTOMOBILE);
        supportedPlateTypes.add(PlateType.MOTORCYCLE);
        supportedPlateTypes.add(PlateType.AGRICULTURAL);
    }

    private DefaultHttpClient httpClient;
    private HttpContext httpContext;
    private HttpUriRequest dummyPageViewRequest;
    // correct name matching the Server certificate!
    private String captchaId = "";
    private CookieStore cookieStore;
    private HttpPost captchaRequest;
    private List<NameValuePair> captchaPostParams;
    private List<NameValuePair> searchFormParams;
    private HttpPost searchRequest;
    private PlateOwner plateOwner;
    private HttpGet resultRequest;
    private String debugHtml; // TODO Remove

    public AsyncViacarAutoIndexProvider(String cantonAbbr) {
        super();
        this.cantonAbbr = cantonAbbr.toLowerCase();
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return supportedPlateTypes.contains(plateType);
    }

    @Override
    public boolean isCaptchaUppercaseOnly() {
        return true;
    }

    @Override
    protected void makeRequestBeforeCaptchaEntered(Plate plate) throws ProviderException {
        if (httpClient == null) {
            try {
                // httpClient = new DecompressingHttpClient(new DefaultHttpClient()); // for gzip
                httpClient = new DefaultHttpClient();
                HttpParams httpParams = httpClient.getParams();
                httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                httpParams.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "https://www.viacar.ch");
                httpParams.setParameter(CoreProtocolPNames.USER_AGENT,
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.22 (KHTML, like Gecko) " +
                                "Ubuntu Chromium/25.0.1364.160 Chrome/25.0.1364.160 Safari/537.22");
                httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
                httpParams.setParameter(ClientPNames.DEFAULT_HEADERS, new ArrayList<Header>());
                // httpParams.setParameter("http.protocol.single-cookie-header", true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        makeRequestBeforeCaptchaEntered(plate, httpClient);
    }

    @Override
    protected void makeRequestBeforeCaptchaEntered(Plate plate, HttpClient httpClient) throws ProviderException {
        // HTTP handling
        if (httpContext == null) {
            httpContext = new BasicHttpContext();
        }

        // Load page a first time and get that session cookie! (+ hidden POST fields)
        dummyPageViewRequest = new HttpGet(getLoginUrl());
        if (cookieStore == null) {
            cookieStore = ((AbstractHttpClient) httpClient).getCookieStore();
        }
        try {
            HttpResponse dummyResponse = httpClient.execute(dummyPageViewRequest, httpContext);
            StatusLine statusLine = dummyResponse.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                firePlateRequestException(plate, new ProviderException("Bad status when doing the dummy page view request to get a session: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase(), plate));
                return;
            }
            captchaPostParams = makeFormParams(dummyResponse.getEntity().getContent(), "utf-8", getLoginUrl());

            captchaId = extractCaptchaId(debugHtml);
            //dummyResponse.getEntity().getContent().close();
            // System.out.println(debugHtml);

            // Check not requests exceeded
            if (debugHtml.contains("Sie haben die Anzahl")) { // zul&auml;ssiger Abfragen f&uuml;r heute erreicht."))
                throw new NumberOfRequestsExceededException();
            }
        } catch (IOException e) {
            firePlateRequestException(plate, new ProviderException("Could not do the dummy page view request to get a session.", e, plate));
            return;
        }

        if (cookieStore != null) {
            // Check number of request above MAX_REQUESTS

            // Find cookie ViaIndZH (or similar)
            String cookieName = "ViaInd" + cantonAbbr.toUpperCase();
            Cookie viaCookie = null;
            for (Cookie cookie : cookieStore.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    viaCookie = cookie;
                    break;
                }
            }
            if (viaCookie != null) {

                // The cookie has a value such as "Anzahl=0&Date=29.06.2013&de-CH=de-CH"
                // Number of request so far
                int requests = -1;
                Matcher matcher = Pattern.compile(".*Anzahl=(\\d+).*").matcher(viaCookie.getValue());
                if (matcher != null) {
                    matcher.find();
                    requests = Integer.valueOf(matcher.group(1));
                }
                if (requests == MAX_REQUESTS) {
                    cookieStore.clear();
                    cookieStore = null;
                    dummyPageViewRequest = null;
                    // Call itself to regenerate cookie store and do a dummy page request
                    makeRequestBeforeCaptchaEntered(plate, httpClient);
                    return;
                }
            }
        }

        fireCaptchaCodeRequested(plate, generateCaptchaImageUrl(), httpClient, new HttpHost("www.viacar.ch", 443,
                "https"),
                httpContext,
                "www.viacar.ch", this);
    }

    private String extractCaptchaId(String html) {
        Matcher matcher = Pattern.compile("JpegGenerate\\.aspx\\?ID=([A-Z0-9]*)\"").matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private List<NameValuePair> makeFormParams(InputStream content, String encoding, String pageUri) throws IOException {
        List<NameValuePair> list = new LinkedList<NameValuePair>();

        // Look for "<input>" tags
        Document doc = Jsoup.parse(content, encoding, pageUri);
        debugHtml = doc.outerHtml();
        // DO NOT CALL NORMALIZE! It will delete the style attribute we use to detect where is the captcha.
        Elements elements = doc.select("input");
        for (Element element : elements) {
            // Add element. For the Captcha one, we cheat and use a "THIS_IS_THE_CAPTCHA" value
            // Get this element by eliminating all others
            if (!"__VIEWSTATE".equals(element.attr("name")) && !"__EVENTVALIDATION".equals(element.attr("name")) &&
                    !"Button1".equals(element.attr("name")) && !"BtLogin".equals(element.attr("name"))) {
                list.add(new BasicNameValuePair(element.attr("name"), THIS_IS_THE_CAPTCHA));
            } else {
                if (element.hasAttr("value")) {
                    list.add(new BasicNameValuePair(element.attr("name"), element.attr("value")));
                }
            }
        }

        return list;
    }

    private String getLoginUrl() {
        return "https://www.viacar.ch/eindex/login.aspx?kanton=" + cantonAbbr;
    }

    public int MAX_REQUESTS = 3;

    private Set<Header> getHttpHeaders(String referer) {
        Set<Header> headers = new LinkedHashSet<Header>();
        headers.add(new BasicHeader("Origin", "https://www.viacar.ch"));
        //noinspection SpellCheckingInspection
        if (!"".equals(referer)) {
            headers.add(new BasicHeader("Referer", referer));
        }

        return headers;
    }

    @Override
    protected void doRequestAfterCaptchaEntered(String captchaCode, Plate plate, HttpClient httpClient, HttpContext httpContext) throws ProviderException {
        printProgress(0, 30);

        // Send captcha
        try {
            captchaRequest = makeCaptchaRequest(captchaCode);
        } catch (UnsupportedEncodingException e) {
            firePlateRequestException(plate, new ProviderException("Unsupported encoding for plate owner request parameters", e, plate));
            return;
        }

        HttpResponse captchaResponse;
        try {
            captchaResponse = httpClient.execute(captchaRequest, httpContext);
            printProgress(10, 30);
            if (captchaResponse.getStatusLine().getStatusCode() != 200) {
                firePlateRequestException(plate, new ProviderException("Got status " + captchaResponse.getStatusLine()
                        .getStatusCode()
                        + " from server when executing request to get the search page from captcha page. ", plate));
                return;
            }
        } catch (IOException e) {
            firePlateRequestException(plate, new ProviderException("Problem with captcha", new CaptchaException(e),
                    plate));
            return;
        }

        try {
            searchRequest = makeSearchRequest(captchaResponse, plate);

            /* Check not back on login page */
            for (NameValuePair searchFormParam : searchFormParams) {
                if ("BtLogin".equals(searchFormParam.getName())) {
                    firePlateRequestException(plate, new ProviderException("Problem with captcha",
                            new CaptchaException("Went back to captcha page"), plate));
                    return;
                }
            }
            // Check we were not discovered
            if (debugHtml.contains("Bitte rufen Sie den Autoindex")) {
                throw new ProviderException("Not well emulated. It tells us we're not on the autoindex website!", plate);
            }
            // Check this isn't the / (root) page https://www.viacar.ch/
            if (debugHtml.contains("Diese Seite oder dieser Service ist im Moment")) {
                throw new ProviderException("Unavailable message!", plate);
            }
            // Check no timeout
            if (debugHtml.contains("Die Zeit ist abgelaufen, bitte neu anmelden.")) {
                throw new ProviderException("Time out from provider", plate);
            }

            try {
                captchaResponse.getEntity().getContent().close();
            } catch (Throwable t) {
                // ignore
            }

            // Perform search
            HttpResponse searchResponse = httpClient.execute(searchRequest, httpContext);
            printProgress(20, 30);
            if (searchResponse.getStatusLine().getStatusCode() != 200 && searchResponse.getStatusLine().getStatusCode
                    () != 302) {
                firePlateRequestException(plate, new ProviderException("Got status " + searchResponse.getStatusLine()
                        .getStatusCode()
                        + " from server when executing request to get plate owner of plate " + plate, plate));
                return;
            }

            try {
                searchResponse.getEntity().getContent().close();
            } catch (Throwable t) {
                // ignore
            }

            // Execute request for the real result page
            resultRequest = makeResultRequest();
            HttpResponse resultResponse = httpClient.execute(resultRequest, httpContext);
            printProgress(30, 30);

            // Extract the plate owner from the HTML response
            plateOwner = htmlToPlateOwner(resultResponse, resultUri, plate);
            fireCaptchaCodeAccepted(plate);

            // Close connection and release resources
            // Disabled as a workaround for "java.lang.IllegalStateException: Connection manager has been shut down"
            try {
                // httpClient.getConnectionManager().shutdown();
            } catch (Throwable t) {
                // ignore
            }

            firePlateOwnerFound(plate, plateOwner);
        } catch (IOException e) {
            firePlateRequestException(plate, new ProviderException("Request exception", e, plate)
            );
            return;
        } catch (PlateOwnerNotFoundException e) {
            firePlateRequestException(plate, e);
        }
    }

    private void printProgress(int step, int numberOfSteps) {
        System.out.print("\r|");
        for (int i = 0; i < step; i++) {
            System.out.print("*");
        }
        for (int i = 0; i < numberOfSteps - step; i++) {
            System.out.print(" ");
        }
        System.out.println("|");
    }

    private PlateOwner htmlToPlateOwner(HttpResponse resultResponse, String baseUri, Plate plate) throws IOException, PlateOwnerNotFoundException {
        Document doc = Jsoup.parse(resultResponse.getEntity().getContent(),
                "utf-8", baseUri);
        debugHtml = doc.normalise().outerHtml();
        System.out.println("Result response");

        Elements elements = doc.select("table[bgcolor=whitesmoke]");

        /*
        Typical results
        Art: Motorrad Name: Schenkel-Albrecht Marcella Ursulina Strasse: Goldschmiedstrasse 10 Ort: 8102 Oberengstringen
        Art: Motorwagen Name: Rentra AG Strasse: Kronenweg 4 Ort: 8712 Stäfa
         */
        Pattern pattern = Pattern.compile("Art: (.*) Name: (.*) Strasse: (.*) Ort: (\\d+) (.*)");

        for (Element element : elements) {
            String text = element.text();
            System.out.println();
            System.out.println("Text: \n" + text);
            System.out.println();
            Matcher matcher = pattern.matcher(text);
            if (matcher != null) {
                while (matcher.find()) {
                    String art = matcher.group(1);
                    if (artMatches(art, plate.getType())) {
                        String ownerName = matcher.group(2);
                        String street = matcher.group(3);
                        String zipStr = matcher.group(4);
                        int zip = Integer.valueOf(zipStr);
                        String town = matcher.group(5);
                        return new PlateOwner(ownerName, street, "", zip, town);
                    }
                }
            }
        }

        throw new PlateOwnerNotFoundException("Not found or page could not be parsed.", plate);
    }

    private boolean artMatches(String art, PlateType type) {
        if (art == null) return false;
        return plateTypeMapping.containsKey(art) && plateTypeMapping.get(art).equals(type);
    }

    private static Map<String, PlateType> plateTypeMapping = new HashMap<String, PlateType>();

    static {
        // TODO Add "Anhänger" (wie auf Viacar gesehen)
        plateTypeMapping.put("Motorrad", PlateType.MOTORCYCLE);
        plateTypeMapping.put("Motorwagen", PlateType.AUTOMOBILE);
        plateTypeMapping.put("Landw. Motorfahrzeug", PlateType.AGRICULTURAL);
    }

    private HttpGet makeResultRequest() {
        resultRequest = new HttpGet(resultUri);
        for (Header header : getHttpHeaders("https://www.viacar.ch/eindex/Search.aspx?kanton=" + cantonAbbr)) {
            resultRequest.setHeader(header);
        }
        return resultRequest;
    }

    private HttpPost makeSearchRequest(HttpResponse captchaResponse, Plate plate) throws IOException {
        searchFormParams = makeFormParams(captchaResponse.getEntity().getContent(),
                "utf-8", getLoginUrl());

        try {
//            captchaResponse.getEntity().getContent().close();
        } catch (Throwable t) { /* ignore */ }

        // Remove param with name "TextBoxKontrollschild" if it exists
        if (searchFormParams.isEmpty()) {
            throw new RuntimeException("Bad page (expected the search page).");
        }

        for (NameValuePair formParam : new LinkedList<NameValuePair>(searchFormParams)) {
            if ("TextBoxKontrollschild".equals(formParam.getName())) {
                searchFormParams.remove(formParam);
            }
        }
        searchFormParams.add(new BasicNameValuePair("TextBoxKontrollschild", Integer.toString(plate.getNumber())));

        if (searchRequest == null) {
            searchRequest = new HttpPost("https://www.viacar.ch/eindex/Search.aspx?kanton=" + cantonAbbr);
            for (Header header : getHttpHeaders(getLoginUrl())) {
                searchRequest.setHeader(header);
            }
        }
        searchRequest.setEntity(new UrlEncodedFormEntity(searchFormParams));
        return searchRequest;
    }

    // Make the Captcha page request used to do the search on the response page
    private HttpPost makeCaptchaRequest(String captchaCode) throws UnsupportedEncodingException {
        if (captchaRequest == null) {
            captchaRequest = new HttpPost(getLoginUrl());
            for (Header header : getHttpHeaders(getLoginUrl())) {
                captchaRequest.setHeader(header);
            }
        }

        // Modify captcha in params
        for (NameValuePair postParam : new LinkedList<NameValuePair>(captchaPostParams)) {
            if (THIS_IS_THE_CAPTCHA.equals(postParam.getValue())) {
                captchaPostParams.remove(postParam);
                captchaPostParams.add(new BasicNameValuePair(postParam.getName(), captchaCode));
            } else if ("Button1".equals(postParam.getName())) {
                captchaPostParams.remove(postParam);
            }
        }

        captchaRequest.setEntity(new UrlEncodedFormEntity(captchaPostParams));

        return captchaRequest;
    }

    @Override
    public String generateCaptchaImageUrl() {
        return "https://www.viacar.ch/eindex/JpegGenerate.aspx?ID=" + captchaId;
    }
}
