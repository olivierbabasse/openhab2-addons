package org.openhab.binding.miinternetspeaker.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * Created by Ondrej Pecta on 18.07.2017.
 */
public class Utils {
    public static String readResponse(HttpURLConnection connection) throws Exception {
        InputStream stream = connection.getInputStream();
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        return line;
    }

    public static boolean isOKPacket(String sentence) {
        return sentence.startsWith("HTTP/1.1 200 OK");
    }
}
