package me.yassigame.sable_beyond.fabric.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.mass.EntityMassConfigIO;
import me.yassigame.sable_beyond.config.mass.GlobalMassConfigIO;
import me.yassigame.sable_beyond.config.mass.ItemEntityMassConfigIO;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class EntityMassConfigLoader {
    private static final Path GLOBAL_CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("global_mass.json");
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("entity_masses.json");
    private static final Path ITEM_ENTITY_CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(SableBeyond.MOD_ID)
            .resolve("mass")
            .resolve("item_entity_masses.json");

    private EntityMassConfigLoader() {
    }

    public static void load() {
        GlobalMassConfigIO.loadIntoRegistry(GLOBAL_CONFIG_PATH);
        EntityMassConfigIO.loadIntoRegistry(CONFIG_PATH);
        ItemEntityMassConfigIO.loadIntoRegistry(ITEM_ENTITY_CONFIG_PATH);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(SableBeyond.MOD_ID);
    }
}
