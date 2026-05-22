package me.yassigame.sable_beyond.utils;

import java.util.regex.Pattern;

public class HttpUtil {
    public static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public static boolean isHttpUrl(final String link) {
        return link.startsWith("https://") || link.startsWith("http://");
    }
}
