package com.mathieuclement.lib.autoindex.provider.common;

/**
 * A listener for the progress of a task.
 */
public interface ProgressListener {
    void onProgress(int current, int maximum);
}
