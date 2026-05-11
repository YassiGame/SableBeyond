package me.yassigame.sable_beyond.mass;

import me.yassigame.sable_beyond.api.mass.MassRegistry;
import me.yassigame.sable_beyond.api.mass.MassSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MassRegistryTest {
    @Test
    void exposesDefaultMassConfigValues() {
        assertEquals("mass", MassRegistry.getNbtKey());
        assertEquals(MassRegistry.DEFAULT_BASE_MASS, MassRegistry.getBaseMass());
        assertEquals(MassRegistry.DEFAULT_VOLUME_MULTIPLIER, MassRegistry.getVolumeMultiplier());
    }

    @Test
    void massResolutionKeepsMassAndSourceTogether() {
        final MassRegistry.MassResolution resolution = new MassRegistry.MassResolution(12.5, MassSource.AUTO);

        assertEquals(12.5, resolution.mass());
        assertEquals(MassSource.AUTO, resolution.source());
    }
}
