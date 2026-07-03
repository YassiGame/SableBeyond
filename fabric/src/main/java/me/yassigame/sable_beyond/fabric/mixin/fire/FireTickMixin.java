package me.yassigame.sable_beyond.fabric.mixin.fire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public abstract class FireTickMixin {

    @Invoker("getBurnOdds")
    protected abstract int invokeGetBurnOdds(BlockState state);

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V"
            )
    )
    private void sableBeyond$redirectCheckBurnOut(
            FireBlock instance, Level level, BlockPos pos, int chance, RandomSource random, int age, Operation<Void> original
    ) {
        if (!SableBeyondConfig.common().fire.fire_spreading) {
            return;
        }

        final BlockPos resolvedPos = SableSubLevelPosHelper.findMatchingBlockPos(
                level,
                pos,
                (candidateSubLevel, candidatePos) -> this.invokeGetBurnOdds(level.getBlockState(candidatePos)) > 0
        );

        original.call(instance, level, resolvedPos != null ? resolvedPos : pos, chance, random, age);
    }
}
