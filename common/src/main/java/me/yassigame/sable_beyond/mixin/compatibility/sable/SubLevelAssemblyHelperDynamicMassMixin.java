package me.yassigame.sable_beyond.mixin.compatibility.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keeps the Custom Mass
@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class SubLevelAssemblyHelperDynamicMassMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false)
    private static void sableBeyond$moveDynamicMasses(
            final ServerLevel level,
            final SubLevelAssemblyHelper.AssemblyTransform transform,
            final Iterable<BlockPos> blocks,
            final CallbackInfo ci
    ) {
        final ServerLevel resultingLevel = transform.getLevel();
        for (final BlockPos block : blocks) {
            DynamicMass.moveBlockMass(level, block, resultingLevel, transform.apply(block));
        }
    }
}
