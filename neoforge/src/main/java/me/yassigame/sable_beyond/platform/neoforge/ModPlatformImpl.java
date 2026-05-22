package me.yassigame.sable_beyond.platform.neoforge;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import me.yassigame.sable_beyond.neoforge.config.cloth.SableBeyondNeoForgeClothConfig;
import me.yassigame.sable_beyond.platform.LoadedModInfo;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ModPlatformImpl {
    public static String getLoaderName() {
        return "NeoForge";
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(SableBeyond.MOD_ID);
    }

    public static void openConfigScreen() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(SableBeyondClothConfigScreen.create(
                minecraft.screen,
                FMLPaths.CONFIGDIR.get(),
                SableBeyondNeoForgeClothConfig::addCategories,
                SableBeyondNeoForgeClothConfig::saveAll
        ));
    }

    public static List<LoadedModInfo> getLoadedMods() {
        return ModList.get().getMods().stream()
                .map(mod -> new LoadedModInfo(mod.getModId(), mod.getVersion().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static Set<String> getLoadedModIds() {
        return ModList.get().getMods().stream()
                .map(mod -> mod.getModId())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
