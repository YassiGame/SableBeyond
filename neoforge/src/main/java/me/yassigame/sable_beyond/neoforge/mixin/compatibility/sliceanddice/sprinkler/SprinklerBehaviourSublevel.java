package me.yassigame.sable_beyond.neoforge.mixin.compatibility.sliceanddice.sprinkler;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "com.possible_triangle.sliceanddice.block.sprinkler.SprinkleBehaviour$Companion")
public class SprinklerBehaviourSublevel {

    // easy fix
    @ModifyVariable(method = "actAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos sableBeyond$changePos(
            BlockPos original,
            BlockPos pos,
            ServerLevel world,
            FluidStack fluid,
            RandomSource random
    ) {
        final SubLevel spoutSubLevel = Sable.HELPER.getContaining(world, pos);
        if (!(spoutSubLevel instanceof ServerSubLevel serverSubLevel)) {
            return original;
        }

        Vec3 globalCenter = serverSubLevel.logicalPose().transformPosition(Vec3.atCenterOf(original));
        return BlockPos.containing(globalCenter);
    }
}
