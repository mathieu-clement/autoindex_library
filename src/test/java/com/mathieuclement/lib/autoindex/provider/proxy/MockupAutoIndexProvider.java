package com.mathieuclement.lib.autoindex.provider.proxy;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.lib.autoindex.provider.exception.*;

/**
* Created by mathieu on 12/29/14.
*/
class MockupAutoIndexProvider extends CaptchaAutoIndexProvider {

    protected MockupAutoIndexProvider(CaptchaHandler captchaHandler) {
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
    public PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException, PlateOwnerNotFoundException, PlateOwnerHiddenException, UnsupportedPlateException, CaptchaException, RequestCancelledException {
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
