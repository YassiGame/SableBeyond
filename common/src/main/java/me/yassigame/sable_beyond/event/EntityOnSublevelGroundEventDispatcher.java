package me.yassigame.sable_beyond.event;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import me.yassigame.sable_beyond.api.event.EntityOnSublevelGroundEvent;
import me.yassigame.sable_beyond.api.event.SableBeyondEvents;
import me.yassigame.sable_beyond.api.mass.MassRegistry;
import me.yassigame.sable_beyond.sublevel.SableBeyondSubLevelEntityHelper;
import net.minecraft.world.entity.Entity;

public final class EntityOnSublevelGroundEventDispatcher {

    public static void onPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        for (final Entity entity : physicsSystem.getLevel().getAllEntities()) {
            final ServerSubLevel subLevel = SableBeyondSubLevelEntityHelper.getGroundedTrackingSubLevel(entity);
            if (subLevel == null) {
                continue;
            }

            // global event for entity on sublevel
            SableBeyondEvents.fireEntityOnSublevelGround(new EntityOnSublevelGroundEvent(entity, subLevel, timeStep));

            // event for mass applied on sublevel by entity
            if (MassRegistry.isMassAppliedEntity(entity)) {
                SableBeyondEvents.fireEntityOnSublevelGround(new EntityOnSublevelGroundEvent(entity, subLevel, timeStep));
            }
        }
    }
}
