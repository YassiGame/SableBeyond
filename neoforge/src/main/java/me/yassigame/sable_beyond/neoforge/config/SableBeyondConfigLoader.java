package me.yassigame.sable_beyond.neoforge.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

public final class SableBeyondConfigLoader {

    public static void load() {
        final Path configDirectory = FMLPaths.CONFIGDIR.get();
        SableBeyondConfig.loadAndApply(configDirectory);
        SableBeyondNeoForgeConfig.load(configDirectory);
    }

    public static void reloadOnServerStarting(final ServerStartingEvent event) {
        load();
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(SableBeyond.MOD_ID);
    }
}
