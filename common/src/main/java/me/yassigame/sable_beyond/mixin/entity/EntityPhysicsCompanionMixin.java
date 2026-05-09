package me.yassigame.sable_beyond.mixin.entity;

import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.yassigame.sable_beyond.api.mass.MassRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// experimental player physics adding a sable collision box to the player a mass to it
// not fan at all with this code, hello neighbor type of physics really buggy

@Mixin(Entity.class)
public abstract class EntityPhysicsCompanionMixin {
    @Unique
    private static final double REBUILD_EPSILON = 1.0E-6;
    @Unique
    private static final boolean DEBUG_COLLISION_BOX = false;
    @Unique
    private static final double TARGET_VELOCITY_SCALE = 20.0;
    @Unique
    private static final double FOLLOW_STRENGTH = 0.3;
    @Unique
    private static final double MAX_TARGET_VELOCITY = 6.0;
    @Unique
    private static final double MAX_DELTA_VELOCITY = 0.6;
    @Unique
    private static final double PLAYER_COLLISION_TOP = 0.1;
    @Unique
    private static final double PLAYER_INTERACTION_FEET_OFFSET = 0.08;
    @Unique
    private static final double MIN_HALF_HEIGHT = 0.1;

    @Unique
    private final Pose3d physicsCompanionPose = new Pose3d();
    @Unique
    private final Vector3d physicsCompanionCenter = new Vector3d();
    @Unique
    private final Quaterniond physicsCompanionOrientation = new Quaterniond();
    @Unique
    private final Vector3d lastPlayerCenter = new Vector3d();
    @Unique
    private boolean hasLastPlayerCenter;

