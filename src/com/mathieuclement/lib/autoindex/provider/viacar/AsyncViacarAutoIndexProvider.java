package com.mathieuclement.lib.autoindex.provider.viacar;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
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
    private final String resultUri = "/eindex/Result.aspx?Var=1";
    private String cantonAbbr;
    private static Set<PlateType> supportedPlateTypes = new LinkedHashSet<PlateType>();

    static {
        supportedPlateTypes.add(PlateType.AUTOMOBILE);
        supportedPlateTypes.add(PlateType.MOTORCYCLE);
        supportedPlateTypes.add(PlateType.AGRICULTURAL);
        supportedPlateTypes.add(PlateType.BOAT);
    }

    private DefaultHttpClient httpClient;
    private HttpContext httpContext;
    private HttpRequest dummyPageViewRequest;
    private HttpHost httpHost = new HttpHost("www.viacar.ch", 443, "https"); // Request has to be done with the
    // correct name matching the Server certificate!
    private String captchaId = "";
    private CookieStore cookieStore;
    private BasicHttpEntityEnclosingRequest captchaRequest;
    private List<NameValuePair> captchaPostParams;
    private List<NameValuePair> searchFormParams;
    private BasicHttpEntityEnclosingRequest searchRequest;
    private PlateOwner plateOwner;
    private BasicHttpEntityEnclosingRequest resultRequest;
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
    protected void makeRequestBeforeCaptchaEntered(Plate plate) {
        if (httpClient == null) {
            try {
                // httpClient = new DecompressingHttpClient(new DefaultHttpClient()); // for gzip
                httpClient = new DefaultHttpClient();
                HttpParams httpParams = httpClient.getParams();
                httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                httpParams.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "https://www.viacar.ch");
                httpParams.setParameter("http.protocol.single-cookie-header", true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        makeRequestBeforeCaptchaEntered(plate, httpClient);
    }

    @Override
    protected void makeRequestBeforeCaptchaEntered(Plate plate, HttpClient httpClient) {
        // HTTP handling
        if (httpContext == null) {
            httpContext = new BasicHttpContext();
        }

        // Load page a first time and get that session cookie! (+ hidden POST fields)
        dummyPageViewRequest = new BasicHttpEntityEnclosingRequest("GET", getShortLoginUrl(), HttpVersion.HTTP_1_1);
        if (cookieStore == null) {
            cookieStore = ((AbstractHttpClient)httpClient).getCookieStore();
        }
        //dummyPageViewRequest.setHeader(getHostHeader());
        for (Header header : getHttpHeaders("")) {
            dummyPageViewRequest.setHeader(header);
        }
        try {
            HttpResponse dummyResponse = httpClient.execute(httpHost, dummyPageViewRequest, httpContext);
            StatusLine statusLine = dummyResponse.getStatusLine();
            if (statusLine.getStatusCode() != 200) {
                firePlateRequestException(plate, new ProviderException("Bad status when doing the dummy page view request to get a session: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase(), plate));
                return;
            }
            captchaPostParams = makeFormParams(dummyResponse.getEntity().getContent(), "utf-8", getLoginUrl());
            captchaId = extractCaptchaId(debugHtml);
            EntityUtils.consume(dummyResponse.getEntity());
            //printDebugHtml();
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
            if (viaCookie == null) {
                throw new RuntimeException("Cookie " + cookieName + " was not found.");
            }

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

        fireCaptchaCodeRequested(plate, generateCaptchaImageUrl(), httpClient, httpHost, httpContext,
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
        printDebugHtml();
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

    private String getShortLoginUrl() {
        return "/eindex/login.aspx?kanton=" + cantonAbbr;
        //return getLoginUrl();
    }

    private static final int MAX_REQUESTS = 5;

    private Set<Header> getHttpHeaders(String referer) {
        Set<Header> headers = new LinkedHashSet<Header>();

        //headers.add(new BasicHeader("User-Agent", "Swiss-AutoIndex/0.1"));

//        headers.add(new BasicHeader("Host", "www.viacar.ch"));
//        headers.add(new BasicHeader("User-Agent",
//                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:21.0) Gecko/20100101 Firefox/21.0"));
//        headers.add(new BasicHeader("Cache-Control", "max-age=0"));
//        headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
//        //headers.add(new BasicHeader("Content-Length", "1268"));
//        headers.add(new BasicHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3"));
//        headers.add(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
//        headers.add(new BasicHeader("Accept-Language", "en-US,en;q=0.5"));
////        headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate,dsch"));
//        headers.add(new BasicHeader("Accept-Encoding", "deflate"));
//        headers.add(new BasicHeader("Connection", "keep-alive"));
//        headers.add(new BasicHeader("Origin", "https://www.viacar.ch"));
//        //noinspection SpellCheckingInspection
//        if (!"".equals(referer)) {
//            headers.add(new BasicHeader("Referer", referer));
//        }

        return headers;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    protected void doRequestAfterCaptchaEntered(String captchaCode, Plate plate, HttpClient httpClient, HttpContext httpContext) {
        // Send captcha
        try {
            captchaRequest = makeCaptchaRequest(captchaCode);
        } catch (UnsupportedEncodingException e) {
            firePlateRequestException(plate, new ProviderException("Unsupported encoding for plate owner request parameters", e, plate));
            return;
        }

//        for (Cookie cookie : cookieStore.getCookies()) {
//            captchaRequest.addHeader("Cookie", cookie.getName() + "=" + cookie.getValue());
//        }

        HttpResponse captchaResponse;
        try {
            captchaResponse = httpClient.execute(httpHost, captchaRequest, httpContext);
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
            BasicHttpEntityEnclosingRequest searchRequest = makeSearchRequest(captchaResponse, plate);

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
                throw new RuntimeException("Not well emulated. It tells us we're not on the autoindex website!");
            }
            // Check this isn't the / (root) page https://www.viacar.ch/
            if (debugHtml.contains("Diese Seite oder dieser Service ist im Moment")) {
                printDebugHtml();
                throw new RuntimeException("Unavailable message!");
            }

            EntityUtils.consume(captchaResponse.getEntity());

            // Perform search
            HttpResponse searchResponse = httpClient.execute(httpHost, searchRequest, httpContext);
            if (searchResponse.getStatusLine().getStatusCode() != 200 && searchResponse.getStatusLine().getStatusCode
                    () != 302) {
                firePlateRequestException(plate, new ProviderException("Got status " + searchResponse.getStatusLine()
                        .getStatusCode()
                        + " from server when executing request to get plate owner of plate " + plate, plate));
                return;
            }
//            searchResponse.getEntity().getContent().close();
//            captchaResponse.getEntity().getContent().close();

            EntityUtils.consume(searchResponse.getEntity());

            // Execute request for the real result page
            BasicHttpEntityEnclosingRequest resultRequest = makeResultRequest();
            HttpResponse resultResponse = httpClient.execute(httpHost, resultRequest, httpContext);

            // Extract the plate owner from the HTML response
            plateOwner = htmlToPlateOwner(resultResponse, resultUri, plate);
            fireCaptchaCodeAccepted(plate);

            // Close connection and release resources
            // Disabled as a workaround for "java.lang.IllegalStateException: Connection manager has been shut down"
            //httpClient.getConnectionManager().shutdown();

            firePlateOwnerFound(plate, plateOwner);
        } catch (IOException e) {
            firePlateRequestException(plate, new ProviderException("Request exception", e, plate)
            );
            return;
        }
    }

    private void printDebugHtml() {
        System.out.println(debugHtml);
        System.out.println("----------------------------------------------------------------------------------------");
    }

    private PlateOwner htmlToPlateOwner(HttpResponse resultResponse, String baseUri, Plate plate) throws IOException {
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

        throw new ParseException("Could not parse result page.");
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
        plateTypeMapping.put("Schiff", PlateType.BOAT);
        plateTypeMapping.put("Landw. Motorfahrzeug", PlateType.AGRICULTURAL);
    }

    private BasicHttpEntityEnclosingRequest makeResultRequest() {
        resultRequest = new BasicHttpEntityEnclosingRequest("GET",
                resultUri, HttpVersion.HTTP_1_1);
        for (Header header : getHttpHeaders("https://www.viacar.ch/eindex/Search.aspx?kanton=" + cantonAbbr)) {
            resultRequest.setHeader(header);
        }
        return resultRequest;
    }

    private BasicHttpEntityEnclosingRequest makeSearchRequest(HttpResponse captchaResponse, Plate plate) throws IOException {
        searchFormParams = makeFormParams(captchaResponse.getEntity().getContent(),
                "utf-8", getLoginUrl());

        try {
//            captchaResponse.getEntity().getContent().close();
        } catch (Throwable t) { /* ignore */ }

        // Remove param with name "TextBoxKontrollschild" if it exists
        if (searchFormParams.isEmpty()) {
            printDebugHtml();
            throw new RuntimeException("Bad page (expected the search page).");
        }

        for (NameValuePair formParam : new LinkedList<NameValuePair>(searchFormParams)) {
            if ("TextBoxKontrollschild".equals(formParam.getName())) {
                searchFormParams.remove(formParam);
            }
        }
        searchFormParams.add(new BasicNameValuePair("TextBoxKontrollschild", Integer.toString(plate.getNumber())));

        if (searchRequest == null) {
            searchRequest = new BasicHttpEntityEnclosingRequest("POST", "/eindex/Search.aspx?kanton=" + cantonAbbr,
                    HttpVersion.HTTP_1_1);
            for (Header header : getHttpHeaders(getLoginUrl())) {
                searchRequest.setHeader(header);
            }
        }
        searchRequest.setEntity(new UrlEncodedFormEntity(searchFormParams));
        return searchRequest;
    }

    // Make the Captcha page request used to do the search on the response page
    private BasicHttpEntityEnclosingRequest makeCaptchaRequest(String captchaCode) throws UnsupportedEncodingException {
        if (captchaRequest == null) {
            captchaRequest = new BasicHttpEntityEnclosingRequest("POST", getShortLoginUrl(), HttpVersion.HTTP_1_1);
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