package me.yassigame.sable_beyond.config.mass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemEntityMassConfigData {
    public List<String> _comment = List.of(
            " - Configuration for the mass of minecraft:item entities (dropped item entities) - ",
            "",
            "enabled -> toggles mass application for item entities",
            "items -> maps item ids to fixed masses and overrides the generic item formula for those specific drops",
            " ⇢ formula supports variables: width, height, depth, volume, auto_mass, base_mass, volume_multiplier, block_mass, count, stack_count",
            " ⇢ formula supports operators: +, -, *, /, %, ^ and functions: abs, sqrt, floor, ceil, round, min, max, pow, clamp"
    );
    public boolean enabled = true;
    public String formula = "min(block_mass * count, 120)";
    public Double fallback_mass = 0.2;
    public Map<String, Double> items = new LinkedHashMap<>();

    public static ItemEntityMassConfigData defaults() {
        return new ItemEntityMassConfigData();
    }
}
