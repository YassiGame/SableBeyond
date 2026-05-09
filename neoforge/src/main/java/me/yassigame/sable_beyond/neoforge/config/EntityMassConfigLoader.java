package me.yassigame.sable_beyond.neoforge.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.mass.EntityMassConfigIO;
import me.yassigame.sable_beyond.config.mass.GlobalMassConfigIO;
import me.yassigame.sable_beyond.config.mass.ItemEntityMassConfigIO;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

public final class EntityMassConfigLoader {
    private static final Path GLOBAL_CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("global_mass.json");
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("entity_masses.json");
    private static final Path ITEM_ENTITY_CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("item_entity_masses.json");

    public static void load() {
        GlobalMassConfigIO.loadIntoRegistry(GLOBAL_CONFIG_PATH);
        EntityMassConfigIO.loadIntoRegistry(CONFIG_PATH);
        ItemEntityMassConfigIO.loadIntoRegistry(ITEM_ENTITY_CONFIG_PATH);
    }

    public static void reloadOnServerStarting(final ServerStartingEvent event) {
        load();
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(SableBeyond.MOD_ID);
    }
}
