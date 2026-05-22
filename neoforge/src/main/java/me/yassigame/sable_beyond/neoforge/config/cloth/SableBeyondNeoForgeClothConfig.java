package me.yassigame.sable_beyond.neoforge.config.cloth;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.config.cloth.ClothButtonRowEntry;
import me.yassigame.sable_beyond.config.cloth.ClothImageEntry;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import me.yassigame.sable_beyond.neoforge.SableBeyondNeoForgeClient;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.util.List;

public final class SableBeyondNeoForgeClothConfig {

    public static void addCategories(final ConfigBuilder builder) {
        SableBeyondNeoForgeConfig.load(FMLPaths.CONFIGDIR.get());

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.compatibility"));
        final SubCategoryBuilder createGroup = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.create"));
        final SubCategoryBuilder mechanicalArmGroup = entryBuilder.startSubCategory(
                Component.translatable("config.sable_beyond.create.mechanical_arm")
        );

        createGroup.setExpanded(true);

        // mechanical arm
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
        final SubCategoryBuilder encasedFanGroup = entryBuilder.startSubCategory(
                Component.translatable("config.sable_beyond.create.encased_fan")
        );
        encasedFanGroup.setExpanded(true);
        encasedFanGroup.add(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.compatibility.create.encased_fan_airflow.apply_force_to_touched_sublevels.description")).build());
        encasedFanGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.encased_fan_airflow.apply_force_to_touched_sublevels"),
                Component.empty(),
                true,
                SableBeyondNeoForgeConfig::applyForceToTouchedSublevels,
                SableBeyondNeoForgeConfig::applyForceToTouchedSublevels
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
                () -> basinConfig.basin_empty_input_inventory,
                (value) -> basinConfig.basin_empty_input_inventory = value
        ));
        basinGroup.add(SableBeyondClothConfigScreen.booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_output_inventory"),
                Component.translatable("config.sable_beyond.compatibility.create.basin.upside_down.empty_output_inventory.tooltip"),
                false,
                () -> basinConfig.basin_empty_output_inventory,
                (value) -> basinConfig.basin_empty_output_inventory = value
        ));

        createGroup.add(basinGroup.build());


        category.addEntry(createGroup.build());
    }

    public static void saveAll() {
        SableBeyondNeoForgeConfig.save(FMLPaths.CONFIGDIR.get());
    }
}
