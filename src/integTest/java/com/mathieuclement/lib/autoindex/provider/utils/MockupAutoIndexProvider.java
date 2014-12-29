package com.mathieuclement.lib.autoindex.provider.utils;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.exception.RequestCancelledException;
import com.mathieuclement.lib.autoindex.provider.exception.UnsupportedPlateException;

/**
* Mockup for auto index provider.
*/
public class MockupAutoIndexProvider extends CaptchaAutoIndexProvider {

    public MockupAutoIndexProvider(CaptchaHandler captchaHandler) {
        super(captchaHandler);
    }

    @Override
    public String regenerateCaptchaImageUrl() {
        return "";
    }

    @Override
    public boolean isIndeterminateProgress() {
        return false;
    }

    @Override
    public PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException,
            PlateOwnerNotFoundException, PlateOwnerHiddenException,
            UnsupportedPlateException, CaptchaException, RequestCancelledException {
        throw new AssertionError("Proxy mustn't have called the real provider");
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return false;
    }

    @Override
    public void cancel(int requestId) {

    }

    @Override
    public boolean isCancelled(int requestId) {
        return false;
    }
}
