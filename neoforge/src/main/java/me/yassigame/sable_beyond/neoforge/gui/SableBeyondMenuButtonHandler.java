package me.yassigame.sable_beyond.neoforge.gui;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.gui.SableBeyondMenuButtonPlacer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = SableBeyond.MOD_ID, value = Dist.CLIENT)
public final class SableBeyondMenuButtonHandler {

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event) {
        SableBeyondMenuButtonPlacer.createButton(event.getScreen(), event.getListenersList())
                .ifPresent(event::addListener);
    }
}
