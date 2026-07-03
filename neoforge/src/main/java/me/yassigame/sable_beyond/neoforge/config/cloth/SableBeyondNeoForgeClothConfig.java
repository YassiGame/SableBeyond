package me.yassigame.sable_beyond.neoforge.config.cloth;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.cloth.ClothImageEntry;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import me.yassigame.sable_beyond.platform.ModPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public final class SableBeyondNeoForgeClothConfig {

    private static final ResourceLocation TOBYFOX_GIF = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/toby_fox.png"
    );

    public static void addCategories(final ConfigBuilder builder) {
        SableBeyondNeoForgeConfig.load(FMLPaths.CONFIGDIR.get());

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.compatibility"));
        if (ModPlatform.isModLoaded("create")) {
            category.addEntry(createCategory(entryBuilder).build());
        } else {
            category.addEntry(ClothImageEntry.spritesheetGrid(
                    TOBYFOX_GIF,
                    200, 200,
                    270, 270,
                    8,
                    4,
                    250
            ));
        }
    }

    public static SubCategoryBuilder createCategory(ConfigEntryBuilder entryBuilder) {
        final SubCategoryBuilder createGroup = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.create"));
        createGroup.setExpanded(true);

        // mechanical arm
        final SubCategoryBuilder mechanicalArmGroup = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.create.mechanical_arm"));
        mechanicalArmGroup.setExpanded(true);

        /**
        mechanicalArmGroup.add(ClothImageEntry.spritesheetGrid(
                VID_TEST,
                360, 218,
                360, 218,
                176,
                16,
                42
        ));
         */

        mechanicalArmGroup.add(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.compatibility.create.mechanical_arm.ignore_placement_range_for_sublevel_targets.description")).build());
        mechanicalArmGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.mechanical_arm.ignore_placement_range_for_sublevel_targets"),
                Component.empty(),
                true,
                SableBeyondNeoForgeConfig::ignorePlacementRangeForSublevelTargets,
                SableBeyondNeoForgeConfig::ignorePlacementRangeForSublevelTargets
        ));

        // encased fan
        final SableBeyondNeoForgeConfig.EncasedFanConfig fanConfig = SableBeyondNeoForgeConfig.current().compatibility.create.encased_fan;
        final SubCategoryBuilder encasedFanGroup = entryBuilder.startSubCategory(
                Component.translatable("config.sable_beyond.create.encased_fan")
        );
        encasedFanGroup.setExpanded(true);
        encasedFanGroup.add(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.compatibility.create.encased_fan.airflow.apply_force_to_touched_sublevels.description")).build());
        encasedFanGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.encased_fan.airflow.apply_force_to_touched_sublevels"),
                Component.empty(),
                true,
                SableBeyondNeoForgeConfig::applyForceToTouchedSublevels,
                SableBeyondNeoForgeConfig::applyForceToTouchedSublevels
        ));
        encasedFanGroup.add(SableBeyondClothConfigScreen.doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.encased_fan.airflow.force_multiplier"),
                Component.translatable("config.sable_beyond.compatibility.create.encased_fan.airflow.force_multiplier.tooltip"),
                0.2d,
                () -> fanConfig.fan_force_multiplier,
                (value) -> fanConfig.fan_force_multiplier = value
        ));
        createGroup.add(mechanicalArmGroup.build());
        createGroup.add(encasedFanGroup.build());

        // basin
        final SableBeyondNeoForgeConfig.BasinConfig basinConfig = SableBeyondNeoForgeConfig.current().compatibility.create.basin;
        final SubCategoryBuilder basinGroup = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.create.basin"));
        basinGroup.setExpanded(true);
        basinGroup.add(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.description")).build());
        basinGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_input_inventory"),
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_input_inventory.tooltip"),
                false,
                () -> basinConfig.empty_input_inventory,
                (value) -> basinConfig.empty_input_inventory = value
        ));
        basinGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_output_inventory"),
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_output_inventory.tooltip"),
                false,
                () -> basinConfig.empty_output_inventory,
                (value) -> basinConfig.empty_output_inventory = value
        ));
        basinGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.fluid_escaping"),
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.fluid_escaping.tooltip"),
                false,
                () -> basinConfig.fluid_escaping,
                (value) -> basinConfig.fluid_escaping = value
        ));
        basinGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.basin.basin_fill_from_world_fluid"),
                Component.translatable("config.sable_beyond.compatibility.create.basin.basin_fill_from_world_fluid.tooltip"),
                false,
                () -> basinConfig.fill_from_world_fluid,
                (value) -> basinConfig.fill_from_world_fluid = value
        ));
        createGroup.add(basinGroup.build());

        // water wheel
        final SableBeyondNeoForgeConfig.WaterWheelConfig waterWheelConfig = SableBeyondNeoForgeConfig.current().compatibility.create.water_wheel;
        final SubCategoryBuilder waterWheelGroup = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.create.water_wheel"));
        waterWheelGroup.setExpanded(true);
        waterWheelGroup.add(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.compatibility.create.water_wheel.description")).build());
        waterWheelGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.enabled.tooltip"),
                true,
                () -> waterWheelConfig.thrust_enabled,
                (value) -> waterWheelConfig.thrust_enabled = value
        ));
        waterWheelGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.realistic_thrust_mode"),
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.realistic_thrust_mode.tooltip"),
                false,
                () -> waterWheelConfig.realistic_thrust_mode,
                (value) -> waterWheelConfig.realistic_thrust_mode = value
        ));
        waterWheelGroup.add(SableBeyondClothConfigScreen.doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.thrust_per_rpm"),
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.thrust_per_rpm.tooltip"),
                2d,
                () -> waterWheelConfig.thrust_per_rpm,
                (value) -> waterWheelConfig.thrust_per_rpm = value
        ));
        waterWheelGroup.add(SableBeyondClothConfigScreen.doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.small_wheel_factor"),
                Component.translatable("config.sable_beyond.compatibility.create.water_wheel.small_wheel_factor.tooltip"),
                0.5d,
                () -> waterWheelConfig.small_wheel_factor,
                (value) -> waterWheelConfig.small_wheel_factor = value
        ));

        createGroup.add(waterWheelGroup.build());
        return createGroup;
    }

    public static void saveAll() {
        SableBeyondNeoForgeConfig.save(FMLPaths.CONFIGDIR.get());
    }
}