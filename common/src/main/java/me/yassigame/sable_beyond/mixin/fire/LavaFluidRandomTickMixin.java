package me.yassigame.sable_beyond.mixin.fire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LavaFluid.class)
public abstract class LavaFluidRandomTickMixin {

    @Invoker("hasFlammableNeighbours")
    protected abstract boolean invokeHasFlammableNeighbours(LevelReader level, BlockPos pos);

    @Invoker("isFlammable")
    protected abstract boolean invokeIsFlammable(LevelReader level, BlockPos pos);

    @WrapOperation(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 0
            )
    )
    private BlockPos sableBeyond$redirectRandomTickOffset(
            BlockPos instance, int offsetX, int offsetY, int offsetZ, Operation<BlockPos> original, Level level
    ) {
        final BlockPos offsetPos = original.call(instance, offsetX, offsetY, offsetZ);

        if (!SableBeyondConfig.common().fire.lava_fire_on_sublevel) {
            return offsetPos;
        }

        final BlockPos matchingPos = SableSubLevelPosHelper.findMatchingBlockPos(
                level,
                offsetPos,
                (candidateSubLevel, candidatePos) -> {
                    if (!level.isLoaded(candidatePos)) {
                        return false;
                    }

                    final BlockState candidateState = level.getBlockState(candidatePos);
                    return candidateState.blocksMotion() || candidateState.isAir() && this.invokeHasFlammableNeighbours(level, candidatePos);
                }
        );
        return matchingPos != null ? matchingPos : offsetPos;
    }

    @WrapOperation(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 1
            )
    )
    private BlockPos sableBeyond$redirectRandomTickOffsetForIgnition(
            BlockPos instance, int offsetX, int offsetY, int offsetZ, Operation<BlockPos> original, Level level
    ) {
        final BlockPos offsetPos = original.call(instance, offsetX, offsetY, offsetZ);

        if (!SableBeyondConfig.common().fire.lava_fire_on_sublevel) {
            return offsetPos;
        }

        final BlockPos matchingPos = SableSubLevelPosHelper.findMatchingBlockPos(
                level,
                offsetPos,
                (candidateSubLevel, candidatePos) ->
                        level.isLoaded(candidatePos)
                                && level.isEmptyBlock(candidatePos.above())
                                && this.invokeIsFlammable(level, candidatePos)
        );
        return matchingPos != null ? matchingPos : offsetPos;
    }
}
