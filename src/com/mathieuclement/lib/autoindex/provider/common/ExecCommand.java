package com.mathieuclement.lib.autoindex.provider.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Mathieu Clément
 * @since 29.12.2013
 */
public class ExecCommand {
    public static String exec(String cmd) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // read the output from the command
        String s = stdInput.readLine();
        stdInput.close();
        return s;
    }
}
