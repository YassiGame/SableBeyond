package me.yassigame.sable_beyond.api.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class SableBeyondEntityApi {
    public static final String MASS_NBT_TAG = "sable_beyond";

    private SableBeyondEntityApi() {
    }

    public static CompoundTag getMassNbt(final Entity entity) {
        if (entity instanceof final SableBeyondEntityMassNbtAccess massNbtAccess) {
            return massNbtAccess.getMassNbt();
        }

        throw new IllegalStateException("Entity " + entity.getType() + " does not implement SableBeyondEntityMassNbtAccess.");
    }
}
