package me.yassigame.sable_beyond.fabric;

import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.gui.SableBeyondMenuButtonPlacer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

public final class SableBeyondFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            final var widgets = Screens.getButtons(screen);
            SableBeyondMenuButtonPlacer.createButton(screen, widgets)
                    .ifPresent(widgets::add);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DynamicMass.resetRuntimeBlockMasses());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DynamicMass.resetRuntimeBlockMasses());
    }
}
