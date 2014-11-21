package com.mathieuclement.lib.autoindex.provider.common.captcha.event;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.provider.exception.PlateRequestException;

public interface PlateRequestListener {
    void onPlateOwnerFound(Plate plate, PlateOwner plateOwner);

    void onPlateRequestException(Plate plate, PlateRequestException exception);
}
