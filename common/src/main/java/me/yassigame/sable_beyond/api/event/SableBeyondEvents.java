package me.yassigame.sable_beyond.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SableBeyondEvents {
    private static final List<EntityOnSublevelGroundListener> ENTITY_ON_SUBLEVEL_GROUND_LISTENERS = new CopyOnWriteArrayList<>();
    //private static final List<EntityMassOnSublevelListener> ENTITY_MASS_ON_SUBLEVEL_LISTENERS = new CopyOnWriteArrayList<>();

    private SableBeyondEvents() {
    }

    public static void registerEntityOnSublevelGround(final EntityOnSublevelGroundListener listener) {
        ENTITY_ON_SUBLEVEL_GROUND_LISTENERS.add(listener);
    }

    public static void fireEntityOnSublevelGround(final EntityOnSublevelGroundEvent event) {
        for (final EntityOnSublevelGroundListener listener : ENTITY_ON_SUBLEVEL_GROUND_LISTENERS) {
            listener.onEntityOnSublevelGround(event);
        }
    }

    //public static void registerEntityMassOnSublevel(final EntityMassOnSublevelListener listener) {
    //    ENTITY_MASS_ON_SUBLEVEL_LISTENERS.add(listener);
    //}

    //public static void fireEntityMassOnSublevel(final EntityOnSublevelGroundEvent event) {
    //    for (final EntityMassOnSublevelListener listener : ENTITY_MASS_ON_SUBLEVEL_LISTENERS) {
    //        listener.onEntityMassOnSublevel(event);
    //    }
    //}

    @FunctionalInterface
    public interface EntityOnSublevelGroundListener {
        void onEntityOnSublevelGround(EntityOnSublevelGroundEvent event);
    }

    //@FunctionalInterface
    //public interface EntityMassOnSublevelListener {
    //    void onEntityMassOnSublevel(EntityOnSublevelGroundEvent event);
    //}
}
