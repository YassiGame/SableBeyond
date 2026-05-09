package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.mechanical_arm;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Pseudo
@Mixin(ArmBlockEntity.class)
public abstract class MechanicalArmConfigurationRefresh extends KineticBlockEntity {
    @Shadow
    private List<ArmInteractionPoint> inputs;

    @Shadow
    private List<ArmInteractionPoint> outputs;

    @Shadow
    private ListTag interactionPointTag;

    @Shadow
    private boolean updateInteractionPoints;

    @Shadow
    protected abstract boolean isAreaActuallyLoaded(BlockPos center, int range);

    protected MechanicalArmConfigurationRefresh(final BlockEntityType<?> typeIn, final BlockPos pos,
                                                final BlockState state) {
        super(typeIn, pos, state);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sableBeyond$refreshConfigurationAfterSubLevelMove(final CallbackInfo ci) {
        if (this.updateInteractionPoints || this.interactionPointTag == null || this.level == null) {
            return;
        }

        if (!this.isAreaActuallyLoaded(this.worldPosition, ArmBlockEntity.getRange() + 1)) {
            return;
        }

        if (this.sableBeyond$matchesSerializedConfiguration()) {
            return;
        }

        // Split/move operations can relocate the arm and its targets to a new plot
        // while Create still holds runtime points with the previous absolute positions.
        this.updateInteractionPoints = true;
    }

    @Unique
    private boolean sableBeyond$matchesSerializedConfiguration() {
        final Set<PointSignature> expectedPoints = new HashSet<>();
        int expectedCount = 0;

        for (final Tag tag : this.interactionPointTag) {
            if (!(tag instanceof final CompoundTag compoundTag)) {
                continue;
            }

            final ArmInteractionPoint point =
                    ArmInteractionPoint.deserialize(compoundTag, this.level, this.worldPosition);
            if (point == null) {
                continue;
            }

            expectedPoints.add(PointSignature.from(point));
            expectedCount++;
        }

        final Set<PointSignature> actualPoints = new HashSet<>();
        int actualCount = 0;

        for (final ArmInteractionPoint point : this.inputs) {
            actualPoints.add(PointSignature.from(point));
            actualCount++;
        }

        for (final ArmInteractionPoint point : this.outputs) {
            actualPoints.add(PointSignature.from(point));
            actualCount++;
        }

        return expectedCount == actualCount && expectedPoints.equals(actualPoints);
    }

    @Unique
    private record PointSignature(BlockPos pos, ArmInteractionPoint.Mode mode, ArmInteractionPointType type) {
        private static PointSignature from(final ArmInteractionPoint point) {
            return new PointSignature(point.getPos(), point.getMode(), point.getType());
        }
    }
}
