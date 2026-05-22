package me.yassigame.sable_beyond.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.mass.EntityMass;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityMassConfig {
    public static final String FILE_NAME = "entity-mass.json";
    public static final boolean DEFAULT_ENABLED = false;

    public boolean enabled = DEFAULT_ENABLED;
    public boolean experimental_player_mass = false;
    public boolean experimental_player_sublevel_interaction = false;
    public LivingEntityConfig living_entity = LivingEntityConfig.defaults();
    public ItemEntityConfig item_entity = ItemEntityConfig.defaults();

    public static final class LivingEntityConfig {
        public boolean enabled = true;
        public boolean only_player = false;
        public double base_mass = EntityMass.DEFAULT_BASE_MASS;
        public double volume_multiplier = EntityMass.DEFAULT_VOLUME_MULTIPLIER;
        public LivingFormulaConfig formula = LivingFormulaConfig.defaults();
        public Map<String, Double> entities = new LinkedHashMap<>();

        public static LivingEntityConfig defaults() {
            return new LivingEntityConfig();
        }
    }

    public static final class LivingFormulaConfig {
        public boolean enabled = true;
        public String expression = EntityMass.DEFAULT_ENTITY_FORMULA;
        public Double fallback_mass = EntityMass.DEFAULT_BASE_MASS;

        public static LivingFormulaConfig defaults() {
            return new LivingFormulaConfig();
        }
    }

    public static final class ItemEntityConfig {
        public boolean enabled = true;
        public ItemFormulaConfig formula = ItemFormulaConfig.defaults();
        public Map<String, Double> items = new LinkedHashMap<>();

        public static ItemEntityConfig defaults() {
            return new ItemEntityConfig();
        }
    }

    public static final class ItemFormulaConfig {
        public boolean enabled = true;
        public String expression = EntityMass.DEFAULT_ITEM_ENTITY_FORMULA;
        public Double fallback_mass = EntityMass.DEFAULT_ITEM_ENTITY_FALLBACK_MASS;

        public static ItemFormulaConfig defaults() {
            return new ItemFormulaConfig();
        }
    }

    public static EntityMassConfig defaults() {
        return new EntityMassConfig();
    }

    public static EntityMassConfig load(final Path configDirectory) {
        final EntityMassConfig config = JsonConfigFile.load(filePath(configDirectory), EntityMassConfig.class, defaults());
        config.normalize();
        config.save(configDirectory);
        return config;
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

    public LivingEntityConfig livingEntity() {
        if (living_entity == null) {
            living_entity = LivingEntityConfig.defaults();
        }

        if (living_entity.formula == null) {
            living_entity.formula = LivingFormulaConfig.defaults();
        }

        if (living_entity.entities == null) {
            living_entity.entities = new LinkedHashMap<>();
        }

        return living_entity;
    }

    public ItemEntityConfig itemEntity() {
        if (item_entity == null) {
            item_entity = ItemEntityConfig.defaults();
        }

        if (item_entity.formula == null) {
            item_entity.formula = ItemFormulaConfig.defaults();
        }

        if (item_entity.items == null) {
            item_entity.items = new LinkedHashMap<>();
        }

        return item_entity;
    }

    private void normalize() {
        livingEntity();
        itemEntity();
    }
}
