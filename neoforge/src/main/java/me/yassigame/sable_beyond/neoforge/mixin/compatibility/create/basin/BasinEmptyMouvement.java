package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping.FluidEscaping;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping.FluidEscapingAccess;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinBlockEntity.class)
public class BasinEmptyMouvement implements FluidEscapingAccess, BlockEntitySubLevelActor {
    @Unique
    private boolean sableBeyond$fluidEscapingActive;

    @Unique
    private int sableBeyond$fluidEscapingTicks;

    @Unique
    private int sableBeyond$fluidEscapingFadeTicks;

    @Unique
    private int sableBeyond$fluidEscapingDelayTicks;

    @Unique
    private boolean sableBeyond$fluidEscapingFadingOut;

    @Unique
    private FluidStack sableBeyond$lastRenderedEscapingFluid = FluidStack.EMPTY;

    @Unique
    private FluidStack sableBeyond$pendingWholeBucketTransfer = FluidStack.EMPTY;

    @Unique
    private int sableBeyond$pendingWholeBucketTransferDrainedAmount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void sableBeyond$basinEmptyMouvement(CallbackInfo ci) {
        final BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        final Level level = basin.getLevel();
        if (level == null) {
            return;
        }

        FluidEscaping.tick(basin, this);
        if (level.isClientSide()) {
            return;
        }

        if (FluidEscaping.getContainingSubLevel(level, basin.getBlockPos()) instanceof ServerSubLevel) {
            return;
        }

        sableBeyond$serverTick(basin, level);
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        final BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        final Level level = basin.getLevel() != null ? basin.getLevel() : subLevel.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        sableBeyond$serverTick(basin, level);
    }

