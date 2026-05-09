package me.yassigame.sable_beyond;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SableBeyond {
    public static final String MOD_ID = "sable_beyond";
    public static final Logger LOGGER = LoggerFactory.getLogger("SableBeyond");
    private static volatile Boolean sableAvailable;

    public static void init() {
        LOGGER.info("Hi there");
    }

    // to be honest i don't know why i coded this function
    public static boolean isSableAvailable() {
        final Boolean cached = sableAvailable;
        if (cached != null) {
            return cached;
        }

        boolean available;
        try {
            Class.forName("dev.ryanhcode.sable.Sable", false, SableBeyond.class.getClassLoader());
            available = true;
        } catch (final ClassNotFoundException | LinkageError ignored) {
            available = false;
        }

        sableAvailable = available;
        return available;
    }
}
