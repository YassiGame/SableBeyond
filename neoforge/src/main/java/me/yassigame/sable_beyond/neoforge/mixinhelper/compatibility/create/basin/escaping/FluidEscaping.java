package me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public final class FluidEscaping {
    public static final int ANIMATION_DURATION_TICKS = 10;
    public static final int FADE_OUT_DURATION_TICKS = 5;
    public static final int START_DELAY_TICKS = 5;
    public static final int FLUID_DRAIN_PER_TICK = 50;
    public static final int SOURCE_BLOCK_AMOUNT = 1000; // 1 bucket to prevent fluid duplication
    public static final double WORLD_RAY_START_Y = 1.01d;
    public static final double WORLD_RAY_START_OFFSET = 1.0d / 32.0d;
    public static final double WORLD_RAY_END_PADDING = 1.0d;
    public static final double MAX_FLUID_RAYCAST_DISTANCE = 30.0d;
    public static final double UPSIDE_DOWN_DOT_THRESHOLD = -0.5d;
    public static final double FLUID_UPSIDE_DOWN_DOT_THRESHOLD = -0.9d;
    public static final double FLUID_RIGHT_SIDE_UP_DOT_THRESHOLD = 0.8d;

    private static final float STREAM_START_Y_CLOSED = 10.0f / 16.0f;
    private static final float STREAM_START_Y_OPEN = 14.0f / 16.0f;
    private static final int STREAM_PARTICLE_COUNT = 10;
    private static final int IMPACT_PARTICLE_COUNT = 4;
    private static final double STREAM_PARTICLE_JITTER = 1.0d / 16.0d;

    public static void tick(BasinBlockEntity basin, FluidEscapingAccess access) {
        final Level level = basin.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }

        final FluidStack renderedFluid = findEscapingFluid(basin);
        if (!shouldAnimate(basin) || renderedFluid.isEmpty()) {
            tickFadeOut(access);
            return;
        }

        final FluidStack previousFluid = access.sableBeyond$getLastRenderedEscapingFluid();
        if (!previousFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(previousFluid, renderedFluid)) {
            if (access.sableBeyond$isFluidEscapingActive() || access.sableBeyond$isFluidEscapingFadingOut()) {
                tickFadeOut(access);
                return;
            }

            access.sableBeyond$resetFluidEscaping();
            return;
        }

        if (access.sableBeyond$isFluidEscapingFadingOut()) {
            access.sableBeyond$resetFluidEscaping();
        }

        access.sableBeyond$setLastRenderedEscapingFluid(renderedFluid.copy());
        if (!tickStartupDelay(access)) {
            resetAnimation(access);
            return;
        }

        access.sableBeyond$setFluidEscapingActive(true);
        access.sableBeyond$setFluidEscapingTicks(Math.min(
                access.sableBeyond$getFluidEscapingTicks() + 1,
                ANIMATION_DURATION_TICKS
        ));

        spawnClientParticles(basin, level, renderedFluid, getProgress(access, 0.0f));
    }

    private static void tickFadeOut(FluidEscapingAccess access) {
        if (access.sableBeyond$isFluidEscapingActive()) {
            final int fadeTicks = Math.max(1, Mth.ceil(
                    getProgress(access, 0.0f) * FADE_OUT_DURATION_TICKS
            ));
            access.sableBeyond$setFluidEscapingActive(false);
            access.sableBeyond$setFluidEscapingFadingOut(true);
            access.sableBeyond$setFluidEscapingFadeTicks(fadeTicks);
            access.sableBeyond$setFluidEscapingDelayTicks(0);
            return;
        }

        if (!access.sableBeyond$isFluidEscapingFadingOut()) {
            access.sableBeyond$resetFluidEscaping();
            return;
        }

        final int fadeTicks = access.sableBeyond$getFluidEscapingFadeTicks() - 1;
        if (fadeTicks <= 0) {
            access.sableBeyond$resetFluidEscaping();
            return;
        }

        access.sableBeyond$setFluidEscapingFadeTicks(fadeTicks);
    }

    public static boolean shouldAnimate(BasinBlockEntity basin) {
        if (!SableBeyondNeoForgeConfig.current().compatibility.create.basin.fluid_escaping) {
            return false;
        }

        final Level level = basin.getLevel();
        if (level == null) {
            return false;
        }

        return isFluidUpsideDownEnough(level, basin.getBlockPos()) && hasAnyFluid(basin) && isDistanceEnough(level, basin.getBlockPos());
    }

    public static boolean tickStartupDelay(FluidEscapingAccess access) {
        if (access.sableBeyond$getFluidEscapingDelayTicks() >= START_DELAY_TICKS) {
            return true;
        }

        access.sableBeyond$setFluidEscapingDelayTicks(access.sableBeyond$getFluidEscapingDelayTicks() + 1);
        return false;
    }

    public static void resetAnimation(FluidEscapingAccess access) {
        access.sableBeyond$setFluidEscapingActive(false);
        access.sableBeyond$setFluidEscapingTicks(0);
    }

    public static boolean isUpsideDownEnough(Level level, BlockPos pos) {
        return isUpsideDownEnough(getBasinOpeningMotion(level, pos), UPSIDE_DOWN_DOT_THRESHOLD);
    }

    public static boolean isFluidUpsideDownEnough(Level level, BlockPos pos) {
        return isUpsideDownEnough(getBasinOpeningMotion(level, pos), FLUID_UPSIDE_DOWN_DOT_THRESHOLD);
    }

    public static boolean isFluidRightSideUpEnough(Level level, BlockPos pos) {
        final Vec3 openingMotion = getBasinOpeningMotion(level, pos);
        if (openingMotion.lengthSqr() == 0.0d) {
            return false;
        }

        return openingMotion.normalize().dot(new Vec3(0, 1, 0)) >= FLUID_RIGHT_SIDE_UP_DOT_THRESHOLD;
    }

    public static boolean isFluid(Level level, BlockPos pos) {
        return !level.getFluidState(pos).isEmpty();
    }

    public static boolean tryFillBasin(BasinBlockEntity basin, FluidStack fluidStack) {
        if (fluidStack.isEmpty() || fluidStack.getAmount() <= 0) {
            return false;
        }

        for (SmartFluidTankBehaviour behaviour : basin.getTanks()) {
            if (behaviour == null) {
                continue;
            }

            final IFluidHandler handler = behaviour.getCapability();
            if (handler == null) {
                continue;
            }

            final FluidStack fluidToInsert = fluidStack.copy();
            if (handler.fill(fluidToInsert, FluidAction.SIMULATE) != fluidToInsert.getAmount()) {
                continue;
            }

            if (handler.fill(fluidToInsert, FluidAction.EXECUTE) == fluidToInsert.getAmount()) {
                basin.notifyUpdate();
                return true;
            }
        }

        return false;
    }

    private static boolean isUpsideDownEnough(Vec3 openingMotion, double threshold) {
        if (openingMotion.lengthSqr() == 0.0d) {
            return false;
        }

        return openingMotion.normalize().dot(new Vec3(0, 1, 0)) <= threshold;
    }

    public static boolean isDistanceEnough(Level level, BlockPos blockPos) {
        if (FluidEscaping.getRealWorldDownDistance(level, blockPos) <= 0.5D) {
            return false;
        }
        return true;
    }

    public static Vec3 getBasinOpeningMotion(Level level, BlockPos pos) {
        final SubLevel subLevel = getContainingSubLevel(level, pos);
        final Vec3 localUp = new Vec3(0.0d, 1.0d, 0.0d);
        final Vec3 worldOpeningDirection = subLevel == null
                ? localUp
                : subLevel.logicalPose().transformNormal(localUp).normalize();

        return worldOpeningDirection.scale(0.05d);
    }

    public static @Nullable SubLevel getContainingSubLevel(Level level, BlockPos pos) {
        return level.isClientSide()
                ? Sable.HELPER.getContainingClient(pos)
                : Sable.HELPER.getContaining(level, pos);
    }

    public static Vec3 getWorldOpeningPosition(Level level, BlockPos pos, double localY) {
        final SubLevel subLevel = getContainingSubLevel(level, pos);
        final Vec3 localPosition = Vec3.atLowerCornerOf(pos).add(0.5d, localY, 0.5d);
        return subLevel == null ? localPosition : subLevel.logicalPose().transformPosition(localPosition);
    }

    public static Vec3 getWorldStreamStart(Level level, BlockPos pos, float progress) {
        return getWorldOpeningPosition(level, pos, getStreamStartY(progress));
    }

    public static @Nullable BlockHitResult raycastRealWorldDown(Level level, BlockPos pos) {
        final Vec3 worldStart = getWorldOpeningPosition(level, pos, WORLD_RAY_START_Y)
                .add(0.0d, -WORLD_RAY_START_OFFSET, 0.0d);
        return raycastRealWorldDown(level, worldStart);
    }

    public static @Nullable BlockHitResult raycastRealWorldDown(Level level, Vec3 worldStart) {
        final Vec3 worldEnd = new Vec3(worldStart.x, level.getMinBuildHeight() - WORLD_RAY_END_PADDING, worldStart.z);
        final BlockHitResult hit = level.clip(new ClipContext(
                worldStart,
                worldEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                CollisionContext.empty()
        ));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    public static double getRealWorldDownDistance(Level level, BlockPos pos) {
        final Vec3 worldStart = getWorldOpeningPosition(level, pos, WORLD_RAY_START_Y)
                .add(0.0d, -WORLD_RAY_START_OFFSET, 0.0d);
        return getRealWorldDownDistance(level, worldStart);
    }

    public static double getRealWorldDownDistance(Level level, Vec3 worldStart) {
        final BlockHitResult hit = raycastRealWorldDown(level, worldStart);
        final Vec3 worldEnd = hit != null
                ? getWorldRaycastLocation(level, hit)
                : new Vec3(worldStart.x, level.getMinBuildHeight(), worldStart.z);
        return worldStart.distanceTo(worldEnd);
    }

    public static Vec3 getWorldRaycastLocation(Level level, BlockHitResult hit) {
        final SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hit.getBlockPos());
        return hitSubLevel == null
                ? hit.getLocation()
                : hitSubLevel.logicalPose().transformPosition(hit.getLocation());
    }

    public static float getProgress(FluidEscapingAccess access, float partialTicks) {
        if (access.sableBeyond$isFluidEscapingFadingOut()) {
            return Mth.clamp(
                    (access.sableBeyond$getFluidEscapingFadeTicks() - partialTicks) / FADE_OUT_DURATION_TICKS,
                    0.0f,
                    1.0f
            );
        }

        if (!access.sableBeyond$isFluidEscapingActive()) {
            return 0.0f;
        }

        return Mth.clamp(
                (access.sableBeyond$getFluidEscapingTicks() + partialTicks) / ANIMATION_DURATION_TICKS,
                0.0f,
                1.0f
        );
    }

    public static float getStreamStartY(float progress) {
        return Mth.lerp(progress, STREAM_START_Y_CLOSED, STREAM_START_Y_OPEN);
    }

    public static FluidStack findEscapingFluid(BasinBlockEntity basin) {
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

                return fluidInTank.copy();
            }
        }

        return FluidStack.EMPTY;
    }

    public static boolean hasAnyFluid(BasinBlockEntity basin) {
        for (SmartFluidTankBehaviour behaviour : basin.getTanks()) {
            if (behaviour == null) {
                continue;
            }

            final IFluidHandler handler = behaviour.getCapability();
            if (handler == null) {
                continue;
            }

            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (!handler.getFluidInTank(tank).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static FluidStack getRenderedEscapingFluid(BasinBlockEntity basin, FluidEscapingAccess access,
                                                       float partialTicks) {
        final FluidStack renderedFluid = findEscapingFluid(basin);
        if (renderedFluid.isEmpty()) {
            return access.sableBeyond$getLastRenderedEscapingFluid();
        }

        final FluidStack previousFluid = access.sableBeyond$getLastRenderedEscapingFluid();
        if (!previousFluid.isEmpty()
                && !FluidStack.isSameFluidSameComponents(previousFluid, renderedFluid)
                && (access.sableBeyond$isFluidEscapingActive() || access.sableBeyond$isFluidEscapingFadingOut())) {
            return previousFluid;
        }

        return renderedFluid;
    }

    private static void spawnClientParticles(BasinBlockEntity basin, Level level, FluidStack fluidStack, float progress) {
        if (fluidStack.isEmpty()) {
            return;
        }

        final ParticleOptions streamParticle = getStreamParticle(fluidStack);
        if (streamParticle == null) {
            return;
        }

        final float partialTicks = FluidEscapingRenderer.getPartialTicks();
        final Vec3 worldStart = FluidEscapingRenderer.getWorldStreamStart(level, basin.getBlockPos(), progress, partialTicks);
        final BlockHitResult hit = FluidEscapingRenderer.raycastRealWorldDown(
                level,
                worldStart.add(0.0d, -WORLD_RAY_START_OFFSET, 0.0d),
                partialTicks
        );
        final Vec3 worldEnd = hit != null
                ? FluidEscapingRenderer.getWorldRaycastLocation(level, hit, partialTicks)
                : new Vec3(worldStart.x, level.getMinBuildHeight(), worldStart.z);
        final double length = Math.max(worldStart.distanceTo(worldEnd), 1.0d / 16.0d);

        for (int i = 0; i < STREAM_PARTICLE_COUNT; i++) {
            final double t = level.random.nextDouble() * length;
            final double x = worldStart.x + (level.random.nextDouble() - 0.5d) * STREAM_PARTICLE_JITTER;
            final double y = worldStart.y - t;
            final double z = worldStart.z + (level.random.nextDouble() - 0.5d) * STREAM_PARTICLE_JITTER;
            level.addParticle(streamParticle, x, y, z, 0.0d, -0.12d, 0.0d);
        }

        if (hit == null) {
            return;
        }

        final ParticleOptions impactParticle = getImpactParticle(fluidStack);
        for (int i = 0; i < IMPACT_PARTICLE_COUNT; i++) {
            final double x = worldEnd.x + (level.random.nextDouble() - 0.5d) * 0.2d;
            final double y = worldEnd.y + 0.02d;
            final double z = worldEnd.z + (level.random.nextDouble() - 0.5d) * 0.2d;
            level.addParticle(impactParticle, x, y, z,
                    (level.random.nextDouble() - 0.5d) * 0.03d,
                    0.03d + level.random.nextDouble() * 0.04d,
                    (level.random.nextDouble() - 0.5d) * 0.03d);
        }
    }

    private static @Nullable ParticleOptions getStreamParticle(FluidStack fluidStack) {
        final Fluid stillFluid = FluidHelper.convertToStill(fluidStack.getFluid());
        if (stillFluid instanceof FlowingFluid && FluidHelper.hasBlockState(stillFluid)) {
            return new BlockParticleOption(ParticleTypes.BLOCK, stillFluid.defaultFluidState().createLegacyBlock());
        }

        return ParticleTypes.CLOUD;
    }

    private static ParticleOptions getImpactParticle(FluidStack fluidStack) {
        final Fluid stillFluid = FluidHelper.convertToStill(fluidStack.getFluid());
        if (FluidHelper.isWater(stillFluid)) {
            return ParticleTypes.SPLASH;
        }
        if (stillFluid instanceof FlowingFluid && FluidHelper.hasBlockState(stillFluid)) {
            return new BlockParticleOption(ParticleTypes.BLOCK, stillFluid.defaultFluidState().createLegacyBlock());
        }
        return ParticleTypes.CLOUD;
    }
}
