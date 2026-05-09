package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.mechanical_arm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmInteractionPoint.class)
public abstract class ArmInteractionSubLevelPointAnimation {

    @Shadow
    protected Level level;

    @Shadow
    protected abstract Vec3 getInteractionPositionVector();

    @Shadow
    protected abstract Direction getInteractionDirection();

    @Inject(method = "getTargetAngles", at = @At("HEAD"), cancellable = true)
    private void sableBeyond$useProjectedTargetPosition(final BlockPos armPos, final boolean ceiling,
                                                        final CallbackInfoReturnable<ArmAngleTarget> cir) {
        final SubLevel armSubLevel = Sable.HELPER.getContaining(this.level, armPos);
        final Vec3 interactionPos = this.getInteractionPositionVector();
        final SubLevel targetSubLevel = Sable.HELPER.getContaining(this.level, interactionPos);

        if (armSubLevel == targetSubLevel) {
            return;
        }

        Vec3 localTarget = Sable.HELPER.projectOutOfSubLevel(this.level, interactionPos);
        if (armSubLevel != null) {
            localTarget = armSubLevel.logicalPose().transformPositionInverse(localTarget);
        }

        Vec3 localDirection = Vec3.atLowerCornerOf(this.getInteractionDirection().getNormal());
        if (targetSubLevel != null) {
            localDirection = targetSubLevel.logicalPose().transformNormal(localDirection);
        }
        if (armSubLevel != null) {
            localDirection = armSubLevel.logicalPose().transformNormalInverse(localDirection);
        }

        final Direction interactionDirection =
                Direction.getNearest(localDirection.x, localDirection.y, localDirection.z);

        cir.setReturnValue(new ArmAngleTarget(armPos, localTarget, interactionDirection, ceiling));
    }
}
