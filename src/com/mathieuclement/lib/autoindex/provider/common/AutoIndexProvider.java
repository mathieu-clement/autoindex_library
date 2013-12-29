package com.mathieuclement.lib.autoindex.provider.common;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.exception.*;

/**
 * Provider of data about owners of number plates
 */
public interface AutoIndexProvider {
    /**
     * Returns the owner of a number plate.
     *
     * @param plate     Plate to look for
     * @param requestId Id identifying this request. Is useful for the {@link #cancel(int)} method.
     * @return the owner of a number plate.
     * @throws ProviderException           in case a problem happened with the provider, like a connection issue, an error returned by the server, etc.
     * @throws PlateOwnerNotFoundException if no owner could be found
     * @throws PlateOwnerHiddenException   if the owner is hidden (probably because he decided to)
     * @throws UnsupportedPlateException   if the given plate is not supported by this provider. See {@link #isPlateTypeSupported(PlateType)}
     */
    PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException, PlateOwnerNotFoundException,
            PlateOwnerHiddenException, UnsupportedPlateException, CaptchaException, RequestCancelledException;

    /**
     * Returns true if the plate type is supported
     *
     * @param plateType Plate type
     * @return true if the plate type is supported
     */
    boolean isPlateTypeSupported(PlateType plateType);

    /**
     * Stop any action (at appropriate times though) for the specified request Id.
     *
     * @param requestId request identifier, passed to {@link #getPlateOwner(com.mathieuclement.lib.autoindex.plate.Plate, int)}
     */
    void cancel(int requestId);

    /**
     * Returns true if this request was cancelled any time before.
     * @param requestId request id
     * @return true if this request was cancelled any time before.
     */
    boolean isCancelled(int requestId);

}
