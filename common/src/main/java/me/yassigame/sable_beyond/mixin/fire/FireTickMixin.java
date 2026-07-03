package me.yassigame.sable_beyond.mixin.fire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireTickMixin {

    @Invoker("isValidFireLocation")
    protected abstract boolean invokeIsValidFireLocation(BlockGetter level, BlockPos pos);

    @Invoker("getIgniteOdds")
    protected abstract int invokeGetIgniteOdds(LevelReader level, BlockPos pos);

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void sableBeyond$extinguishSubLevelFireInWater(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (!SableBeyondConfig.common().fire.fire_extinguish) {
            return;
        }

        if (this.hasBlockingOverlapAcrossSubLevels(level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void sableBeyond$allowCrossSubLevelFireSupport(
            final BlockState state,
            final LevelReader level,
            final BlockPos pos,
            final CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof Level actualLevel)) {
            return;
        }

        if (this.hasBlockingOverlapAcrossSubLevels(actualLevel, pos) && SableBeyondConfig.common().fire.fire_extinguish) {
            cir.setReturnValue(false);
            return;
        }

        if (this.isValidFireLocationAcrossSubLevels(actualLevel, pos) && SableBeyondConfig.common().fire.fire_spreading) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;isValidFireLocation(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean sableBeyond$redirectValidFireLocation(
            FireBlock instance,
            BlockGetter level,
            BlockPos pos,
            Operation<Boolean> original
    ) {
        if (!SableBeyondConfig.common().fire.fire_spreading) {
            return original.call(instance, level, pos);
        }

        if (level instanceof Level actualLevel) {
            return this.isValidFireLocationAcrossSubLevels(actualLevel, pos);
        }

        return original.call(instance, level, pos);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"
            )
    )
    private int sableBeyond$redirectGetIgniteOdds(
            FireBlock instance, LevelReader level, BlockPos pos, Operation<Integer> original
    ) {
        if (!SableBeyondConfig.common().fire.fire_spreading) {
            return original.call(instance, level, pos);
        }

        if (level instanceof Level actualLevel) {
            final BlockPos resolvedPos = SableSubLevelPosHelper.findMatchingBlockPos(
                    actualLevel,
                    pos,
                    (candidateSubLevel, candidatePos) -> this.invokeGetIgniteOdds(level, candidatePos) > 0
            );

            if (resolvedPos != null) {
                if (pos instanceof BlockPos.MutableBlockPos mutablePos) {
                    mutablePos.set(resolvedPos);
                }

                return original.call(instance, level, resolvedPos);
            }
        }

        return original.call(instance, level, pos);
    }

    @Unique
    private boolean isValidFireLocationAcrossSubLevels(final Level level, final BlockPos pos) {
        return SableSubLevelPosHelper.findIncludingSubLevels(
                level,
                pos,
                (candidateSubLevel, candidatePos) -> this.invokeIsValidFireLocation(level, candidatePos)
        );
    }

    @Unique
    private boolean hasBlockingOverlapAcrossSubLevels(final Level level, final BlockPos pos) {
        return SableSubLevelPosHelper.findMatchingBlockPos(
                level,
                pos,
                (candidateSubLevel, candidatePos) -> {
                    if (candidatePos.equals(pos)) {
                        return false;
                    }

                    return level.getFluidState(candidatePos).is(FluidTags.WATER) || level.getBlockState(candidatePos).isSolid();
                }
        ) != null;
    }
}
