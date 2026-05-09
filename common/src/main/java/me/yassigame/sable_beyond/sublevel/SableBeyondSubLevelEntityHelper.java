package me.yassigame.sable_beyond.sublevel;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class SableBeyondSubLevelEntityHelper {
    private SableBeyondSubLevelEntityHelper() {
    }

    public static boolean isOnSublevelGround(final Entity entity) {
        return getGroundedTrackingSubLevel(entity) != null;
    }

    public static @Nullable ServerSubLevel getGroundedTrackingSubLevel(final Entity entity) {
        if (entity.level().isClientSide || !entity.onGround()) {
            return null;
        }

        if (!(Sable.HELPER.getTrackingSubLevel(entity) instanceof final ServerSubLevel subLevel)) {
            return null;
        }

        return subLevel.isRemoved() ? null : subLevel;
    }
}