    @Unique
    private BoxPhysicsObject physicsCompanion;
    @Unique
    private ServerLevel physicsCompanionLevel;
    @Unique
    private double physicsCompanionHalfX;
    @Unique
    private double physicsCompanionHalfY;
    @Unique
    private double physicsCompanionHalfZ;
    @Unique
    private double physicsCompanionMass;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sableBeyond$syncPhysicsCompanion(final CallbackInfo ci) {
        final Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player) || !(entity.level() instanceof final ServerLevel serverLevel)) {
            return;
        }

        if (!MassRegistry.isMassAppliedEntity(entity)) {
            this.removePhysicsCompanion();
            return;
        }

        if (!MassRegistry.isExperimentalPlayerSublevelInteractionEnabled() && !MassRegistry.isExperimentalPlayerMassEnabled()) {
            this.removePhysicsCompanion();
            return;
        }

        double mass;
        if (MassRegistry.isExperimentalPlayerMassEnabled()) {
            mass = MassRegistry.resolveMass(entity);
            if (mass <= 0.0) {
                this.removePhysicsCompanion();
                return;
            }
        } else {
            mass = 0;
        }

        final AABB box = entity.getBoundingBox();
        final double halfX = box.getXsize() * 0.5;
        final double halfY = this.getCompanionHalfY(box);
        final double halfZ = box.getZsize() * 0.5;

        if (halfX <= 0.0 || halfY <= 0.0 || halfZ <= 0.0) {
            this.removePhysicsCompanion();
            return;
        }

        if (this.shouldRebuild(serverLevel, halfX, halfY, halfZ, mass)) {
            this.removePhysicsCompanion();
            this.createPhysicsCompanion(entity, serverLevel, mass, halfX, halfY, halfZ, box);
            return;
        }

        if (this.physicsCompanion == null || !this.physicsCompanion.isActive()) {
            return;
        }

        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        if (physicsSystem == null) {
            return;
        }

        this.updateCompanionCenter(box);

        physicsSystem.getPipeline().teleport(this.physicsCompanion, this.physicsCompanionCenter, this.physicsCompanionOrientation.identity());

        /**
        final Vector3d targetVelocity = this.getTargetVelocity(entity);
        this.clampMagnitude(targetVelocity, MAX_TARGET_VELOCITY);
        final Vector3d currentVelocity = physicsSystem.getPipeline().getLinearVelocity(this.physicsCompanion, new Vector3d());
        final Vector3d deltaVelocity = targetVelocity.sub(currentVelocity, new Vector3d()).mul(FOLLOW_STRENGTH);
        this.clampMagnitude(deltaVelocity, MAX_DELTA_VELOCITY);

        physicsSystem.getPipeline().addLinearAndAngularVelocity(this.physicsCompanion, deltaVelocity, new Vector3d());
        */

        this.physicsCompanion.updatePose();
        this.renderDebugCollisionBox(serverLevel);
        this.updateLastPlayerCenter(entity);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void sableBeyond$removePhysicsCompanionOnRemove(final Entity.RemovalReason removalReason, final CallbackInfo ci) {
        this.removePhysicsCompanion();
    }

    @Unique
    private boolean shouldRebuild(final ServerLevel serverLevel, final double halfX, final double halfY, final double halfZ, final double mass) {
        if (this.physicsCompanion == null || !this.physicsCompanion.isActive()) {
            return true;
        }

        if (this.physicsCompanionLevel != serverLevel) {
            return true;
        }

        if (java.lang.Math.abs(this.physicsCompanionHalfX - halfX) > REBUILD_EPSILON
                || java.lang.Math.abs(this.physicsCompanionHalfY - halfY) > REBUILD_EPSILON
                || java.lang.Math.abs(this.physicsCompanionHalfZ - halfZ) > REBUILD_EPSILON) {
            return true;
        }

        return java.lang.Math.abs(this.physicsCompanionMass - mass) > REBUILD_EPSILON;
    }

    @Unique
    private void createPhysicsCompanion(final Entity entity, final ServerLevel serverLevel, final double mass, final double halfX, final double halfY, final double halfZ, final AABB box) {
        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        if (physicsSystem == null) {
            return;
        }

        this.updateCompanionCenter(box);
        this.physicsCompanionPose.position().set(this.physicsCompanionCenter);
        this.physicsCompanionOrientation.identity();

        this.physicsCompanion = new BoxPhysicsObject(this.physicsCompanionPose, new Vector3d(halfX, halfY, halfZ), mass);
        physicsSystem.addObject(this.physicsCompanion);
        physicsSystem.getPipeline().teleport(this.physicsCompanion, this.physicsCompanionCenter, this.physicsCompanionOrientation);
        this.physicsCompanion.updatePose();
        this.renderDebugCollisionBox(serverLevel);
        this.updateLastPlayerCenter(entity);

        this.physicsCompanionLevel = serverLevel;
        this.physicsCompanionHalfX = halfX;
        this.physicsCompanionHalfY = halfY;
        this.physicsCompanionHalfZ = halfZ;
        this.physicsCompanionMass = mass;
    }

    @Unique
    private void removePhysicsCompanion() {
        if (this.physicsCompanion != null && this.physicsCompanionLevel != null && this.physicsCompanion.isActive()) {
            final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(this.physicsCompanionLevel);
            if (physicsSystem != null) {
                physicsSystem.removeObject(this.physicsCompanion);
            }
        }

        this.physicsCompanion = null;
        this.physicsCompanionLevel = null;
        this.physicsCompanionHalfX = 0.0;
        this.physicsCompanionHalfY = 0.0;
        this.physicsCompanionHalfZ = 0.0;
        this.physicsCompanionMass = 0.0;
        this.hasLastPlayerCenter = false;
    }

    @Unique
    private Vector3d getTargetVelocity(final Entity entity) {
        if (!this.hasLastPlayerCenter) {
            this.updateLastPlayerCenter(entity);
            return new Vector3d();
        }

        final AABB box = entity.getBoundingBox();
        final Vector3d currentCenter = this.getCompanionCenter(box, new Vector3d());
        final Vector3d velocity = new Vector3d(
                (currentCenter.x - this.lastPlayerCenter.x) * TARGET_VELOCITY_SCALE,
                (currentCenter.y - this.lastPlayerCenter.y) * TARGET_VELOCITY_SCALE,
                (currentCenter.z - this.lastPlayerCenter.z) * TARGET_VELOCITY_SCALE
        );

        // getDeltaMovement keeps vanilla gravity even while grounded. The companion box should
        // instead follow the player's actual world displacement from this tick.
        if (entity.onGround() && velocity.y < 0.0) {
            velocity.y = 0.0;
        }

        return velocity;
    }

    @Unique
    private void updateLastPlayerCenter(final Entity entity) {
        final AABB box = entity.getBoundingBox();
        this.getCompanionCenter(box, this.lastPlayerCenter);
        this.hasLastPlayerCenter = true;
    }

    @Unique
    private void updateCompanionCenter(final AABB box) {
        this.getCompanionCenter(box, this.physicsCompanionCenter);
    }

    @Unique
    private double getCompanionHalfY(final AABB box) {
        double minY = box.minY;
        double maxY = box.maxY + PLAYER_COLLISION_TOP;

        if (!MassRegistry.isExperimentalPlayerMassEnabled()) {
            minY += PLAYER_INTERACTION_FEET_OFFSET;
        }

        return java.lang.Math.max((maxY - minY) * 0.5, MIN_HALF_HEIGHT);
    }

    @Unique
    private Vector3d getCompanionCenter(final AABB box, final Vector3d dest) {
        final double minX = box.minX;
        final double maxX = box.maxX;
        final double minZ = box.minZ;
        final double maxZ = box.maxZ;
        final double centerX = (minX + maxX) * 0.5;
        final double centerZ = (minZ + maxZ) * 0.5;

        final double minY = box.minY + (!MassRegistry.isExperimentalPlayerMassEnabled() ? PLAYER_INTERACTION_FEET_OFFSET : 0.0);
        final double halfY = this.getCompanionHalfY(box);
        return dest.set(centerX, minY + halfY, centerZ);
    }

    @Unique
    private void clampMagnitude(final Vector3d vector, final double maxLength) {
        final double maxLengthSquared = maxLength * maxLength;
        if (vector.lengthSquared() <= maxLengthSquared) {
            return;
        }

        vector.normalize(maxLength);
    }

    @Unique
    private void renderDebugCollisionBox(final ServerLevel serverLevel) {
        if (!DEBUG_COLLISION_BOX || this.physicsCompanion == null || !this.physicsCompanion.isActive()) {
            return;
        }

        if ((serverLevel.getGameTime() & 1L) != 0L) {
            return;
        }

        final Pose3dc pose = this.physicsCompanion.getPose();
        final Vector3d localPoint = new Vector3d();
        final Vector3d globalPoint = new Vector3d();

        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ, this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ);

        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ, this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ);

        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, -this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, this.physicsCompanionHalfX, -this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, -this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
        this.spawnDebugEdge(serverLevel, pose, localPoint, globalPoint, this.physicsCompanionHalfX, this.physicsCompanionHalfY, -this.physicsCompanionHalfZ, this.physicsCompanionHalfX, this.physicsCompanionHalfY, this.physicsCompanionHalfZ);
    }

    @Unique
    private void spawnDebugEdge(final ServerLevel serverLevel, final Pose3dc pose, final Vector3d localPoint, final Vector3d globalPoint,
                                            final double startX, final double startY, final double startZ,
                                            final double endX, final double endY, final double endZ) {
        for (int step = 0; step <= 4; step++) {
            final double alpha = step / 4.0;
            localPoint.set(
                    startX + (endX - startX) * alpha,
                    startY + (endY - startY) * alpha,
                    startZ + (endZ - startZ) * alpha
            );
            pose.transformPosition(localPoint, globalPoint);
            serverLevel.sendParticles(ParticleTypes.END_ROD, globalPoint.x, globalPoint.y, globalPoint.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
