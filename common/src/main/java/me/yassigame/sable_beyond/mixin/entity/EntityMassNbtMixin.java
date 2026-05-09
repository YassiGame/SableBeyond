package me.yassigame.sable_beyond.mixin.entity;

import me.yassigame.sable_beyond.api.entity.SableBeyondEntityApi;
import me.yassigame.sable_beyond.api.entity.SableBeyondEntityMassNbtAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Entity.class)
public abstract class EntityMassNbtMixin implements SableBeyondEntityMassNbtAccess {
    private CompoundTag massNbt = new CompoundTag();

    @Override
    public CompoundTag getMassNbt() {
        return this.massNbt;
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void sableBeyond$loadMassNbt(final CompoundTag compoundTag, final CallbackInfo ci) {
        this.massNbt = new CompoundTag();

        if (compoundTag.contains(SableBeyondEntityApi.MASS_NBT_TAG, Tag.TAG_COMPOUND)) {
            this.massNbt = compoundTag.getCompound(SableBeyondEntityApi.MASS_NBT_TAG).copy();
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void sableBeyond$saveMassNbt(final CompoundTag compoundTag, final CallbackInfoReturnable<CompoundTag> cir) {
        if (this.massNbt.isEmpty()) {
            return;
        }

        cir.getReturnValue().put(SableBeyondEntityApi.MASS_NBT_TAG, this.massNbt.copy());
    }
}
