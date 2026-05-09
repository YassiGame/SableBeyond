package me.yassigame.sable_beyond.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SableBeyondNeoForgeConfig {
    private static final String MECHANICAL_ARM_TRANSLATION_ROOT =
            "config.sable_beyond.compatibility.create.mechanical_arm.";
    private static final String ENCASED_FAN_AIRFLOW_TRANSLATION_ROOT =
            "config.sable_beyond.compatibility.create.encased_fan_airflow.";

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;
    private static final ModConfigSpec.BooleanValue IGNORE_PLACEMENT_RANGE_FOR_SUBLEVEL_TARGETS;
    private static final ModConfigSpec.BooleanValue APPLY_FORCE_TO_TOUCHED_SUBLEVELS;

    static {
        // CLIENT SIDE CONFIG
        final ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();

        clientBuilder.push("compatibility");
        clientBuilder.push("create");
        clientBuilder.push("mechanicalArm");

        IGNORE_PLACEMENT_RANGE_FOR_SUBLEVEL_TARGETS = clientBuilder
                .comment(
                        "If enabled, configuring a Mechanical Arm will not remove interaction points for being out of range",
                        "whenever the arm or one of the selected targets is currently inside a Sable sub-level.",
                        "This only changes the configuration/placement step and does not alter the arm runtime search range."
                )
                .translation(MECHANICAL_ARM_TRANSLATION_ROOT + "ignore_placement_range_for_sublevel_targets")
                .define("ignorePlacementRangeForSublevelTargets", true);

        clientBuilder.pop(3);
        CLIENT_SPEC = clientBuilder.build();

        // SERVER SIDE CONFIG
        final ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();

        commonBuilder.push("compatibility");
        commonBuilder.push("create");
        commonBuilder.push("encasedFanAirflow");

        APPLY_FORCE_TO_TOUCHED_SUBLEVELS = commonBuilder
                .comment(
                        "If enabled, Create Encased Fan airflow will apply force to Sable sub-levels touched by the airflow.",
                        "This affects server-side physics behaviour."
                )
                .translation(ENCASED_FAN_AIRFLOW_TRANSLATION_ROOT + "apply_force_to_touched_sublevels")
                .define("applyForceToTouchedSublevels", true);

        commonBuilder.pop(3);
        COMMON_SPEC = commonBuilder.build();
    }

    private SableBeyondNeoForgeConfig() {
    }

    public static boolean ignorePlacementRangeForSublevelTargets() {
        return IGNORE_PLACEMENT_RANGE_FOR_SUBLEVEL_TARGETS.get();
    }

    public static boolean applyForceToTouchedSublevels() {
        return APPLY_FORCE_TO_TOUCHED_SUBLEVELS.get();
    }
}
