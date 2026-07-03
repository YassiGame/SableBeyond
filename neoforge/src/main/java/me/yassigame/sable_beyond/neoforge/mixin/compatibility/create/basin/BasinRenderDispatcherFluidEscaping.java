package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.basin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping.FluidEscapingAccess;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping.FluidEscapingRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BasinRenderDispatcherFluidEscaping {

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("TAIL")
    )
    private <E extends BlockEntity> void sableBeyond$renderFluidEscaping(E blockEntity, float partialTick,
                                                                         PoseStack poseStack, MultiBufferSource buffer,
                                                                         CallbackInfo ci) {
        if (!(blockEntity instanceof BasinBlockEntity basin) || !(blockEntity instanceof FluidEscapingAccess access)) {
            return;
        }

        final Level level = basin.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }

        final int light = LevelRenderer.getLightColor(level, basin.getBlockPos());
        FluidEscapingRenderer.render(basin, access, partialTick, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
    }
}
