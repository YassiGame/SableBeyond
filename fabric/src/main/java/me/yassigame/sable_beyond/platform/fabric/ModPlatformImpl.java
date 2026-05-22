package me.yassigame.sable_beyond.platform.fabric;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import me.yassigame.sable_beyond.platform.LoadedModInfo;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ModPlatformImpl {
    public static String getLoaderName() {
        return "Fabric";
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(SableBeyond.MOD_ID);
    }

    public static void openConfigScreen() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(SableBeyondClothConfigScreen.create(
                minecraft.screen,
                FabricLoader.getInstance().getConfigDir()
        ));
    }

    public static List<LoadedModInfo> getLoadedMods() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> new LoadedModInfo(
                        mod.getMetadata().getId(),
                        mod.getMetadata().getVersion().getFriendlyString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static Set<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
