package me.yassigame.sable_beyond.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import me.yassigame.sable_beyond.api.entity.SableBeyondEntityApi;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.api.mass.EntityMass;
import me.yassigame.sable_beyond.api.mass.MassSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SableBeyondMassCommand {
    private static final String VIEWED_TARGET = "@v";
    private static final SimpleCommandExceptionType ERROR_NO_VIEWED_ENTITY = new SimpleCommandExceptionType(Component.literal("No entity in sight."));
    private static final SimpleCommandExceptionType ERROR_UNSUPPORTED_SELECTOR = new SimpleCommandExceptionType(Component.literal("Use @v, @s, @p, a UUID, or a single target selector. @a and @e are disabled here."));
    private static final SimpleCommandExceptionType ERROR_AIR_BLOCK = new SimpleCommandExceptionType(Component.literal("Target block is air."));

    public static void addSubcommands(final LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("mass")
                .then(Commands.literal("block")
                        .then(Commands.literal("info")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> sendBlockMassInfo(
                                                context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("mass", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> setBlockMass(
                                                        context.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                        DoubleArgumentType.getDouble(context, "mass"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> resetBlockMass(
                                                context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
                .then(Commands.literal("entity")
                        .then(Commands.literal("info")
                                .then(Commands.literal(VIEWED_TARGET)
                                        .executes(context -> sendMassInfo(context.getSource(), requireViewedEntity(context.getSource()))))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(SableBeyondMassCommand::suggestTargets)
                                        .executes(context -> sendMassInfo(
                                                context.getSource(),
                                                resolveTarget(context.getSource(), StringArgumentType.getString(context, "target"))))))
                        .then(Commands.literal("set")
                                .then(Commands.literal(VIEWED_TARGET)
                                        .then(Commands.argument("mass", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> setMass(
                                                        context.getSource(),
                                                        requireViewedEntity(context.getSource()),
                                                        DoubleArgumentType.getDouble(context, "mass")))))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(SableBeyondMassCommand::suggestTargets)
                                        .then(Commands.argument("mass", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> setMass(
                                                        context.getSource(),
                                                        resolveTarget(context.getSource(), StringArgumentType.getString(context, "target")),
                                                        DoubleArgumentType.getDouble(context, "mass"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.literal(VIEWED_TARGET)
                                        .executes(context -> resetMass(
                                                context.getSource(),
                                                requireViewedEntity(context.getSource()))))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(SableBeyondMassCommand::suggestTargets)
                                        .executes(context -> resetMass(
                                                context.getSource(),
                                                resolveTarget(context.getSource(), StringArgumentType.getString(context, "target"))))))));
    }

    private static String formatSource(final MassSource source) {
        return switch (source) {
            case NBT_OVERRIDE -> "nbt_override";
            case ENTITY_FORMULA -> "entity_formula";
            case ENTITY_FORMULA_FALLBACK -> "entity_formula_fallback";
            case ITEM_OVERRIDE -> "item_override";
            case ITEM_FORMULA -> "item_formula";
            case ITEM_FORMULA_FALLBACK -> "item_formula_fallback";
            case ITEM_AUTO -> "item_auto";
            case ENTITY_OVERRIDE -> "entity_override";
            case AUTO -> "auto";
            case BASE_FALLBACK -> "base_fallback";
        };
    }

    private static int sendBlockMassInfo(final CommandSourceStack source, final BlockPos pos) throws CommandSyntaxException {
        final ServerLevel level = source.getLevel();
        final BlockState state = requireNonAirBlock(level, pos);
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        final double resolvedMass = PhysicsBlockPropertyHelper.getMass(level, pos, state);
        final var dynamicMass = DynamicMass.getBlockMass(level, pos);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Block %s at %s -> mass %.3f (%s)",
                blockId,
                formatPos(pos),
                resolvedMass,
                dynamicMass.isPresent() ? "dynamic_override" : "sable_default")), false);
        return 1;
    }

    private static int setBlockMass(final CommandSourceStack source, final BlockPos pos, final double mass) throws CommandSyntaxException {
        final ServerLevel level = source.getLevel();
        final BlockState state = requireNonAirBlock(level, pos);
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        final double previousMass = PhysicsBlockPropertyHelper.getMass(level, pos, state);
        final DynamicMass.BlockMassChange change;
        try {
            change = DynamicMass.setBlockMassDetailed(level, pos, mass);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Set dynamic mass %.3f on block %s at %s. Previous resolved mass: %.3f, sublevel sync: %s",
                change.currentMass(),
                blockId,
                formatPos(pos),
                previousMass,
                formatBlockMassSync(change))), true);
        return 1;
    }

    private static int resetBlockMass(final CommandSourceStack source, final BlockPos pos) throws CommandSyntaxException {
        final ServerLevel level = source.getLevel();
        final BlockState state = requireNonAirBlock(level, pos);
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        final DynamicMass.BlockMassChange change;
        try {
            change = DynamicMass.clearBlockMassDetailed(level, pos);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        final double resolvedMass = PhysicsBlockPropertyHelper.getMass(level, pos, state);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "%s dynamic mass on block %s at %s. Current resolved mass: %.3f, sublevel sync: %s",
                change.changed() ? "Reset" : "No",
                blockId,
                formatPos(pos),
                resolvedMass,
                formatBlockMassSync(change))), true);
        return 1;
    }

    private static String formatBlockMassSync(final DynamicMass.BlockMassChange change) {
        if (!change.changed()) {
            return "no override";
        }

        if (!change.syncedSubLevel()) {
            if (Double.compare(change.delta(), 0.0) == 0) {
                return "no mass delta";
            }

            return String.format(Locale.ROOT, "not inside a loaded sublevel (stored delta %.3f)", change.delta());
        }

        return String.format(Locale.ROOT, "synced (delta %.3f, sublevel mass %.3f)", change.delta(), change.subLevelMass());
    }

    private static BlockState requireNonAirBlock(final ServerLevel level, final BlockPos pos) throws CommandSyntaxException {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            throw ERROR_AIR_BLOCK.create();
        }

        return state;
    }

    private static String formatPos(final BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static int sendMassInfo(final CommandSourceStack source, final Entity entity) {
        final EntityMass.MassResolution resolution = EntityMass.resolveMassInfo(entity);
        final String entityName = entity.getName().getString();
        final ResourceLocation entityId = EntityType.getKey(entity.getType());

        String statut;
        if (!EntityMass.isMassAppliedEntity(entity)) {
            statut = "[mass is disabled for this entity]";
        } else {
            statut = "";
        }

        source.sendSuccess(() -> Component.literal(
                String.format(Locale.ROOT,
                        "Entity %s (%s) -> mass %.3f (%s) %s",
                        entityName,
                        entityId,
                        resolution.mass(),
                        formatSource(resolution.source()),
                        statut
                )
        ), false);

        if (resolution.source() == MassSource.NBT_OVERRIDE) {
            source.sendSuccess(() -> Component.literal(
                    "⇢ NBT override path: " + SableBeyondEntityApi.MASS_NBT_TAG + "." + EntityMass.getNbtKey()), false);
        }

        return 1;
    }

    private static int setMass(final CommandSourceStack source, final Entity entity, final double mass) {
        EntityMass.setMassNbt(entity, mass);

        final EntityMass.MassResolution resolution = EntityMass.resolveMassInfo(entity);
        final ResourceLocation entityId = EntityType.getKey(entity.getType());
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Set custom mass %.3f on %s. Current resolved mass: %.3f (%s)",
                mass,
                entityId,
                resolution.mass(),
                formatSource(resolution.source()))), true);
        return 1;
    }

    private static int resetMass(final CommandSourceStack source, final Entity entity) {
        EntityMass.clearMassNbt(entity);

        final EntityMass.MassResolution resolution = EntityMass.resolveMassInfo(entity);
        final ResourceLocation entityId = EntityType.getKey(entity.getType());
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Reset custom mass on %s. Current resolved mass: %.3f (%s)",
                entityId,
                resolution.mass(),
                formatSource(resolution.source()))), true);
        return 1;
    }

    private static Entity requireViewedEntity(final CommandSourceStack source) throws CommandSyntaxException {
        final ServerPlayer player = source.getPlayerOrException();
        final Entity viewedEntity = getViewedEntity(player);
        if (viewedEntity == null) {
            throw ERROR_NO_VIEWED_ENTITY.create();
        }

        return viewedEntity;
    }

    private static Entity getViewedEntity(final ServerPlayer player) {
        final double reach = player.entityInteractionRange();
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(reach));
        final AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        final EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> !entity.isSpectator() && entity.isAlive() && entity != player,
                reach * reach
        );
        if (hitResult != null) {
            return hitResult.getEntity();
        }

        return findViewedItemEntity(player, start, end, searchBox);
    }

    private static Entity resolveTarget(final CommandSourceStack source, final String target) throws CommandSyntaxException {
        if (VIEWED_TARGET.equals(target)) {
            return requireViewedEntity(source);
        }

        if (target.startsWith("@a") || target.startsWith("@e")) {
            throw ERROR_UNSUPPORTED_SELECTOR.create();
        }

        final EntitySelector selector = new EntitySelectorParser(new StringReader(target), true).parse();
        return selector.findSingleEntity(source);
    }

    private static CompletableFuture<Suggestions> suggestTargets(final com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                                                 final SuggestionsBuilder builder) {
        final List<String> suggestions = new ArrayList<>();
        suggestions.add(VIEWED_TARGET);
        suggestions.add("@s");
        suggestions.add("@p");

        for (final ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            suggestions.add(player.getGameProfile().getName());
            suggestions.add(player.getStringUUID());
        }

        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static Entity findViewedItemEntity(final ServerPlayer player, final Vec3 start, final Vec3 end, final AABB searchBox) {
        final Vec3 ray = end.subtract(start);
        final List<ItemEntity> itemEntities = player.level().getEntitiesOfClass(
                ItemEntity.class,
                searchBox,
                entity -> !entity.isSpectator() && entity.isAlive()
        );

        ItemEntity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        for (final ItemEntity itemEntity : itemEntities) {
            final AABB inflatedBox = itemEntity.getBoundingBox().inflate(0.25);
            final var intersection = inflatedBox.clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }

            final double distance = start.distanceToSqr(intersection.get());
            if (distance >= closestDistance || distance > ray.lengthSqr()) {
                continue;
            }

            closestDistance = distance;
            closestEntity = itemEntity;
        }

        return closestEntity;
    }
}
