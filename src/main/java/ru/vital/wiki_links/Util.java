package ru.vital.wiki_links;

import java.io.UnsupportedEncodingException;
import java.util.HexFormat;

public class Util {

    public static String toHex(String str) throws UnsupportedEncodingException {
        HexFormat commaFormat = HexFormat.ofDelimiter("").withPrefix("%");
        byte[] bytes = str.getBytes("UTF-8");
        String strHex = commaFormat.formatHex(bytes);
        return strHex.replaceAll("%20", "_").toUpperCase();
    }
}
