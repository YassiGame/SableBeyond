package me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping;

import net.neoforged.neoforge.fluids.FluidStack;

public interface FluidEscapingAccess {

    boolean sableBeyond$isFluidEscapingActive();

    void sableBeyond$setFluidEscapingActive(boolean active);

    boolean sableBeyond$isFluidEscapingFadingOut();

    void sableBeyond$setFluidEscapingFadingOut(boolean fadingOut);

    int sableBeyond$getFluidEscapingTicks();

    void sableBeyond$setFluidEscapingTicks(int ticks);

    int sableBeyond$getFluidEscapingFadeTicks();

    void sableBeyond$setFluidEscapingFadeTicks(int ticks);

    int sableBeyond$getFluidEscapingDelayTicks();

    void sableBeyond$setFluidEscapingDelayTicks(int ticks);

    FluidStack sableBeyond$getLastRenderedEscapingFluid();

    void sableBeyond$setLastRenderedEscapingFluid(FluidStack fluidStack);

    default void sableBeyond$resetFluidEscaping() {
        sableBeyond$setFluidEscapingActive(false);
        sableBeyond$setFluidEscapingFadingOut(false);
        sableBeyond$setFluidEscapingTicks(0);
        sableBeyond$setFluidEscapingFadeTicks(0);
        sableBeyond$setFluidEscapingDelayTicks(0);
        sableBeyond$setLastRenderedEscapingFluid(FluidStack.EMPTY);
    }
}
