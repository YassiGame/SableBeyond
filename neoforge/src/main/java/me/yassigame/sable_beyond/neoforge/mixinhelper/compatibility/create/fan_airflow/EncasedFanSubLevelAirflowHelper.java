package me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.fan_airflow;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class EncasedFanSubLevelAirflowHelper {
    private static final double[] SAMPLE_OFFSETS = {-0.3d, 0.0d, 0.3d};
    private static final Map<ServerLevel, TickingFanCache> TICKING_FANS = new WeakHashMap<>();

    public static void registerTickingFan(final EncasedFanBlockEntity fan) {
        if (!SableBeyondNeoForgeConfig.applyForceToTouchedSublevels()) {
            return;
        }

        final Level level = fan.getLevel();
        if (level == null || fan.isRemoved() || level.getServer() == null) {
            return;
        }

        final ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
        if (serverLevel == null) {
            return;
        }

        final long gameTime = serverLevel.getGameTime();
        final TickingFanCache cache = TICKING_FANS.computeIfAbsent(serverLevel, ignored -> new TickingFanCache());
        if (cache.gameTime != gameTime) {
            cache.gameTime = gameTime;
            cache.fans.clear();
        }

        cache.fans.add(fan);
    }

    public static void applyRegisteredFansForPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        if (!SableBeyondNeoForgeConfig.applyForceToTouchedSublevels()) {
            return;
        }

        final ServerLevel level = physicsSystem.getLevel();
        final TickingFanCache cache = TICKING_FANS.get(level);
        if (cache == null) {
            return;
        }

        final long gameTime = level.getGameTime();
        if (cache.gameTime < gameTime - 1L) {
            cache.fans.clear();
            return;
        }

        for (final EncasedFanBlockEntity fan : cache.fans) {
            final Level fanLevel = fan.getLevel();
            if (fan.isRemoved() || fanLevel == null || fanLevel.getServer() != level.getServer()
                    || fanLevel.dimension() != level.dimension()) {
                continue;
            }

            applyFlowForceToTouchedSubLevels(fan, timeStep);
        }
    }

    public static void applyFlowForceToTouchedSubLevels(final EncasedFanBlockEntity fan, final double timeStep) {
        final Level level = fan.getLevel();
        if (level == null || level.isClientSide || !SableBeyondNeoForgeConfig.applyForceToTouchedSublevels()) {
            return;
        }

        if (!(fan instanceof final BlockEntityPropeller propeller) || !propeller.isActive()) {
            return;
        }

        final AirCurrent airCurrent = fan.getAirCurrent();
        final Direction airflowOrigin = fan.getAirflowOriginSide();
        final Direction flowDirection = fan.getAirFlowDirection();
        if (airCurrent == null || airflowOrigin == null || flowDirection == null || airCurrent.maxDistance <= 0.01f) {
            return;
        }

        final SubLevel parentSubLevel = Sable.HELPER.getContaining(fan);
        final Vec3 center = Vec3.atCenterOf(fan.getBlockPos());
        final Vec3 outwardNormal = Vec3.atLowerCornerOf(airflowOrigin.getNormal());
        final Vec3 globalCenter = Sable.HELPER.projectOutOfSubLevel(level, center);
        final Vec3 flowVector = projectNormalToGlobal(level, center, Vec3.atLowerCornerOf(flowDirection.getNormal()));
        if (flowVector.lengthSqr() <= 1e-8d) {
            return;
        }
        final Vec3 axisA = getSampleAxisA(airflowOrigin);
        final Vec3 axisB = getSampleAxisB(airflowOrigin);
        final double rayLength = airCurrent.maxDistance;
        final double curveScaling = 2.0d;
        final List<TouchedSubLevelHit> touchedHits = new ArrayList<>();

        for (final double offsetA : SAMPLE_OFFSETS) {
            for (final double offsetB : SAMPLE_OFFSETS) {
                final Vec3 start = center.add(outwardNormal.scale(0.501d))
                        .add(axisA.scale(offsetA))
                        .add(axisB.scale(offsetB));
                final Vec3 end = start.add(outwardNormal.scale(rayLength));
                final Vec3 globalStart = start.equals(center) ? globalCenter : Sable.HELPER.projectOutOfSubLevel(level, start);
                final Vec3 globalEnd = Sable.HELPER.projectOutOfSubLevel(level, end);
                final FlowHit flowHit = traceTouchedSubLevel(level, globalStart, globalEnd, parentSubLevel);
                if (flowHit == null) {
                    continue;
                }

                final BlockHitResult clip = flowHit.hitResult();
                final ServerSubLevel hitServerSubLevel = flowHit.subLevel();
                final Vec3 globalHit = Sable.HELPER.projectOutOfSubLevel(level, clip.getLocation());
                Vec3 hitDiff = globalHit.subtract(globalStart);
                if (hitDiff.lengthSqr() <= 1e-8d) {
                    continue;
                }

                final double inverseHitPercentage;
                if (clip.isInside()) {
                    inverseHitPercentage = 1.0d;
                } else {
                    inverseHitPercentage = Math.clamp(
                            curveScaling - ((hitDiff.length() / rayLength) * curveScaling),
                            0.0d,
                            1.0d
                    );
                }

                if (inverseHitPercentage <= 1e-6d) {
                    continue;
                }

                touchedHits.add(new TouchedSubLevelHit(hitServerSubLevel, clip, inverseHitPercentage));
            }
        }

        if (touchedHits.isEmpty()) {
            return;
        }

        final double forceMagnitude = Math.abs(propeller.getScaledThrust()) * timeStep * SableBeyondNeoForgeConfig.current().compatibility.create.encased_fan.fan_force_multiplier;
        final double hitScale = 1.0d / touchedHits.size();
        for (final TouchedSubLevelHit touchedHit : touchedHits) {
            final Vec3 globalImpulse = flowVector.scale(forceMagnitude * touchedHit.inverseHitPercentage() * hitScale);
            final Vector3d localImpulse = touchedHit.subLevel().logicalPose()
                    .transformNormalInverse(JOMLConversion.toJOML(globalImpulse));

            touchedHit.subLevel().getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
                    .applyAndRecordPointForce(JOMLConversion.toJOML(touchedHit.hitResult().getLocation()), localImpulse);
        }
    }

    private static FlowHit traceTouchedSubLevel(final Level level, final Vec3 globalStart, final Vec3 globalEnd,
                                                final SubLevel parentSubLevel) {
        final ClipContext mainContext = new ClipContext(
                globalStart,
                globalEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );
        ((ClipContextExtension) mainContext).sable$setDoNotProject(true);

        final BlockHitResult mainHit = level.clip(mainContext);
        double closestDistance = mainHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : mainHit.getLocation().distanceToSqr(globalStart);
        FlowHit closestFlowHit = null;

        for (final SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(globalStart, globalEnd))) {
            if (!(subLevel instanceof final ServerSubLevel hitServerSubLevel) || subLevel == parentSubLevel) {
                continue;
            }

            final Vec3 localStart = hitServerSubLevel.logicalPose().transformPositionInverse(globalStart);
            final Vec3 localEnd = hitServerSubLevel.logicalPose().transformPositionInverse(globalEnd);
            final ClipContext subContext = new ClipContext(
                    localStart,
                    localEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    CollisionContext.empty()
            );
            ((ClipContextExtension) subContext).sable$setDoNotProject(true);

            final BlockHitResult subHit = hitServerSubLevel.getLevel().clip(subContext);
            if (subHit.getType() == HitResult.Type.MISS) {
                continue;
            }

            final double distance = subHit.getLocation().distanceToSqr(localStart);
            if (distance >= closestDistance) {
                continue;
            }

            closestDistance = distance;
            closestFlowHit = new FlowHit(hitServerSubLevel, subHit);
        }

        return closestFlowHit;
    }

    private static Vec3 getSampleAxisA(final Direction airflowOrigin) {
        return switch (airflowOrigin.getAxis()) {
            case X -> new Vec3(0.0d, 1.0d, 0.0d);
            case Y -> new Vec3(1.0d, 0.0d, 0.0d);
            case Z -> new Vec3(1.0d, 0.0d, 0.0d);
        };
    }

    private static Vec3 getSampleAxisB(final Direction airflowOrigin) {
        return switch (airflowOrigin.getAxis()) {
            case X -> new Vec3(0.0d, 0.0d, 1.0d);
            case Y -> new Vec3(0.0d, 0.0d, 1.0d);
            case Z -> new Vec3(0.0d, 1.0d, 0.0d);
        };
    }

    private static Vec3 projectNormalToGlobal(final Level level, final Vec3 origin, final Vec3 localNormal) {
        final Vec3 globalOrigin = Sable.HELPER.projectOutOfSubLevel(level, origin);
        final Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, origin.add(localNormal));
        return globalTarget.subtract(globalOrigin).normalize();
    }

    private record FlowHit(ServerSubLevel subLevel, BlockHitResult hitResult) {
    }

    private record TouchedSubLevelHit(ServerSubLevel subLevel, BlockHitResult hitResult, double inverseHitPercentage) {
    }

    private static final class TickingFanCache {
        private long gameTime = Long.MIN_VALUE;
        private final List<EncasedFanBlockEntity> fans = new ArrayList<>();
    }
}
