package me.yassigame.sable_beyond.utils;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;

public final class SableMassCompat {

    // gets the mass sable put on the block of that minecraft item entity
    public static double getBlockMass(final ItemEntity itemEntity, final BlockItem blockItem) {
        final var state = blockItem.getBlock().defaultBlockState();
        final var pos = BlockPos.containing(itemEntity.position());
        return PhysicsBlockPropertyHelper.getMass(itemEntity.level(), pos, state);
    }
}
