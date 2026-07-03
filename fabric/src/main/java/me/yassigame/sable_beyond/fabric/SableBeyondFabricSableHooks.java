package me.yassigame.sable_beyond.fabric;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import me.yassigame.sable_beyond.api.event.SableBeyondEvents;
import me.yassigame.sable_beyond.event.EntityMassOnSublevelListener;
import me.yassigame.sable_beyond.event.EntityOnSublevelGroundEventDispatcher;
import me.yassigame.sable_beyond.event.FlowingFluidSubLevelForceHandler;

final class SableBeyondFabricSableHooks {

    static void register() {
        SableEventPlatform.INSTANCE.onPhysicsTick(EntityOnSublevelGroundEventDispatcher::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPhysicsTick(FlowingFluidSubLevelForceHandler::onPhysicsTick);
        SableBeyondEvents.registerEntityOnSublevelGround(EntityMassOnSublevelListener::onEntityMassOnSublevel);
    }
}
