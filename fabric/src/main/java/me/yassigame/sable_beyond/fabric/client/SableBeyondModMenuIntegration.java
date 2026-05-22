package me.yassigame.sable_beyond.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

public final class SableBeyondModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> SableBeyondClothConfigScreen.create(parent, FabricLoader.getInstance().getConfigDir());
    }
}
