package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinBlockEntity.class)
public class BasinTankDynamicMass {
    @Inject(method = "tick", at = @At("HEAD"))
    private void sableBeyond$basinTankDynamicMass(CallbackInfo ci) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level = basin.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockPos pos = basin.getBlockPos();
        if (!DynamicMass.isEnabled() || !SableBeyondConfig.dynamicMass().create.basin) {
            DynamicMass.clearBlockMass(level, pos);
            return;
        }

        // if not in sublevel "c'est ciao"
        final SubLevel subLevel = Sable.HELPER.getContaining(basin);
        if (subLevel == null) {
            return;
        }

        float totalAmount = 0;
        for (SmartFluidTankBehaviour tank : basin.getTanks()) {
            totalAmount += tank.getPrimaryTank().getTotalUnits(0);
        }

        if (totalAmount <= 0) {
            DynamicMass.clearBlockMass(level, pos);
            return;
        }

        final double mass = DynamicMass.liquidToMass(totalAmount) + DynamicMass.getDefaultBlockMass(level, pos);
        DynamicMass.setBlockMass(level, pos, mass);
    }
}
