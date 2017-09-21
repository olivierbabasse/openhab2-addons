/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jablotron.internal;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {
    public static String getSessionCookie(HttpsURLConnection connection) {

        String headerName;
        for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                if (connection.getHeaderField(i).startsWith("PHPSESSID")) {
                    int semicolon = connection.getHeaderField(i).indexOf(";");
                    return connection.getHeaderField(i).substring(0, semicolon);
                }
            }
        }
        return "";
    }

    public static String readResponse(HttpsURLConnection connection) throws Exception {
        InputStream stream = connection.getInputStream();
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        //logger.debug(line);
        return line;
    }

    public static String getBrowserTimestamp() {
        return "_=" + System.currentTimeMillis();
    }
}
