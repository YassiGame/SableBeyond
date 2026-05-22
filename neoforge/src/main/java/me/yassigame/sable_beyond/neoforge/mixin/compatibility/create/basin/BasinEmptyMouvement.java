package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import me.yassigame.sable_beyond.neoforge.SableBeyondNeoForge;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondNeoForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinBlockEntity.class)
public class BasinEmptyMouvement {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void sableBeyond$basinEmptyMouvement(CallbackInfo ci) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level =  basin.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockPos blockPos = basin.getBlockPos();
        Vec3 motion = getBasinOpeningMotion(level, blockPos);

        if (motion.normalize().dot(new Vec3(0, 1, 0)) > -0.5) {
            return; // not upside down enough
        }

        // what a long config name type shit
        if (SableBeyondNeoForgeConfig.current().compatibility.create.basin.basin_empty_input_inventory) {
            dumpInventory(level, blockPos, basin.getInputInventory(), motion);
        }
        if (SableBeyondNeoForgeConfig.current().compatibility.create.basin.basin_empty_output_inventory) {
            dumpInventory(level, blockPos, basin.getOutputInventory(), motion);
        }

    }

    private static Vec3 getBasinOpeningMotion(Level level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);

        Vec3 localUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 worldOpeningDirection = subLevel == null
                ? localUp
                : subLevel.logicalPose().transformNormal(localUp).normalize();

        return worldOpeningDirection.scale(0.05);
    }

    private static void dumpInventory(Level level, BlockPos pos, IItemHandlerModifiable inventory, Vec3 motion) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemEntity item = new ItemEntity(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.95,
                    pos.getZ() + 0.5,
                    stack.copy()
            );
            item.setDeltaMovement(motion);
            level.addFreshEntity(item);

            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
