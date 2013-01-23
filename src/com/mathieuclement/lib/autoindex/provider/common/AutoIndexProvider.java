package com.mathieuclement.lib.autoindex.provider.common;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.exception.UnsupportedPlateException;

/**
 * Provider of data about owners of number plates
 */
public interface AutoIndexProvider {
    /**
     * Returns the owner of a number plate.
     *
     * @param plate Plate to look for
     * @return the owner of a number plate.
     * @throws ProviderException           in case a problem happened with the provider, like a connection issue, an error returned by the server, etc.
     * @throws PlateOwnerNotFoundException if no owner could be found
     * @throws PlateOwnerHiddenException   if the owner is hidden (probably because he decided to)
     * @throws UnsupportedPlateException   if the given plate is not supported by this provider. See {@link #isPlateTypeSupported(PlateType)}
     */
    PlateOwner getPlateOwner(Plate plate) throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, UnsupportedPlateException;

    /**
     * Returns true if the plate type is supported
     *
     * @param plateType Plate type
     * @return true if the plate type is supported
     */
    public abstract boolean isPlateTypeSupported(PlateType plateType);
}
