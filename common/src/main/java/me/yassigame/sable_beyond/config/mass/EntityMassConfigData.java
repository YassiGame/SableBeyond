package me.yassigame.sable_beyond.config.mass;

import me.yassigame.sable_beyond.api.mass.MassRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityMassConfigData {
    public List<String> _comment = List.of(
            " - Configuration for the mass of Living Entities - ",
            "",
            "enabled -> toggles mass application for LivingEntity values",
            "automatic_mass = collision box volume * volume_multiplier",
            "base_mass is used if automatic or override mass resolution fails",
            "entities -> maps entity ids to fixed masses and overrides formula/automatic resolution for those entity ids",
            "entity_formula -> applies to LivingEntity only",
            " ⇢ supports variables: width, height, depth, volume, auto_mass, base_mass, volume_multiplier, health, max_health, age_scale",
            " ⇢ supports operators: +, -, *, /, %, ^ and functions: abs, sqrt, floor, ceil, round, min, max, pow, clamp"
    );
    public boolean enabled = true;
    public double base_mass = MassRegistry.DEFAULT_BASE_MASS;
    public double volume_multiplier = MassRegistry.DEFAULT_VOLUME_MULTIPLIER;
    public Map<String, Double> entities = new LinkedHashMap<>();
    public EntityFormulaMassConfigData entity_formula = EntityFormulaMassConfigData.defaults();

    public static EntityMassConfigData defaults() {
        return new EntityMassConfigData();
    }

    public static final class EntityFormulaMassConfigData {
        public boolean enabled = true;
        public String formula = "min(auto_mass, 15)";
        public Double fallback_mass = null;

        public static EntityFormulaMassConfigData defaults() {
            return new EntityFormulaMassConfigData();
        }
    }
}
