package me.yassigame.sable_beyond.fabric;

import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.command.SableBeyondCommand;
import me.yassigame.sable_beyond.fabric.config.SableBeyondConfigLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class SableBeyondFabric implements ModInitializer {
    // why fabric ? why not fabric has nothing about sable (and also is an excuse to learn how to dev multiplatform project)

    @Override
    public void onInitialize() {
        SableBeyondConfigLoader.load();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            DynamicMass.resetRuntimeBlockMasses();
            SableBeyondConfigLoader.load();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(DynamicMass::restoreSavedMasses);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DynamicMass.resetRuntimeBlockMasses());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SableBeyondCommand.register(dispatcher, registryAccess));
        SableBeyondFabricSableHooks.register();

        SableBeyond.init();
    }
}
