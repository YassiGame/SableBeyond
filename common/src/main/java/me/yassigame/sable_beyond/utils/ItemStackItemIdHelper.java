package me.yassigame.sable_beyond.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemStackItemIdHelper {
    public static @Nullable ResourceLocation getItemId(final ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
