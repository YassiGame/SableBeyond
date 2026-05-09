package me.yassigame.sable_beyond.neoforge;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.event.SableBeyondEvents;
import me.yassigame.sable_beyond.command.SableBeyondCommand;
import me.yassigame.sable_beyond.event.EntityMassOnSublevelListener;
import me.yassigame.sable_beyond.event.EntityOnSublevelGroundEventDispatcher;
import me.yassigame.sable_beyond.neoforge.config.EntityMassConfigLoader;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.fan_airflow.EncasedFanSubLevelAirflowHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SableBeyond.MOD_ID)
public final class SableBeyondNeoForge {
    public SableBeyondNeoForge() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, SableBeyondNeoForgeConfig.CLIENT_SPEC, SableBeyond.MOD_ID + "/client.toml");
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, SableBeyondNeoForgeConfig.COMMON_SPEC, SableBeyond.MOD_ID + "/common.toml");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            SableBeyondNeoForgeClient.registerConfigScreen(ModLoadingContext.get().getActiveContainer());
        }

        EntityMassConfigLoader.load();
        // doing this to get physics timeStep because i cannot override sable$physicsTick
        // TODO see if possible to have timeSetp without an other event and use only the encased fan mixin tick or create a sableBeyond$physicsTick
        SableEventPlatform.INSTANCE.onPhysicsTick(EncasedFanSubLevelAirflowHelper::applyRegisteredFansForPhysicsTick);
        SableEventPlatform.INSTANCE.onPhysicsTick(EntityOnSublevelGroundEventDispatcher::onPhysicsTick);
        SableBeyondEvents.registerEntityOnSublevelGround(EntityMassOnSublevelListener::onEntityMassOnSublevel);
        NeoForge.EVENT_BUS.addListener(EntityMassConfigLoader::reloadOnServerStarting);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Run our common setup.
        SableBeyond.init();
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        SableBeyondCommand.register(event.getDispatcher(), event.getBuildContext());
    }
}
