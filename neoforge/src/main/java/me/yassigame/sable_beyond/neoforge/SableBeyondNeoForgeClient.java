package me.yassigame.sable_beyond.neoforge;

import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.config.cloth.SableBeyondClothConfigScreen;
import me.yassigame.sable_beyond.neoforge.config.cloth.SableBeyondNeoForgeClothConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class SableBeyondNeoForgeClient {

    public static void registerConfigScreen(final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, modListScreen) -> SableBeyondClothConfigScreen.create(
                        modListScreen,
                        FMLPaths.CONFIGDIR.get(),
                        SableBeyondNeoForgeClothConfig::addCategories,
                        SableBeyondNeoForgeClothConfig::saveAll
                ));
    }

    public static void registerClientEvents() {
        NeoForge.EVENT_BUS.addListener(SableBeyondNeoForgeClient::clearDynamicMassOnClientLogin);
        NeoForge.EVENT_BUS.addListener(SableBeyondNeoForgeClient::clearDynamicMassOnClientLogout);
    }

    private static void clearDynamicMassOnClientLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        DynamicMass.resetRuntimeBlockMasses();
    }

    private static void clearDynamicMassOnClientLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        DynamicMass.resetRuntimeBlockMasses();
    }
}
