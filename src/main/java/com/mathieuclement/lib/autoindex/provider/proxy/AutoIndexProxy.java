package com.mathieuclement.lib.autoindex.provider.proxy;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.exception.*;
import com.mathieuclement.lib.autoindex.provider.utils.ResponseUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Looks like an AutoIndexProvider (in fact it is one)
 * but it will in fact query the cache before trying to use
 * the real provider.
 */
public class AutoIndexProxy implements AutoIndexProvider {
    private AutoIndexProvider realProvider;

    private static final String CACHE_URL = "https://clement.gotdns.ch/autoindex/";

    public AutoIndexProxy(AutoIndexProvider realProvider) {
        this.realProvider = realProvider;
    }

    @Override
    public PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, UnsupportedPlateException, CaptchaException, RequestCancelledException {

        try {
            URIBuilder uriBuilder = new URIBuilder(CACHE_URL);
            System.err.println("INVALID CREDENTIALS from AutoIndexProxy");
            uriBuilder.addParameter("email", "toto@truc.ch");
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
                    return realProvider.getPlateOwner(plate, requestId);

                case 400:
                    throw new ProviderException(ResponseUtils.toString(getOwnerResponse), plate);

                case 403:
                    throw new PlateOwnerHiddenException(ResponseUtils.toString(getOwnerResponse), plate);

                case 404:
                    throw new PlateOwnerNotFoundException(ResponseUtils.toString(getOwnerResponse), plate);

            }
        } catch (URISyntaxException | IOException | ParserConfigurationException | SAXException e) {
            throw new ProviderException(e.getMessage(), e, plate);
        }

        return null;
    }

    private PlateOwner xmlToPlateOwner(InputStream xmlStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(xmlStream);
        document.getDocumentElement().normalize();

        PlateOwner plateOwner = new PlateOwner();
        plateOwner.setName(document.getElementsByTagName("name").item(0).getTextContent());
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

    }

    @Override
    public boolean isCancelled(int requestId) {
        return false;
    }
}
