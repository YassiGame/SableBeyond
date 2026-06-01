package me.yassigame.sable_beyond.neoforge.mixin;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class SableBeyondNeoForgeMixinPlugin implements IMixinConfigPlugin {
    private static final String SLICE_AND_DICE_PACKAGE =
            "me.yassigame.sable_beyond.neoforge.mixin.compatibility.sliceanddice.";

    private static boolean isModLoadedEarly(String modId) {
        LoadingModList loading = FMLLoader.getLoadingModList();
        return loading != null && loading.getModFileById(modId) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(SLICE_AND_DICE_PACKAGE)) {
            return isModLoadedEarly("sliceanddice");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}