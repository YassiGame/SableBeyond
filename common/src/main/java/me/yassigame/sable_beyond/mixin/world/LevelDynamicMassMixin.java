package me.yassigame.sable_beyond.mixin.world;

import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// For removing old block mass on block change
@Mixin(Level.class)
public abstract class LevelDynamicMassMixin {
    @Inject(method = "onBlockStateChange", at = @At("TAIL"))
    private void sableBeyond$discardDynamicMassWhenBlockChanges(
            final BlockPos pos,
            final BlockState oldState,
            final BlockState newState,
            final CallbackInfo ci
    ) {
        if (oldState.getBlock() == newState.getBlock()) {
            return;
        }

        DynamicMass.discardBlockMassAfterBlockChange((Level) (Object) this, pos);
    }
}
