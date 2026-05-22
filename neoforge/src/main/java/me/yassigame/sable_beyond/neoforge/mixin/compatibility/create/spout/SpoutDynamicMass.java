package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.spout;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SpoutBlockEntity.class)
public abstract class SpoutDynamicMass {

    @Shadow
    protected abstract FluidStack getCurrentFluidInTank();

    @Inject(method = "tick", at = @At("HEAD"))
    private void sableBeyond$spoutDynamicMass(CallbackInfo ci) {
        SpoutBlockEntity spout = (SpoutBlockEntity) (Object) this;
        Level level = spout.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!DynamicMass.isEnabled() || !SableBeyondConfig.dynamicMass().create.spout) {
            DynamicMass.clearBlockMass(level, spout.getBlockPos());
            return;
        }

        BlockPos pos = spout.getBlockPos();
        // if not in sublevel "c'est ciao"
        final SubLevel subLevel = Sable.HELPER.getContaining(spout);
        if (subLevel == null) {
            return;
        }

        FluidStack fluidStack = this.getCurrentFluidInTank();
        double defaultMass = DynamicMass.getDefaultBlockMass(level, pos);
        double amount = fluidStack.getAmount();

        if (amount <= 0) {
            DynamicMass.clearBlockMass(level, pos);
            return;
        }

        double mass = DynamicMass.liquidToMass(amount) + defaultMass;
        //System.out.println(mass);
        DynamicMass.setBlockMass(level, pos, mass);
    }
}
