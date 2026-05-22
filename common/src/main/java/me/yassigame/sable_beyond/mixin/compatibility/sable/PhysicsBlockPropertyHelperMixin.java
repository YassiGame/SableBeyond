package me.yassigame.sable_beyond.mixin.compatibility.sable;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalDouble;

// Injects the dynamic mass logic (the best solution i thought for)
@Mixin(value = PhysicsBlockPropertyHelper.class, remap = false)
public abstract class PhysicsBlockPropertyHelperMixin {
    @Inject(method = "getMass", at = @At("HEAD"), cancellable = true)
    private static void sableBeyond$getDynamicBlockMass(
            final BlockGetter level,
            final BlockPos pos,
            final BlockState state,
            final CallbackInfoReturnable<Double> cir
    ) {
        if (DynamicMass.isBypassingBlockMassOverrides()) {
            return;
        }

        final OptionalDouble mass = DynamicMass.getBlockMass(level, pos);
        if (mass.isPresent()) {
            cir.setReturnValue(mass.getAsDouble());
        }
    }
}
