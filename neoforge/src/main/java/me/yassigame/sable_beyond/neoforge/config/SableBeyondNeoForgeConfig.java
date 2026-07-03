package me.yassigame.sable_beyond.neoforge.config;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.JsonConfigFile;

import java.nio.file.Path;

// FIXME needs cleaning (lazy ass)

public final class SableBeyondNeoForgeConfig {
    public static final String FILE_NAME = "neoforge-compatibility.json";

    private static SableBeyondNeoForgeConfig current = defaults();

    public CompatibilityConfig compatibility = CompatibilityConfig.defaults();

    public static final class CompatibilityConfig {
        public CreateConfig create = CreateConfig.defaults();

        public static CompatibilityConfig defaults() {
            return new CompatibilityConfig();
        }
    }

    public static final class CreateConfig {
        public MechanicalArmConfig mechanical_arm = MechanicalArmConfig.defaults();
        public EncasedFanConfig encased_fan = EncasedFanConfig.defaults();
        public BasinConfig basin = BasinConfig.defaults();
        public WaterWheelConfig water_wheel = WaterWheelConfig.defaults();

        public static CreateConfig defaults() {
            return new CreateConfig();
        }
    }

    public static final class MechanicalArmConfig {
        public boolean ignore_placement_range_for_sublevel_targets = true;

        public static MechanicalArmConfig defaults() {
            return new MechanicalArmConfig();
        }
    }

    public static final class EncasedFanConfig {
        public boolean apply_force_to_touched_sublevels = true;
        public double fan_force_multiplier = 0.2d;

        public static EncasedFanConfig defaults() {
            return new EncasedFanConfig();
        }
    }

    public static final class BasinConfig {
        public boolean empty_input_inventory = false;
        public boolean empty_output_inventory = false;
        public boolean fluid_escaping = false;
        public boolean fill_from_world_fluid = false;

        public static BasinConfig defaults() {
            return new BasinConfig();
        }
    }

    public static final class WaterWheelConfig {
        public boolean thrust_enabled = true;
        public boolean realistic_thrust_mode = false;
        public double thrust_per_rpm = 2d;
        public double small_wheel_factor = 0.5d;

        public static WaterWheelConfig defaults() {
            return new WaterWheelConfig();
        }
    }

    // base functions for config

    public static SableBeyondNeoForgeConfig defaults() {
        return new SableBeyondNeoForgeConfig();
    }

    public static synchronized void load(final Path configDirectory) {
        current = JsonConfigFile.load(filePath(configDirectory), SableBeyondNeoForgeConfig.class, defaults());
        current.normalize();
        current.write(configDirectory);
    }

    public static synchronized void save(final Path configDirectory) {
        current.normalize();
        current.write(configDirectory);
    }

    public static SableBeyondNeoForgeConfig current() {
        return current;
    }

    public static Path filePath(final Path configDirectory) {
        return configDirectory.resolve(SableBeyond.MOD_ID).resolve(FILE_NAME);
    }

    public static boolean ignorePlacementRangeForSublevelTargets() {
        return current.mechanicalArm().ignore_placement_range_for_sublevel_targets;
    }

    public static void ignorePlacementRangeForSublevelTargets(final boolean value) {
        current.mechanicalArm().ignore_placement_range_for_sublevel_targets = value;
    }

    public static boolean applyForceToTouchedSublevels() {
        return current.encasedFanAirflow().apply_force_to_touched_sublevels;
    }

    public static void applyForceToTouchedSublevels(final boolean value) {
        current.encasedFanAirflow().apply_force_to_touched_sublevels = value;
    }

    private void write(final Path configDirectory) {
        JsonConfigFile.save(filePath(configDirectory), this);
    }

    private CompatibilityConfig compatibility() {
        if (compatibility == null) {
            compatibility = CompatibilityConfig.defaults();
        }

        return compatibility;
    }

    private CreateConfig create() {
        final CompatibilityConfig compatibilityConfig = compatibility();
        if (compatibilityConfig.create == null) {
            compatibilityConfig.create = CreateConfig.defaults();
        }

        return compatibilityConfig.create;
    }

    private MechanicalArmConfig mechanicalArm() {
        final CreateConfig createConfig = create();
        if (createConfig.mechanical_arm == null) {
            createConfig.mechanical_arm = MechanicalArmConfig.defaults();
        }

        return createConfig.mechanical_arm;
    }

    private EncasedFanConfig encasedFanAirflow() {
        final CreateConfig createConfig = create();
        if (createConfig.encased_fan == null) {
            createConfig.encased_fan = EncasedFanConfig.defaults();
        }

        return createConfig.encased_fan;
    }

    private BasinConfig basin() {
        final CreateConfig createConfig = create();
        if (createConfig.basin == null) {
            createConfig.basin = BasinConfig.defaults();
        }

        return createConfig.basin;
    }

    private WaterWheelConfig waterWheel() {
        final CreateConfig createConfig = create();
        if (createConfig.water_wheel == null) {
            createConfig.water_wheel = WaterWheelConfig.defaults();
        }

        return createConfig.water_wheel;
    }

    private void normalize() {
        mechanicalArm();
        encasedFanAirflow();
        basin();
        waterWheel();

    }
}