package me.yassigame.sable_beyond.config;

import me.yassigame.sable_beyond.api.mass.EntityMass;

import java.nio.file.Path;

public final class SableBeyondConfig {
    private static DynamicMassConfig dynamicMass = DynamicMassConfig.defaults();
    private static EntityMassConfig entityMass = EntityMassConfig.defaults();
    private static CommonConfig common = CommonConfig.defaults();
    private static OtherConfig other = OtherConfig.defaults();

    private SableBeyondConfig() {
    }

    public static synchronized void load(final Path configDirectory) {
        dynamicMass = DynamicMassConfig.load(configDirectory);
        entityMass = EntityMassConfig.load(configDirectory);
        common = CommonConfig.load(configDirectory);
        other = OtherConfig.load(configDirectory);
    }

    public static synchronized void loadAndApply(final Path configDirectory) {
        load(configDirectory);
        apply();
    }

    public static synchronized void save(final Path configDirectory) {
        dynamicMass.save(configDirectory);
        entityMass.save(configDirectory);
        common.save(configDirectory);
        other.save(configDirectory);
    }

    public static synchronized void saveAndApply(final Path configDirectory) {
        save(configDirectory);
        apply();
    }

    public static synchronized void apply() {
        EntityMass.applyConfig(entityMass);
    }

    public static DynamicMassConfig dynamicMass() {
        return dynamicMass;
    }

    public static EntityMassConfig entityMass() {
        return entityMass;
    }

    public static OtherConfig other() {
        return other;
    }

    public static CommonConfig common() {
        return common;
    }
}
