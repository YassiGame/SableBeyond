package me.yassigame.sable_beyond.api.mass;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.saved_data.DynamicMassSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DynamicMass {
    private static final ConcurrentMap<BlockMassKey, Double> BLOCK_MASSES = new ConcurrentHashMap<>();
    private static final Set<BlockMassKey> PRESERVED_BLOCK_STATE_CHANGE_KEYS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> BYPASS_BLOCK_MASS_OVERRIDES = ThreadLocal.withInitial(() -> false); // for the bypass of the override and thread local because not global
    private static volatile MinecraftServer restoredServer;

    public static boolean isEnabled() {
        return SableBeyondConfig.dynamicMass().enabled;
    }

    public static double setBlockMass(final Level level, final BlockPos pos, final double mass) {
        return setBlockMassDetailed(level, pos, mass).currentMass();
    }

    public static BlockMassChange setBlockMassDetailed(final Level level, final BlockPos pos, final double mass) {
        restoreSavedMasses(level);
        final double validMass = requireValidMass(mass);
        final BlockState state = level.getBlockState(pos);
        final double previousMass = resolveCurrentMass(level, pos, state);
        final BlockMassSync sync = syncContainingSubLevelMass(level, pos, state, previousMass, validMass);
        final BlockMassKey key = BlockMassKey.from(level, pos);
        putBlockMass(key, validMass);
        saveBlockMass(level, key, validMass);
        return new BlockMassChange(true, sync.syncedSubLevel(), previousMass, validMass, sync.delta(), sync.subLevelMass());
    }

    public static OptionalDouble getBlockMass(final Level level, final BlockPos pos) {
        restoreSavedMasses(level);
        return getBlockMass(BlockMassKey.from(level, pos));
    }

    public static OptionalDouble getBlockMass(final BlockGetter blockGetter, final BlockPos pos) {
        if (!(blockGetter instanceof final Level level)) {
            return OptionalDouble.empty();
        }

        return getBlockMass(level, pos);
    }

    public static double getDefaultBlockMass(final Level level, final BlockPos pos) {
        return getDefaultBlockMass(level, pos, level.getBlockState(pos));
    }

    public static double getDefaultBlockMass(final Level level, final BlockPos pos, final BlockState state) {
        return resolveDefaultMass(level, pos, state);
    }

    public static boolean hasBlockMass(final Level level, final BlockPos pos) {
        restoreSavedMasses(level);
        return hasBlockMass(BlockMassKey.from(level, pos));
    }

    public static boolean clearBlockMass(final Level level, final BlockPos pos) {
        return clearBlockMassDetailed(level, pos).changed();
    }

    public static boolean discardBlockMass(final Level level, final BlockPos pos) {
        final BlockMassKey key = BlockMassKey.from(level, pos);
        removeSavedBlockMass(level, key);
        return discardBlockMass(key);
    }

    public static boolean clearBlockMassBeforeBlockChange(final Level level, final BlockPos pos) {
        restoreSavedMasses(level);
        final BlockMassKey key = BlockMassKey.from(level, pos);
        if (PRESERVED_BLOCK_STATE_CHANGE_KEYS.remove(key)) {
            return false;
        }

        if (!hasBlockMass(key)) {
            return false;
        }

        return clearBlockMassDetailed(level, pos).changed();
    }

    public static boolean moveBlockMass(
            final ServerLevel originLevel,
            final BlockPos oldPos,
            final ServerLevel resultingLevel,
            final BlockPos newPos
    ) {
        restoreSavedMasses(originLevel);

        final BlockMassKey oldKey = BlockMassKey.from(originLevel, oldPos);
        final BlockMassKey newKey = BlockMassKey.from(resultingLevel, newPos);
        if (oldKey.equals(newKey)) {
            return false;
        }

        final Double mass = BLOCK_MASSES.remove(oldKey);
        if (mass == null) {
            return false;
        }

        PRESERVED_BLOCK_STATE_CHANGE_KEYS.remove(oldKey);
        removeSavedBlockMass(originLevel, oldKey);
        putBlockMass(newKey, mass);
        saveBlockMass(resultingLevel, newKey, mass);
        PRESERVED_BLOCK_STATE_CHANGE_KEYS.add(newKey);
        return true;
    }

    public static BlockMassChange clearBlockMassDetailed(final Level level, final BlockPos pos) {
        restoreSavedMasses(level);
        final BlockMassKey key = BlockMassKey.from(level, pos);
        final Double previousOverride = BLOCK_MASSES.get(key);
        if (previousOverride == null) {
            PRESERVED_BLOCK_STATE_CHANGE_KEYS.remove(key);
            removeSavedBlockMass(level, key);
            final BlockState state = level.getBlockState(pos);
            final double currentMass = resolveCurrentMass(level, pos, state);
            return new BlockMassChange(false, false, currentMass, currentMass, 0.0, Double.NaN);
        }

        final BlockState state = level.getBlockState(pos);
        final double defaultMass = resolveDefaultMass(level, pos, state);
        final BlockMassSync sync = syncContainingSubLevelMass(level, pos, state, previousOverride, defaultMass);
        discardBlockMass(key);
        removeSavedBlockMass(level, key);
        return new BlockMassChange(true, sync.syncedSubLevel(), previousOverride, defaultMass, sync.delta(), sync.subLevelMass());
    }

    public static void clearAllBlockMasses() {
        final MinecraftServer server = restoredServer;
        if (server == null) {
            resetRuntimeBlockMasses();
            return;
        }

        clearAllBlockMasses(server);
    }

    public static void clearAllBlockMasses(final MinecraftServer server) {
        if (server == null) {
            resetRuntimeBlockMasses();
            return;
        }

        restoreSavedMasses(server);
        final List<BlockMassKey> keys = List.copyOf(BLOCK_MASSES.keySet());
        for (final BlockMassKey key : keys) {
            final ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                discardBlockMass(key);
                DynamicMassSavedData.get(server).removeMass(key);
                continue;
            }

            clearBlockMassDetailed(level, BlockPos.of(key.pos()));
        }

        DynamicMassSavedData.get(server).clearMasses();
        PRESERVED_BLOCK_STATE_CHANGE_KEYS.clear();
        restoredServer = server;
    }

    public static void resetRuntimeBlockMasses() {
        BLOCK_MASSES.clear();
        PRESERVED_BLOCK_STATE_CHANGE_KEYS.clear();
        restoredServer = null;
    }

    public static synchronized void restoreSavedMasses(final MinecraftServer server) {
        if (server == null || restoredServer == server) {
            return;
        }

        final Map<BlockMassKey, Double> savedMasses = DynamicMassSavedData.get(server).getMasses();
        BLOCK_MASSES.clear();
        PRESERVED_BLOCK_STATE_CHANGE_KEYS.clear();
        BLOCK_MASSES.putAll(savedMasses);
        restoredServer = server;
    }

    public static double liquidToMass(final double liquid_mb) {
        return (liquid_mb / 1000) * SableBeyondConfig.dynamicMass().mass_of_bucket;
    }

    public static List<BlockMassOverride> getBlockMasses() {
        return BLOCK_MASSES.entrySet().stream()
                .map(entry -> new BlockMassOverride(
                        entry.getKey().dimension(),
                        BlockPos.of(entry.getKey().pos()),
                        entry.getValue()))
                .toList();
    }

    // to get the default sable mass (BYPASS_BLOCK_MASS_OVERRIDES tell to the sable get mass mixin to shut the fck up)
    public static boolean isBypassingBlockMassOverrides() {
        return BYPASS_BLOCK_MASS_OVERRIDES.get();
    }

    static void putBlockMass(final BlockMassKey key, final double mass) {
        BLOCK_MASSES.put(key, requireValidMass(mass));
    }

    static OptionalDouble getBlockMass(final BlockMassKey key) {
        final Double mass = BLOCK_MASSES.get(key);
        return mass == null ? OptionalDouble.empty() : OptionalDouble.of(mass);
    }

    static boolean hasBlockMass(final BlockMassKey key) {
        return BLOCK_MASSES.containsKey(key);
    }

    static boolean discardBlockMass(final BlockMassKey key) {
        PRESERVED_BLOCK_STATE_CHANGE_KEYS.remove(key);
        return BLOCK_MASSES.remove(key) != null;
    }

    private static void restoreSavedMasses(final Level level) {
        if (!level.isClientSide()) {
            final MinecraftServer server = level.getServer();
            if (server != null && restoredServer != server) {
                restoreSavedMasses(server);
            }
        }
    }

    private static void saveBlockMass(final Level level, final BlockMassKey key, final double mass) {
        if (level instanceof final ServerLevel serverLevel) {
            DynamicMassSavedData.get(serverLevel.getServer()).setMass(key, mass);
        }
    }

    private static void removeSavedBlockMass(final Level level, final BlockMassKey key) {
        if (level instanceof final ServerLevel serverLevel) {
            DynamicMassSavedData.get(serverLevel.getServer()).removeMass(key);
        }
    }

    private static double requireValidMass(final double mass) {
        if (!Double.isFinite(mass) || mass < 0.0) {
            throw new IllegalArgumentException("Dynamic block mass must be finite and non-negative.");
        }

        return mass;
    }

    private static double resolveCurrentMass(final Level level, final BlockPos pos, final BlockState state) {
        return state.isAir() ? 0.0 : PhysicsBlockPropertyHelper.getMass(level, pos, state);
    }

    private static double resolveDefaultMass(final Level level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return 0.0;
        }

        BYPASS_BLOCK_MASS_OVERRIDES.set(true);
        try {
            return PhysicsBlockPropertyHelper.getMass(level, pos, state);
        } finally {
            BYPASS_BLOCK_MASS_OVERRIDES.remove();
        }
    }

    // sync is important here because if the cache mass is different of the current dynamic mass, sable freaks out and breaks the sublevel (silly sable)
    private static BlockMassSync syncContainingSubLevelMass(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final double previousMass,
            final double nextMass
    ) {
        final double delta = nextMass - previousMass;
        if (state.isAir() || previousMass == nextMass) {
            return BlockMassSync.unsynced(delta);
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (!(subLevel instanceof final ServerSubLevel serverSubLevel)) {
            return BlockMassSync.unsynced(delta);
        }

        final MassTracker massTracker = serverSubLevel.getSelfMassTracker();
        final double resultingSubLevelMass = massTracker.getMass() + delta;
        if (resultingSubLevelMass <= 0.0) {
            throw new IllegalArgumentException("Dynamic block mass would make the containing sublevel mass invalid.");
        }

        final Vec3 inertia = PhysicsBlockPropertyHelper.getInertia(level, pos, state);
        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.require(level);
        massTracker.addBlockMass(level, state, pos, delta, inertia);
        serverSubLevel.updateMergedMassData((float) physicsSystem.getPartialPhysicsTick());
        physicsSystem.getPipeline().onStatsChanged(serverSubLevel);
        physicsSystem.getPipeline().wakeUp(serverSubLevel); // Navi: "Hello, Link hum... Sublevel! Wake up!"
        return new BlockMassSync(true, delta, resultingSubLevelMass);
    }

    public record BlockMassOverride(ResourceKey<Level> dimension, BlockPos pos, double mass) {
    }

    public record BlockMassChange(
            boolean changed,
            boolean syncedSubLevel,
            double previousMass,
            double currentMass,
            double delta,
            double subLevelMass
    ) {
    }

    private record BlockMassSync(boolean syncedSubLevel, double delta, double subLevelMass) {
        private static BlockMassSync unsynced(final double delta) {
            return new BlockMassSync(false, delta, Double.NaN);
        }
    }

    public record BlockMassKey(ResourceKey<Level> dimension, long pos) {
        public static BlockMassKey from(final Level level, final BlockPos pos) {
            return new BlockMassKey(level.dimension(), pos.asLong());
        }
    }
}
