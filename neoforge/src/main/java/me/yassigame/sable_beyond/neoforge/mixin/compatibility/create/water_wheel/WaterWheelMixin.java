package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.water_wheel;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import me.yassigame.sable_beyond.utils.SableSubLevelPosHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(WaterWheelBlockEntity.class)
public abstract class WaterWheelMixin extends GeneratingKineticBlockEntity implements BlockEntitySubLevelActor {

    protected WaterWheelMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    protected abstract Axis getAxis();

    @Shadow
    protected abstract Set<BlockPos> getOffsetsToCheck();

    @Shadow
    protected abstract int getSize();

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!SableBeyondNeoForgeConfig.current().compatibility.create.water_wheel.thrust_enabled || !sableBeyond$isPoweredByNetwork() || level == null || timeStep <= 0.0d) {
            return;
        }

        final List<BlockPos> waterContacts = sableBeyond$getWaterContacts();
        if (waterContacts.isEmpty()) {
            return;
        }

        final double rpm = -getSpeed();
        double forcePerContact = Math.abs(rpm) * SableBeyondNeoForgeConfig.current().compatibility.create.water_wheel.thrust_per_rpm * timeStep / waterContacts.size();
        // small water wheel
        if (this.getSize() == 1) {
            forcePerContact *= SableBeyondNeoForgeConfig.current().compatibility.create.water_wheel.small_wheel_factor;
        }

        final double rotationSign = Math.signum(rpm);

        // normal mode
        if (!SableBeyondNeoForgeConfig.current().compatibility.create.water_wheel.realistic_thrust_mode) {
            sableBeyond$applyStraightForce(
                    subLevel,
                    waterContacts,
                    forcePerContact * waterContacts.size(),
                    rotationSign
            );
            return;
        }

        // realistic mode
        for (BlockPos contactPos : waterContacts) {
            final Vec3 radialDirection = Vec3.atLowerCornerOf(contactPos.subtract(worldPosition)).normalize();
            final Vec3 bladeMotion = VecHelper.rotate(radialDirection, 90.0d, getAxis()).scale(rotationSign);
            final Vec3 reactionForce = bladeMotion.scale(forcePerContact);

            subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
                    .applyAndRecordPointForce(
                            JOMLConversion.atCenterOf(contactPos),
                            new Vector3d(reactionForce.x, reactionForce.y, reactionForce.z)
                    );
        }
    }

    @Unique
    private void sableBeyond$applyStraightForce(ServerSubLevel subLevel, List<BlockPos> waterContacts,
                                                double force, double rotationSign) {
        final Vector3d averageContact = new Vector3d();
        for (BlockPos contactPos : waterContacts) {
            averageContact.add(JOMLConversion.atCenterOf(contactPos));
        }
        averageContact.div(waterContacts.size());

        final Vec3 wheelCenter = Vec3.atCenterOf(worldPosition);
        final Vec3 radialDirection = new Vec3(
                averageContact.x - wheelCenter.x,
                averageContact.y - wheelCenter.y,
                averageContact.z - wheelCenter.z
        );
        if (radialDirection.lengthSqr() <= 1.0E-8d) {
            return;
        }

        final Vec3 straightForce = VecHelper.rotate(radialDirection.normalize(), 90.0d, getAxis())
                .scale(rotationSign * force);

        subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
                .applyAndRecordPointForce(
                        averageContact,
                        new Vector3d(straightForce.x, straightForce.y, straightForce.z)
                );
    }

    @Unique
    private boolean sableBeyond$isPoweredByNetwork() {
        return !isSource() && hasSource() && Math.abs(getSpeed()) > 0.01f;
    }

    @Unique
    private List<BlockPos> sableBeyond$getWaterContacts() {
        if (level==null) { return List.of(); }
        final List<BlockPos> contacts = new ArrayList<>();
        for (BlockPos offset : getOffsetsToCheck()) {
            final BlockPos localPos = offset.offset(worldPosition);
            final BlockPos realPos = SableSubLevelPosHelper.resolveRealBlockPos(level, localPos);
            if (level.getFluidState(realPos).is(FluidTags.WATER)) {
                contacts.add(localPos);
            }
        }
        return contacts;
    }
}