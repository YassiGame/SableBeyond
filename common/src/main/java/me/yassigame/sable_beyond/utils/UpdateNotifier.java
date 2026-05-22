package me.yassigame.sable_beyond.utils;

import net.minecraft.data.HashCache;

// TODO add a update notifier to notify about new updates (needs to be async)

public final class UpdateNotifier {
    private static final long CHECK_INTERVAL_MS = 4L * 60L * 60L * 1000L; // 4 hour

    private static long lastCheckTime = 0L;
    private static boolean checkInProgress = false;
    private static HashCache.UpdateResult cachedResult = null;

    public static void checkIfNeeded() {
        long now = System.currentTimeMillis();

        if (checkInProgress) {
            return;
        }

    }
}