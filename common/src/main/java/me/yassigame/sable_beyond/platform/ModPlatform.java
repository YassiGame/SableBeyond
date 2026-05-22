package me.yassigame.sable_beyond.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class ModPlatform {
    @ExpectPlatform
    public static String getLoaderName() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigPath() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openConfigScreen() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<LoadedModInfo> getLoadedMods() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Set<String> getLoadedModIds() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }
}
