package me.yassigame.sable_beyond.api.mass;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.entity.SableBeyondEntityApi;
import me.yassigame.sable_beyond.config.mass.EntityMassConfigData;
import me.yassigame.sable_beyond.config.mass.GlobalMassConfigData;
import me.yassigame.sable_beyond.config.mass.ItemEntityMassConfigData;
import me.yassigame.sable_beyond.utils.ItemStackItemIdHelper;
import me.yassigame.sable_beyond.common.FormulaManager;
import me.yassigame.sable_beyond.utils.SableMassCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MassRegistry {
    public static final double DEFAULT_BASE_MASS = 0.5;
    public static final double DEFAULT_VOLUME_MULTIPLIER = 4.0;
    public static final String DEFAULT_NBT_KEY = "mass";
    public static final String DEFAULT_ENTITY_FORMULA = "min(auto_mass, 15)";
    public static final String DEFAULT_ITEM_ENTITY_FORMULA = "block_mass * count";

    private static final Map<ResourceLocation, Double> ENTITY_OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Double> ITEM_OVERRIDES = new ConcurrentHashMap<>();

    private static volatile boolean globalEnabled = true;
    private static volatile boolean experimentalPlayerSublevelInteraction = false;
    private static volatile boolean experimentalPlayerMass = false;
    private static volatile boolean livingEntityEnabled = true;
    private static volatile double baseMass = DEFAULT_BASE_MASS;
    private static volatile double volumeMultiplier = DEFAULT_VOLUME_MULTIPLIER;
    private static volatile boolean entityFormulaEnabled;
    private static volatile boolean entityFormulaInvalid;
    private static volatile @Nullable FormulaManager entityFormula;
    private static volatile String entityFormulaExpression = DEFAULT_ENTITY_FORMULA;
    private static volatile @Nullable Double entityFormulaFallbackMass;
    private static volatile boolean itemEntityFormulaEnabled = true;
    private static volatile boolean itemEntityFormulaInvalid;
    private static volatile String itemEntityFormulaExpression = DEFAULT_ITEM_ENTITY_FORMULA;
    private static volatile @Nullable FormulaManager itemEntityFormula = FormulaManager.compile(DEFAULT_ITEM_ENTITY_FORMULA);
    private static volatile @Nullable Double itemEntityFallbackMass;

    public record MassResolution(double mass, MassSource source) {
    }

    public static void applyGlobalConfig(final GlobalMassConfigData configData) {
        final GlobalMassConfigData defaults = GlobalMassConfigData.defaults();
        final GlobalMassConfigData effective = configData == null ? defaults : configData;

        globalEnabled = effective.enabled;
        experimentalPlayerSublevelInteraction = effective.experimental_player_sublevel_interaction;
        experimentalPlayerMass = effective.experimental_player_mass;
    }

    public static void applyConfig(final EntityMassConfigData configData) {
        ENTITY_OVERRIDES.clear();

        if (configData == null) {
            livingEntityEnabled = true;
            baseMass = DEFAULT_BASE_MASS;
            volumeMultiplier = DEFAULT_VOLUME_MULTIPLIER;
            entityFormulaEnabled = true;
            entityFormulaInvalid = false;
            entityFormula = FormulaManager.compile(DEFAULT_ENTITY_FORMULA);
            entityFormulaExpression = DEFAULT_ENTITY_FORMULA;
            entityFormulaFallbackMass = null;
            itemEntityFormulaEnabled = true;
            itemEntityFormulaInvalid = false;
            itemEntityFormulaExpression = DEFAULT_ITEM_ENTITY_FORMULA;
            itemEntityFormula = FormulaManager.compile(DEFAULT_ITEM_ENTITY_FORMULA);
            itemEntityFallbackMass = null;
            return;
        }

        livingEntityEnabled = configData.enabled;
        baseMass = sanitizeMass(configData.base_mass, DEFAULT_BASE_MASS);
        volumeMultiplier = sanitizeMass(configData.volume_multiplier, DEFAULT_VOLUME_MULTIPLIER);
        applyEntityFormulaConfig(configData.entity_formula);

        if (configData.entities == null) {
            return;
        }

        for (final Map.Entry<String, Double> entry : configData.entities.entrySet()) {
            final ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                continue;
            }

            ENTITY_OVERRIDES.put(id, sanitizeMass(entry.getValue(), baseMass));
        }
    }

    public static double resolveMass(final Entity entity, final @Nullable CompoundTag massNbt) {
        return resolveMassInfo(entity, massNbt).mass();
    }

    public static double resolveMass(final Entity entity) {
        return resolveMass(entity, SableBeyondEntityApi.getMassNbt(entity));
    }

    public static MassResolution resolveMassInfo(final Entity entity) {
        return resolveMassInfo(entity, SableBeyondEntityApi.getMassNbt(entity));
    }

    public static MassResolution resolveMassInfo(final Entity entity, final @Nullable CompoundTag massNbt) {
        final Double nbtMass = getMassFromNbt(massNbt);
        if (nbtMass != null) {
            return new MassResolution(sanitizeMass(nbtMass, baseMass), MassSource.NBT_OVERRIDE);
        }

        final MassResolution itemOverrideMass = resolveSpecificItemOverride(entity);
        if (itemOverrideMass != null) {
            return itemOverrideMass;
        }

        final Double overrideMass = ENTITY_OVERRIDES.get(EntityType.getKey(entity.getType()));
        if (overrideMass != null) {
            return new MassResolution(sanitizeMass(overrideMass, baseMass), MassSource.ENTITY_OVERRIDE);
        }

        final MassResolution itemEntityMass = resolveItemEntityMass(entity);
        if (itemEntityMass != null) {
            return itemEntityMass;
        }

        final MassResolution entityFormulaMass = resolveEntityFormulaMass(entity);
        if (entityFormulaMass != null) {
            return entityFormulaMass;
        }

        return computeAutomaticMassInfo(entity);
    }

    public static double computeAutomaticMass(final Entity entity) {
        return computeAutomaticMassInfo(entity).mass();
    }

    public static MassResolution computeAutomaticMassInfo(final Entity entity) {
        final AABB box = entity.getBoundingBox();
        if (box == null) {
            return new MassResolution(baseMass, MassSource.BASE_FALLBACK);
        }

        final double volume = box.getXsize() * box.getYsize() * box.getZsize();
        if (!Double.isFinite(volume) || volume <= 0.0) {
            return new MassResolution(baseMass, MassSource.BASE_FALLBACK);
        }

        final double mass = volume * volumeMultiplier;
        return new MassResolution(sanitizeMass(mass, baseMass), entity instanceof ItemEntity ? MassSource.ITEM_AUTO : MassSource.AUTO);
    }

    public static double evaluateEntityFormula(final Entity entity, final String formula) {
        final double result = FormulaManager.compile(formula).evaluate(buildFormulaVariables(entity));
        return requireValidMass("Entity formula result", result);
    }

    public static double setMassNbt(final Entity entity, final double mass) {
        final double validMass = requireValidMass("Mass NBT", mass);
        SableBeyondEntityApi.getMassNbt(entity).putDouble(DEFAULT_NBT_KEY, validMass);
        return validMass;
    }

    public static void clearMassNbt(final Entity entity) {
        SableBeyondEntityApi.getMassNbt(entity).remove(DEFAULT_NBT_KEY);
    }

    public static String getNbtKey() {
        return DEFAULT_NBT_KEY;
    }

    public static String getLivingEntityFormula() {
        return entityFormulaExpression;
    }

    public static String getItemEntityFormula() {
        return itemEntityFormulaExpression;
    }

    public static boolean isExperimentalPlayerSublevelInteractionEnabled() {
        return experimentalPlayerSublevelInteraction;
    }

    public static boolean isExperimentalPlayerMassEnabled() {
        return experimentalPlayerMass;
    }

    public static boolean isEnabled() {
        return globalEnabled;
    }

    public static boolean isLivingEntityMassEnabled() {
        return globalEnabled && livingEntityEnabled;
    }

    public static boolean isItemEntityMassEnabled() {
        return globalEnabled && itemEntityFormulaEnabled;
    }

    public static boolean isMassAppliedEntity(final Entity entity) {
        return entity instanceof LivingEntity
                ? isLivingEntityMassEnabled()
                : entity instanceof ItemEntity && isItemEntityMassEnabled();
    }

    public static double getBaseMass() {
        return baseMass;
    }

    public static double getVolumeMultiplier() {
        return volumeMultiplier;
    }

    private static @Nullable MassResolution resolveEntityFormulaMass(final Entity entity) {
        if (!entityFormulaEnabled || !(entity instanceof final LivingEntity livingEntity)) {
            return null;
        }

        if (entityFormula == null) {
            return entityFormulaInvalid ? fallbackEntityFormulaMass() : null;
        }

        try {
            final double result = entityFormula.evaluate(buildLivingEntityVariables(livingEntity));
            if (Double.isFinite(result) && result >= 0.0) {
                return new MassResolution(result, MassSource.ENTITY_FORMULA);
            }
        } catch (final RuntimeException ignored) {
            return fallbackEntityFormulaMass();
        }

        return fallbackEntityFormulaMass();
    }

    private static @Nullable MassResolution resolveItemEntityMass(final Entity entity) {
        if (!itemEntityFormulaEnabled || !(entity instanceof final ItemEntity itemEntity)) {
            return null;
        }

        if (itemEntityFormula == null) {
            return itemEntityFormulaInvalid ? fallbackItemMass() : null;
        }

        try {
            final double result = itemEntityFormula.evaluate(buildItemEntityVariables(itemEntity));
            if (Double.isFinite(result) && result >= 0.0) {
                return new MassResolution(result, MassSource.ITEM_FORMULA);
            }
        } catch (final RuntimeException ignored) {
            return fallbackItemMass();
        }

        return fallbackItemMass();
    }

    private static @Nullable MassResolution resolveSpecificItemOverride(final Entity entity) {
        if (!(entity instanceof final ItemEntity itemEntity)) {
            return null;
        }

        final ResourceLocation resolvedItemId = ItemStackItemIdHelper.getItemId(itemEntity.getItem());
        //System.out.println(resolvedItemId);
        if (resolvedItemId == null) {
            return null;
        }

        final Double overrideMass = ITEM_OVERRIDES.get(resolvedItemId);
        if (overrideMass == null) {
            return null;
        }

        return new MassResolution(sanitizeMass(overrideMass, baseMass), MassSource.ITEM_OVERRIDE);
    }

    private static Map<String, Double> buildItemEntityVariables(final ItemEntity itemEntity) {
        final Map<String, Double> variables = buildCommonEntityVariables(itemEntity);
        final ItemStack stack = itemEntity.getItem();
        final double count = stack.getCount();

        variables.put("count", count);
        variables.put("stack_count", count);

        if (stack.getItem() instanceof final BlockItem blockItem) {
            variables.put("block_mass", SableMassCompat.getBlockMass(itemEntity, blockItem));
        }

        return variables;
    }

    private static Map<String, Double> buildLivingEntityVariables(final LivingEntity entity) {
        final Map<String, Double> variables = buildCommonEntityVariables(entity);
        final float health = entity.getHealth();
        final float maxHealth = entity.getMaxHealth();
        final float ageScale = entity.getAgeScale();

        variables.put("health", (double) health);
        variables.put("max_health", (double) maxHealth);
        variables.put("age_scale", (double) ageScale);
        return variables;
    }

    private static Map<String, Double> buildFormulaVariables(final Entity entity) {
        if (entity instanceof final LivingEntity livingEntity) {
            return buildLivingEntityVariables(livingEntity);
        }

        if (entity instanceof final ItemEntity itemEntity) {
            return buildItemEntityVariables(itemEntity);
        }

        return buildCommonEntityVariables(entity);
    }

    private static Map<String, Double> buildCommonEntityVariables(final Entity entity) {
        final Map<String, Double> variables = new HashMap<>();
        final AABB box = entity.getBoundingBox();
        final double width = (box == null) ? 0.0 : box.getXsize();
        final double height = (box == null) ? 0.0 : box.getYsize();
        final double depth = (box == null) ? 0.0 : box.getZsize();
        final double volume = width * height * depth;

        variables.put("width", width);
        variables.put("height", height);
        variables.put("depth", depth);
        variables.put("volume", Double.isFinite(volume) ? volume : 0.0);
        variables.put("auto_mass", computeAutomaticMass(entity));
        variables.put("base_mass", baseMass);
        variables.put("volume_multiplier", volumeMultiplier);
        return variables;
    }

    private static @Nullable MassResolution fallbackItemMass() {
        return itemEntityFallbackMass != null
                ? new MassResolution(sanitizeMass(itemEntityFallbackMass, baseMass), MassSource.ITEM_FORMULA_FALLBACK)
                : null;
    }

    private static @Nullable MassResolution fallbackEntityFormulaMass() {
        return entityFormulaFallbackMass != null
                ? new MassResolution(sanitizeMass(entityFormulaFallbackMass, baseMass), MassSource.ENTITY_FORMULA_FALLBACK)
                : null;
    }

    private static void applyEntityFormulaConfig(final @Nullable EntityMassConfigData.EntityFormulaMassConfigData configData) {
        final EntityMassConfigData.EntityFormulaMassConfigData defaults = EntityMassConfigData.EntityFormulaMassConfigData.defaults();
        final EntityMassConfigData.EntityFormulaMassConfigData effective = configData == null ? defaults : configData;

        entityFormulaEnabled = effective.enabled;
        entityFormulaFallbackMass = effective.fallback_mass == null ? null : sanitizeMass(effective.fallback_mass, baseMass);
        entityFormulaInvalid = false;
        entityFormula = null;
        entityFormulaExpression = effective.formula == null ? defaults.formula : effective.formula;

        if (!entityFormulaEnabled) {
            return;
        }

        final String formula = entityFormulaExpression;
        if (formula.isBlank()) {
            entityFormulaInvalid = true;
            SableBeyond.LOGGER.warn("Entity mass formula is blank, falling back to the optional entity formula fallback mass.");
            return;
        }

        try {
            entityFormula = FormulaManager.compile(formula);
        } catch (final IllegalArgumentException exception) {
            entityFormulaInvalid = true;
            SableBeyond.LOGGER.error("Invalid entity mass formula '{}', falling back to the optional entity formula fallback mass.", formula, exception);
        }
    }

    public static void applyItemEntityConfig(final @Nullable ItemEntityMassConfigData configData) {
        ITEM_OVERRIDES.clear();

        final ItemEntityMassConfigData defaults = ItemEntityMassConfigData.defaults();
        final ItemEntityMassConfigData effective = configData == null ? defaults : configData;

        itemEntityFormulaEnabled = effective.enabled;
        itemEntityFallbackMass = effective.fallback_mass == null ? null : sanitizeMass(effective.fallback_mass, baseMass);
        itemEntityFormulaInvalid = false;
        itemEntityFormula = null;
        itemEntityFormulaExpression = effective.formula == null ? defaults.formula : effective.formula;

        if (effective.items != null) {
            for (final Map.Entry<String, Double> entry : effective.items.entrySet()) {
                final ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null) {
                    continue;
                }

                ITEM_OVERRIDES.put(id, sanitizeMass(entry.getValue(), baseMass));
            }
        }

        if (!itemEntityFormulaEnabled) {
            return;
        }

        final String formula = effective.formula == null ? defaults.formula : effective.formula;
        if (formula.isBlank()) {
            itemEntityFormulaInvalid = true;
            SableBeyond.LOGGER.warn("Item entity mass formula is blank, falling back to the optional item fallback mass.");
            return;
        }

        try {
            itemEntityFormula = FormulaManager.compile(formula);
        } catch (final IllegalArgumentException exception) {
            itemEntityFormulaInvalid = true;
            SableBeyond.LOGGER.error("Invalid item entity mass formula '{}', falling back to the optional item fallback mass.", formula, exception);
        }
    }

    private static @Nullable Double getMassFromNbt(final @Nullable CompoundTag massNbt) {
        if (massNbt == null || !massNbt.contains(DEFAULT_NBT_KEY, Tag.TAG_ANY_NUMERIC)) {
            return null;
        }

        return massNbt.getDouble(DEFAULT_NBT_KEY);
    }

    private static double sanitizeMass(final @Nullable Double mass, final double fallback) {
        if (mass == null || !Double.isFinite(mass) || mass < 0.0) {
            return fallback;
        }

        return mass;
    }

    private static double requireValidMass(final String name, final double mass) {
        if (!Double.isFinite(mass) || mass < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }

        return mass;
    }
}
