package me.yassigame.sable_beyond.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class SableBeyondNeoForgeClient {

    public static void registerConfigScreen(final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, modListScreen) -> new ConfigurationScreen(modContainer, modListScreen));
    }
}
