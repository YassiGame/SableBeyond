package me.yassigame.sable_beyond.event;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import me.yassigame.sable_beyond.api.event.EntityOnSublevelGroundEvent;
import me.yassigame.sable_beyond.api.mass.MassRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;


// just a trash class testing other methods
public final class WipEntityMassOnSublevelListener {
    private static final ResourceLocation GRAVITY_FORCE_GROUP_ID = Sable.sablePath("gravity");

    public static void onEntityMassOnSublevel(final EntityOnSublevelGroundEvent event) {
        final ServerSubLevel subLevel = event.getSubLevel();
        final Entity entity = event.getEntity();
        if (!MassRegistry.isMassAppliedEntity(entity)) {
            return;
        }

        final double resolvedMass = MassRegistry.resolveMass(entity);
        if (resolvedMass <= 0.0) {
            return;
        }

        // If player mass is in experimental mode disable the normal one for players
        if (entity instanceof Player && MassRegistry.isExperimentalPlayerMassEnabled()) {
            return;
        }

        final Vector3d globalPoint = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        final Vector3d rawLocalPoint = subLevel.logicalPose().transformPositionInverse(new Vector3d(globalPoint));
        final BlockPos supportPos = BlockPos.containing(rawLocalPoint.x, rawLocalPoint.y - 0.01, rawLocalPoint.z);


        final Vector3d globalGravity = DimensionPhysicsData.getGravity(subLevel.getLevel(), globalPoint, new Vector3d());

        // Convert the world gravity vector into the sublevel's local frame so the load follows the sublevel orientation.
        final Vector3d localImpulse = subLevel.logicalPose()
                .transformNormalInverse(globalGravity)
                .mul(resolvedMass * event.getTimeStep());
        final MassData massData = subLevel.getMassTracker();
        final Vector3dc centerOfMass = massData.getCenterOfMass();

        System.out.println("test");

        final ForceGroup gravityForceGroup = ForceGroups.REGISTRY.get(GRAVITY_FORCE_GROUP_ID);
        if (gravityForceGroup == null) {
            return;
        }

        final ForceTotal forceTotal = subLevel.getOrCreateQueuedForceGroup(ForceGroups.REGISTRY.get(GRAVITY_FORCE_GROUP_ID)).getForceTotal();
        forceTotal.applyImpulseAtPoint(subLevel.getMassTracker(), JOMLConversion.toJOML(Vec3.atCenterOf(supportPos)), localImpulse);

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
