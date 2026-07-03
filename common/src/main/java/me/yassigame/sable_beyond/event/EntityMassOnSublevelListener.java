package me.yassigame.sable_beyond.event;

import dev.leo.sableplayerragdoll.physics.RagdollAssemblyHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import me.yassigame.sable_beyond.api.event.EntityOnSublevelGroundEvent;
import me.yassigame.sable_beyond.api.mass.EntityMass;
import me.yassigame.sable_beyond.platform.ModPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.Objects;

public final class EntityMassOnSublevelListener {
    private static final String BEARING_CONTRAPTION_ENTITY_SUFFIX = "BearingContraptionEntity";
    private static final double MIN_SUPPORT_MARGIN = 0.125;
    private static final double BASE_CENTER_FORCE_RATIO = 0.15;
    private static final double MAX_CENTER_FORCE_RATIO = 0.65;
    private static final ResourceLocation GRAVITY_FORCE_GROUP_ID = Sable.sablePath("gravity");

    public static void onEntityMassOnSublevel(final EntityOnSublevelGroundEvent event) {
        final ServerSubLevel subLevel = event.getSubLevel();
        final Entity entity = event.getEntity();
        if (!EntityMass.isMassAppliedEntity(entity)) {
            return;
        }

        final double resolvedMass = EntityMass.resolveMass(entity);
        if (resolvedMass <= 0.0) {
            return;
        }

        // If player mass is in experimental mode disable the normal one for players
        if (entity instanceof Player && EntityMass.isExperimentalPlayerMassEnabled()) {
            return;
        }

        // If only player mass activated
        if (!(entity instanceof Player) && EntityMass.isLivingEntityOnlyPlayerMassEnabled()) {
            return;
        }

        // TODO DO BETTER
        // If ragdol activated (only on neoforge)
        if (Objects.equals(ModPlatform.getLoaderName().toLowerCase(), "neoforge")) {
            if (ModPlatform.isModLoaded("sable_player_ragdoll")) {
                if (RagdollAssemblyHelper.isRagdollPart(subLevel.getUniqueId())) {
                    return;
                }
            }
        }

        final Vector3d globalPoint = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        final Vector3d rawLocalPoint = subLevel.logicalPose().transformPositionInverse(new Vector3d(globalPoint));
        final Vector3d supportPoint = getSupportPoint(subLevel, entity, rawLocalPoint);
        final Vector3d globalGravity = DimensionPhysicsData.getGravity(subLevel.getLevel(), globalPoint, new Vector3d());

        // if fluid dont apply force
        if (!subLevel.getLevel().getFluidState(BlockPos.containing(rawLocalPoint.x, rawLocalPoint.y, rawLocalPoint.z)).isEmpty()) {
            return;
        }

        // Convert the world gravity vector into the sublevel's local frame so the load follows the sublevel orientation.
        final Vector3d localImpulse = subLevel.logicalPose()
                .transformNormalInverse(globalGravity)
                .mul(resolvedMass * event.getTimeStep());
        final MassData massData = subLevel.getMassTracker();
        final Vector3dc centerOfMass = massData.getCenterOfMass();
        final double edgeFactor = getEdgeFactor(rawLocalPoint, supportPoint, entity);

        // Keep the Aeronautics bearing case centered so the load does not create a fake torque on a constrained mount.
        final double centerForceRatio = centerOfMass == null
                ? 0.0
                : isBearingMount(subLevel)
                ? 1.0
                : Mth.clamp(BASE_CENTER_FORCE_RATIO + edgeFactor * (MAX_CENTER_FORCE_RATIO - BASE_CENTER_FORCE_RATIO), 0.0, 1.0);
        final double pointForceRatio = 1.0 - centerForceRatio;
        final ForceGroup gravityForceGroup = ForceGroups.REGISTRY.get(GRAVITY_FORCE_GROUP_ID);
        if (gravityForceGroup == null) {
            return;
        }

        final var gravityGroup = subLevel.getOrCreateQueuedForceGroup(gravityForceGroup);

        if (pointForceRatio > 0.0) {
            // Apply most of the load under the entity so off-center weight can still tilt normal sublevels.
            gravityGroup.applyAndRecordPointForce(supportPoint, new Vector3d(localImpulse).mul(pointForceRatio));
        }

        if (centerForceRatio > 0.0) {
            // Blend part of the load back to the center to reduce extreme edge behavior.
            gravityGroup.applyAndRecordPointForce(centerOfMass, new Vector3d(localImpulse).mul(centerForceRatio));
        }
    }

    private static Vector3d getSupportPoint(final ServerSubLevel subLevel, final Entity entity, final Vector3d localPoint) {
        final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        final double supportMargin = Math.max(MIN_SUPPORT_MARGIN, entity.getBbWidth() * 0.35);
        final double minX = bounds.minX();
        final double maxX = bounds.maxX() + 1.0;
        final double minZ = bounds.minZ();
        final double maxZ = bounds.maxZ() + 1.0;

        // Clamp the support point to a safe support area to not create absurd torques.
        return new Vector3d(
                clampAxis(localPoint.x, minX, maxX, supportMargin),
                localPoint.y,
                clampAxis(localPoint.z, minZ, maxZ, supportMargin)
        );
    }

    private static double getEdgeFactor(final Vector3d rawLocalPoint, final Vector3d supportPoint, final Entity entity) {
        final double maxDisplacement = Math.max(
                Math.abs(rawLocalPoint.x - supportPoint.x),
                Math.abs(rawLocalPoint.z - supportPoint.z)
        );
        final double supportMargin = Math.max(MIN_SUPPORT_MARGIN, entity.getBbWidth() * 0.35);

        if (supportMargin <= 1.0E-6) {
            return 0.0;
        }

        return Mth.clamp(maxDisplacement / supportMargin, 0.0, 1.0);
    }

    private static boolean isBearingMount(final ServerSubLevel subLevel) {
        if (subLevel.getPlot() == null) {
            return false;
        }

        Collection<KinematicContraption> contraptionCollection = subLevel.getPlot().getContraptions();
        for (final KinematicContraption contraption : contraptionCollection) {
            if (contraption.getClass().getSimpleName().endsWith(BEARING_CONTRAPTION_ENTITY_SUFFIX)) {
                return true;
            }
        }

        return false;
    }

    private static double clampAxis(final double value, final double minInclusive, final double maxExclusive, final double supportMargin) {
        final double minSupport = minInclusive + supportMargin;
        final double maxSupport = maxExclusive - supportMargin;

        if (minSupport > maxSupport) {
            return (minInclusive + maxExclusive) * 0.5;
        }

        return Mth.clamp(value, minSupport, maxSupport);
    }
}
