package me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.basin.escaping;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class FluidEscapingRenderer {
    private static final float STREAM_HALF_WIDTH = 4.0f / 16.0f;

    public static void render(BasinBlockEntity basin, FluidEscapingAccess access, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        final Level level = basin.getLevel();
        if (level == null || (!access.sableBeyond$isFluidEscapingActive()
                && !access.sableBeyond$isFluidEscapingFadingOut())) {
            return;
        }

        final FluidStack fluidStack = FluidEscaping.getRenderedEscapingFluid(basin, access, partialTicks);
        if (fluidStack.isEmpty()) {
            return;
        }

        final float progress = FluidEscaping.getProgress(access, partialTicks);
        if (progress <= 0.0f) {
            return;
        }

        final BlockPos basinPos = basin.getBlockPos();
        final SubLevel subLevel = FluidEscaping.getContainingSubLevel(level, basinPos);
        final Vec3 worldStart = getWorldStreamStart(level, basinPos, progress, partialTicks);
        final BlockHitResult hit = raycastRealWorldDown(
                level,
                worldStart.add(0.0d, -FluidEscaping.WORLD_RAY_START_OFFSET, 0.0d),
                partialTicks
        );
        final Vec3 worldEnd = hit != null
                ? getWorldRaycastLocation(level, hit, partialTicks)
                : new Vec3(worldStart.x, level.getMinBuildHeight(), worldStart.z);
        final double worldLength = worldStart.distanceTo(worldEnd);
        final Vec3 localStart = transformWorldToRenderLocal(subLevel, basinPos, worldStart, partialTicks);
        final Vec3 localEnd = transformWorldToRenderLocal(subLevel, basinPos, worldEnd, partialTicks);
        final Vec3 localDirection = localEnd.subtract(localStart);
        final double localLength = localDirection.length();
        if (!isFinite(localStart)
                || !isFinite(localEnd)
                || !Double.isFinite(localLength)
                || localLength <= 0.0d
                || worldLength > FluidEscaping.MAX_FLUID_RAYCAST_DISTANCE) {
            return;
        }

        final Vec3 normalizedDirection = localDirection.normalize();
        final Quaternionf streamRotation = new Quaternionf().rotationTo(
                0.0f, 1.0f, 0.0f,
                (float) normalizedDirection.x, (float) normalizedDirection.y, (float) normalizedDirection.z
        );

        poseStack.pushPose();
        poseStack.translate(localStart.x, localStart.y, localStart.z);
        poseStack.mulPose(streamRotation);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(
                fluidStack,
                -STREAM_HALF_WIDTH * progress, -0.5f, -STREAM_HALF_WIDTH * progress,
                STREAM_HALF_WIDTH * progress, (float) localLength, STREAM_HALF_WIDTH * progress,
                buffer, poseStack, light, true, true
        );
        poseStack.popPose();
    }

    public static float getPartialTicks() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static Vec3 getWorldStreamStart(Level level, BlockPos pos, float progress, float partialTicks) {
        final Vec3 localPosition = Vec3.atLowerCornerOf(pos)
                .add(0.5d, FluidEscaping.getStreamStartY(progress), 0.5d);
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        return subLevel == null ? localPosition : getRenderPose(subLevel, partialTicks).transformPosition(localPosition);
    }

    public static @Nullable BlockHitResult raycastRealWorldDown(Level level, Vec3 worldStart, float partialTicks) {
        if (!(level instanceof LevelPoseProviderExtension extension)) {
            return FluidEscaping.raycastRealWorldDown(level, worldStart);
        }

        extension.sable$pushPoseSupplier(subLevel -> getRenderPose((SubLevel) subLevel, partialTicks));
        try {
            return FluidEscaping.raycastRealWorldDown(level, worldStart);
        } finally {
            extension.sable$popPoseSupplier();
        }
    }

    public static Vec3 getWorldRaycastLocation(Level level, BlockHitResult hit, float partialTicks) {
        final SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hit.getBlockPos());
        return hitSubLevel == null
                ? hit.getLocation()
                : getRenderPose(hitSubLevel, partialTicks).transformPosition(hit.getLocation());
    }

    private static Pose3dc getRenderPose(SubLevel subLevel, float partialTicks) {
        return subLevel instanceof ClientSubLevel clientSubLevel
                ? clientSubLevel.renderPose(partialTicks)
                : subLevel.logicalPose();
    }

    private static Vec3 transformWorldToRenderLocal(@Nullable SubLevel subLevel, BlockPos basinPos, Vec3 worldPosition,
                                                    float partialTicks) {
        final Vec3 localPosition;
        if (subLevel instanceof ClientSubLevel clientSubLevel) {
            localPosition = clientSubLevel.renderPose(partialTicks).transformPositionInverse(worldPosition);
        } else if (subLevel != null) {
            localPosition = subLevel.logicalPose().transformPositionInverse(worldPosition);
        } else {
            localPosition = worldPosition;
        }

        return localPosition.subtract(Vec3.atLowerCornerOf(basinPos));
    }

    private static boolean isFinite(Vec3 vec) {
        return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }
}
