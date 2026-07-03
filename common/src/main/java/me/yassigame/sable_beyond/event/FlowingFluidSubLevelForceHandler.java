package me.yassigame.sable_beyond.event;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;

public final class FlowingFluidSubLevelForceHandler {
    private static final double CONTACT_EXPANSION = 0.18d;
    private static final double MIN_FLOW_LENGTH_SQR = 1.0E-6d;
    private static final int MAX_SAMPLES_PER_SUBLEVEL = 512;
    private static final ResourceLocation PROPULSION_FORCE_GROUP_ID = Sable.sablePath("propulsion");

    public static void onPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        if (!SableBeyondConfig.common().flowingFluid.enabled) {
            return;
        }

        if (timeStep <= 0.0d) {
            return;
        }

        final ServerLevel level = physicsSystem.getLevel();
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        final ForceGroup propulsionForceGroup = ForceGroups.REGISTRY.get(PROPULSION_FORCE_GROUP_ID);
        if (propulsionForceGroup == null) {
            return;
        }

        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved()) {
                continue;
            }

            applyFlowingFluidForces(level, subLevel, timeStep, propulsionForceGroup);
        }
    }

    private static void applyFlowingFluidForces(final ServerLevel level, final ServerSubLevel subLevel,
                                                final double timeStep, final ForceGroup propulsionForceGroup) {
        final BoundingBox3d bounds = new BoundingBox3d(subLevel.getPlot().getBoundingBox()).transform(subLevel.logicalPose());
        if (bounds.volume() <= 1.0E-6d) {
            return;
        }

        final int minX = Mth.floor(bounds.minX() - CONTACT_EXPANSION);
        final int minY = Mth.floor(bounds.minY() - CONTACT_EXPANSION);
        final int minZ = Mth.floor(bounds.minZ() - CONTACT_EXPANSION);
        final int maxX = Mth.floor(bounds.maxX() + CONTACT_EXPANSION);
        final int maxY = Mth.floor(bounds.maxY() + CONTACT_EXPANSION);
        final int maxZ = Mth.floor(bounds.maxZ() + CONTACT_EXPANSION);
        final int sizeX = Math.max(1, maxX - minX + 1);
        final int sizeY = Math.max(1, maxY - minY + 1);
        final int sizeZ = Math.max(1, maxZ - minZ + 1);
        final int stride = sampleStride(sizeX, sizeY, sizeZ);
        final Set<Long> sampledBlocks = new HashSet<>();
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double appliedForce = 0.0d;

        for (int x = minX; x <= maxX; x = nextSample(x, maxX, stride)) {
            for (int y = minY; y <= maxY; y = nextSample(y, maxY, stride)) {
                for (int z = minZ; z <= maxZ; z = nextSample(z, maxZ, stride)) {
                    mutablePos.set(x, y, z);
                    if (!sampledBlocks.add(mutablePos.asLong())) {
                        continue;
                    }

                    appliedForce = sampleFluidBlock(level, subLevel, mutablePos, timeStep, appliedForce, propulsionForceGroup);
                    if (appliedForce >= SableBeyondConfig.common().flowingFluid.max_force) {
                        return;
                    }
                }
            }
        }
    }

    private static double sampleFluidBlock(final ServerLevel level, final ServerSubLevel subLevel, final BlockPos pos,
                                           final double timeStep, final double alreadyAppliedForce,
                                           final ForceGroup propulsionForceGroup) {
        if (!level.isLoaded(pos)) {
            return alreadyAppliedForce;
        }

        final FluidState fluidState = level.getFluidState(pos);
        if (fluidState.isEmpty()) {
            return alreadyAppliedForce;
        }

        final Vec3 flow = fluidState.getFlow(level, pos);
        final double flowLengthSqr = flow.lengthSqr();
        if (flowLengthSqr <= MIN_FLOW_LENGTH_SQR) {
            return alreadyAppliedForce;
        }

        final double flowLength = Math.sqrt(flowLengthSqr);
        final double fluidMultiplier = fluidState.is(FluidTags.LAVA) ? SableBeyondConfig.common().flowingFluid.lava_force_multiplier : 1.0d;
        final double remainingForce = SableBeyondConfig.common().flowingFluid.max_force - alreadyAppliedForce;
        final double contactForce = Math.min(SableBeyondConfig.common().flowingFluid.force * Math.min(flowLength, 1.0d) * fluidMultiplier, remainingForce);
        if (contactForce <= 0.0d) {
            return alreadyAppliedForce;
        }

        final Vector3d globalImpulse = new Vector3d(flow.x, flow.y, flow.z)
                .normalize()
                .mul(contactForce * timeStep);
        final Vector3d localImpulse = subLevel.logicalPose().transformNormalInverse(globalImpulse, new Vector3d());
        final Vector3d localPoint = subLevel.logicalPose()
                .transformPositionInverse(new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d));

        subLevel.getOrCreateQueuedForceGroup(propulsionForceGroup)
                .applyAndRecordPointForce(clampToPlot(localPoint, subLevel.getPlot().getBoundingBox()), localImpulse);

        return alreadyAppliedForce + contactForce;
    }

    private static Vector3d clampToPlot(final Vector3d localPoint, final BoundingBox3ic plotBounds) {
        return new Vector3d(
                Mth.clamp(localPoint.x, plotBounds.minX(), plotBounds.maxX() + 1.0d),
                Mth.clamp(localPoint.y, plotBounds.minY(), plotBounds.maxY() + 1.0d),
                Mth.clamp(localPoint.z, plotBounds.minZ(), plotBounds.maxZ() + 1.0d)
        );
    }

    private static int sampleStride(final int sizeX, final int sizeY, final int sizeZ) {
        final long volume = (long) sizeX * sizeY * sizeZ;
        if (volume <= MAX_SAMPLES_PER_SUBLEVEL) {
            return 1;
        }

        final int varyingAxes = (sizeX > 1 ? 1 : 0) + (sizeY > 1 ? 1 : 0) + (sizeZ > 1 ? 1 : 0);
        final double sampleRatio = (double) volume / MAX_SAMPLES_PER_SUBLEVEL;
        final double rawStride = switch (varyingAxes) {
            case 0, 1 -> sampleRatio;
            case 2 -> Math.sqrt(sampleRatio);
            default -> Math.cbrt(sampleRatio);
        };

        return Math.max(1, Mth.ceil(rawStride));
    }

    private static int nextSample(final int current, final int max, final int stride) {
        if (current == max) {
            return max + 1;
        }

        final int next = current + stride;
        return Math.min(next, max);
    }
}
