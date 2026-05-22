package me.yassigame.sable_beyond.api.mass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//FIXME Better test
class EntityMassTest {
    @Test
    void exposesDefaultMassConfigValues() {
        assertEquals("mass", EntityMass.getNbtKey());
        assertEquals(EntityMass.DEFAULT_BASE_MASS, EntityMass.getBaseMass());
        assertEquals(EntityMass.DEFAULT_VOLUME_MULTIPLIER, EntityMass.getVolumeMultiplier());
    }

    @Test
    void massResolutionKeepsMassAndSourceTogether() {
        final EntityMass.MassResolution resolution = new EntityMass.MassResolution(12.5, MassSource.AUTO);

        assertEquals(12.5, resolution.mass());
        assertEquals(MassSource.AUTO, resolution.source());
    }
}
