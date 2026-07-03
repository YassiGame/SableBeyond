package me.yassigame.sable_beyond.config.cloth;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.mass.EntityMass;
import me.yassigame.sable_beyond.config.*;
import me.yassigame.sable_beyond.platform.ModPlatform;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SableBeyondClothConfigScreen {
    private static final ResourceLocation SB_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/old_icon.png"
    );
    private static final ResourceLocation GITHUB_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/github_icon.png"
    );
    private static final ResourceLocation ISSUE_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/issue_icon.png"
    );
    private static final ResourceLocation CURSEFORGE_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/curseforge_icon.png"
    );
    private static final ResourceLocation MODRINTH_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/modrinth_icon.png"
    );
    private static final ResourceLocation WIKI_ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/wiki_icon.png"
    );

    public static Screen create(final Screen parent, final Path configDirectory) {
        return create(parent, configDirectory, builder -> {
        }, () -> {
        });
    }

    public static Screen create(
            final Screen parent,
            final Path configDirectory,
            final Consumer<ConfigBuilder> platformCategories,
            final Runnable platformSave
    ) {
        SableBeyondConfig.load(configDirectory);

        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.sable_beyond.title", "Sable Beyond"))
                .setSavingRunnable(() -> {
                    SableBeyondConfig.saveAndApply(configDirectory);
                    platformSave.run();
                });

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        addWelcomeConfigCategory(builder, entryBuilder);
        addEntityMassCategory(builder, entryBuilder, SableBeyondConfig.entityMass());
        addDynamicMassCategory(builder, entryBuilder, SableBeyondConfig.dynamicMass());
        addCommonCategory(builder, entryBuilder, SableBeyondConfig.common());
        addOtherCategory(builder, entryBuilder, SableBeyondConfig.other());
        platformCategories.accept(builder);

        return builder.build();
    }

    private static void addWelcomeConfigCategory(
            final ConfigBuilder builder,
            final ConfigEntryBuilder entryBuilder
    ) {
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.welcome"));
        category.addEntry(new ClothImageEntry(SB_ICON, 64, 64, 256, 256));
        category.addEntry(new ClothButtonRowEntry(List.of(
                ClothButtonRowEntry.ButtonLink.of(
                        Component.literal("GitHub"),
                        "https://github.com/YassiGame/SableBeyond",
                        ClothButtonRowEntry.ButtonIcon.of(GITHUB_ICON, 256, 256)
                ),
                ClothButtonRowEntry.ButtonLink.of(
                        Component.literal("Issue"),
                        "https://github.com/YassiGame/SableBeyond/issues",
                        ClothButtonRowEntry.ButtonIcon.of(ISSUE_ICON, 256, 256)
                ),
                ClothButtonRowEntry.ButtonLink.of(
                        Component.literal("Modrinth"),
                        "https://modrinth.com/mod/sable_beyond",
                        ClothButtonRowEntry.ButtonIcon.of(MODRINTH_ICON, 256, 256)
                ),
                ClothButtonRowEntry.ButtonLink.of(
                        Component.literal("Curseforge"),
                        "https://www.curseforge.com/minecraft/mc-mods/sable-beyond",
                        ClothButtonRowEntry.ButtonIcon.of(CURSEFORGE_ICON, 256, 256)
                ),
                ClothButtonRowEntry.ButtonLink.of(
                        Component.literal("Wiki"),
                        "https://github.com/YassiGame/SableBeyond/wiki",
                        ClothButtonRowEntry.ButtonIcon.of(WIKI_ICON, 256, 256)
                )
        )));
        category.addEntry(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.welcome.description")).build());
    }

    private static void addDynamicMassCategory(
            final ConfigBuilder builder,
            final ConfigEntryBuilder entryBuilder,
            final DynamicMassConfig dynamicMass
    ) {
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.dynamic_mass"));
        final AbstractConfigListEntry<Boolean> enabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.dynamic_mass.enabled.tooltip"),
                DynamicMassConfig.DEFAULT_ENABLED,
                dynamicMass::enabled,
                dynamicMass::enabled
        );

        final AbstractConfigListEntry<Double> massOfBucketEntry = doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.dynamic_mass.mass_of_bucket"),
                Component.translatable("config.sable_beyond.dynamic_mass.mass_of_bucket.tooltip"),
                1,
                () -> dynamicMass.mass_of_bucket,
                (mass) -> dynamicMass.mass_of_bucket = mass
        );

        category.addEntry(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.dynamic_mass.description")).build());
        category.addEntry(enabledEntry);
        category.addEntry(displayWhen(massOfBucketEntry, isEnabled(enabledEntry)));

        if (ModPlatform.isModLoaded("create")) {
            // for create dynamic mass
            category.addEntry(displayWhen(dynamicMassCreateGroup(entryBuilder, dynamicMass), isEnabled(enabledEntry)));
        } else {
            // description for to apologize about the lack of features
            if (Objects.equals(ModPlatform.getLoaderName().toLowerCase(), "fabric")) {
                category.addEntry(displayWhen(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.dynamic_mass.fabric")).build(), isEnabled(enabledEntry)));
            }
        }

    }

    private static AbstractConfigListEntry<?> dynamicMassCreateGroup(
            final ConfigEntryBuilder entryBuilder,
            final DynamicMassConfig dynamicMass
    ) {
        final SubCategoryBuilder group = entryBuilder.startSubCategory(Component.translatable("config.sable_beyond.dynamic_mass.create"));
        group.setExpanded(true);
        group.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.create.fluid_tank"),
                Component.translatable("config.sable_beyond.dynamic_mass.create.generic_tooltip", Component.translatable("config.sable_beyond.create.fluid_tank")),
                true,
                () -> dynamicMass.create.fluid_tank,
                value -> dynamicMass.create.fluid_tank = value
        ));
        group.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.create.basin"),
                Component.translatable("config.sable_beyond.dynamic_mass.create.generic_tooltip", Component.translatable("config.sable_beyond.create.basin")),
                true,
                () -> dynamicMass.create.basin,
                value -> dynamicMass.create.basin = value
        ));
        group.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.create.spout"),
                Component.translatable("config.sable_beyond.dynamic_mass.create.generic_tooltip", Component.translatable("config.sable_beyond.create.spout")),
                true,
                () -> dynamicMass.create.spout,
                value -> dynamicMass.create.spout = value
        ));
        group.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.create.drain"),
                Component.translatable("config.sable_beyond.dynamic_mass.create.generic_tooltip", Component.translatable("config.sable_beyond.create.drain")),
                true,
                () -> dynamicMass.create.drain,
                value -> dynamicMass.create.drain = value
        ));

        return group.build();
    }

    private static void addEntityMassCategory(
            final ConfigBuilder builder,
            final ConfigEntryBuilder entryBuilder,
            final EntityMassConfig entityMass
    ) {
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.entity_mass"));
        category.addEntry(entryBuilder.startTextDescription(Component.translatable("config.sable_beyond.entity_mass.description")).build());


        final AbstractConfigListEntry<Boolean> enabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.entity_mass.enabled.tooltip"),
                EntityMassConfig.DEFAULT_ENABLED,
                entityMass::enabled,
                entityMass::enabled
        );
        category.addEntry(enabledEntry);

        // requirement for entity mass show / hide
        final Requirement entityMassEnabled = isEnabled(enabledEntry);

        category.addEntry(displayWhen(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.experimental_player_mass"),
                Component.translatable("config.sable_beyond.entity_mass.experimental_player_mass.tooltip"),
                false,
                () -> entityMass.experimental_player_mass,
                value -> entityMass.experimental_player_mass = value
        ), entityMassEnabled));

        category.addEntry(displayWhen(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.experimental_player_sublevel_interaction"),
                Component.translatable("config.sable_beyond.entity_mass.experimental_player_sublevel_interaction.tooltip"),
                false,
                () -> entityMass.experimental_player_sublevel_interaction,
                value -> entityMass.experimental_player_sublevel_interaction = value
        ), entityMassEnabled));


        category.addEntry(displayWhen(livingEntityGroup(entryBuilder, entityMass.livingEntity()), entityMassEnabled));
        category.addEntry(displayWhen(itemEntityGroup(entryBuilder, entityMass.itemEntity()), entityMassEnabled));
    }

    private static AbstractConfigListEntry<?> livingEntityGroup(
            final ConfigEntryBuilder entryBuilder,
            final EntityMassConfig.LivingEntityConfig livingEntity
    ) {
        final EntityMassConfig.LivingFormulaConfig formulaDefaults = EntityMassConfig.LivingFormulaConfig.defaults();
        final List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        final AbstractConfigListEntry<Boolean> enabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.enabled.tooltip"),
                true,
                () -> livingEntity.enabled,
                value -> livingEntity.enabled = value
        );
        final Requirement livingEntityEnabled = isEnabled(enabledEntry);

        entries.add(enabledEntry);
        entries.add(displayWhen(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.living_entity.only_player"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.only_player.tooltip"),
                false,
                () -> livingEntity.only_player,
                value -> livingEntity.only_player = value
        ), livingEntityEnabled));
        entries.add(displayWhen(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.living_entity.base_mass"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.base_mass.tooltip"),
                EntityMass.DEFAULT_BASE_MASS,
                () -> livingEntity.base_mass,
                value -> livingEntity.base_mass = value
        ), livingEntityEnabled));
        entries.add(displayWhen(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.living_entity.volume_multiplier"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.volume_multiplier.tooltip"),
                EntityMass.DEFAULT_VOLUME_MULTIPLIER,
                () -> livingEntity.volume_multiplier,
                value -> livingEntity.volume_multiplier = value
        ), livingEntityEnabled));
        entries.add(displayWhen(entryBuilder.startTextDescription(Component.translatable(
                "config.sable_beyond.entity_mass.living_entity.formula.variables"
        )).build(), livingEntityEnabled));
        entries.add(displayWhen(entryBuilder.startTextDescription(Component.translatable(
                "config.sable_beyond.entity_mass.formula.syntax"
        )).build(), livingEntityEnabled));
        final AbstractConfigListEntry<Boolean> formulaEnabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.living_entity.formula.enabled"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.formula.enabled.tooltip"),
                formulaDefaults.enabled,
                () -> livingEntity.formula.enabled,
                value -> livingEntity.formula.enabled = value
        );
        final Requirement livingFormulaEnabled = Requirement.all(livingEntityEnabled, isEnabled(formulaEnabledEntry));
        entries.add(displayWhen(formulaEnabledEntry, livingEntityEnabled));
        entries.add(displayWhen(stringOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.formula"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.formula.tooltip"),
                formulaDefaults.expression,
                () -> livingEntity.formula.expression,
                value -> livingEntity.formula.expression = value
        ), livingFormulaEnabled));
        entries.add(displayWhen(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.formula_fallback_mass"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.formula_fallback_mass.tooltip"),
                EntityMass.DEFAULT_BASE_MASS,
                () -> livingEntity.formula.fallback_mass == null
                        ? EntityMass.DEFAULT_BASE_MASS
                        : livingEntity.formula.fallback_mass,
                value -> livingEntity.formula.fallback_mass = value
        ), livingFormulaEnabled));
        entries.add(displayWhen(massOverrideList(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.living_entity.entity_overrides"),
                Component.translatable("config.sable_beyond.entity_mass.living_entity.entity_overrides.tooltip"),
                "minecraft:zombie=8.0",
                () -> massMapToEntries(livingEntity.entities),
                values -> livingEntity.entities = entriesToMassMap(values)
        ), livingEntityEnabled));
        return subCategory(Component.translatable("config.sable_beyond.living_entity"), true, entries);
    }

    private static AbstractConfigListEntry<?> itemEntityGroup(
            final ConfigEntryBuilder entryBuilder,
            final EntityMassConfig.ItemEntityConfig itemEntity
    ) {
        final EntityMassConfig.ItemFormulaConfig formulaDefaults = EntityMassConfig.ItemFormulaConfig.defaults();
        final List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        final AbstractConfigListEntry<Boolean> enabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.entity_mass.item_entity.enabled.tooltip"),
                true,
                () -> itemEntity.enabled,
                value -> itemEntity.enabled = value
        );
        final Requirement itemEntityEnabled = isEnabled(enabledEntry);

        entries.add(enabledEntry);
        entries.add(displayWhen(entryBuilder.startTextDescription(Component.translatable(
                "config.sable_beyond.entity_mass.item_entity.formula.variables"
        )).build(), itemEntityEnabled));
        entries.add(displayWhen(entryBuilder.startTextDescription(Component.translatable(
                "config.sable_beyond.entity_mass.formula.syntax"
        )).build(), itemEntityEnabled));
        final AbstractConfigListEntry<Boolean> formulaEnabledEntry = booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.item_entity.formula.enabled"),
                Component.translatable("config.sable_beyond.entity_mass.item_entity.formula.enabled.tooltip"),
                formulaDefaults.enabled,
                () -> itemEntity.formula.enabled,
                value -> itemEntity.formula.enabled = value
        );
        final Requirement itemFormulaEnabled = Requirement.all(itemEntityEnabled, isEnabled(formulaEnabledEntry));
        entries.add(displayWhen(formulaEnabledEntry, itemEntityEnabled));
        entries.add(displayWhen(stringOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.formula"),
                Component.translatable("config.sable_beyond.entity_mass.item_entity.formula.tooltip"),
                formulaDefaults.expression,
                () -> itemEntity.formula.expression,
                value -> itemEntity.formula.expression = value
        ), itemFormulaEnabled));
        entries.add(displayWhen(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.formula_fallback_mass"),
                Component.translatable("config.sable_beyond.entity_mass.item_entity.formula_fallback_mass.tooltip"),
                EntityMass.DEFAULT_ITEM_ENTITY_FALLBACK_MASS,
                () -> itemEntity.formula.fallback_mass == null
                        ? EntityMass.DEFAULT_ITEM_ENTITY_FALLBACK_MASS
                        : itemEntity.formula.fallback_mass,
                value -> itemEntity.formula.fallback_mass = value
        ), itemFormulaEnabled));
        entries.add(displayWhen(massOverrideList(
                entryBuilder,
                Component.translatable("config.sable_beyond.entity_mass.item_entity.item_overrides"),
                Component.translatable("config.sable_beyond.entity_mass.item_entity.item_overrides.tooltip"),
                "minecraft:anvil=30.0",
                () -> massMapToEntries(itemEntity.items),
                values -> itemEntity.items = entriesToMassMap(values)
        ), itemEntityEnabled));
        return subCategory(Component.translatable("config.sable_beyond.item_entity"), true, entries);
    }

    private static void addCommonCategory(
            final ConfigBuilder builder,
            final ConfigEntryBuilder entryBuilder,
            final CommonConfig commonConfig
    ) {
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.common"));
        category.addEntry(commonFireGroup(entryBuilder, commonConfig.fire));
        category.addEntry(commonFlowingFluidGroup(entryBuilder, commonConfig.flowingFluid));

    }

    private static AbstractConfigListEntry<?> commonFireGroup(
            final ConfigEntryBuilder entryBuilder,
            final CommonConfig.FireConfig fireConfig
    ) {
        final List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.fire.extinguish"),
                Component.translatable("config.sable_beyond.common.fire.extinguish.tooltip"),
                true,
                () -> fireConfig.fire_extinguish,
                value -> fireConfig.fire_extinguish = value
        ));

        entries.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.fire.spreading"),
                Component.translatable("config.sable_beyond.common.fire.spreading.tooltip"),
                true,
                () -> fireConfig.fire_spreading,
                value -> fireConfig.fire_spreading = value
        ));

        entries.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.fire.lava"),
                Component.translatable("config.sable_beyond.common.fire.lava.tooltip"),
                true,
                () -> fireConfig.lava_fire_on_sublevel,
                value -> fireConfig.lava_fire_on_sublevel = value
        ));

        return subCategory(Component.translatable("config.sable_beyond.common.fire"), true ,entries);
    }

    private static AbstractConfigListEntry<?> commonFlowingFluidGroup(
            final ConfigEntryBuilder entryBuilder,
            final CommonConfig.FlowingFluidConfig flowingFluidConfig
    ) {
        final List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

        entries.add(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.enabled"),
                Component.translatable("config.sable_beyond.common.flowing_fluid.enabled.tooltip"),
                true,
                () -> flowingFluidConfig.enabled,
                value -> flowingFluidConfig.enabled = value
        ));

        entries.add(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.flowing_fluid.force"),
                Component.translatable("config.sable_beyond.common.flowing_fluid.force.tooltip"),
                1.5d,
                () -> flowingFluidConfig.force,
                value -> flowingFluidConfig.force = value
        ));

        entries.add(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.flowing_fluid.lava_force_multiplier"),
                Component.translatable("config.sable_beyond.common.flowing_fluid.lava_force_multiplier.tooltip"),
                0.6d,
                () -> flowingFluidConfig.lava_force_multiplier,
                value -> flowingFluidConfig.lava_force_multiplier = value
        ));

        entries.add(doubleOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.common.flowing_fluid.max_force"),
                Component.translatable("config.sable_beyond.common.flowing_fluid.max_force.tooltip"),
                120.0d,
                () -> flowingFluidConfig.max_force,
                value -> flowingFluidConfig.max_force = value
        ));

        return subCategory(Component.translatable("config.sable_beyond.common.flowing_fluid"), true ,entries);
    }

    private static void addOtherCategory(
            final ConfigBuilder builder,
            final ConfigEntryBuilder entryBuilder,
            final OtherConfig otherConfig
    ) {
        final ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.sable_beyond.other"));

        category.addEntry(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.other.pause_menu"),
                Component.translatable("config.sable_beyond.other.pause_menu.tooltip"),
                true,
                () -> otherConfig.buton_on_pausemenu,
                value -> otherConfig.buton_on_pausemenu = value
        ));

        category.addEntry(booleanOption(
                entryBuilder,
                Component.translatable("config.sable_beyond.other.main_menu"),
                Component.translatable("config.sable_beyond.other.main_menu.tooltip"),
                true,
                () -> otherConfig.buton_on_mainmenu,
                value -> otherConfig.buton_on_mainmenu = value
        ));
    }

    private static Requirement isEnabled(final AbstractConfigListEntry<Boolean> entry) {
        return () -> entry.getValue().equals(true);
    }

    private static <T extends AbstractConfigListEntry<?>> T displayWhen(final T entry, final Requirement requirement) {
        entry.setDisplayRequirement(requirement);
        return entry;
    }

    private static AbstractConfigListEntry<?> subCategory(
            final Component name,
            final boolean expanded,
            final List<? extends AbstractConfigListEntry<?>> entries
    ) {
        return new LiveSubCategoryListEntry(name, entries, expanded);
    }

    public static AbstractConfigListEntry<Boolean> booleanOption(
            final ConfigEntryBuilder entryBuilder,
            final Component name,
            final Component tooltip,
            final boolean defaultValue,
            final Supplier<Boolean> getter,
            final Consumer<Boolean> setter
    ) {
        return entryBuilder.startBooleanToggle(name, getter.get())
                .setDefaultValue(defaultValue)
                .setTooltip(tooltip)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<Double> doubleOption(
            final ConfigEntryBuilder entryBuilder,
            final Component name,
            final Component tooltip,
            final double defaultValue,
            final Supplier<Double> getter,
            final Consumer<Double> setter
    ) {
        return entryBuilder.startDoubleField(name, getter.get())
                .setDefaultValue(defaultValue)
                .setMin(0.0D)
                .setTooltip(tooltip)
                .setSaveConsumer(setter)
                .build();
    }

    public static AbstractConfigListEntry<String> stringOption(
            final ConfigEntryBuilder entryBuilder,
            final Component name,
            final Component tooltip,
            final String defaultValue,
            final Supplier<String> getter,
            final Consumer<String> setter
    ) {
        return entryBuilder.startStrField(name, getter.get() == null ? "" : getter.get())
                .setDefaultValue(defaultValue)
                .setTooltip(tooltip)
                .setSaveConsumer(setter)
                .build();
    }

    private static AbstractConfigListEntry<List<String>> massOverrideList(
            final ConfigEntryBuilder entryBuilder,
            final Component name,
            final Component tooltip,
            final String defaultEntry,
            final Supplier<List<String>> getter,
            final Consumer<List<String>> setter
    ) {
        return entryBuilder.startStrList(name, getter.get())
                .setDefaultValue(List.of())
                .setTooltip(tooltip, Component.translatable("config.sable_beyond.mass_override.new_entry_example", defaultEntry))
                .setAddButtonTooltip(Component.translatable("config.sable_beyond.mass_override.add"))
                .setRemoveButtonTooltip(Component.translatable("config.sable_beyond.mass_override.remove"))
                .setInsertInFront(false)
                .setExpanded(false)
                .setCellErrorSupplier(SableBeyondClothConfigScreen::massOverrideError)
                .setSaveConsumer(setter)
                .build();
    }

    private static List<String> massMapToEntries(final Map<String, Double> masses) {
        final List<String> entries = new ArrayList<>();
        if (masses == null) {
            return entries;
        }

        masses.forEach((id, mass) -> {
            if (id != null && !id.isBlank() && mass != null) {
                entries.add(id + "=" + mass);
            }
        });
        return entries;
    }

    private static Map<String, Double> entriesToMassMap(final List<String> entries) {
        final Map<String, Double> masses = new LinkedHashMap<>();
        for (final String entry : entries) {
            if (massOverrideError(entry).isPresent()) {
                continue;
            }

            final int separatorIndex = entry.indexOf('=');
            masses.put(entry.substring(0, separatorIndex).trim(), Double.parseDouble(entry.substring(separatorIndex + 1).trim()));
        }
        return masses;
    }

    private static Optional<Component> massOverrideError(final String entry) {
        if (entry == null || entry.isBlank()) {
            return Optional.of(Component.translatable("config.sable_beyond.mass_override.error.format"));
        }

        final int separatorIndex = entry.indexOf('=');
        if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
            return Optional.of(Component.translatable("config.sable_beyond.mass_override.error.format"));
        }

        final String id = entry.substring(0, separatorIndex).trim();
        if (ResourceLocation.tryParse(id) == null) {
            return Optional.of(Component.translatable("config.sable_beyond.mass_override.error.resource_id"));
        }

        try {
            final double mass = Double.parseDouble(entry.substring(separatorIndex + 1).trim());
            if (!Double.isFinite(mass) || mass < 0.0D) {
                return Optional.of(Component.translatable("config.sable_beyond.mass_override.error.mass_range"));
            }
        } catch (NumberFormatException exception) {
            return Optional.of(Component.translatable("config.sable_beyond.mass_override.error.mass_number"));
        }

        return Optional.empty();
    }

    private static final class LiveSubCategoryListEntry extends SubCategoryListEntry {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private LiveSubCategoryListEntry(
                final Component categoryName,
                final List<? extends AbstractConfigListEntry<?>> entries,
                final boolean expanded
        ) {
            super(categoryName, (List) entries, expanded);
        }

        @Override
        public List<AbstractConfigListEntry> filteredEntries() {
            tickChildren();
            return super.filteredEntries();
        }

        @Override
        public void tick() {
            tickSelf();
            tickChildren();
        }

        private void tickSelf() {
            enabled = getRequirement() == null || getRequirement().check();
            displayed = getDisplayRequirement() == null || getDisplayRequirement().check();
        }

        private void tickChildren() {
            for (final AbstractConfigListEntry entry : getValue()) {
                entry.tick();
            }
        }
    }
}
