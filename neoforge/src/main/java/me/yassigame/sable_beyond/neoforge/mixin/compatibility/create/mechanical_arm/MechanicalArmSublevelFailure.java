package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.mechanical_arm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(value = ArmInteractionPointHandler.class, priority = 2000)
public abstract class MechanicalArmSublevelFailure {

    @Shadow
    private static List<ArmInteractionPoint> currentSelection;

    @Shadow
    private static ItemStack currentItem;

    @Inject(method = "flushSettings", at = @At("HEAD"), cancellable = true)
    private static void sableBeyond$flushSettings(final BlockPos pos, final CallbackInfo ci) {
        if (currentSelection == null) {
            ci.cancel();
            return;
        }

        final Level level = Minecraft.getInstance().level;
        final SubLevel parentSubLevel = level == null ? null : Sable.HELPER.getContainingClient(pos);
        final boolean ignoreRangeForSublevels = SableBeyondNeoForgeConfig.ignorePlacementRangeForSublevelTargets();
        final double maxDistanceSquared = Mth.square(ArmBlockEntity.getRange());

        int removed = 0;
        int pointsRemovedSubLevel = 0;

        for (final Iterator<ArmInteractionPoint> iterator = currentSelection.iterator(); iterator.hasNext(); ) {
            final ArmInteractionPoint point = iterator.next();
            final BlockPos pointPos = point.getPos();

            final boolean withinRange;
            if (level == null) {
                withinRange = pointPos.closerThan(pos, ArmBlockEntity.getRange());
            } else {
                final SubLevel pointSubLevel = Sable.HELPER.getContainingClient(pointPos);
                if (ignoreRangeForSublevels && (parentSubLevel != null || pointSubLevel != null)) {
                    withinRange = true;
                } else {
                    final double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(level,
                            pointPos.getX(), pointPos.getY(), pointPos.getZ(),
                            pos.getX(), pos.getY(), pos.getZ());
                    withinRange = distanceSquared < maxDistanceSquared;
                    if (!withinRange && parentSubLevel != pointSubLevel) {
                        pointsRemovedSubLevel++;
                    }
                }
            }

            if (withinRange) {
                continue;
            }

            iterator.remove();
            removed++;
        }

        final LocalPlayer player = Minecraft.getInstance().player;
        if (removed > 0) {
            if (pointsRemovedSubLevel == 0) {
                CreateLang.builder()
                        .translate("mechanical_arm.points_outside_range", removed)
                        .style(ChatFormatting.RED)
                        .sendStatus(player);
            } else if (removed == pointsRemovedSubLevel) {
                CreateLang.builder()
                        .add(Component.translatable("sable_beyond.create.mechanical_arm.points_removed_distant_sublevel", removed)
                                .withStyle(ChatFormatting.RED))
                        .sendStatus(player);
            } else {
                CreateLang.builder()
                        .add(Component.translatable("sable_beyond.create.mechanical_arm.points_removed_distant_sublevel_and_range", removed)
                                .withStyle(ChatFormatting.RED))
                        .sendStatus(player);
            }
        } else {
            int inputs = 0;
            int outputs = 0;

            for (final ArmInteractionPoint armInteractionPoint : currentSelection) {
                if (armInteractionPoint.getMode() == ArmInteractionPoint.Mode.DEPOSIT) {
                    outputs++;
                } else {
                    inputs++;
                }
            }

            if (inputs + outputs > 0) {
                CreateLang.builder()
                        .translate("mechanical_arm.summary", inputs, outputs)
                        .style(ChatFormatting.WHITE)
                        .sendStatus(player);
            }
        }

        CatnipServices.NETWORK.sendToServer(new ArmPlacementPacket(currentSelection, pos));
        currentSelection.clear();
        currentItem = null;
        ci.cancel();
    }
}
