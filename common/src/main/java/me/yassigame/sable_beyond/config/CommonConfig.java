package me.yassigame.sable_beyond.config;

import me.yassigame.sable_beyond.SableBeyond;
import java.nio.file.Path;

public final class CommonConfig {
    public static final String FILE_NAME = "common.json";

    public FireConfig fire = FireConfig.defaults();
    public FlowingFluidConfig flowingFluid = FlowingFluidConfig.defaults();

    public static final class FireConfig {
        public boolean fire_extinguish = true;
        public boolean fire_spreading = true;
        public boolean lava_fire_on_sublevel = true;

        public static FireConfig defaults() {
            return new FireConfig();
        }
    }

    public static final class FlowingFluidConfig {
        public boolean enabled = true;
        public double force = 1.5d;
        public double lava_force_multiplier = 0.6d;
        public double max_force = 120.0d;

        public static FlowingFluidConfig defaults() {
            return new FlowingFluidConfig();
        }
    }

    public static CommonConfig defaults() {
        return new CommonConfig();
    }

    public static CommonConfig load(final Path configDirectory) {
        return JsonConfigFile.load(filePath(configDirectory), CommonConfig.class, defaults());
    }

    public void save(final Path configDirectory) {
        JsonConfigFile.save(filePath(configDirectory), this);
    }

    public static Path filePath(final Path configDirectory) {
        return configDirectory.resolve(SableBeyond.MOD_ID).resolve(FILE_NAME);
    }
}
