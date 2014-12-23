package com.mathieuclement.lib.autoindex.provider.proxy;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.ProgressListener;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.*;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import com.mathieuclement.lib.autoindex.provider.utils.URIBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

/**
 * Looks like an AutoIndexProvider (in fact it is one)
 * but it will in fact query the cache before trying to use
 * the real provider.
 */
public class CaptchaAutoIndexProxy extends CaptchaAutoIndexProvider {
    private CaptchaAutoIndexProvider realProvider;
    private String email;

    private static final String CACHE_URL = "https://clement.gotdns.ch/autoindex/";

    public CaptchaAutoIndexProxy(CaptchaAutoIndexProvider realProvider, String email, CaptchaHandler captchaHandler) {
        super(captchaHandler);
        this.realProvider = realProvider;
        this.email = email;
    }

    @Override
    public PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, UnsupportedPlateException, CaptchaException, RequestCancelledException {

        try {
            URIBuilder uriBuilder = new URIBuilder(CACHE_URL);
            uriBuilder.addParameter("email", this.email);
            uriBuilder.addParameter("locale", Locale.getDefault().toString());
            uriBuilder.addParameter("method", "get_owner");
            uriBuilder.addParameter("canton", plate.getCanton().getAbbreviation());
            uriBuilder.addParameter("number", String.valueOf(plate.getNumber()));
            uriBuilder.addParameter("plate_type", plate.getType().getName().toUpperCase());
            URI getOwnerUri = uriBuilder.build();

            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse getOwnerResponse = httpClient.execute(new HttpGet(getOwnerUri));
            int getOwnerStatusCode = getOwnerResponse.getStatusLine().getStatusCode();

            switch (getOwnerStatusCode) {
                case 200:
                    return xmlToPlateOwner(getOwnerResponse.getEntity().getContent());

                case 305:
                    return makeRealSearch(plate, requestId);

                case 400:
                    throw new ProviderException(ResponseUtils.toString(getOwnerResponse), plate);

                case 403:
                    throw new PlateOwnerHiddenException(ResponseUtils.toString(getOwnerResponse), plate);

                case 404:
                    throw new PlateOwnerNotFoundException(ResponseUtils.toString(getOwnerResponse), plate);

                default:
                    throw new ProviderException(ResponseUtils.toString(getOwnerResponse), plate);

            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ProviderException(e.getMessage(), e, plate);
        }
    }

    private PlateOwner makeRealSearch(Plate plate, int requestId) throws CaptchaException, UnsupportedPlateException, ProviderException, RequestCancelledException, PlateOwnerNotFoundException, PlateOwnerHiddenException {
        try {
            PlateOwner owner = realProvider.getPlateOwner(plate, requestId);
            updateCache(plate, owner);
            return owner;
        } catch (PlateOwnerNotFoundException e) {
            updateCache(plate, "UNKNOWN");
            throw e;
        } catch (PlateOwnerHiddenException e) {
            updateCache(plate, "HIDDEN");
            throw e;
        }
    }

    private void updateCache(Plate plate, PlateOwner owner) throws ProviderException {
        doUpdateCache(plate, owner, "FOUND");
    }

    private void updateCache(Plate plate, String status) throws ProviderException {
        doUpdateCache(plate, null, status);
    }

    private void doUpdateCache(Plate plate, PlateOwner plateOwner, String status) throws ProviderException {
        try {
            URIBuilder uriBuilder = new URIBuilder(CACHE_URL);
            uriBuilder.addParameter("method", "add_or_update");
            uriBuilder.addParameter("canton", plate.getCanton().getAbbreviation());
            uriBuilder.addParameter("number", String.valueOf(plate.getNumber()));
            uriBuilder.addParameter("plate_type", plate.getType().getName().toUpperCase());
            if (plateOwner != null) {
                uriBuilder.addParameter("name", plateOwner.getName());
                uriBuilder.addParameter("address", plateOwner.getAddress());
                uriBuilder.addParameter("addressComplement", plateOwner.getAddressComplement());
                uriBuilder.addParameter("zip", String.valueOf(plateOwner.getZip()));
                uriBuilder.addParameter("city", plateOwner.getTown());
            }
            uriBuilder.addParameter("status", status);
            URI getOwnerUri = uriBuilder.build();

            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse getOwnerResponse = httpClient.execute(new HttpGet(getOwnerUri));
            int getOwnerStatusCode = getOwnerResponse.getStatusLine().getStatusCode();

            switch (getOwnerStatusCode) {
                case 400:
                    LoggerFactory.getLogger(getClass()).warn("Could not update cache");
                    break;

                case 200:
                case 403:
                    return;
            }
        } catch (IOException e) {
            throw new ProviderException(e.getMessage(), e, plate);
        }
    }

    private PlateOwner xmlToPlateOwner(InputStream xmlStream) throws ParserConfigurationException, IOException, SAXException, ProviderException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(xmlStream);
        document.getDocumentElement().normalize();

        PlateOwner plateOwner = new PlateOwner();
        String name = document.getElementsByTagName("name").item(0).getTextContent();
        if(name == null || name.length() < 3) {
            throw new ProviderException("Something went wrong with the cache");
        }
        plateOwner.setName(name);
        plateOwner.setAddress(document.getElementsByTagName("address").item(0).getTextContent());
        plateOwner.setAddressComplement(document.getElementsByTagName("addressComplement").item(0).getTextContent());
        plateOwner.setZip(Integer.parseInt(document.getElementsByTagName("zip").item(0).getTextContent()));
        plateOwner.setTown(document.getElementsByTagName("city").item(0).getTextContent());
        return plateOwner;
    }


    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return realProvider.isPlateTypeSupported(plateType);
    }

    @Override
    public void cancel(int requestId) {
        realProvider.cancel(requestId);
    }

    @Override
    public boolean isCancelled(int requestId) {
        return realProvider.isCancelled(requestId);
    }

    @Override
    public String regenerateCaptchaImageUrl() {
        return realProvider.regenerateCaptchaImageUrl();
    }

    @Override
    public boolean isIndeterminateProgress() {
        return realProvider.isIndeterminateProgress();
    }

    @Override
    public void addListener(ProgressListener listener) {
        realProvider.addListener(listener);
    }

    @Override
    public void removeListener(ProgressListener listener) {
        realProvider.removeListener(listener);
    }

    @Override
    public void fireProgress(int current, int maximum) {
        realProvider.fireProgress(current, maximum);
    }
}
