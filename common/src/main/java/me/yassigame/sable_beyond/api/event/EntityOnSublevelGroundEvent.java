package me.yassigame.sable_beyond.api.event;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.Entity;

public final class EntityOnSublevelGroundEvent {
    private final Entity entity;
    private final ServerSubLevel subLevel;
    private final double timeStep;

    public EntityOnSublevelGroundEvent(final Entity entity, final ServerSubLevel subLevel, final double timeStep) {
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
