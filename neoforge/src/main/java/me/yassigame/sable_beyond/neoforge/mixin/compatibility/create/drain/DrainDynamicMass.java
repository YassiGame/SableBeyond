package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.drain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemDrainBlockEntity.class)
public abstract class DrainDynamicMass {
    @Shadow
    private SmartFluidTankBehaviour internalTank;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sableBeyond$drainDynamicMass(final CallbackInfo ci) {
        final ItemDrainBlockEntity drain = (ItemDrainBlockEntity) (Object) this;
        final Level level = drain.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = drain.getBlockPos();

        // if not in sublevel "c'est ciao"
        final SubLevel subLevel = Sable.HELPER.getContaining(drain);
        if (subLevel == null) {
            return;
        }

        if (!DynamicMass.isEnabled() || !SableBeyondConfig.dynamicMass().create.drain) {
            DynamicMass.clearBlockMass(level, pos);
            return;
        }

        final int amount = internalTank == null ? 0 : internalTank.getPrimaryHandler().getFluidAmount();
        if (amount <= 0) {
            DynamicMass.clearBlockMass(level, pos);
            return;
        }

        final double defaultMass = DynamicMass.getDefaultBlockMass(level, pos);
        DynamicMass.setBlockMass(level, pos, defaultMass + DynamicMass.liquidToMass(amount));
    }
}
