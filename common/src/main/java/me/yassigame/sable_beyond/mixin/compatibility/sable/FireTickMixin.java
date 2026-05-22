package me.yassigame.sable_beyond.mixin.compatibility.sable;

// if fire in a sublevel submerged in water its gets shut

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// FIXME make fire extinguishes when sable sublevel is in water
@Mixin(value = FireBlock.class, remap = false)
public class FireTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void sableBeyond$FireTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource, CallbackInfo ci) {

    }

}
