package me.yassigame.sable_beyond.utils;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class SableSubLevelPosHelper {

    public static ResolvedLevelPos resolveRealLevelPos(final Level level, final BlockPos pos) {
        final var projectedCenter = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel == null && BlockPos.containing(projectedCenter).equals(pos)) {
            return new ResolvedLevelPos(level, pos.immutable());
        }
        return new ResolvedLevelPos(level, BlockPos.containing(projectedCenter));
    }

    public static BlockPos resolveRealBlockPos(final Level level, final BlockPos pos) {
        return resolveRealLevelPos(level, pos).pos();
    }

    public static <T> @Nullable T runIncludingSubLevels(
            final Level level,
            final BlockPos pos,
            final BiFunction<@Nullable SubLevelAccess, BlockPos, T> converter
    ) {
        final var subLevel = Sable.HELPER.getContaining(level, pos);
        return Sable.HELPER.runIncludingSubLevels(level, Vec3.atCenterOf(pos), true, subLevel, converter);
    }

    public static boolean findIncludingSubLevels(
            final Level level,
            final BlockPos pos,
            final BiFunction<@Nullable SubLevelAccess, BlockPos, Boolean> converter
    ) {
        final var subLevel = Sable.HELPER.getContaining(level, pos);
        return Sable.HELPER.findIncludingSubLevels(level, Vec3.atCenterOf(pos), true, subLevel, converter);
    }

    public static @Nullable BlockPos findMatchingBlockPos(
            final Level level,
            final BlockPos pos,
            final BiFunction<@Nullable SubLevelAccess, BlockPos, Boolean> matcher
    ) {
        return runIncludingSubLevels(
                level,
                pos,
                (candidateSubLevel, candidatePos) -> Boolean.TRUE.equals(matcher.apply(candidateSubLevel, candidatePos))
                        ? candidatePos
                        : null
        );
    }

    public record ResolvedLevelPos(Level level, BlockPos pos) {
    }
}
