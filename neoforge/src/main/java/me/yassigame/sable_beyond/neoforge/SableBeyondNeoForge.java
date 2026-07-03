package me.yassigame.sable_beyond.neoforge;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.event.SableBeyondEvents;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import me.yassigame.sable_beyond.command.SableBeyondCommand;
import me.yassigame.sable_beyond.event.EntityMassOnSublevelListener;
import me.yassigame.sable_beyond.event.EntityOnSublevelGroundEventDispatcher;
import me.yassigame.sable_beyond.event.FlowingFluidSubLevelForceHandler;
import me.yassigame.sable_beyond.neoforge.config.SableBeyondConfigLoader;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.fan_airflow.EncasedFanSubLevelAirflowHelper;
import me.yassigame.sable_beyond.platform.ModPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(SableBeyond.MOD_ID)
public final class SableBeyondNeoForge {
    public SableBeyondNeoForge() {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            SableBeyondNeoForgeClient.registerConfigScreen(ModLoadingContext.get().getActiveContainer());
            SableBeyondNeoForgeClient.registerClientEvents();
        }

        SableBeyondConfigLoader.load();
        if (ModPlatform.isModLoaded("create")) {
            SableEventPlatform.INSTANCE.onPhysicsTick(EncasedFanSubLevelAirflowHelper::applyRegisteredFansForPhysicsTick);
        }
        SableEventPlatform.INSTANCE.onPhysicsTick(EntityOnSublevelGroundEventDispatcher::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPhysicsTick(FlowingFluidSubLevelForceHandler::onPhysicsTick);
        SableBeyondEvents.registerEntityOnSublevelGround(EntityMassOnSublevelListener::onEntityMassOnSublevel);
        NeoForge.EVENT_BUS.addListener(SableBeyondConfigLoader::reloadOnServerStarting);
        NeoForge.EVENT_BUS.addListener(this::clearDynamicMassOnServerStarting);
        NeoForge.EVENT_BUS.addListener(this::clearDynamicMassOnServerStopped);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Run our common setup.
        SableBeyond.init();
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        SableBeyondCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    private void clearDynamicMassOnServerStarting(final ServerStartedEvent event) {
        DynamicMass.resetRuntimeBlockMasses();
        DynamicMass.restoreSavedMasses(event.getServer());
    }

    private void clearDynamicMassOnServerStopped(final ServerStoppedEvent event) {
        DynamicMass.resetRuntimeBlockMasses();
    }
}
