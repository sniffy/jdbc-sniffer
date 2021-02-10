package io.sniffy.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class StringUtil {

    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

    public static String escapeJsonString(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         c;
        int          i;
        int          len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String       t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (i > 0 && string.charAt(i - 1) == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String[] splitByLastSlashAndDecode(String connectionString) throws UnsupportedEncodingException {
        int lastIndexOf = connectionString.lastIndexOf("/");
        if (lastIndexOf >= 0) {
            return new String[] {
                    doubleURLDecode(connectionString.substring(0, lastIndexOf)),
                    lastIndexOf + 1 >= connectionString.length() ? "" : doubleURLDecode(connectionString.substring(lastIndexOf + 1))
            };
        } else {
            return new String[] {doubleURLDecode(connectionString)};
        }
    }

    public static String doubleURLDecode(String string) throws UnsupportedEncodingException {
        return URLDecoder.decode(URLDecoder.decode(string, "UTF-8"), "UTF-8");
    }

}
