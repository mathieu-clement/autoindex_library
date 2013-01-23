package com.mathieuclement.lib.autoindex.provider.common;

/**
 * TODO Use that somewhere...
 */
public interface DailyLimitListener {
    void dailyLimitExceeded();

    void dailyLimitUpdated(int remainingRequests, int total);
}
