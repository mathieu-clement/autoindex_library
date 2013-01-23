package com.mathieuclement.lib.autoindex.provider.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.IOException;

public class ResponseUtils {
    public static String toString(HttpResponse httpResponse) throws IOException {
        // Use default encoding if none is returned
        Header contentType = httpResponse.getEntity().getContentType();
        String value = contentType.getValue();
        if (value != null && !value.equals("")) {
            String[] split = value.split("text/html;charset=");
            if (split.length > 1) {
                String encodingName = split[1];
                return IOUtils.toString(httpResponse.getEntity().getContent(), encodingName);
            }
        }

        return IOUtils.toString(httpResponse.getEntity().getContent());
    }

    /**
     * Replace 2 or more spaces with only one + strip first and last space if any
     *
     * @param str a String possibly containing useless spaces
     * @return the string with multiple spaces replaced by only one, and with beginning and ending space(s) removed.
     */
    public static String removeUselessSpaces(String str) {
        // Recipe contains other interesting things (remove spaces only in the middle for instance)
        // http://stackoverflow.com/questions/2932392/java-how-to-replace-2-or-more-spaces-with-single-space-in-string-and-delete-lead
        return str.trim().replaceAll(" +", " ");
    }
}
