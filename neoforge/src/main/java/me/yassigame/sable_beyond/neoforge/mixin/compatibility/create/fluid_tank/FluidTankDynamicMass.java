package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.fluid_tank;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.config.DynamicMassConfig;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO add config to this
@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankDynamicMass {

    @Shadow
    public abstract FluidTankBlockEntity getControllerBE();

    @Shadow
    public abstract boolean isController();

    @Inject(method = "tick", at = @At("HEAD"))
    private void sableBeyond$fluidTankDynamicMass(CallbackInfo ci) {
        FluidTankBlockEntity fluidTank = (FluidTankBlockEntity) (Object) this;
        final Level level = fluidTank.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!fluidTank.isController()) {
            return;
        }

        // if not in sublevel "c'est ciao"
        final SubLevel subLevel = Sable.HELPER.getContaining(fluidTank);
        if (subLevel == null) {
            return;
        }

        if (!DynamicMass.isEnabled() || !SableBeyondConfig.dynamicMass().create.fluid_tank) {
            clearTankMasses(level, fluidTank);
            return;
        }

        int amount = fluidTank.getFluid(0).getAmount();
        if (amount == 0) {
            clearTankMasses(level, fluidTank);
            return;
        }

        int width = fluidTank.getWidth();
        int height = fluidTank.getHeight();
        BlockPos origin = fluidTank.getBlockPos();

        double fillState = fluidTank.getFillState(); // 0.0 -> 1.0
        double capacityPerBlock = FluidTankBlockEntity.getCapacityMultiplier();
        double fullBlockFluidMass = DynamicMass.liquidToMass(capacityPerBlock);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    double filledHeight = fillState * height;
                    double cellFill = Math.max(0.0, Math.min(1.0, filledHeight - y));

                    double defaultMass = DynamicMass.getDefaultBlockMass(level, pos, state);
                    double fluidMass = fullBlockFluidMass * cellFill;

                    if (fluidMass <= 0.0001) {
                        DynamicMass.clearBlockMass(level, pos);
                    } else {
                        DynamicMass.setBlockMass(level, pos, defaultMass + fluidMass);
                    }
                }
            }
        }
    }

    private void clearTankMasses(final Level level, final FluidTankBlockEntity fluidTank) {
        final FluidTankBlockEntity controller = fluidTank.isController() ? fluidTank : fluidTank.getControllerBE();
        if (controller == null) {
            DynamicMass.clearBlockMass(level, fluidTank.getBlockPos());
            return;
        }

        final int width = Math.max(1, controller.getWidth());
        final int height = Math.max(1, controller.getHeight());
        final BlockPos origin = controller.getBlockPos();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    DynamicMass.clearBlockMass(level, origin.offset(x, y, z));
                }
            }
        }
    }
}
