package me.yassigame.sable_beyond.mixin.world;

import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Removes dynamic mass while the old block state is still available to update its center of mass correctly.
@Mixin(LevelChunk.class)
public abstract class LevelDynamicMassMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void sableBeyond$clearDynamicMassBeforeBlockChanges(
            final BlockPos pos,
            final BlockState newState,
            final boolean isMoving,
            final CallbackInfoReturnable<BlockState> cir
    ) {
        if (this.level.isClientSide()) {
            return;
        }

        final BlockState oldState = this.level.getBlockState(pos);
        if (oldState.getBlock() == newState.getBlock()) {
            return;
        }

        DynamicMass.clearBlockMassBeforeBlockChange(this.level, pos);
    }
}
