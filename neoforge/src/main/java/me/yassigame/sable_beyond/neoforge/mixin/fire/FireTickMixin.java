package me.yassigame.sable_beyond.neoforge.mixin.fire;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Invoker;

// THX NEOFORGE BY ADDING A DIRECTION ON checkBurnOut AND canCatchFire NEEDED TO MAKE TWO DIFERENT MIXINS FOR THE SAME FEATURE

@Mixin(FireBlock.class)
public abstract class FireTickMixin {

    @Invoker("canCatchFire")
    protected abstract boolean invokeCanCatchFire(BlockGetter level, BlockPos pos, Direction face);

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;ILnet/minecraft/core/Direction;)V"
            )
    )
    private void sableBeyond$redirectCheckBurnOut(
            FireBlock instance, Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face, Operation<Void> original
    ) {
        final BlockPos resolvedPos = SableSubLevelPosHelper.findMatchingBlockPos(
                level,
                pos,
                (candidateSubLevel, candidatePos) -> this.invokeCanCatchFire(level, candidatePos, face)
        );

        original.call(instance, level, resolvedPos != null ? resolvedPos : pos, chance, random, age, face);
    }
}
