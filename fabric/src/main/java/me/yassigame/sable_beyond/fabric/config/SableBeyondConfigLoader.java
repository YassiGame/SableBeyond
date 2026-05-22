package me.yassigame.sable_beyond.fabric.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class SableBeyondConfigLoader {
    private SableBeyondConfigLoader() {
    }

    public static void load() {
        SableBeyondConfig.loadAndApply(FabricLoader.getInstance().getConfigDir());
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(SableBeyond.MOD_ID);
    }
}
