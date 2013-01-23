/*
 * Filename: Canton.java
 *
 * Copyright 1987-2012 by Informatique-MTF, SA, Route du Bleuet 1, CH-1762
 * Givisiez/Fribourg, Switzerland All rights reserved.
 *
 *========================================================================
 */
package com.mathieuclement.lib.autoindex.canton;

import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;

public class Canton {
    private String abbreviation;
    private boolean isAutoIndexSupported;
    private AutoIndexProvider syncAutoIndexProvider;
    private AsyncAutoIndexProvider asyncAutoIndexProvider;

    public Canton() {
    }

    public Canton(String abbreviation, boolean autoIndexSupported, AsyncAutoIndexProvider asyncAutoIndexProvider) {
        this.abbreviation = abbreviation;
        isAutoIndexSupported = autoIndexSupported;
        this.asyncAutoIndexProvider = asyncAutoIndexProvider;
    }

    public Canton(String abbreviation, boolean autoIndexSupported, AutoIndexProvider autoIndexProvider) {
        this.abbreviation = abbreviation;
        isAutoIndexSupported = autoIndexSupported;
        this.syncAutoIndexProvider = autoIndexProvider;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public boolean isAutoIndexSupported() {
        return isAutoIndexSupported;
    }

    public void setAutoIndexSupported(boolean autoIndexSupported) {
        isAutoIndexSupported = autoIndexSupported;
    }

    public AutoIndexProvider getSyncAutoIndexProvider() {
        return syncAutoIndexProvider;
    }

    public void setSyncAutoIndexProvider(AutoIndexProvider autoIndexProvider) {
        this.syncAutoIndexProvider = autoIndexProvider;
    }

    public AsyncAutoIndexProvider getAsyncAutoIndexProvider() {
        return asyncAutoIndexProvider;
    }

    public void setAsyncAutoIndexProvider(AsyncAutoIndexProvider asyncAutoIndexProvider) {
        this.asyncAutoIndexProvider = asyncAutoIndexProvider;
    }

    public String toString() {
        return this.abbreviation;
    }

    @Override
    public int hashCode() {
        return 31 * (abbreviation == null ? 0 : abbreviation.hashCode()) + 2 * (syncAutoIndexProvider == null ? 0 : syncAutoIndexProvider.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Canton) {
            Canton otherCanton = (Canton) obj;
            return (otherCanton.abbreviation != null && this.abbreviation != null && otherCanton.abbreviation.equals(this.abbreviation))
                    && otherCanton.isAutoIndexSupported == this.isAutoIndexSupported
                    && (this.syncAutoIndexProvider != null && otherCanton.syncAutoIndexProvider != null && otherCanton.syncAutoIndexProvider.equals(this.syncAutoIndexProvider));
        }
        return false;
    }
}