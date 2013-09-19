package com.mathieuclement.lib.autoindex.provider.common.captcha.event;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.exception.PlateRequestException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

import java.util.LinkedList;
import java.util.List;

public abstract class AsyncAutoIndexProvider {

    private List<CaptchaListener> captchaListeners = new LinkedList<CaptchaListener>();
    private List<PlateRequestListener> plateRequestListeners = new LinkedList<PlateRequestListener>();

    public abstract boolean isPlateTypeSupported(PlateType plateType);

    /**
     * Do the same as {@link AutoIndexProvider#getPlateOwner(com.mathieuclement.lib.autoindex.plate.Plate)}, except that this method returns immediately
     * and a event is thrown to captchaListeners of this class. See {@link #addListener(CaptchaListener)}
     *
     * @param plate Requested plate
     */
    public final void requestPlateOwner(Plate plate) throws ProviderException {
        makeRequestBeforeCaptchaEntered(plate);
    }

    /**
     * Do the same as {@link #requestPlateOwner(com.mathieuclement.lib.autoindex.plate.Plate)}. You can pass your own http client.
     *
     * @param plate      Requested plate
     * @param httpClient your own custom version of HttpClient
     */
    public final void requestPlateOwner(Plate plate, HttpClient httpClient) throws ProviderException {
        makeRequestBeforeCaptchaEntered(plate, httpClient);
    }

    public final void addListener(CaptchaListener listener) {
        captchaListeners.add(listener);
    }

    public final void removeListener(CaptchaListener listener) {
        captchaListeners.remove(listener);
    }

    public final void addListener(PlateRequestListener listener) {
        plateRequestListeners.add(listener);
    }

    public final void removeListener(PlateRequestListener listener) {
        plateRequestListeners.remove(listener);
    }

    public final void pushCaptchaCode(String captchaCode, Plate plate, HttpClient httpClient, HttpContext httpContext) throws ProviderException {
        doRequestAfterCaptchaEntered(captchaCode, plate, httpClient, httpContext);
    }

    protected final void firePlateOwnerFound(Plate plate, PlateOwner plateOwner) {
        for (PlateRequestListener plateRequestListener : plateRequestListeners) {
            plateRequestListener.onPlateOwnerFound(plate, plateOwner);
        }
    }

    protected final void firePlateRequestException(Plate plate, PlateRequestException exception) {
        for (PlateRequestListener plateRequestListener : plateRequestListeners) {
            plateRequestListener.onPlateRequestException(plate, exception);
        }
    }

    protected final void fireCaptchaCodeRequested(Plate plate, String captchaImageUrl,
                                                  HttpClient httpClient, HttpHost httpHost, HttpContext httpContext, String httpHostHeaderValue,
                                                  AsyncAutoIndexProvider provider) throws ProviderException {
        for (CaptchaListener listener : captchaListeners) {
            listener.onCaptchaCodeRequested(plate, captchaImageUrl, httpClient, httpHost, httpContext, httpHostHeaderValue, provider);
        }
    }

    protected final void fireCaptchaCodeAccepted(Plate plate) {
        for (CaptchaListener captchaListener : captchaListeners) {
            captchaListener.onCaptchaCodeAccepted(plate);
        }
    }

    protected abstract void makeRequestBeforeCaptchaEntered(Plate plate) throws ProviderException;

    protected abstract void makeRequestBeforeCaptchaEntered(Plate plate, HttpClient httpClient) throws ProviderException;

    protected abstract void doRequestAfterCaptchaEntered(String captchaCode, Plate plate, HttpClient httpClient, HttpContext httpContext) throws ProviderException;

    public abstract String generateCaptchaImageUrl();

}
