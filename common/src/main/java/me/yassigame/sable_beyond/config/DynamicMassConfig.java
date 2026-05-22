package me.yassigame.sable_beyond.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.mass.EntityMass;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicMassConfig {
    public static final String FILE_NAME = "dynamic-mass.json";
    public static final boolean DEFAULT_ENABLED = false;

    public boolean enabled = DEFAULT_ENABLED;
    public double mass_of_bucket = 1;
    public CreateConfig create = CreateConfig.defaults();

    public static DynamicMassConfig defaults() {
        return new DynamicMassConfig();
    }

    public static final class CreateConfig {
        public boolean fluid_tank = true;
        public boolean spout = true;
        public boolean drain = true;
        public boolean basin = true;

        public static CreateConfig defaults() {
            return new CreateConfig();
        }
    }

    public static DynamicMassConfig load(final Path configDirectory) {
        return JsonConfigFile.load(filePath(configDirectory), DynamicMassConfig.class, defaults());
    }

    public void save(final Path configDirectory) {
        JsonConfigFile.save(filePath(configDirectory), this);
    }

    public static Path filePath(final Path configDirectory) {
        return configDirectory.resolve(SableBeyond.MOD_ID).resolve(FILE_NAME);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(final boolean value) {
        enabled = value;
    }
}