    @Unique
    private void sableBeyond$serverTick(BasinBlockEntity basin, Level level) {
        final BlockPos blockPos = basin.getBlockPos();

        // Fill the basin from a source above it in the real world when its opening faces up.
        final SableSubLevelPosHelper.ResolvedLevelPos realBasin = SableSubLevelPosHelper.resolveRealLevelPos(level, blockPos);
        final BlockPos sourcePos = realBasin.pos().above();
        final FluidState sourceState = level.getFluidState(sourcePos);
        if (FluidEscaping.isFluidRightSideUpEnough(level, blockPos) && sourceState.isSource() && SableBeyondNeoForgeConfig.current().compatibility.create.basin.fill_from_world_fluid) {
            final Fluid sourceFluid = FluidHelper.convertToStill(sourceState.getType());
            final FluidStack sourceBucket = new FluidStack(sourceFluid, FluidEscaping.SOURCE_BLOCK_AMOUNT);
            if (FluidEscaping.tryFillBasin(basin, sourceBucket)) {
                final BlockState sourceBlockState = level.getBlockState(sourcePos);
                if (!sourceBlockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        if (!FluidEscaping.isUpsideDownEnough(level, basin.getBlockPos())) {
            sableBeyond$resetFluidTransferState();
            return;
        }

        final Vec3 motion = FluidEscaping.getBasinOpeningMotion(level, blockPos);
        if (SableBeyondNeoForgeConfig.current().compatibility.create.basin.empty_input_inventory) {
            dumpInventory(level, blockPos, basin.getInputInventory(), motion);
        }
        if (SableBeyondNeoForgeConfig.current().compatibility.create.basin.empty_output_inventory) {
            dumpInventory(level, blockPos, basin.getOutputInventory(), motion);
        }

        // Fluid Logic
        if (!SableBeyondNeoForgeConfig.current().compatibility.create.basin.fluid_escaping) {
            sableBeyond$resetFluidEscaping();
            return;
        }

        if (!FluidEscaping.isFluidUpsideDownEnough(level, blockPos) || !FluidEscaping.hasAnyFluid(basin)) {
            sableBeyond$resetFluidTransferState();
            return;
        }

        if (!FluidEscaping.isDistanceEnough(level, blockPos)) {
            sableBeyond$resetFluidTransferState();
            return;
        }

        if (!sableBeyond$prepareFluidDrainSelection(basin)) {
            return;
        }

        if (!FluidEscaping.tickStartupDelay(this)) {
            FluidEscaping.resetAnimation(this);
            sableBeyond$resetPendingWholeBucketTransfer();
            return;
        }

        if (FluidEscaping.getRealWorldDownDistance(level, blockPos) > FluidEscaping.MAX_FLUID_RAYCAST_DISTANCE) {
            sableBeyond$resetPendingWholeBucketTransfer();
            sableBeyond$drainProgressively(basin);
            return;
        }

        if (sableBeyond$hasPendingWholeBucketTransfer()) {
            sableBeyond$continueWholeBucketTransfer(basin, level, blockPos);
            return;
        }

        final FluidStack exactSourceBucket = sableBeyond$getExactEscapingSourceBucket(basin);
        if (!exactSourceBucket.isEmpty()) {
            this.sableBeyond$pendingWholeBucketTransfer = exactSourceBucket;
            this.sableBeyond$pendingWholeBucketTransferDrainedAmount = 0;
            sableBeyond$continueWholeBucketTransfer(basin, level, blockPos);
            return;
        }

        sableBeyond$resetPendingWholeBucketTransfer();
        sableBeyond$drainProgressively(basin);
    }

    @Unique
    private void sableBeyond$resetFluidTransferState() {
        sableBeyond$resetPendingWholeBucketTransfer();
        sableBeyond$resetFluidEscaping();
    }

    @Unique
    private static void dumpInventory(Level level, BlockPos pos, IItemHandlerModifiable inventory, Vec3 motion) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            final ItemEntity item = new ItemEntity(
                    level,
                    pos.getX() + 0.5d,
                    pos.getY() + 0.95d,
                    pos.getZ() + 0.5d,
                    stack.copy()
            );
            item.setDeltaMovement(motion);
            level.addFreshEntity(item);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Unique
    private static FluidStack sableBeyond$drainFluid(BasinBlockEntity basin, int maxDrain) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        final FluidStack escapingFluid = FluidEscaping.findEscapingFluid(basin);
        if (escapingFluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        for (SmartFluidTankBehaviour behaviour : basin.getTanks()) {
            if (behaviour == null) {
                continue;
            }

            final IFluidHandler handler = behaviour.getCapability();
            if (handler == null) {
                continue;
            }

            for (int tank = 0; tank < handler.getTanks(); tank++) {
                final FluidStack fluidInTank = handler.getFluidInTank(tank);
                if (fluidInTank.isEmpty()) {
                    continue;
                }
                if (!FluidStack.isSameFluidSameComponents(fluidInTank, escapingFluid)) {
                    continue;
                }

                final int drainAmount = Math.min(maxDrain, fluidInTank.getAmount());
                return handler.drain(FluidHelper.copyStackWithAmount(fluidInTank, drainAmount), FluidAction.EXECUTE);
            }
        }

        return FluidStack.EMPTY;
    }

    @Unique
    private void sableBeyond$drainProgressively(BasinBlockEntity basin) {
        final FluidStack drainedFluid = sableBeyond$drainFluid(basin, FluidEscaping.FLUID_DRAIN_PER_TICK);
        if (!FluidEscaping.hasAnyFluid(basin)) {
            sableBeyond$resetFluidTransferState();
            return;
        }

        final FluidStack nextEscapingFluid = FluidEscaping.findEscapingFluid(basin);
        if (!drainedFluid.isEmpty()
                && !nextEscapingFluid.isEmpty()
                && !FluidStack.isSameFluidSameComponents(drainedFluid, nextEscapingFluid)) {
            sableBeyond$resetPendingWholeBucketTransfer();
            FluidEscaping.resetAnimation(this);
            sableBeyond$setFluidEscapingDelayTicks(0);
            basin.notifyUpdate();
        }
    }

    @Unique
    private boolean sableBeyond$prepareFluidDrainSelection(BasinBlockEntity basin) {
        final FluidStack escapingFluid = FluidEscaping.findEscapingFluid(basin);
        if (escapingFluid.isEmpty()) {
            sableBeyond$resetFluidTransferState();
            return false;
        }

        final FluidStack previousFluid = this.sableBeyond$lastRenderedEscapingFluid;
        this.sableBeyond$lastRenderedEscapingFluid = escapingFluid.copy();
        if (previousFluid.isEmpty() || FluidStack.isSameFluidSameComponents(previousFluid, escapingFluid)) {
            return true;
        }

        sableBeyond$resetPendingWholeBucketTransfer();
        FluidEscaping.resetAnimation(this);
        sableBeyond$setFluidEscapingDelayTicks(0);
        return false;
    }

    @Unique
    private static FluidStack sableBeyond$getExactEscapingSourceBucket(BasinBlockEntity basin) {
        final FluidStack escapingFluid = FluidEscaping.findEscapingFluid(basin);
        if (escapingFluid.isEmpty() || escapingFluid.getAmount() != FluidEscaping.SOURCE_BLOCK_AMOUNT) {
            return FluidStack.EMPTY;
        }

        return escapingFluid;
    }

    @Unique
    private void sableBeyond$continueWholeBucketTransfer(BasinBlockEntity basin, Level level, BlockPos blockPos) {
        if (!sableBeyond$hasPendingWholeBucketTransfer() || !sableBeyond$isPendingWholeBucketTransferValid(basin)) {
            sableBeyond$resetPendingWholeBucketTransfer();
            return;
        }

        final FluidStack drainedFluid = sableBeyond$drainFluid(basin, FluidEscaping.FLUID_DRAIN_PER_TICK);
        if (drainedFluid.isEmpty()
                || !FluidStack.isSameFluidSameComponents(drainedFluid, this.sableBeyond$pendingWholeBucketTransfer)) {
            sableBeyond$resetPendingWholeBucketTransfer();
            return;
        }

        this.sableBeyond$pendingWholeBucketTransferDrainedAmount += drainedFluid.getAmount();
        if (this.sableBeyond$pendingWholeBucketTransferDrainedAmount < FluidEscaping.SOURCE_BLOCK_AMOUNT) {
            if (!sableBeyond$isPendingWholeBucketTransferValid(basin)) {
                sableBeyond$resetPendingWholeBucketTransfer();
            }
            return;
        }

        sableBeyond$placeFluidAtRayHit(level, blockPos, this.sableBeyond$pendingWholeBucketTransfer.copy());
        sableBeyond$resetPendingWholeBucketTransfer();
        if (!FluidEscaping.hasAnyFluid(basin)) {
            sableBeyond$resetFluidTransferState();
            return;
        }

        FluidEscaping.resetAnimation(this);
        sableBeyond$setFluidEscapingDelayTicks(0);
        basin.notifyUpdate();
    }

    @Unique
    private boolean sableBeyond$hasPendingWholeBucketTransfer() {
        return !this.sableBeyond$pendingWholeBucketTransfer.isEmpty()
                && this.sableBeyond$pendingWholeBucketTransfer.getAmount() == FluidEscaping.SOURCE_BLOCK_AMOUNT;
    }

    @Unique
    private void sableBeyond$resetPendingWholeBucketTransfer() {
        this.sableBeyond$pendingWholeBucketTransfer = FluidStack.EMPTY;
        this.sableBeyond$pendingWholeBucketTransferDrainedAmount = 0;
    }

    @Unique
    private boolean sableBeyond$isPendingWholeBucketTransferValid(BasinBlockEntity basin) {
        if (!sableBeyond$hasPendingWholeBucketTransfer()) {
            return false;
        }

        final FluidStack escapingFluid = FluidEscaping.findEscapingFluid(basin);
        if (escapingFluid.isEmpty()) {
            return this.sableBeyond$pendingWholeBucketTransferDrainedAmount == FluidEscaping.SOURCE_BLOCK_AMOUNT;
        }

        return FluidStack.isSameFluidSameComponents(escapingFluid, this.sableBeyond$pendingWholeBucketTransfer)
                && escapingFluid.getAmount() + this.sableBeyond$pendingWholeBucketTransferDrainedAmount
                == FluidEscaping.SOURCE_BLOCK_AMOUNT;
    }

    @Unique
    private static boolean sableBeyond$placeFluidAtRayHit(Level level, BlockPos basinPos, FluidStack spilledFluid) {
        if (spilledFluid.isEmpty()) {
            return false;
        }

        final BlockHitResult hit = FluidEscaping.raycastRealWorldDown(level, basinPos);
        if (hit == null) {
            return false;
        }

        if (sableBeyond$tryFillRaycastedFluidHandler(level, hit, spilledFluid)) {
            return true;
        }

        final BlockPos targetPos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.isLoaded(targetPos)) {
            return false;
        }

        final Fluid stillFluid = FluidHelper.convertToStill(spilledFluid.getFluid());
        if (!(stillFluid instanceof FlowingFluid) || !FluidHelper.hasBlockState(stillFluid)) {
            return false;
        }

        final BlockState targetState = level.getBlockState(targetPos);
        final FluidState existingFluid = targetState.getFluidState();
        if (!existingFluid.isEmpty()) {
            return FluidHelper.convertToStill(existingFluid.getType()) == stillFluid;
        }

        if (!level.getFluidState(targetPos.below()).isEmpty()) {
            level.setBlock(targetPos.below(), stillFluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            return true;
        }

        if (targetState.hasProperty(BlockStateProperties.WATERLOGGED) && FluidHelper.isWater(stillFluid)) {
            level.setBlock(targetPos, targetState.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL);
            return true;
        }

        if (!targetState.canBeReplaced()) {
            return false;
        }

        level.setBlock(targetPos, stillFluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        return true;
    }

    @Unique
    private static boolean sableBeyond$tryFillRaycastedFluidHandler(Level level, BlockHitResult hit, FluidStack spilledFluid) {
        final BlockPos hitPos = hit.getBlockPos();
        if (!level.isLoaded(hitPos)) {
            return false;
        }

        IFluidHandler handler = sableBeyond$getFluidHandler(level, hitPos, hit.getDirection());
        if (handler == null) {
            handler = sableBeyond$getFluidHandler(level, hitPos, null);
        }
        if (handler == null) {
            return false;
        }

        handler.fill(spilledFluid.copy(), FluidAction.EXECUTE);
        return true;
    }

    @Unique
    private static IFluidHandler sableBeyond$getFluidHandler(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    @Override
    public boolean sableBeyond$isFluidEscapingActive() {
        return this.sableBeyond$fluidEscapingActive;
    }

    @Override
    public void sableBeyond$setFluidEscapingActive(boolean active) {
        this.sableBeyond$fluidEscapingActive = active;
    }

    @Override
    public boolean sableBeyond$isFluidEscapingFadingOut() {
        return this.sableBeyond$fluidEscapingFadingOut;
    }

    @Override
    public void sableBeyond$setFluidEscapingFadingOut(boolean fadingOut) {
        this.sableBeyond$fluidEscapingFadingOut = fadingOut;
    }

    @Override
    public int sableBeyond$getFluidEscapingTicks() {
        return this.sableBeyond$fluidEscapingTicks;
    }

    @Override
    public void sableBeyond$setFluidEscapingTicks(int ticks) {
        this.sableBeyond$fluidEscapingTicks = ticks;
    }

    @Override
    public int sableBeyond$getFluidEscapingFadeTicks() {
        return this.sableBeyond$fluidEscapingFadeTicks;
    }

    @Override
    public void sableBeyond$setFluidEscapingFadeTicks(int ticks) {
        this.sableBeyond$fluidEscapingFadeTicks = ticks;
    }

    @Override
    public int sableBeyond$getFluidEscapingDelayTicks() {
        return this.sableBeyond$fluidEscapingDelayTicks;
    }

    @Override
    public void sableBeyond$setFluidEscapingDelayTicks(int ticks) {
        this.sableBeyond$fluidEscapingDelayTicks = ticks;
    }

    @Override
    public FluidStack sableBeyond$getLastRenderedEscapingFluid() {
        return this.sableBeyond$lastRenderedEscapingFluid;
    }

    @Override
    public void sableBeyond$setLastRenderedEscapingFluid(FluidStack fluidStack) {
        this.sableBeyond$lastRenderedEscapingFluid = fluidStack;
    }
}
