package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.mechanical_arm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import static com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.getRange;

@Pseudo
@Mixin(ArmBlockEntity.class)
public abstract class MechanicalArmSublevelInteraction extends KineticBlockEntity {
    protected MechanicalArmSublevelInteraction(final BlockEntityType<?> typeIn, final BlockPos pos,
                                               final BlockState state) {
        super(typeIn, pos, state);
    }

    @WrapOperation(
            method = "searchForItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;isValid()Z"
            ),
            remap = false
    )
    public boolean sableBeyond$searchSubLevelsForItem(final ArmInteractionPoint instance,
                                                      final Operation<Boolean> original,
                                                      @Local final ArmInteractionPoint armInteractionPoint) {
        final BlockPos armPos = getBlockPos();
        final BlockPos pointPos = instance.getPos();
        final double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(
                getLevel(),
                armPos.getX(), armPos.getY(), armPos.getZ(),
                pointPos.getX(), pointPos.getY(), pointPos.getZ()
        );
        if (distanceSquared > Mth.square(getRange())) {
            return false;
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "searchForDestination",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;isValid()Z"
            ),
            remap = false
    )
    public boolean sableBeyond$searchSubLevelsForDestination(final ArmInteractionPoint instance,
                                                             final Operation<Boolean> original,
                                                             @Local final ArmInteractionPoint armInteractionPoint) {
        final BlockPos armPos = getBlockPos();
        final BlockPos pointPos = instance.getPos();
        final double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(
                getLevel(),
                armPos.getX(), armPos.getY(), armPos.getZ(),
                pointPos.getX(), pointPos.getY(), pointPos.getZ()
        );
        if (distanceSquared > Mth.square(getRange())) {
            return false;
        }
        return original.call(instance);
    }
}
