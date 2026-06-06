package me.yassigame.sable_beyond.mixin.fire;

import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LavaFluid.class)
public abstract class LavaFluidRandomTickMixin {

    @Invoker("hasFlammableNeighbours")
    protected abstract boolean invokeHasFlammableNeighbours(LevelReader level, BlockPos pos);

    @Invoker("isFlammable")
    protected abstract boolean invokeIsFlammable(LevelReader level, BlockPos pos);

    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 0
            )
    )
    private BlockPos sableBeyond$redirectRandomTickOffset(
            BlockPos blockPos,
            int offsetX,
            int offsetY,
            int offsetZ,
            Level level,
            BlockPos pos,
            FluidState state,
            RandomSource random
    ) {
        final BlockPos offsetPos = blockPos.offset(offsetX, offsetY, offsetZ);
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

    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 1
            )
    )
    private BlockPos sableBeyond$redirectRandomTickOffsetForIgnition(
            BlockPos blockPos,
            int offsetX,
            int offsetY,
            int offsetZ,
            Level level,
            BlockPos pos,
            FluidState state,
            RandomSource random
    ) {
        final BlockPos offsetPos = blockPos.offset(offsetX, offsetY, offsetZ);
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
