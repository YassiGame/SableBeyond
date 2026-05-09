package me.yassigame.sable_beyond.api.event;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.Entity;

// FOR NOW
// using only the EntityOnSublevelGroundEvent as the event for the mass on sublevel
// THIS IS NOT WORKING FOR NOW

// TODO adding this to the public api so addons can use it

public final class EntityMassOnSublevelEvent {
    private final Entity entity;
    private final ServerSubLevel subLevel;
    private final double timeStep;

    public EntityMassOnSublevelEvent(final Entity entity, final ServerSubLevel subLevel, final double timeStep) {
        this.entity = entity;
        this.subLevel = subLevel;
        this.timeStep = timeStep;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public ServerSubLevel getSubLevel() {
        return this.subLevel;
    }

    public double getTimeStep() {
        return this.timeStep;
    }
}
